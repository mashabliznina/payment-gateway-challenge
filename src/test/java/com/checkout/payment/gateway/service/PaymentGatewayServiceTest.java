package com.checkout.payment.gateway.service;

import com.checkout.payment.gateway.client.BankClient;
import com.checkout.payment.gateway.client.BankProcessPaymentRequest;
import com.checkout.payment.gateway.client.BankProcessPaymentResponse;
import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.exception.BankProcessingException;
import com.checkout.payment.gateway.exception.EventProcessingException;
import com.checkout.payment.gateway.exception.PaymentPersistenceException;
import com.checkout.payment.gateway.model.PostPaymentResponse;
import com.checkout.payment.gateway.model.ProcessPaymentRequest;
import com.checkout.payment.gateway.repository.PaymentsRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PaymentGatewayServiceTest {

  private static final String VALID_CARD_NUMBER = "2222405343248877";
  private static final int LAST_FOUR_DIGITS = 8877;
  private static final int EXPIRY_MONTH = 12;
  private static final int EXPIRY_YEAR = 2027;
  private static final int AMOUNT = 10;
  private static final String CURRENCY = "USD";
  private static final String CVV = "123";
  private static final String EXPIRY_DATE = "12/2028";

  @Mock
  private PaymentsRepository paymentsRepository;

  @Mock
  private BankClient bankClient;

  @InjectMocks
  private PaymentGatewayService paymentGatewayService;

  @Test
  void getPaymentReturnPaymentWhenIdExist() {

    UUID paymentId = UUID.randomUUID();
    PostPaymentResponse response = buildPostPaymentResponse(paymentId);

    when(paymentsRepository.get(paymentId)).thenReturn(Optional.ofNullable(response));

    PostPaymentResponse result = paymentGatewayService.getPaymentById(paymentId);

    assertNotNull(result);
    assertEquals(response, result);

    verify(paymentsRepository, times(1)).get(paymentId);
  }

  @Test
  void shouldThrowExceptionWhenPaymentNotExist() {
    UUID id = UUID.randomUUID();

    when(paymentsRepository.get(id)).thenReturn(Optional.empty());

    EventProcessingException ex = assertThrows(EventProcessingException.class,
        () -> paymentGatewayService.getPaymentById(id));

    assertEquals("Invalid ID", ex.getMessage());

    verify(paymentsRepository, times(1)).get(id);
  }

  @Test
  void processPaymentReturnAuthorisedOPaymentDetails() {

    ProcessPaymentRequest request = buildProcessPaymentRequest();

    BankProcessPaymentResponse bankResponse = BankProcessPaymentResponse.builder()
            .authorized(true)
            .build();

    when(bankClient.makePayment(any(BankProcessPaymentRequest.class))).thenReturn(bankResponse);

    PostPaymentResponse result = paymentGatewayService.processPayment(request);

    assertNotNull(result);
    assertThat(result)
        .satisfies(payment -> {
          assertThat(payment.getStatus()).isEqualTo(PaymentStatus.AUTHORIZED);
          assertThat(payment.getCardNumberLastFour()).isEqualTo(LAST_FOUR_DIGITS);
          assertThat(payment.getExpiryMonth()).isEqualTo(EXPIRY_MONTH);
          assertThat(payment.getCurrency()).isEqualTo(CURRENCY);
          assertThat(payment.getAmount()).isEqualTo(AMOUNT);

        });

    verify(bankClient,times(1)).makePayment(any(BankProcessPaymentRequest.class));

    verify(paymentsRepository, times(1)).add(any(PostPaymentResponse.class));
  }

  @Test
  void processPaymentReturnDeclinedPaymentDetails() {

    ProcessPaymentRequest request = buildProcessPaymentRequest();

    BankProcessPaymentResponse bankResponse = BankProcessPaymentResponse.builder()
        .authorized(false)
        .authorizationCode(UUID.randomUUID())
        .build();

    when(bankClient.makePayment(any(BankProcessPaymentRequest.class))).thenReturn(bankResponse);

    PostPaymentResponse result = paymentGatewayService.processPayment(request);

    assertNotNull(result);
    assertThat(result)
        .satisfies(payment -> {
          assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REJECTED);
          assertThat(payment.getCardNumberLastFour()).isEqualTo(LAST_FOUR_DIGITS);
          assertThat(payment.getExpiryMonth()).isEqualTo(EXPIRY_MONTH);
          assertThat(payment.getCurrency()).isEqualTo(CURRENCY);
          assertThat(payment.getAmount()).isEqualTo(AMOUNT);

        });

    verify(bankClient,times(1)).makePayment(any(BankProcessPaymentRequest.class));

    verify(paymentsRepository, times(1)).add(any(PostPaymentResponse.class));
  }

  @Test
  void shouldThrowExceptionWhenBankFails() {

    ProcessPaymentRequest request = buildProcessPaymentRequest();

    when(bankClient.makePayment(any()))
        .thenThrow(new RuntimeException());

    BankProcessingException exception = assertThrows(BankProcessingException.class,
            () -> paymentGatewayService.processPayment(request));

    assertEquals("Unable to process payment with bank", exception.getMessage());

    verify(paymentsRepository, never()).add(any());
  }

  @Test
  void shouldThrowExceptionWhenRepositoryFails() {

    ProcessPaymentRequest request = buildProcessPaymentRequest();

    BankProcessPaymentResponse bankResponse = BankProcessPaymentResponse.builder()
        .authorized(true)
        .authorizationCode(UUID.randomUUID())
        .build();

    when(bankClient.makePayment(any())).thenReturn(bankResponse);

    doThrow(new PaymentPersistenceException("Failed to save payment"))
        .when(paymentsRepository).add(any(PostPaymentResponse.class));

    PaymentPersistenceException exception = assertThrows(PaymentPersistenceException.class,
            () -> paymentGatewayService.processPayment(request));

    assertEquals("Failed to save payment", exception.getMessage());
  }

  private PostPaymentResponse buildPostPaymentResponse(UUID paymentId) {
    return PostPaymentResponse.builder()
        .id(paymentId)
        .status(PaymentStatus.AUTHORIZED)
        .cardNumberLastFour(LAST_FOUR_DIGITS)
        .expiryMonth(EXPIRY_MONTH)
        .expiryYear(EXPIRY_YEAR)
        .amount(AMOUNT)
        .currency(CURRENCY)
        .build();
  }

  private ProcessPaymentRequest buildProcessPaymentRequest() {
    return ProcessPaymentRequest.builder()
        .cardNumber(VALID_CARD_NUMBER)
        .expiryMonth(EXPIRY_MONTH)
        .expiryYear(EXPIRY_YEAR)
        .amount(AMOUNT)
        .currency(CURRENCY)
        .cvv(CVV)
        .build();
  }
}
