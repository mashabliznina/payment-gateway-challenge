package com.checkout.payment.gateway.model;

import com.checkout.payment.gateway.enums.PaymentStatus;
import lombok.Value;
import java.util.UUID;

@Value
public class ProcessedPaymentResponse {
  UUID id;
  PaymentStatus status;
  int cardNumberLastFour;
  int expiryMonth;
  int expiryYear;
  String currency;
  int amount;
}
