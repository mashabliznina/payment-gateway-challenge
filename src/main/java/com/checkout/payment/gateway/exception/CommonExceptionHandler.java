package com.checkout.payment.gateway.exception;

import com.checkout.payment.gateway.model.ErrorResponse;
import com.checkout.payment.gateway.model.PaymentRejectedError;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class CommonExceptionHandler {

  private static final Logger LOG = LoggerFactory.getLogger(CommonExceptionHandler.class);

  @ExceptionHandler(EventProcessingException.class)
  public ResponseEntity<ErrorResponse> handleException(EventProcessingException ex) {
    LOG.error("Exception happened", ex);
    return ResponseEntity
        .status(HttpStatus.NOT_FOUND)
        .body(new ErrorResponse(ex.getMessage()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<PaymentRejectedError> handleMethodArgumentNotValidException(MethodArgumentNotValidException exception) {
    List<String> errors = exception.getBindingResult().getFieldErrors().stream()
        .map(DefaultMessageSourceResolvable::getDefaultMessage)
        .collect(Collectors.toCollection(ArrayList::new));

    return ResponseEntity
        .status(HttpStatus.BAD_REQUEST)
        .body(new PaymentRejectedError(errors));
  }

  @ExceptionHandler(BankProcessingException.class)
  public ResponseEntity<ErrorResponse> handleBankProcessingException(BankProcessingException exception) {
    return ResponseEntity
        .status(HttpStatus.BAD_GATEWAY)
        .body(new ErrorResponse(exception.getMessage()));
    }

  @ExceptionHandler(PaymentPersistenceException.class)
  public ResponseEntity<ErrorResponse> handleBankProcessingException(PaymentPersistenceException exception) {
    return ResponseEntity
        .status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(new ErrorResponse(exception.getMessage()));
  }
  }
