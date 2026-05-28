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
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PaymentGatewayService {

  private static final Logger LOG = LoggerFactory.getLogger(PaymentGatewayService.class);

  private final PaymentsRepository paymentsRepository;
  private final BankClient bankClient;

  public PaymentGatewayService(PaymentsRepository paymentsRepository, BankClient bankClient) {
    this.paymentsRepository = paymentsRepository;
    this.bankClient = bankClient;
  }

  public PostPaymentResponse getPaymentById(UUID id) {
    LOG.debug("Requesting access to to payment with ID {}", id);
    return paymentsRepository.get(id).orElseThrow(() -> new EventProcessingException("Invalid ID"));
  }

  public PostPaymentResponse processPayment(ProcessPaymentRequest paymentRequest) {
    String expiryDate =
        String.format("%02d/%d", paymentRequest.getExpiryMonth(), paymentRequest.getExpiryYear());
    String cardNumber = paymentRequest.getCardNumber();
    int lastFourDigits = Integer.parseInt(cardNumber.substring(cardNumber.length() - 4));
    UUID paymentId = UUID.randomUUID();
    BankProcessPaymentRequest bankRequest = BankProcessPaymentRequest.builder()
        .cardNumber(cardNumber)
        .expiryDate(expiryDate)
        .currency(paymentRequest.getCurrency())
        .amount(paymentRequest.getAmount())
        .cvv(paymentRequest.getCvv())
        .build();
    BankProcessPaymentResponse bankResponse;

    try {
      bankResponse = bankClient.makePayment(bankRequest);
    } catch (RuntimeException exception) {
      LOG.error("Bank is unavailable, payment can not be processed");
      throw new BankProcessingException("Unable to process payment with bank");
    }

    if(!bankResponse.isAuthorized()) {
      PostPaymentResponse paymentResponse = buildPostPaymentResponse(paymentId, PaymentStatus.REJECTED,
          lastFourDigits, paymentRequest.getExpiryMonth(), paymentRequest.getExpiryYear(),
          paymentRequest.getAmount(), paymentRequest.getCurrency());
      try {
        paymentsRepository.add(paymentResponse);
      } catch (RuntimeException exception) {
        logAndThrowPaymentPersistenceException(paymentId);
      }
      return paymentResponse;
    }

    PostPaymentResponse paymentResponse = buildPostPaymentResponse(paymentId, PaymentStatus.AUTHORIZED, lastFourDigits,
        paymentRequest.getExpiryMonth(), paymentRequest.getExpiryYear(), paymentRequest.getAmount(),
        paymentRequest.getCurrency());
    try{
      paymentsRepository.add(paymentResponse);
    } catch (RuntimeException ex) {
      logAndThrowPaymentPersistenceException(paymentId);
    }

    return paymentResponse;
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

  private void logAndThrowPaymentPersistenceException(UUID paymentId) {
    LOG.error("Payment with id {} has not been saved", paymentId);
    throw new PaymentPersistenceException("Failed to save payment");
  }
}
