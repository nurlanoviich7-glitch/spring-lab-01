package kz.iitu.springlab.web;

import java.net.URI;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/** Standard MVC fallback for failures raised before a controller is selected. */
@ControllerAdvice
@Order(0)
public class MvcProtocolExceptionHandler extends ResponseEntityExceptionHandler {
    // Boot's automatic fallback backs off when the scoped API handler exists.
    // This advice handles standard protocol failures; business errors stay in
    // ApiExceptionHandler, and HTML missing-book errors keep their page handler.
    @Override
    protected ResponseEntity<Object> createResponseEntity(Object body, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        if (body instanceof ProblemDetail problem
                && (problem.getType() == null || URI.create("about:blank").equals(problem.getType()))) {
            problem.setType(URI.create("https://api.iitu.kz/errors/http-" + status.value()));
        }
        return super.createResponseEntity(body, headers, status, request);
    }
}
