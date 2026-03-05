#!/usr/bin/env python3
"""
Generate broad contract-audit signal artifacts.

Outputs:
- <out_dir>/signals.json
- <out_dir>/signals.md

This script does not modify source files. It only emits candidate findings
that must be triaged with checklist rules.
"""

from __future__ import annotations

import argparse
import json
import re
from dataclasses import dataclass, asdict
from datetime import datetime
from pathlib import Path
from typing import Dict, List, Optional, Set, Tuple


HTTP_METHODS = {"get", "post", "put", "patch", "delete"}


@dataclass
class Signal:
    code: str
    category: str
    severity: str
    status: str
    title: str
    summary: str
    evidence: List[str]


def read_text(path: Path) -> str:
    try:
        return path.read_text(encoding="utf-8")
    except UnicodeDecodeError:
        return path.read_text(encoding="utf-8", errors="ignore")


def rel(root: Path, path: Path) -> str:
    try:
        return str(path.relative_to(root))
    except ValueError:
        return str(path)


def normalize_path(path: str) -> str:
    if not path:
        return "/"
    if not path.startswith("/"):
        path = "/" + path
    return re.sub(r"/+", "/", path)


def join_paths(base: str, sub: str) -> str:
    base_n = normalize_path(base)
    if base_n == "/":
        base_n = ""
    sub_n = normalize_path(sub)
    if sub_n == "/":
        sub_n = ""
    return normalize_path(f"{base_n}/{sub_n}")


def first_quoted(text: str) -> str:
    m = re.search(r'"([^"]+)"', text)
    return m.group(1) if m else ""


def load_openapi_paths(openapi_path: Path) -> Set[Tuple[str, str]]:
    if not openapi_path.exists():
        return set()
    try:
        data = json.loads(read_text(openapi_path))
    except json.JSONDecodeError:
        return set()
    out: Set[Tuple[str, str]] = set()
    for path, methods in data.get("paths", {}).items():
        if not isinstance(methods, dict):
            continue
        for method in methods.keys():
            method_l = str(method).lower()
            if method_l in HTTP_METHODS and path.startswith("/api/"):
                out.add((method_l, normalize_path(path)))
    return out


def parse_controller_endpoints(root: Path, backend_main: Path) -> List[Dict[str, str]]:
    endpoints: List[Dict[str, str]] = []
    for controller in sorted(backend_main.rglob("*Controller.java")):
        txt = read_text(controller)
        lines = txt.splitlines()
        class_base = ""
        for i, line in enumerate(lines, 1):
            if "@RequestMapping(" in line:
                quoted = first_quoted(line)
                if quoted:
                    class_base = quoted
                    break
        for i, line in enumerate(lines, 1):
            m = re.search(r"@(Get|Post|Put|Patch|Delete)Mapping(?:\(([^)]*)\))?", line)
            if not m:
                continue
            method = m.group(1).lower()
            args = m.group(2) or ""
            sub = first_quoted(args)
            full_path = join_paths(class_base, sub)

            method_name = ""
            for j in range(i, min(i + 15, len(lines))):
                m2 = re.search(r"\bpublic\s+[^\(]*\s+([A-Za-z0-9_]+)\s*\(", lines[j])
                if m2:
                    method_name = m2.group(1)
                    break

            endpoints.append(
                {
                    "method": method,
                    "path": full_path,
                    "file": rel(root, controller),
                    "line": str(i),
                    "methodName": method_name,
                }
            )
    return endpoints


def parse_test_http_calls(root: Path, backend_test: Path) -> Dict[Tuple[str, str], List[str]]:
    usage: Dict[Tuple[str, str], List[str]] = {}
    for test in sorted(backend_test.rglob("*.java")):
        lines = read_text(test).splitlines()
        for i, line in enumerate(lines, 1):
            for m in re.finditer(r"\b(get|post|put|patch|delete)\(\"([^\"]+)\"", line):
                key = (m.group(1).lower(), normalize_path(m.group(2)))
                usage.setdefault(key, []).append(f"{rel(root, test)}:{i}")
    return usage


def parse_service_public_methods(root: Path, backend_main: Path) -> List[Dict[str, str]]:
    methods: List[Dict[str, str]] = []
    for svc in sorted(backend_main.rglob("*Service.java")):
        txt = read_text(svc)
        if " class " not in txt:
            continue
        cls = svc.stem
        lines = txt.splitlines()
        for i, line in enumerate(lines, 1):
            m = re.search(r"\bpublic\s+[^\(=;]*\s+([A-Za-z0-9_]+)\s*\(", line)
            if not m:
                continue
            name = m.group(1)
            if name == cls:
                continue
            methods.append(
                {
                    "class": cls,
                    "method": name,
                    "file": rel(root, svc),
                    "line": str(i),
                }
            )
    return methods


def collect_test_files(backend_test: Path) -> List[Path]:
    files = []
    for p in backend_test.rglob("*.java"):
        if re.search(r"(ServiceTest|CoverageTest|UnitTest)\.java$", p.name):
            files.append(p)
    return sorted(files)


def method_has_test_reference(method: str, test_files: List[Path]) -> Optional[str]:
    pattern = re.compile(rf"\b{re.escape(method)}\b")
    for tf in test_files:
        txt = read_text(tf)
        if pattern.search(txt):
            return str(tf)
    return None


def parse_prd_api_refs(prd_path: Path) -> Set[str]:
    if not prd_path.exists():
        return set()
    refs: Set[str] = set()
    txt = read_text(prd_path)
    for m in re.finditer(r"`([A-Z][A-Z0-9-]+)`", txt):
        token = m.group(1)
        if "-" in token:
            refs.add(token)
    return refs


def has_member_password_seed(seed_file: Path) -> bool:
    if not seed_file.exists():
        return False
    txt = read_text(seed_file)
    inserts = re.finditer(r"INSERT\s+INTO\s+member\s*\((.*?)\)\s*VALUES", txt, flags=re.IGNORECASE | re.DOTALL)
    for ins in inserts:
        cols = ins.group(1).lower()
        if "password" in cols:
            return True
    return False


def build_signals(root: Path, out_dir: Path) -> Dict[str, object]:
    backend_main = root / "aini-inu-backend" / "src" / "main" / "java"
    backend_test = root / "aini-inu-backend" / "src" / "test" / "java"
    prd_path = root / "common-docs" / "PROJECT_PRD.md"

    openapi_artifact = out_dir / "openapi_pretty.json"
    openapi_snapshot = root / "common-docs" / "openapi" / "openapi.v1.json"
    openapi_path = openapi_artifact if openapi_artifact.exists() else openapi_snapshot

    signals: List[Signal] = []

    openapi_endpoints = load_openapi_paths(openapi_path)
    controller_endpoints = parse_controller_endpoints(root, backend_main)
    controller_index = {(e["method"], e["path"]): e for e in controller_endpoints}
    test_http_usage = parse_test_http_calls(root, backend_test)
    service_methods = parse_service_public_methods(root, backend_main)
    service_test_files = collect_test_files(backend_test)
    prd_refs = parse_prd_api_refs(prd_path)

    # Endpoint in controller but missing in OpenAPI.
    for ep in controller_endpoints:
        key = (ep["method"], ep["path"])
        if ep["path"].startswith("/api/") and key not in openapi_endpoints:
            signals.append(
                Signal(
                    code="ENDPOINT_MISSING_IN_OPENAPI",
                    category="계약상 불일치",
                    severity="High",
                    status="의심",
                    title=f"{ep['method'].upper()} {ep['path']} is not present in OpenAPI",
                    summary="Controller endpoint exists but runtime/snapshot OpenAPI did not include it.",
                    evidence=[
                        f"{ep['file']}:{ep['line']}",
                        str(openapi_path),
                    ],
                )
            )

    # Endpoint in OpenAPI but cannot find controller mapping.
    for method, path in sorted(openapi_endpoints):
        if (method, path) not in controller_index:
            signals.append(
                Signal(
                    code="OPENAPI_ENDPOINT_WITHOUT_CONTROLLER_MAPPING",
                    category="계약상 불일치",
                    severity="High",
                    status="의심",
                    title=f"{method.upper()} {path} has no matching controller mapping",
                    summary="OpenAPI endpoint could not be matched to parsed controller annotations.",
                    evidence=[
                        str(openapi_path),
                    ],
                )
            )

    # Controller endpoint without direct HTTP slice test evidence.
    for ep in controller_endpoints:
        key = (ep["method"], ep["path"])
        if ep["path"].startswith("/api/") and key not in test_http_usage:
            signals.append(
                Signal(
                    code="CONTROLLER_ENDPOINT_TEST_MISSING",
                    category="미구현",
                    severity="Medium",
                    status="의심",
                    title=f"Missing direct endpoint test for {ep['method'].upper()} {ep['path']}",
                    summary="No MockMvc/WebMvcTest style path evidence was detected for this endpoint.",
                    evidence=[
                        f"{ep['file']}:{ep['line']}",
                    ],
                )
            )

    # Public service methods without test references.
    for sm in service_methods:
        test_ref = method_has_test_reference(sm["method"], service_test_files)
        if test_ref is None:
            signals.append(
                Signal(
                    code="SERVICE_METHOD_TEST_MISSING",
                    category="미구현",
                    severity="Medium",
                    status="의심",
                    title=f"Possible missing service test: {sm['class']}.{sm['method']}",
                    summary="Public service method name was not found in Service/Coverage/Unit test files.",
                    evidence=[
                        f"{sm['file']}:{sm['line']}",
                    ],
                )
            )

    # Auth + seed consistency check.
    auth_service = backend_main / "scit" / "ainiinu" / "member" / "service" / "AuthService.java"
    member_entity = backend_main / "scit" / "ainiinu" / "member" / "entity" / "Member.java"
    seed1 = root / "aini-inu-backend" / "src" / "main" / "resources" / "db" / "seed" / "10_core_sample_seed.sql"
    seed2 = root / "aini-inu-backend" / "src" / "main" / "resources" / "db" / "seed" / "20_status_edge_seed.sql"
    if auth_service.exists() and member_entity.exists():
        auth_txt = read_text(auth_service)
        member_txt = read_text(member_entity)
        if "getPassword()" in auth_txt and "private String password;" in member_txt:
            if not has_member_password_seed(seed1) or not has_member_password_seed(seed2):
                signals.append(
                    Signal(
                        code="AUTH_SEED_PASSWORD_INCONSISTENCY",
                        category="논리적 어긋남",
                        severity="Medium",
                        status="확정",
                        title="Auth password validation and seed member data are inconsistent",
                        summary="Login requires member.password but seed INSERT columns for member do not include password.",
                        evidence=[
                            rel(root, auth_service),
                            rel(root, member_entity),
                            rel(root, seed1),
                            rel(root, seed2),
                        ],
                    )
                )

    payload = {
        "generatedAt": datetime.now().isoformat(),
        "root": str(root),
        "openapiSource": str(openapi_path),
        "summary": {
            "signalCount": len(signals),
            "categories": _count_by(signals, key=lambda s: s.category),
            "severities": _count_by(signals, key=lambda s: s.severity),
            "statuses": _count_by(signals, key=lambda s: s.status),
            "prdApiRefCount": len(prd_refs),
            "openapiEndpointCount": len(openapi_endpoints),
            "controllerEndpointCount": len(controller_endpoints),
            "servicePublicMethodCount": len(service_methods),
        },
        "signals": [asdict(s) for s in signals],
    }
    return payload


def _count_by(signals: List[Signal], key) -> Dict[str, int]:
    out: Dict[str, int] = {}
    for s in signals:
        k = key(s)
        out[k] = out.get(k, 0) + 1
    return dict(sorted(out.items()))


def write_markdown(path: Path, payload: Dict[str, object]) -> None:
    summary = payload["summary"]
    signals = payload["signals"]
    lines: List[str] = []
    lines.append("# Audit Signals")
    lines.append("")
    lines.append(f"- generatedAt: `{payload['generatedAt']}`")
    lines.append(f"- openapiSource: `{payload['openapiSource']}`")
    lines.append(f"- signalCount: **{summary['signalCount']}**")
    lines.append("")
    lines.append("## Summary")
    lines.append("")
    lines.append(f"- categories: `{summary['categories']}`")
    lines.append(f"- severities: `{summary['severities']}`")
    lines.append(f"- statuses: `{summary['statuses']}`")
    lines.append(f"- prdApiRefCount: `{summary['prdApiRefCount']}`")
    lines.append(f"- openapiEndpointCount: `{summary['openapiEndpointCount']}`")
    lines.append(f"- controllerEndpointCount: `{summary['controllerEndpointCount']}`")
    lines.append(f"- servicePublicMethodCount: `{summary['servicePublicMethodCount']}`")
    lines.append("")
    lines.append("## Signals")
    lines.append("")
    lines.append("| code | category | severity | status | title |")
    lines.append("|---|---|---|---|---|")
    for s in signals:
        title = str(s["title"]).replace("|", "\\|")
        lines.append(
            f"| `{s['code']}` | {s['category']} | {s['severity']} | {s['status']} | {title} |"
        )
    lines.append("")
    lines.append("## Evidence")
    lines.append("")
    for idx, s in enumerate(signals, 1):
        lines.append(f"### {idx}. {s['title']}")
        lines.append(f"- code: `{s['code']}`")
        lines.append(f"- category: `{s['category']}`")
        lines.append(f"- severity: `{s['severity']}`")
        lines.append(f"- status: `{s['status']}`")
        lines.append(f"- summary: {s['summary']}")
        lines.append("- evidence:")
        for ev in s["evidence"]:
            lines.append(f"  - `{ev}`")
        lines.append("")
    path.write_text("\n".join(lines), encoding="utf-8")


def main() -> None:
    parser = argparse.ArgumentParser(description="Generate broad contract-audit signals.")
    parser.add_argument("--root", default=".", help="repository root")
    parser.add_argument("--out-dir", default="/tmp/aini_contract_audit", help="artifact output dir")
    args = parser.parse_args()

    root = Path(args.root).resolve()
    out_dir = Path(args.out_dir).resolve()
    out_dir.mkdir(parents=True, exist_ok=True)

    payload = build_signals(root, out_dir)
    signals_json = out_dir / "signals.json"
    signals_md = out_dir / "signals.md"

    signals_json.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")
    write_markdown(signals_md, payload)

    print(f"[audit-signal] generated {payload['summary']['signalCount']} signals")
    print(f"[audit-signal] json: {signals_json}")
    print(f"[audit-signal] markdown: {signals_md}")


if __name__ == "__main__":
    main()
