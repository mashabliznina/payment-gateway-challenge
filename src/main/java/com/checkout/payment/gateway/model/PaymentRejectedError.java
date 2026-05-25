package com.checkout.payment.gateway.model;

import com.checkout.payment.gateway.enums.PaymentStatus;
import lombok.Getter;
import java.util.List;

@Getter
public class PaymentRejectedError {

  private final List<String> errors;
  private final PaymentStatus status = PaymentStatus.REJECTED;
  private final String message = "No payment could be created as invalid information was provided";

  public PaymentRejectedError(List<String> errors) {
    this.errors = errors;
  }
}
