package com.checkout.payment.gateway.client;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class BankClient {

  private final RestTemplate restTemplate;

  public BankClient(RestTemplate restTemplate) {

    this.restTemplate = restTemplate;
  }

  public BankProcessPaymentResponse getPaymentStatus(BankProcessPaymentRequest request) {

    String url = "http://localhost:8080/payments";

    return restTemplate.postForObject(
        url,
        request,
        BankProcessPaymentResponse.class
    );
  }
}
