package com.checkout.payment.gateway.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.model.PostPaymentResponse;
import com.checkout.payment.gateway.model.ProcessPaymentRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
public class PaymentGatewayIntegrationTest {

  private static final String VALID_CARD_NUMBER = "2222405343248877";
  private static final int LAST_FOUR_DIGITS = 8877;
  private static final int EXPIRY_MONTH = 12;
  private static final int EXPIRY_YEAR = 2027;
  private static final int AMOUNT = 10;
  private static final String CURRENCY = "USD";
  private static final String CVV = "123";

  @Autowired
  private MockMvc mvc;

  @Autowired
  private ObjectMapper objectMapper;

  static WireMockServer wireMockServer = new WireMockServer(8080);

  @BeforeAll
  static void beforeAll() {
    wireMockServer.start();
  }

  @AfterAll
  static void afterAll() {
    wireMockServer.stop();
  }

  @Test
  void whenPaymentProcessIsAuthorisedThenCorrectPaymentIsReturned() throws Exception {
    UUID paymentId = UUID.randomUUID();

    PostPaymentResponse response = buildPostPaymentResponse(paymentId, PaymentStatus.AUTHORIZED,
        LAST_FOUR_DIGITS, EXPIRY_MONTH, EXPIRY_YEAR, AMOUNT, CURRENCY);

    ProcessPaymentRequest request = buildProcessPaymentRequest(VALID_CARD_NUMBER, EXPIRY_MONTH,EXPIRY_YEAR,
        AMOUNT, CURRENCY, CVV);

    stubSuccessfulAuthorizedPayment();

    String content = objectMapper.writeValueAsString(request);
    mvc.perform(post("/api/process-payment")
            .contentType(MediaType.APPLICATION_JSON)
            .content(content)
        )
        .andExpect(status().isOk())
        .andDo(print())
        .andExpect(jsonPath("$.status").value(response.getStatus().getName()))
        .andExpect(jsonPath("$.cardNumberLastFour").value(response.getCardNumberLastFour()))
        .andExpect(jsonPath("$.expiryMonth").value(response.getExpiryMonth()))
        .andExpect(jsonPath("$.expiryYear").value(response.getExpiryYear()))
        .andExpect(jsonPath("$.currency").value(response.getCurrency()))
        .andExpect(jsonPath("$.amount").value(response.getAmount()));
  }

  @Test
  void whenPaymentProcessIsUnauthorisedThenPaymentWithStatusDeclinedIsReturned() throws Exception {
    UUID paymentId = UUID.randomUUID();

    PostPaymentResponse response = buildPostPaymentResponse(paymentId, PaymentStatus.REJECTED,
        LAST_FOUR_DIGITS, EXPIRY_MONTH, EXPIRY_YEAR, AMOUNT, CURRENCY);

    ProcessPaymentRequest request = buildProcessPaymentRequest(VALID_CARD_NUMBER, EXPIRY_MONTH,EXPIRY_YEAR,
        AMOUNT, CURRENCY, CVV);

    stubSuccessfulDeclinedPayment();

    String content = objectMapper.writeValueAsString(request);
    mvc.perform(post("/api/process-payment")
            .contentType(MediaType.APPLICATION_JSON)
            .content(content)
        )
        .andExpect(status().isOk())
        .andDo(print())
        .andExpect(jsonPath("$.status").value(response.getStatus().getName()))
        .andExpect(jsonPath("$.cardNumberLastFour").value(response.getCardNumberLastFour()))
        .andExpect(jsonPath("$.expiryMonth").value(response.getExpiryMonth()))
        .andExpect(jsonPath("$.expiryYear").value(response.getExpiryYear()))
        .andExpect(jsonPath("$.currency").value(response.getCurrency()))
        .andExpect(jsonPath("$.amount").value(response.getAmount()));
  }

  private void stubSuccessfulAuthorizedPayment() {
    WireMock.stubFor(WireMock.post(WireMock.urlEqualTo("/payments")
            )
            .willReturn(
                WireMock.aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {
                            "authorized": true,
                            "authorization_code": "1669264d-a5c6-447c-8abf-737de3661a35"
                        }
                        """)
            )
    );
  }

  private void stubSuccessfulDeclinedPayment() {
    WireMock.stubFor(WireMock.post(WireMock.urlEqualTo("/payments")
            )
            .willReturn(
                WireMock.aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {
                            "authorized": false,
                            "authorization_code": ""
                        }
                        """)
            )
    );
  }

  private ProcessPaymentRequest buildProcessPaymentRequest(String cardNumber,
      int expiryMonth, int expiryYear, int amount, String currency, String cvv) {
    return ProcessPaymentRequest.builder()
        .cardNumber(cardNumber)
        .expiryMonth(expiryMonth)
        .expiryYear(expiryYear)
        .amount(amount)
        .currency(currency)
        .cvv(cvv)
        .build();
  }

  private PostPaymentResponse buildPostPaymentResponse(UUID paymentId,
      PaymentStatus paymentStatus, int lastFourDigits, int expiryMonth, int expiryYear, int amount,
      String currency) {

    return PostPaymentResponse.builder()
        .id(paymentId)
        .status(paymentStatus)
        .cardNumberLastFour(lastFourDigits)
        .expiryMonth(expiryMonth)
        .expiryYear(expiryYear)
        .amount(amount)
        .currency(currency)
        .build();
  }
}
