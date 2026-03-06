# Coding Conventions

**Analysis Date:** 2026-03-06

## Naming Patterns

### Backend (Java)

**Packages:**
- Domain-first layout: `scit.ainiinu.{domain}.{layer}`
- Domains: `member`, `pet`, `walk`, `chat`, `lostpet`, `community`
- Layers within each domain: `controller/`, `service/`, `repository/`, `entity/`, `dto/`, `exception/`
- DTOs split into sub-packages: `dto/request/`, `dto/response/`
- Cross-cutting package: `scit.ainiinu.common.{concern}` (e.g., `common/response/`, `common/exception/`, `common/security/`, `common/entity/`)
- The `lostpet` domain uses `domain/` instead of `entity/`, `error/` instead of `exception/`, and `integration/` for external service clients

**Classes:**
- Entities: PascalCase singular nouns (`WalkDiary`, `LostPetReport`, `ChatRoom`)
- Controllers: `{Entity}Controller` (`WalkDiaryController`, `PostController`, `LostPetController`)
- Services: `{Entity}Service` for concrete classes, or `{Entity}Service` interface + `{Entity}ServiceImpl` when interface is used (`WalkDiaryService` vs. `LostPetService` / `LostPetServiceImpl`)
- Repositories: `{Entity}Repository` (`WalkDiaryRepository`, `LostPetReportRepository`)
- DTOs (request): `{Entity}{Action}Request` (`WalkDiaryCreateRequest`, `WalkDiaryPatchRequest`, `LostPetCreateRequest`)
- DTOs (response): `{Entity}Response` or `{Entity}{Detail|Summary}Response` (`WalkDiaryResponse`, `LostPetDetailResponse`, `LostPetSummaryResponse`)
- Error code enums: `{Domain}ErrorCode` (`WalkDiaryErrorCode`, `LostPetErrorCode`, `MemberErrorCode`, `CommunityErrorCode`)
- Domain exceptions: `{Domain}Exception` extending `BusinessException` (`LostPetException`, `MemberException`, `ChatException`)

**Methods:**
- Controller methods: CRUD verbs (`create`, `list`, `detail`, `update`, `delete`) or action names (`analyze`, `approveMatch`)
- Service methods: same naming as controller counterparts
- Entity factory methods: `static {Entity} create(...)` (never use public constructors directly)
- Entity mutation methods: `update(...)`, `softDelete(...)`, domain-specific verbs
- DTO conversion: `static {Response} from({Entity}, ...)` on response DTOs
- Repository derived queries: Spring Data conventions (`findByMemberIdAndDeletedAtIsNull`)

**Variables:**
- camelCase throughout
- `memberId` for authenticated user ID (injected via `@CurrentMember Long memberId`)
- `request` for incoming DTOs, `response` for outgoing DTOs

### Frontend (TypeScript)

**Files:**
- Pages: `page.tsx` inside App Router directories (`src/app/{route}/page.tsx`)
- Components: PascalCase (`FeedItem.tsx`, `Button.tsx`, `StoryArea.tsx`)
- Hooks: `use{Name}.ts` (`useWalkDiaries.ts`, `useFollowToggle.ts`, `useMemberProfile.ts`)
- Form hooks: `src/hooks/forms/use{Name}Form.ts` (`useDiaryForm.ts`, `useDogForm.ts`)
- Stores: `use{Name}Store.ts` (`useUserStore.ts`, `useConfigStore.ts`)
- Services: `{domain}Service.ts` (`memberService.ts`, `threadService.ts`, `postService.ts`)
- Types: centralized in `src/types/index.ts`

**Components:**
- Named exports for reusable components: `export const Button = ...`
- Default exports for pages: `export default function FeedPage()`
- Props interfaces defined above component: `interface FeedItemProps { ... }`

**Types:**
- Type suffix `Type` for domain models: `UserType`, `DogType`, `ThreadType`, `FeedPostType`, `WalkDiaryType`
- Union literal types for enums: `type DogTendency = '...' | '...'`
- Utility types for form data: `type DogFormData = Omit<DogType, 'id' | 'age' | ...>`

## Code Style

**Backend Formatting:**
- No explicit formatter config detected (no Checkstyle, Spotless, or Google Java Format plugin)
- 4-space indentation used consistently
- Opening brace on same line
- Lombok annotations reduce boilerplate: `@Getter`, `@RequiredArgsConstructor`, `@Builder`, `@NoArgsConstructor`

**Backend Linting:**
- No static analysis tool configured (no SpotBugs, PMD, or SonarQube)

**Frontend Formatting:**
- No Prettier config detected
- Tailwind CSS 4 for styling

**Frontend Linting:**
- ESLint 9 with `eslint-config-next` (`core-web-vitals` + `typescript` presets)
- Config at `aini-inu-frontend/eslint.config.mjs`
- TypeScript strict mode enabled in `aini-inu-frontend/tsconfig.json`

## Import Organization

### Backend

**Order (observed pattern):**
1. Jakarta/Spring framework imports
2. Third-party imports (swagger, lombok, etc.)
3. Project imports (`scit.ainiinu.*`)

**No wildcard imports.** All imports are explicit.

### Frontend

**Order:**
1. `'use client'` directive (required for all pages/components using hooks)
2. React/Next.js imports
3. Third-party libraries (`lucide-react`, `sonner`, `clsx`, etc.)
4. Project imports using `@/*` path alias
5. Type imports from `@/types`

**Path Alias:**
- `@/*` maps to `./src/*` (configured in `aini-inu-frontend/tsconfig.json`)

## Error Handling

### Backend

**Pattern:** Domain-specific error codes + centralized exception handler

**Error Code Interface:**
- `scit.ainiinu.common.exception.ErrorCode` (interface with `getHttpStatus()`, `getCode()`, `getMessage()`)
- Each domain implements as enum: `WalkDiaryErrorCode`, `LostPetErrorCode`, `MemberErrorCode`, `CommunityErrorCode`, `ChatErrorCode`, `CommonErrorCode`

**Error Code Naming Conventions (two patterns exist):**
1. Older domains use prefix + number: `M001`, `CO001`, `C001` (member, community, common)
2. Newer domains use prefix + HTTP status + description: `WD400_INVALID_REQUEST`, `L409_DUPLICATE_ACTIVE_REPORT` (walk diary, lost pet)
3. Use the newer HTTP-status-prefixed pattern for new code

**Exception Hierarchy:**
- `BusinessException` (base, in `scit.ainiinu.common.exception`)
- Domain-specific subclasses: `LostPetException`, `MemberException`, `ChatException`, `WalkDiaryException`
- Some domains throw `BusinessException` directly (e.g., walk service), others use domain-specific subclass

**Global Exception Handler at `scit.ainiinu.common.exception.GlobalExceptionHandler`:**
- `BusinessException` -> wrapped in `ApiResponse.error(errorCode)` with appropriate HTTP status
- `MemberException` -> same handling (separate handler exists but same logic)
- `MethodArgumentNotValidException` -> field error map in `ApiResponse.error(status, code, message, data)`
- `Exception` (catch-all) -> `CommonErrorCode.INTERNAL_SERVER_ERROR`

**Throwing pattern:**
```java
throw new BusinessException(WalkDiaryErrorCode.DIARY_NOT_FOUND);
// or domain-specific:
throw new LostPetException(LostPetErrorCode.L404_NOT_FOUND);
```

### Frontend

**Pattern:** try/catch with console.error + toast notifications

```typescript
try {
  const data = await someService.action();
  // handle success
} catch (error) {
  console.error('Failed to do X:', error);
  toast.error('User-facing error message');
}
```

- `apiClient` (`src/services/api/apiClient.ts`) unwraps `ApiResponse.data` and throws on `!response.ok || !result.success`
- 8-second timeout via `AbortController`
- No centralized error boundary detected

## API Response Envelope

**All backend endpoints return `ApiResponse<T>`:**
```json
{
  "success": true,
  "status": 200,
  "data": { ... },
  "errorCode": null,
  "message": null
}
```

**For paginated lists, use `SliceResponse<T>` inside `ApiResponse`:**
```java
return ResponseEntity.ok(ApiResponse.success(SliceResponse.of(slice)));
```

**`SliceResponse` fields:** `content`, `pageNumber`, `pageSize`, `first`, `last`, `hasNext`

**Frontend `apiClient` automatically unwraps `.data`** -- callers receive the inner `T` directly.

## Logging

**Backend Framework:** SLF4J via Lombok `@Slf4j`

**Patterns:**
- Business exceptions: `log.warn("Business Exception: code={}, message={}", ...)`
- Validation exceptions: `log.warn("Validation Exception: {}", errors)`
- Unexpected exceptions: `log.error("Unexpected Exception", e)` (with stack trace)

**Frontend:** `console.error('[API Error] ...', error)` in apiClient, `console.error('Failed to ...', error)` in hooks/stores

## Comments

**Backend:**
- Javadoc used on custom annotations (`@CurrentMember`) with usage examples
- Korean comments common in Korean-language contexts (UI labels, business rules)
- `@Schema(description = "...")` annotations serve as inline documentation for DTOs and API endpoints
- `@Operation(summary = "...", description = "...")` on every controller method

**Frontend:**
- Sparse inline comments, primarily in Korean
- Type definition file (`src/types/index.ts`) has field-level comments explaining purpose
- Runtime extension fields documented with comments: `// 런타임 확장 필드`

## Entity Design

**Base Entity:**
- All entities extend `BaseTimeEntity` (`scit.ainiinu.common.entity.BaseTimeEntity`) providing `createdAt` and `updatedAt` via JPA auditing
- Protected no-arg constructor via `@NoArgsConstructor(access = AccessLevel.PROTECTED)`
- Static factory method `create(...)` for construction
- Private constructor with validation logic

**Soft Delete:**
- Entities with `deletedAt` column implement soft delete via `softDelete(LocalDateTime)` method
- Repository queries filter with `deletedAt is null`

**ID Strategy:**
- `@GeneratedValue(strategy = GenerationType.IDENTITY)` (database auto-increment)

**Optimistic Locking:**
- `@Version private Long version;` used on `WalkDiary` entity

## DTO Design

**Two patterns coexist:**
1. **Class-based DTOs** (older/walk domain): `@Getter @Setter @NoArgsConstructor` with `@Valid` annotations on request DTOs, `@Builder` on response DTOs
2. **Record-based DTOs** (lostpet domain): Java records with `@Builder` for responses (`LostPetResponse`, `LostPetSummaryResponse`)

**Use records for new response DTOs.** Use class-based for request DTOs (Jakarta Validation requires mutable fields with setters).

**All DTO fields annotated with `@Schema(description = "...", example = "...")`** for OpenAPI documentation.

## Controller Design

**Patterns to follow:**
- `@RestController` + `@RequiredArgsConstructor` + `@RequestMapping("/api/v1/{resource}")` on class
- `@Tag(name = "...", description = "...")` for OpenAPI grouping
- `@SecurityRequirement(name = "bearerAuth")` on class for authenticated endpoints
- `@CurrentMember Long memberId` as first parameter for authenticated endpoints
- `@Valid @RequestBody` for request DTOs
- Return `ResponseEntity<ApiResponse<T>>` wrapping `ApiResponse.success(data)`
- `@Operation(summary = "...", description = "...")` on every method
- Pageable parameters documented with `@Parameters` / `@Parameter` annotations

## Service Design

**Class-level annotations:**
- `@Service` + `@RequiredArgsConstructor`
- `@Transactional(readOnly = true)` on class for read-heavy services
- `@Transactional` on individual write methods

**Dependency injection:** Constructor injection via `@RequiredArgsConstructor` (final fields)

**Interface vs. concrete:**
- Most domains use concrete service classes directly
- `lostpet` domain uses interface + impl pattern for some services

## Frontend Component Design

**Pages (`src/app/*/page.tsx`):**
- All pages use `'use client'` directive
- Default exports: `export default function {Name}Page()`
- State via `useState` hooks + Zustand stores
- Data fetching in `useEffect` or custom hooks

**Components (`src/components/`):**
- Named exports with `React.FC<Props>` typing or `React.forwardRef`
- `React.memo()` for performance-critical list items (e.g., `FeedItem`)
- Styling via Tailwind CSS classes with `cn()` utility from `src/lib/utils.ts` (clsx + tailwind-merge)

**Stores (`src/store/`):**
- Zustand with typed state interfaces
- Pattern: `create<StateInterface>((set, get) => ({ ... }))`
- Persistent stores use `persist` middleware: `create<State>()(persist((set) => ({ ... }), { name: 'key' }))`

**Custom Hooks (`src/hooks/`):**
- Return object with state + actions: `{ data, isLoading, fetchData, mutateData }`
- Error handling with try/catch + toast notifications

**API Services (`src/services/api/`):**
- Object literal pattern: `export const memberService = { getMe: () => apiClient.get<T>(...), ... }`
- All methods delegate to `apiClient` which handles the `ApiResponse` envelope

## Validation

**Backend:**
- Jakarta Bean Validation on request DTOs: `@NotBlank`, `@NotNull`, `@Size`
- Entity-level validation in factory methods and `update()` methods
- `MethodArgumentNotValidException` caught by `GlobalExceptionHandler`

**Frontend:**
- Form validation in custom hooks (`src/hooks/forms/`)
- No schema validation library (no Zod, Yup)

---

*Convention analysis: 2026-03-06*
