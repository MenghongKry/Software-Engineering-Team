package edu.itc.se.shop_jpa.web;

import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import edu.itc.se.shop_jpa.service.OutOfStockException;

/** Turns expected errors into clean HTTP answers instead of a 500 with a stack trace. */
@RestControllerAdvice
public class ApiExceptionHandler {

    /**
     * Bad input, e.g. sort=version,desc on the report or a broken feed cursor → 400 Bad Request.
     * Thrown inside a repository, an IllegalArgumentException arrives wrapped in Spring's
     * InvalidDataAccessApiUsageException, so both are handled.
     */
    @ExceptionHandler({IllegalArgumentException.class, InvalidDataAccessApiUsageException.class})
    public ProblemDetail badRequest(RuntimeException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(OutOfStockException.class)
    public ProblemDetail outOfStock(OutOfStockException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }
}
