package kz.iitu.springlab.web;

import kz.iitu.springlab.catalog.BookNotFoundException;
import kz.iitu.springlab.catalog.DuplicateTitleException;
import org.springframework.context.MessageSource;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.util.Locale;

@RestControllerAdvice(annotations = RestController.class)
@Order(-1)
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {
    private final MessageSource messages;

    public ApiExceptionHandler(MessageSource messages) {
        this.messages = messages;
    }

    @ExceptionHandler(BookNotFoundException.class)
    public ProblemDetail notFound(BookNotFoundException exception, Locale locale) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND,
                messages.getMessage("book.notFound", new Object[]{exception.getBookId()}, locale));
        problem.setType(URI.create("https://api.iitu.kz/errors/book-not-found"));
        problem.setTitle(messages.getMessage("book.notFound.title", null, locale));
        return problem;
    }

    @ExceptionHandler(DuplicateTitleException.class)
    public ProblemDetail duplicate(DuplicateTitleException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        problem.setType(URI.create("https://api.iitu.kz/errors/duplicate-title"));
        problem.setTitle("Duplicate title");
        problem.setProperty("conflictingTitle", exception.getTitle());
        return problem;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail invalidRequest(IllegalArgumentException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
        problem.setType(URI.create("https://api.iitu.kz/errors/http-400"));
        problem.setTitle("Invalid request");
        return problem;
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException exception,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        // The base class creates the standard 400; this extension identifies invalid fields.
        ResponseEntity<Object> response = super.handleMethodArgumentNotValid(exception, headers, status, request);
        if (response != null && response.getBody() instanceof ProblemDetail problem) {
            problem.setProperty("errors", exception.getBindingResult().getFieldErrors().stream()
                    .map(error -> new FieldViolation(error.getField(), error.getDefaultMessage()))
                    .toList());
        }
        return response;
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException exception,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ResponseEntity<Object> response = super.handleHttpMessageNotReadable(exception, headers, status, request);
        if (response != null && response.getBody() instanceof ProblemDetail problem) {
            problem.setDetail("The request body must be valid JSON with the expected field types");
        }
        return response;
    }

    @Override
    protected ResponseEntity<Object> createResponseEntity(Object body, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        if (body instanceof ProblemDetail problem
                && (problem.getType() == null || URI.create("about:blank").equals(problem.getType()))) {
            problem.setType(URI.create("https://api.iitu.kz/errors/http-" + status.value()));
        }
        return super.createResponseEntity(body, headers, status, request);
    }

    @ExceptionHandler({org.springframework.dao.DataIntegrityViolationException.class,
            org.springframework.orm.ObjectOptimisticLockingFailureException.class})
    public ProblemDetail databaseConflict(RuntimeException exception) {
        ProblemDetail problem=ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "The update conflicts with an existing record or a concurrent change");
        problem.setType(URI.create("https://api.iitu.kz/errors/database-conflict"));
        problem.setTitle("Database conflict");
        return problem;
    }

    public record FieldViolation(String field, String message) { }
}
