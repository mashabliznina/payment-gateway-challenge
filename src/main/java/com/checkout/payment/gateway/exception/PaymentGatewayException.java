package com.checkout.payment.gateway.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class PaymentGatewayException extends RuntimeException{
  private final HttpStatus status;

  public PaymentGatewayException(
      String message,
      HttpStatus status) {

    super(message);
    this.status = status;
  }

}
