package ru.yandex.practicum.payment.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;

import ru.yandex.practicum.payment.exception.AccountNotFoundException;
import ru.yandex.practicum.payment.exception.InsufficientFundsException;
import ru.yandex.practicum.payment.exception.InvalidPaymentException;

import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class PaymentExceptionHandler {

    @ExceptionHandler(AccountNotFoundException.class)
    public ProblemDetail accountNotFound(AccountNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "Счёт не найден", ex.getMessage());
    }

    @ExceptionHandler(InsufficientFundsException.class)
    public ProblemDetail insufficientFunds(InsufficientFundsException ex) {
        ProblemDetail detail = problem(HttpStatus.CONFLICT, "Недостаточно средств", ex.getMessage());
        detail.setProperty("accountId", ex.getAccountId());
        detail.setProperty("balance", ex.getBalance());
        detail.setProperty("amount", ex.getAmount());
        return detail;
    }

    @ExceptionHandler(InvalidPaymentException.class)
    public ProblemDetail invalidPayment(InvalidPaymentException ex) {
        return problem(HttpStatus.BAD_REQUEST, "Некорректный платёж", ex.getMessage());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail constraintViolation(ConstraintViolationException ex) {
        return problem(HttpStatus.BAD_REQUEST, "Некорректный запрос", ex.getMessage());
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public ProblemDetail bindException(WebExchangeBindException ex) {
        return problem(HttpStatus.BAD_REQUEST, "Некорректное тело запроса", ex.getMessage());
    }

    private ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return problem;
    }
}
