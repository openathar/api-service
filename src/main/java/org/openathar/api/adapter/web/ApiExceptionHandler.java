package org.openathar.api.adapter.web;

import org.openathar.api.adapter.web.dto.ErrorResponse;
import org.springframework.core.log.LogAccessor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Translates every error into one consistent {@link ErrorResponse} shape
 * across all endpoints, so API clients only ever need to handle one error
 * envelope: {@code {"error": "..."}}.
 *
 * Spring's own error rendering ({@code {timestamp, status, error, path}})
 * would otherwise answer unknown paths, wrong methods, wrong content types,
 * unreadable bodies and unexpected exceptions — a second shape clients would
 * have to special-case. Only CORS rejections stay outside this handler: they
 * are written by the CORS processor before any controller runs, and browsers
 * do not expose their body to scripts anyway.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    /** Spring's logging facade — the architecture rules keep this adapter free of direct SLF4J imports. */
    private static final LogAccessor log = new LogAccessor(ApiExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException e) {
        return error(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameter(MissingServletRequestParameterException e) {
        return error(HttpStatus.BAD_REQUEST, "missing required parameter: " + e.getParameterName());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return error(HttpStatus.BAD_REQUEST, "invalid value for parameter '" + e.getName() + "': " + e.getValue());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableBody(HttpMessageNotReadableException e) {
        return error(HttpStatus.BAD_REQUEST, "malformed JSON body");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NoResourceFoundException e) {
        return error(HttpStatus.NOT_FOUND, "not found: /" + e.getResourcePath());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotAllowed(HttpRequestMethodNotSupportedException e) {
        ResponseEntity.BodyBuilder builder = ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED);
        if (e.getSupportedHttpMethods() != null) {
            builder.allow(e.getSupportedHttpMethods().toArray(org.springframework.http.HttpMethod[]::new));
        }
        return builder.body(new ErrorResponse("method " + e.getMethod() + " not allowed"));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException e) {
        MediaType type = e.getContentType();
        // Type/subtype only — echoing parameters (charset, boundary) adds noise, not help.
        String given = type == null ? "none" : type.getType() + "/" + type.getSubtype();
        return error(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "unsupported content type: " + given + " (use application/json)");
    }

    /**
     * Last line of defence. The message goes to the log, never to the
     * client — an exception text can carry internals (class names, SQL,
     * hostnames) that have no business leaving the server.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {
        log.error(e, "Unhandled exception");
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "internal error");
    }

    private static ResponseEntity<ErrorResponse> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(new ErrorResponse(message));
    }
}
