package com.example.demo_pam.exception;

import com.example.demo_pam.dto.response.ApiError;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.authorization.ExpressionAuthorizationDecision;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Centraliza el manejo de errores de todos los @RestController con el mismo esquema ApiError, sin filtrar mensajes internos
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(HttpServletRequest request, ResourceNotFoundException ex) {
        return error(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage(), request, null, null);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiError> handleEntityNotFound(HttpServletRequest request, EntityNotFoundException ex) {
        return error(HttpStatus.NOT_FOUND, "Not Found", "El recurso solicitado no existe", request, null, null);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleNoResource(HttpServletRequest request, NoResourceFoundException ex) {
        return error(HttpStatus.NOT_FOUND, "Not Found", "La ruta solicitada no existe", request, null, null);
    }

    @ExceptionHandler(BusinessRuleViolationException.class)
    public ResponseEntity<ApiError> handleBusinessRule(HttpServletRequest request, BusinessRuleViolationException ex) {
        return error(HttpStatus.CONFLICT, "Conflict", ex.getMessage(), request, null, null);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiError> handleConflict(HttpServletRequest request, ConflictException ex) {
        return error(HttpStatus.CONFLICT, "Conflict", ex.getMessage(), request, null, null);
    }

    @ExceptionHandler(InvalidOrderException.class)
    public ResponseEntity<ApiError> handleInvalidOrder(HttpServletRequest request, InvalidOrderException ex) {
        return error(HttpStatus.CONFLICT, "Conflict", ex.getMessage(), request, null, null);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrity(HttpServletRequest request, DataIntegrityViolationException ex) {
        return error(HttpStatus.CONFLICT, "Conflict",
                "La operación entra en conflicto con datos existentes (integridad de datos)", request, null, null);
    }

    // El @Version detectó que otra operación (p. ej. el callback del banco) ya asentó el pago
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ApiError> handleOptimisticLock(HttpServletRequest request, ObjectOptimisticLockingFailureException ex) {
        return error(HttpStatus.CONFLICT, "Conflict", "El registro fue modificado por otra operación, reintentá", request, null, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(HttpServletRequest request, MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(f -> fieldErrors.put(f.getField(), f.getDefaultMessage()));
        return error(HttpStatus.BAD_REQUEST, "Bad Request",
                "Los campos enviados no cumplen las reglas de validación", request, null, fieldErrors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(HttpServletRequest request, ConstraintViolationException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getConstraintViolations().forEach(v -> fieldErrors.put(v.getPropertyPath().toString(), v.getMessage()));
        return error(HttpStatus.BAD_REQUEST, "Bad Request",
                "Los parámetros no cumplen las restricciones declaradas", request, null, fieldErrors);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiError> handleHandlerValidation(HttpServletRequest request, HandlerMethodValidationException ex) {
        return error(HttpStatus.BAD_REQUEST, "Bad Request", "Algún parámetro o header de la solicitud es inválido", request, null, null);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(HttpServletRequest request, MethodArgumentTypeMismatchException ex) {
        return error(HttpStatus.BAD_REQUEST, "Bad Request", "El parámetro '" + ex.getName() + "' tiene un valor inválido", request, null, null);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> handleMissingParam(HttpServletRequest request, MissingServletRequestParameterException ex) {
        return error(HttpStatus.BAD_REQUEST, "Bad Request", "Falta el parámetro requerido: " + ex.getParameterName(), request, null, null);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadable(HttpServletRequest request, HttpMessageNotReadableException ex) {
        return error(HttpStatus.BAD_REQUEST, "Bad Request", "El cuerpo de la solicitud es inválido o está mal formado", request, null, null);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiError> handleMaxUpload(HttpServletRequest request, MaxUploadSizeExceededException ex) {
        return error(HttpStatus.CONTENT_TOO_LARGE, "Content Too Large", "El archivo supera el tamaño máximo permitido", request, null, null);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> handleMethodNotAllowed(HttpServletRequest request, HttpRequestMethodNotSupportedException ex) {
        return error(HttpStatus.METHOD_NOT_ALLOWED, "Method Not Allowed",
                "El método " + ex.getMethod() + " no está soportado para esta ruta", request, null, null);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiError> handleMediaType(HttpServletRequest request, HttpMediaTypeNotSupportedException ex) {
        return error(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Unsupported Media Type", "El Content-Type enviado no es válido", request, null, null);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiError> handleBadCredentials(HttpServletRequest request, BadCredentialsException ex) {
        return error(HttpStatus.UNAUTHORIZED, "Unauthorized", ex.getMessage(), request, null, null);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthentication(HttpServletRequest request, AuthenticationException ex) {
        return error(HttpStatus.UNAUTHORIZED, "Unauthorized",
                "Autenticación requerida: incluí un Bearer token JWT válido en el header Authorization", request, null, null);
    }

    // Spring Security la lanza al denegar @PreAuthorize; la regla SpEL se traduce al rol/roles exigidos
    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<ApiError> handleAuthorization(HttpServletRequest request, AuthorizationDeniedException ex) {
        List<String> requiredRoles = requiredRolesFrom(ex);
        String message = requiredRoles.isEmpty()
                ? "No tenés permisos para realizar esta operación"
                : "Acceso denegado: se requiere el rol " + String.join(" o el rol ", requiredRoles);
        return error(HttpStatus.FORBIDDEN, "Forbidden", message, request, requiredRoles, null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(HttpServletRequest request, AccessDeniedException ex) {
        return error(HttpStatus.FORBIDDEN, "Forbidden", "No tenés permisos para realizar esta operación", request, null, null);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(HttpServletRequest request, IllegalArgumentException ex) {
        return error(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage(), request, null, null);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiError> handleIllegalState(HttpServletRequest request, IllegalStateException ex) {
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
                "Error interno del servidor, revisá los logs para más detalle", request, null, null);
    }

    // Un ?sort con propiedad inexistente (p. ej. sort=string) rompía la query: es entrada del cliente, nunca un 500
    @ExceptionHandler(InvalidDataAccessApiUsageException.class)
    public ResponseEntity<ApiError> handleInvalidSort(HttpServletRequest request, InvalidDataAccessApiUsageException ex) {
        return error(HttpStatus.BAD_REQUEST, "Bad Request",
                "El parámetro de orden (sort) referencia una propiedad que no existe en el recurso", request, null, null);
    }

    // Red de seguridad: un error inesperado jamás debe filtrar trazas internas al cliente
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(HttpServletRequest request, Exception ex) {
        log.error("Error no manejado en {} {}", request.getMethod(), request.getRequestURI(), ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
                "Error interno del servidor, intentá nuevamente o revisá los logs", request, null, null);
    }

    private ResponseEntity<ApiError> error(HttpStatus status, String shortName, String message,
                                           HttpServletRequest request, List<String> requiredRoles, Map<String, String> fieldErrors) {
        ApiError body = new ApiError(Instant.now(), status.value(), shortName, message,
                request.getRequestURI(), requiredRoles, fieldErrors);
        return ResponseEntity.status(status).body(body);
    }

    // Extrae los roles exigidos de la expresión SpEL del @PreAuthorize (p. ej. hasAnyAuthority('CHEF','ADMIN'))
    private List<String> requiredRolesFrom(AuthorizationDeniedException ex) {
        if (ex.getAuthorizationResult() instanceof ExpressionAuthorizationDecision decision
                && decision.getExpression() != null) {
            List<String> roles = new ArrayList<>();
            Pattern pattern = Pattern.compile("hasAnyAuthority\\(([^)]*)\\)|hasAuthority\\(([^)]*)\\)");
            Matcher matcher = pattern.matcher(decision.getExpression().getExpressionString());
            while (matcher.find()) {
                String group = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
                if (group != null) {
                    for (String role : group.split(",")) {
                        String clean = role.trim().replace("'", "");
                        if (!clean.isEmpty() && !roles.contains(clean)) {
                            roles.add(clean);
                        }
                    }
                }
            }
            return roles;
        }
        return List.of();
    }
}