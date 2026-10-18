package ru.yandex.practicum.mymarket.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.server.ResponseStatusException;

import ru.yandex.practicum.mymarket.exception.EmptyCartException;
import ru.yandex.practicum.mymarket.exception.InsufficientFundsException;
import ru.yandex.practicum.mymarket.exception.NotFoundException;
import ru.yandex.practicum.mymarket.exception.PaymentUnauthorizedException;
import ru.yandex.practicum.mymarket.exception.PaymentUnavailableException;
import ru.yandex.practicum.mymarket.exception.PurchaseInProgressException;

import jakarta.validation.ConstraintViolationException;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String notFound(NotFoundException ex) {
        return "error/404";
    }

    @ExceptionHandler(EmptyCartException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String emptyCart(EmptyCartException ex) {
        return "error/400";
    }

    @ExceptionHandler(InsufficientFundsException.class)
    @ResponseStatus(HttpStatus.PAYMENT_REQUIRED)
    public String insufficientFunds(InsufficientFundsException ex) {
        log.warn("Payment rejected: {}", ex.getMessage());
        return "error/402";
    }

    @ExceptionHandler(PaymentUnavailableException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public String paymentUnavailable(PaymentUnavailableException ex) {
        log.error("Payment service unavailable", ex);
        return "error/503";
    }

    @ExceptionHandler(PaymentUnauthorizedException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    public String paymentUnauthorized(PaymentUnauthorizedException ex) {
        log.error("Storefront is not authorized to call the payment service", ex);
        return "error/502";
    }

    @ExceptionHandler(PurchaseInProgressException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public String purchaseInProgress(PurchaseInProgressException ex) {
        log.warn("Concurrent purchase rejected: {}", ex.getMessage());
        return "error/409";
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String validationError(ConstraintViolationException ex) {
        log.warn("Request validation failed: {}", ex.getMessage());
        return "error/400";
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Void> responseStatus(ResponseStatusException ex) {
        log.warn("Request failed with status {}: {}", ex.getStatusCode(), ex.getReason());
        return ResponseEntity.status(ex.getStatusCode()).build();
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleException(Exception ex) {
        log.error("Unhandled exception", ex);
        return "error/500";
    }
}
