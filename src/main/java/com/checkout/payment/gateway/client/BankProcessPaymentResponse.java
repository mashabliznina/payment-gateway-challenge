package com.checkout.payment.gateway.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;
import java.io.Serializable;
import java.util.UUID;

@Value
public class BankProcessPaymentResponse implements Serializable {
  @JsonProperty("authorized")
  boolean authorized;

  @JsonProperty("authorization_code")
  UUID authorizationCode;
}
