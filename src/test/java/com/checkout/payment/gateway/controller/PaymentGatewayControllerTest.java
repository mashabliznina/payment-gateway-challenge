package com.checkout.payment.gateway.controller;


import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.exception.EventProcessingException;
import com.checkout.payment.gateway.model.ErrorResponse;
import com.checkout.payment.gateway.model.PaymentRejectedError;
import com.checkout.payment.gateway.model.PostPaymentResponse;
import com.checkout.payment.gateway.model.ProcessPaymentRequest;
import com.checkout.payment.gateway.service.PaymentGatewayService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@TestInstance(Lifecycle.PER_CLASS)
@WebMvcTest(PaymentGatewayController.class)
class PaymentGatewayControllerTest {

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

  @MockBean
  private PaymentGatewayService paymentGatewayService;

  @Test
  void whenPaymentProcessIsAuthorisedThenCorrectPaymentIsReturned() throws Exception {
    UUID paymentId = UUID.randomUUID();

    PostPaymentResponse response = buildPostPaymentResponse(paymentId, PaymentStatus.AUTHORIZED,
        LAST_FOUR_DIGITS, EXPIRY_MONTH, EXPIRY_YEAR, AMOUNT, CURRENCY);

    ProcessPaymentRequest request = buildProcessPaymentRequest(VALID_CARD_NUMBER, EXPIRY_MONTH,
        EXPIRY_YEAR,
        AMOUNT, CURRENCY, CVV);

    when(paymentGatewayService.processPayment(request)).thenReturn(response);

    String content = objectMapper.writeValueAsString(request);
    mvc.perform(post("/api/process-payment")
            .contentType(MediaType.APPLICATION_JSON)
            .content(content)
        )
        .andExpect(status().isOk())
        .andDo(print())
        .andExpect(jsonPath("$.id").value(response.getId().toString()))
        .andExpect(jsonPath("$.status").value(response.getStatus().getName()))
        .andExpect(jsonPath("$.cardNumberLastFour").value(response.getCardNumberLastFour()))
        .andExpect(jsonPath("$.expiryMonth").value(response.getExpiryMonth()))
        .andExpect(jsonPath("$.expiryYear").value(response.getExpiryYear()))
        .andExpect(jsonPath("$.currency").value(response.getCurrency()))
        .andExpect(jsonPath("$.amount").value(response.getAmount()));
  }

  @ParameterizedTest
  @MethodSource("invalidData")
  void whenInvalidDataProvidedReturnBadRequest(String cardNumber,
      int expiryMonth, int expiryYear, int amount, String currency, String cvv,
      String errorMessages)
      throws Exception {

    ProcessPaymentRequest request = buildProcessPaymentRequest(cardNumber, expiryMonth, expiryYear,
        amount, currency, cvv);

    PaymentRejectedError error = new PaymentRejectedError(Collections.singletonList(errorMessages));

    mvc.perform(post("/api/process-payment")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isBadRequest())
        .andDo(print())
        .andExpect(jsonPath("$.status").value(error.getStatus().getName()))
        .andExpect(jsonPath("$.message").value(error.getMessage()))
        .andExpect(jsonPath("$.errors").isArray())
        .andExpect(jsonPath("$.errors", hasItem(errorMessages)));
  }

  @Test
  void whenPaymentWithIdExistsThen200IsReturned() throws Exception {
    UUID id = UUID.randomUUID();

    PostPaymentResponse response = PostPaymentResponse.builder()
        .id(id)
        .status(PaymentStatus.AUTHORIZED)
        .cardNumberLastFour(LAST_FOUR_DIGITS)
        .expiryMonth(EXPIRY_MONTH)
        .expiryYear(EXPIRY_YEAR)
        .currency(CURRENCY)
        .amount(AMOUNT)
        .build();

    when(paymentGatewayService.getPaymentById(id)).thenReturn(response);

    mvc.perform(get("/api/payment/" + id)
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andDo(print())
        .andExpect(jsonPath("$.id").value(response.getId().toString()))
        .andExpect(jsonPath("$.status").value(response.getStatus().getName()))
        .andExpect(jsonPath("$.cardNumberLastFour").value(response.getCardNumberLastFour()))
        .andExpect(jsonPath("$.expiryMonth").value(response.getExpiryMonth()))
        .andExpect(jsonPath("$.expiryYear").value(response.getExpiryYear()))
        .andExpect(jsonPath("$.currency").value(response.getCurrency()))
        .andExpect(jsonPath("$.amount").value(response.getAmount()));
  }

  @Test
  void whenPaymentWithIdDoesNotExistThen404IsReturned() throws Exception {
    ErrorResponse error = new ErrorResponse("Invalid ID");
    UUID id = UUID.randomUUID();

    when(paymentGatewayService.getPaymentById(id)).thenThrow(
        new EventProcessingException("Invalid ID"));

    mvc.perform(get("/api/payment/" + id)
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isNotFound())
        .andDo(print())
        .andExpect(jsonPath("$.message").value(error.getMessage()));
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

  private Stream<Arguments> invalidData() {
    return Stream.of(
        Arguments.of("", EXPIRY_MONTH, EXPIRY_YEAR, AMOUNT, CURRENCY, CVV,
            "Card number is required and must not be null or empty"),
        Arguments.of(" ", EXPIRY_MONTH, EXPIRY_YEAR, AMOUNT, CURRENCY, CVV,
            "Card number is required and must not be null or empty"),
        Arguments.of(null, EXPIRY_MONTH, EXPIRY_YEAR, AMOUNT, CURRENCY, CVV,
            "Card number is required and must not be null or empty"),
        Arguments.of("22224053432488ee", EXPIRY_MONTH, EXPIRY_YEAR, AMOUNT, CURRENCY, CVV,
            "Card number must contain only digits"),
        Arguments.of("2222405", EXPIRY_MONTH, EXPIRY_YEAR, AMOUNT, CURRENCY, CVV,
            "Card number must be between 14 and 19 characters"),
        Arguments.of(VALID_CARD_NUMBER, 0, EXPIRY_YEAR, AMOUNT, CURRENCY, CVV,
            "Expiry month must be between 1 and 12"),
        Arguments.of(VALID_CARD_NUMBER, 13, EXPIRY_YEAR, AMOUNT, CURRENCY, CVV,
            "Expiry month must be between 1 and 12"),
        Arguments.of(VALID_CARD_NUMBER, EXPIRY_MONTH, 2024, AMOUNT, CURRENCY, CVV,
            "Expiry year must be in the future"),
        Arguments.of(VALID_CARD_NUMBER, EXPIRY_MONTH, EXPIRY_YEAR, -1, CURRENCY, CVV,
            "Amount must be positive"),
        Arguments.of(VALID_CARD_NUMBER, EXPIRY_MONTH, EXPIRY_YEAR, AMOUNT, null, CVV,
            "Currency is required and must not be null or empty"),
        Arguments.of(VALID_CARD_NUMBER, EXPIRY_MONTH, EXPIRY_YEAR, AMOUNT, "", CVV,
            "Currency is required and must not be null or empty"),
        Arguments.of(VALID_CARD_NUMBER, EXPIRY_MONTH, EXPIRY_YEAR, AMOUNT, " ", CVV,
            "Currency is required and must not be null or empty"),
        Arguments.of(VALID_CARD_NUMBER, EXPIRY_MONTH, EXPIRY_YEAR, AMOUNT, "EU", CVV,
            "Currency must be 3 characters"),
        Arguments.of(VALID_CARD_NUMBER, EXPIRY_MONTH, EXPIRY_YEAR, AMOUNT, CURRENCY, "",
            "CVV is required and must not be null or empty"),
        Arguments.of(VALID_CARD_NUMBER, EXPIRY_MONTH, EXPIRY_YEAR, AMOUNT, CURRENCY, " ",
            "CVV is required and must not be null or empty"),
        Arguments.of(VALID_CARD_NUMBER, EXPIRY_MONTH, EXPIRY_YEAR, AMOUNT, CURRENCY, null,
            "CVV is required and must not be null or empty"),
        Arguments.of(VALID_CARD_NUMBER, EXPIRY_MONTH, EXPIRY_YEAR, AMOUNT, CURRENCY, "12",
            "CVV must be between 3 and 4 characters"),
        Arguments.of(VALID_CARD_NUMBER, EXPIRY_MONTH, EXPIRY_YEAR, AMOUNT, CURRENCY, "12345",
            "CVV must be between 3 and 4 characters"),
        Arguments.of(VALID_CARD_NUMBER, EXPIRY_MONTH, EXPIRY_YEAR, AMOUNT, CURRENCY, "23e",
            "CVV must contain only digits")
    );
  }
}
