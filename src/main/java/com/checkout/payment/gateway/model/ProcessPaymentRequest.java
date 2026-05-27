package com.checkout.payment.gateway.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Value;

@Builder
@Value
public class ProcessPaymentRequest {

  @NotBlank(message = "Card number is required and must not be null or empty")
  @Size(min = 14, max = 19, message = "Card number must be between 14 and 19 characters")
  @Pattern(regexp = "\\d+", message = "Card number must contain only digits")
  String cardNumber;

  @NotNull(message = "Expiry month is required and must not be null or empty")
  @Min(value = 1, message = "Expiry month must be between 1 and 12")
  @Max(value = 12, message = "Expiry month must be between 1 and 12")
  Integer expiryMonth;

  @NotNull(message = "Expiry year is required and must not be null or empty")
  @Min(value = 2026, message = "Expiry year must be in the future")
  Integer expiryYear;

  @NotBlank(message = "Currency is required and must not be null or empty")
  @Size(min = 3, max = 3, message = "Currency must be 3 characters")
  @Pattern(regexp = "^(USD|EUR|GBP)$", message = "Currency must be one of: USD, EUR, GBP")
  String currency;

  @NotNull(message = "Amount is required and must not be null or empty")
  @Positive(message = "Amount must be positive")
  Integer amount;

  @NotBlank(message = "CVV is required and must not be null or empty")
  @Size(min = 3, max = 4, message = "CVV must be between 3 and 4 characters")
  @Pattern(regexp = "\\d+", message = "CVV must contain only digits")
  String cvv;
}
