package br.com.acme.eligibility.presentation.exception;

import br.com.acme.eligibility.domain.exception.DomainValidationException;
import br.com.acme.eligibility.infrastructure.dynamo.CnpjPermissionUnavailableException;
import br.com.acme.eligibility.infrastructure.toggle.ToggleUnavailableException;
import br.com.acme.eligibility.presentation.api.dto.ErrorResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Traduz excecoes em respostas de erro sem vazar detalhes internos. */
@Slf4j
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final String GENERIC_MESSAGE = "unexpected error, try again later";

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDto> handleInvalidPayload(MethodArgumentNotValidException exception) {
        String detail = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("invalid payload");
        return ResponseEntity.badRequest()
                .body(new ErrorResponseDto(ErrorCode.INVALID_REQUEST.name(), detail));
    }

    @ExceptionHandler(DomainValidationException.class)
    public ResponseEntity<ErrorResponseDto> handleDomainValidation(DomainValidationException exception) {
        return ResponseEntity.badRequest()
                .body(new ErrorResponseDto(ErrorCode.INVALID_REQUEST.name(), exception.getMessage()));
    }

    @ExceptionHandler(ToggleUnavailableException.class)
    public ResponseEntity<ErrorResponseDto> handleToggleUnavailable(ToggleUnavailableException exception) {
        log.error("toggle service unavailable", exception);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ErrorResponseDto(ErrorCode.TOGGLE_UNAVAILABLE.name(),
                        "toggle service is unavailable"));
    }

    @ExceptionHandler(CnpjPermissionUnavailableException.class)
    public ResponseEntity<ErrorResponseDto> handleControlUnavailable(
            CnpjPermissionUnavailableException exception) {
        log.error("cnpj permission store unavailable", exception);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ErrorResponseDto(ErrorCode.CONTROL_UNAVAILABLE.name(),
                        "cnpj permission store is unavailable"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleUnexpected(Exception exception) {
        log.error("unexpected error", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponseDto(ErrorCode.INTERNAL_ERROR.name(), GENERIC_MESSAGE));
    }
}
