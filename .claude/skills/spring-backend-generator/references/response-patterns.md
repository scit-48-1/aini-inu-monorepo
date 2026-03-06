# API Response & Error Handling Patterns

## API Response Format

### Standard Response Structure

All API responses MUST use the ApiResponse wrapper:

- All successful requests return HTTP `200 OK` and `ApiResponse.status=200` (including POST/DELETE).

```json
// Success
{
  "success": true,
  "status": 200,
  "data": {
    "id": 1,
    "name": "몽이"
  }
}

// Error
{
  "success": false,
  "status": 400,
  "data": null,
  "errorCode": "P002",
  "message": "최대 10마리까지만 등록 가능합니다"
}
```

### ApiResponse Implementation

Location: `common/response/ApiResponse.java`

```java
package com.ainiinu.common.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ApiResponse<T> {
    private boolean success;
    private int status;
    private T data;
    private String errorCode;
    private String message;

    // Factory methods
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, 200, data, null, null);
    }

    public static <T> ApiResponse<T> error(com.ainiinu.common.exception.ErrorCode errorCode) {
        return new ApiResponse<>(
                false,
                errorCode.getHttpStatus().value(),
                null,
                errorCode.getCode(),
                errorCode.getMessage()
        );
    }
}
```

### PageResponse (for pagination)

Location: `common/response/PageResponse.java`

```java
package com.ainiinu.common.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
@AllArgsConstructor
public class PageResponse<T> {
    private List<T> content;
    private int pageNumber;
    private int pageSize;
    private long totalElements;
    private int totalPages;
    private boolean first;
    private boolean last;
    
    public static <T> PageResponse<T> of(Page<T> page) {
        return new PageResponse<>(
            page.getContent(),
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages(),
            page.isFirst(),
            page.isLast()
        );
    }
}
```

### SliceResponse (for infinite scroll)

Use `SliceResponse` when you want pagination **without** total counts (no count query).  
Expose `hasNext` for infinite scroll UI.

Planned location (when implemented): `common/response/SliceResponse.java`

```java
package com.ainiinu.common.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.data.domain.Slice;

import java.util.List;

@Getter
@AllArgsConstructor
public class SliceResponse<T> {
    private List<T> content;
    private int pageNumber;
    private int pageSize;
    private boolean first;
    private boolean last;
    private boolean hasNext;

    public static <T> SliceResponse<T> of(Slice<T> slice) {
        return new SliceResponse<>(
            slice.getContent(),
            slice.getNumber(),
            slice.getSize(),
            slice.isFirst(),
            slice.isLast(),
            slice.hasNext()
        );
    }
}
```

## Error Code Architecture

### ErrorCode Interface

Location: `common/exception/ErrorCode.java`

```java
package com.ainiinu.common.exception;

import org.springframework.http.HttpStatus;

public interface ErrorCode {
    HttpStatus getHttpStatus();
    String getCode();
    String getMessage();
}
```

### Common Error Codes (Global)

Location: `common/exception/CommonErrorCode.java`

```java
package com.ainiinu.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CommonErrorCode implements ErrorCode {
    // 400 Bad Request
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "C001", "잘못된 입력값입니다"),
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "C002", "입력값 검증에 실패했습니다"),
    
    // 401 Unauthorized
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "C101", "인증이 필요합니다"),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "C102", "유효하지 않은 토큰입니다"),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "C103", "만료된 토큰입니다"),
    
    // 403 Forbidden
    FORBIDDEN(HttpStatus.FORBIDDEN, "C201", "권한이 없습니다"),
    
    // 404 Not Found
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "C301", "요청한 리소스를 찾을 수 없습니다"),
    
    // 500 Internal Server Error
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C999", "서버 내부 오류가 발생했습니다");
    
    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
```

### Domain-Specific Error Codes

Each bounded context has its own error code enum.

**Example: Member Context**

Location: `member/exception/MemberErrorCode.java`

```java
package com.ainiinu.member.exception;

import com.ainiinu.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum MemberErrorCode implements ErrorCode {
    // 4xx Client Errors
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "M001", "회원을 찾을 수 없습니다"),
    INVALID_NICKNAME(HttpStatus.BAD_REQUEST, "M002", "닉네임이 유효하지 않습니다"),
    DUPLICATE_NICKNAME(HttpStatus.CONFLICT, "M003", "이미 사용 중인 닉네임입니다"),
    INVALID_SOCIAL_TOKEN(HttpStatus.UNAUTHORIZED, "M004", "유효하지 않은 소셜 토큰입니다"),
    MEMBER_BANNED(HttpStatus.FORBIDDEN, "M005", "정지된 회원입니다"),
    
    // 5xx Server Errors
    SOCIAL_LOGIN_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "M501", "소셜 로그인 처리 중 오류가 발생했습니다");
    
    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
```

**Example: Pet Context**

Location: `pet/exception/PetErrorCode.java`

```java
package com.ainiinu.pet.exception;

import com.ainiinu.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PetErrorCode implements ErrorCode {
    PET_NOT_FOUND(HttpStatus.NOT_FOUND, "P001", "반려견을 찾을 수 없습니다"),
    MAX_PET_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "P002", "최대 10마리까지만 등록 가능합니다"),
    ALREADY_MAIN_PET_EXISTS(HttpStatus.BAD_REQUEST, "P003", "이미 메인 반려견이 존재합니다"),
    BREED_NOT_FOUND(HttpStatus.NOT_FOUND, "P004", "견종을 찾을 수 없습니다"),
    INVALID_PET_AGE(HttpStatus.BAD_REQUEST, "P005", "유효하지 않은 나이입니다"),
    NOT_PET_OWNER(HttpStatus.FORBIDDEN, "P006", "해당 반려견의 소유자가 아닙니다");
    
    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
```

**Example: Thread (Walk) Context**

Location: `walk/exception/ThreadErrorCode.java`

```java
package com.ainiinu.walk.exception;

import com.ainiinu.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ThreadErrorCode implements ErrorCode {
    THREAD_NOT_FOUND(HttpStatus.NOT_FOUND, "T001", "스레드를 찾을 수 없습니다"),
    ALREADY_HAS_ACTIVE_THREAD(HttpStatus.BAD_REQUEST, "T002", "이미 작성한 스레드가 있습니다"),
    NOT_THREAD_AUTHOR(HttpStatus.FORBIDDEN, "T003", "스레드 작성자가 아닙니다"),
    THREAD_ALREADY_CLOSED(HttpStatus.BAD_REQUEST, "T004", "이미 종료된 스레드입니다"),
    MAX_PARTICIPANTS_EXCEEDED(HttpStatus.BAD_REQUEST, "T005", "최대 참가자 수를 초과했습니다"),
    NON_PET_OWNER_NOT_ALLOWED(HttpStatus.FORBIDDEN, "T006", "비애견인은 참여할 수 없습니다"),
    FILTER_NOT_SATISFIED(HttpStatus.BAD_REQUEST, "T007", "참가 조건을 충족하지 않습니다");
    
    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
```

### Error Code Naming Convention

Format: `{CONTEXT_PREFIX}{NUMBER}`

| Context | Prefix | Range | Example |
|---------|--------|-------|---------|
| Common | C | 001-999 | C001, C999 |
| Member | M | 001-599 | M001, M501 |
| Pet | P | 001-599 | P001, P002 |
| Walk (Thread) | T | 001-599 | T001, T002 |
| Chat | CH | 001-599 | CH001, CH002 |
| Community | CO | 001-599 | CO001, CO002 |
| LostPet | L | 001-599 | L001, L002 |
| Notification | N | 001-599 | N001, N002 |

- 001-399: Client errors (4xx)
- 501-599: Server errors (5xx)

## BusinessException

Location: `common/exception/BusinessException.java`

```java
package com.ainiinu.common.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
    private final ErrorCode errorCode;
    
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
    
    public BusinessException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }
}
```

### Usage in Services

```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PetService {
    private final PetRepository petRepository;
    
    public PetResponse getPet(Long petId) {
        Pet pet = petRepository.findById(petId)
            .orElseThrow(() -> new BusinessException(PetErrorCode.PET_NOT_FOUND));
        
        return PetResponse.from(pet);
    }
    
    @Transactional
    public PetResponse registerPet(Long memberId, PetRegisterRequest request) {
        // Business rule validation
        long petCount = petRepository.countByMemberIdAndDeletedAtIsNull(memberId);
        if (petCount >= 10) {
            throw new BusinessException(PetErrorCode.MAX_PET_LIMIT_EXCEEDED);
        }
        
        // ... registration logic
    }
}
```

## GlobalExceptionHandler

Location: `common/exception/GlobalExceptionHandler.java`

```java
package com.ainiinu.common.exception;

import com.ainiinu.common.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    /**
     * Handle all BusinessException (from all domains)
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Object>> handleBusinessException(BusinessException e) {
        ErrorCode errorCode = e.getErrorCode();
        
        log.warn("Business Exception: code={}, message={}", 
                errorCode.getCode(), errorCode.getMessage());
        
        ApiResponse<Object> response = ApiResponse.error(errorCode);
        
        return ResponseEntity
            .status(errorCode.getHttpStatus())
            .body(response);
    }
    
    /**
     * Handle validation errors (@Valid failed)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationException(
            MethodArgumentNotValidException e) {
        
        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        log.warn("Validation Exception: {}", errors);
        
        ApiResponse<Map<String, String>> response = new ApiResponse<>(
            false,
            HttpStatus.BAD_REQUEST.value(),
            errors,
            CommonErrorCode.VALIDATION_FAILED.getCode(),
            CommonErrorCode.VALIDATION_FAILED.getMessage()
        );
        
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(response);
    }
    
    /**
     * Handle unexpected exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleException(Exception e) {
        log.error("Unexpected Exception", e);
        
        ApiResponse<Object> response = ApiResponse.error(
            CommonErrorCode.INTERNAL_SERVER_ERROR
        );
        
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(response);
    }
}
```

## Controller Response Pattern

### MANDATORY: Use ResponseEntity<ApiResponse<T>>

```java
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/pets")
public class PetController {
    private final PetService petService;
    
    // ✅ GET - 200 OK
    @GetMapping("/{petId}")
    public ResponseEntity<ApiResponse<PetResponse>> getPet(@PathVariable Long petId) {
        PetResponse response = petService.getPet(petId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    // ✅ POST - 200 OK
    @PostMapping
    public ResponseEntity<ApiResponse<PetResponse>> registerPet(
            @Valid @RequestBody PetRegisterRequest request) {
        PetResponse response = petService.registerPet(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    // ✅ PUT - 200 OK
    @PutMapping("/{petId}")
    public ResponseEntity<ApiResponse<PetResponse>> updatePet(
            @PathVariable Long petId,
            @Valid @RequestBody PetUpdateRequest request) {
        PetResponse response = petService.updatePet(petId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    // ✅ DELETE - 200 OK
    @DeleteMapping("/{petId}")
    public ResponseEntity<ApiResponse<Void>> deletePet(@PathVariable Long petId) {
        petService.deletePet(petId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
    
    // ✅ GET with Pagination
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<PetResponse>>> getPets(
            @RequestParam Long memberId,
            Pageable pageable) {
        Page<PetResponse> page = petService.getPets(memberId, pageable);
        PageResponse<PetResponse> response = PageResponse.of(page);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
```

### Status Code Guidelines

| Operation | Success Code | Error Codes |
|-----------|--------------|-------------|
| GET (single) | 200 OK | 404 Not Found, 403 Forbidden |
| GET (list) | 200 OK | 400 Bad Request |
| POST | 200 OK | 400 Bad Request, 409 Conflict |
| PUT | 200 OK | 400 Bad Request, 404 Not Found |
| DELETE | 200 OK | 404 Not Found, 403 Forbidden |

## Complete Flow Example

```java
// 1. Define ErrorCode
@Getter
@RequiredArgsConstructor
public enum PetErrorCode implements ErrorCode {
    PET_NOT_FOUND(HttpStatus.NOT_FOUND, "P001", "반려견을 찾을 수 없습니다");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}

// 2. Service throws BusinessException
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PetService {
    public PetResponse getPet(Long petId) {
        Pet pet = petRepository.findById(petId)
                .orElseThrow(() -> new BusinessException(PetErrorCode.PET_NOT_FOUND));
        return PetResponse.from(pet);
    }
}

// 3. Controller returns ResponseEntity<ApiResponse<T>>
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/pets")
public class PetController {
    @GetMapping("/{petId}")
    public ResponseEntity<ApiResponse<PetResponse>> getPet(@PathVariable Long petId) {
        PetResponse response = petService.getPet(petId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}

// 4. GlobalExceptionHandler catches and formats error
// Automatically converts BusinessException to proper JSON response
```

## Best Practices

1. **One ErrorCode enum per context** - Keeps errors organized
2. **Always throw BusinessException** - Never raw RuntimeException with strings
3. **Log appropriately** - WARN for business errors, ERROR for system errors
4. **Use specific HTTP status codes** - Match the semantic meaning
5. **Consistent error messages** - User-friendly Korean messages
6. **Include error codes** - For frontend to handle programmatically
7. **Validate early** - Use jakarta.validation in DTOs
8. **Don't expose stack traces** - Keep internal errors hidden from API responses
