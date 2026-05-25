package com.checkout.payment.gateway.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;
import java.io.Serializable;

@Builder
@Value
public class BankProcessPaymentRequest implements Serializable {
  @JsonProperty("card_number")
  String cardNumber;
  @JsonProperty("expiry_date")
  String expiryDate;
  String currency;
  int amount;
  String cvv;
}
