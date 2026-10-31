package edu.itc.se.shop.web;

import java.util.Comparator;
import java.util.List;

import edu.itc.se.shop.order.OrderExceptions;
import edu.itc.se.shop.service.InsufficientStockException;
import edu.itc.se.shop.service.ProductNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(annotations = RestController.class)
public class ApiExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException exception) {
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "Request validation failed.");
        List<FieldErrorDetail> errors = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldErrorDetail(error.getField(), error.getDefaultMessage()))
                .sorted(Comparator.comparing(FieldErrorDetail::field))
                .toList();
        log.warn("validation failed: {}", errors);
        problem.setProperty("errors", errors);
        return problem;
    }

    @ExceptionHandler(ProductNotFoundException.class)
    public ProblemDetail handleNotFound(ProductNotFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ProblemDetail handleInsufficientStock(InsufficientStockException exception) {
        return problem(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public ProblemDetail handleDuplicateKey(DuplicateKeyException exception) {
        return problem(HttpStatus.CONFLICT, "A record with the same unique value already exists.");
    }

    // ---------- Challenge: order errors ----------

    @ExceptionHandler(OrderExceptions.NotFound.class)
    public ProblemDetail handleOrderNotFound(OrderExceptions.NotFound exception) {
        return problem(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(OrderExceptions.CustomerNotFound.class)
    public ProblemDetail handleCustomerNotFound(OrderExceptions.CustomerNotFound exception) {
        return problem(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(OrderExceptions.InvalidState.class)
    public ProblemDetail handleInvalidState(OrderExceptions.InvalidState exception) {
        return problem(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(OrderExceptions.InvalidOrder.class)
    public ProblemDetail handleInvalidOrder(OrderExceptions.InvalidOrder exception) {
        return problem(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    // ---------- fallback (keep last) ----------

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception exception) {
        log.error("unexpected error", exception);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.");
    }

    private ProblemDetail problem(HttpStatus status, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(status.getReasonPhrase());
        return problem;
    }

    private record FieldErrorDetail(String field, String message) {}
}
