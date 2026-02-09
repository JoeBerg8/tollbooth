package com.tollbooth.stripe;

import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.CustomerBalanceTransaction;
import com.stripe.model.checkout.Session;
import com.stripe.param.CustomerBalanceTransactionCollectionCreateParams;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.CustomerListParams;
import com.stripe.param.checkout.SessionCreateParams;
import com.tollbooth.config.TollProperties;
import java.util.Map;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class StripeService {

  private static final Logger logger = LogManager.getLogger(StripeService.class);

  @Autowired private TollProperties tollProperties;

  /**
   * Gets or creates a Stripe customer for an email sender. This is used for balance-based toll
   * payments.
   *
   * @param senderEmail The sender's email address
   * @return The Stripe customer ID
   */
  public String getOrCreateSenderCustomer(String senderEmail) {
    try {
      // First try to find existing customer by email
      CustomerListParams listParams =
          CustomerListParams.builder().setEmail(senderEmail).setLimit(1L).build();

      var customers = Customer.list(listParams);

      if (customers.getData().size() > 0) {
        String customerId = customers.getData().get(0).getId();
        logger.debug("Found existing customer {} for sender {}", customerId, senderEmail);
        return customerId;
      }

      // Customer doesn't exist, create new one
      CustomerCreateParams createParams =
          CustomerCreateParams.builder()
              .setEmail(senderEmail)
              .setName(senderEmail)
              .setMetadata(Map.of("inbox_toll_customer", "true"))
              .build();

      Customer customer = Customer.create(createParams);
      logger.info("Created new customer {} for sender {}", customer.getId(), senderEmail);

      return customer.getId();

    } catch (StripeException e) {
      logger.error(
          "Failed to get or create customer for sender {}: {}", senderEmail, e.getMessage(), e);
      throw new RuntimeException("Failed to create customer for sender", e);
    }
  }

  /**
   * Checks if a sender has sufficient balance for a toll payment.
   *
   * @param senderCustomerId The sender's Stripe customer ID
   * @param tollAmount The toll amount to check (in dollars)
   * @return True if sender has sufficient balance, false otherwise
   */
  public boolean checkSenderBalance(String senderCustomerId, double tollAmount) {
    try {
      Customer customer = Customer.retrieve(senderCustomerId);

      // Convert toll amount to cents for comparison
      long tollAmountCents = Math.round(tollAmount * 100);

      // Check if customer balance is sufficient (balance is in cents, negative means they
      // have
      // credit)
      boolean sufficient = customer.getBalance() <= -tollAmountCents;

      logger.debug(
          "Sender {} balance: {} cents, toll: {} cents, sufficient: {}",
          senderCustomerId,
          customer.getBalance(),
          tollAmountCents,
          sufficient);

      return sufficient;

    } catch (StripeException e) {
      logger.error(
          "Failed to check balance for customer {}: {}", senderCustomerId, e.getMessage(), e);
      return false;
    }
  }

  /**
   * Credits the sender's balance with the specified amount.
   *
   * @param senderCustomerId The sender's Stripe customer ID
   * @param creditAmountCents The amount to credit to the balance in cents
   * @param description Description for the transaction
   * @return True if credit was successful, false otherwise
   */
  public boolean creditSenderBalance(
      String senderCustomerId, long creditAmountCents, String description) {
    try {
      // Create balance transaction to credit the amount (negative amount credits the balance)
      CustomerBalanceTransactionCollectionCreateParams params =
          CustomerBalanceTransactionCollectionCreateParams.builder()
              .setAmount(creditAmountCents * -1) // Negative amount credits the balance
              .setCurrency("usd")
              .setDescription(description)
              .build();

      Customer customer = Customer.retrieve(senderCustomerId);

      // Use the Customer service to create balance transaction
      CustomerBalanceTransaction transaction = customer.balanceTransactions().create(params);
      var creditAmountDollars = creditAmountCents / 100.0;
      logger.info(
          "Successfully credited ${} to customer {} balance (transaction: {})",
          creditAmountDollars,
          senderCustomerId,
          transaction.getId());

      return true;

    } catch (StripeException e) {
      logger.error(
          "Failed to credit balance for customer {}: {}", senderCustomerId, e.getMessage(), e);
      return false;
    }
  }

  /**
   * Debits the toll amount from sender's balance. Simplified version - no PendingTollTransfer
   * needed since payments go directly to user's Stripe account.
   *
   * @param senderCustomerId The sender's Stripe customer ID
   * @param tollAmount The toll amount to debit (in dollars)
   * @param emailMetaId The ID of the email meta record this toll is for
   * @return True if debit was successful, false otherwise
   */
  public boolean debitSenderBalance(String senderCustomerId, double tollAmount, UUID emailMetaId) {
    try {
      // Convert toll amount to cents (positive amount debits the balance)
      long tollAmountCents = Math.round(tollAmount * 100);

      // Create balance transaction to debit the amount
      CustomerBalanceTransactionCollectionCreateParams params =
          CustomerBalanceTransactionCollectionCreateParams.builder()
              .setAmount(tollAmountCents) // Positive amount debits the balance
              .setCurrency("usd")
              .setDescription("Inbox toll payment: " + emailMetaId.toString())
              .build();

      Customer customer = Customer.retrieve(senderCustomerId);

      // Use the Customer service to create balance transaction
      CustomerBalanceTransaction transaction = customer.balanceTransactions().create(params);

      logger.info(
          "Successfully debited ${} from customer {} balance (transaction: {})",
          tollAmount,
          senderCustomerId,
          transaction.getId());

      return true;

    } catch (StripeException e) {
      logger.error(
          "Failed to debit balance for customer {}: {}", senderCustomerId, e.getMessage(), e);
      return false;
    }
  }

  /**
   * Creates a Stripe Checkout session for topping up sender's balance.
   *
   * @param senderEmail The sender's email address
   * @param senderCustomerId The sender's Stripe customer ID
   * @param messageId The Gmail message ID
   * @param emailMetaId The email meta ID
   * @return The Checkout session URL, or null if creation failed
   */
  public String createTopUpCheckoutSession(
      String senderEmail, String senderCustomerId, String messageId, UUID emailMetaId) {
    try {
      Map<String, String> metadata =
          Map.of(
              "sessionType",
              "inbox_toll_topup",
              "emailMetaId",
              emailMetaId.toString(),
              "messageId",
              messageId,
              "senderEmail",
              senderEmail,
              "senderCustomerId",
              senderCustomerId,
              "tollAmountAtTopUp",
              String.valueOf(tollProperties.getTollAmount()));

      double netAmount = Math.max(1.00, tollProperties.getTollAmount());
      // Calculate the gross amount to charge to cover Stripe fees (2.9% + $0.30)
      double grossAmount = (netAmount + 0.30) / (1 - 0.029);
      // Stripe requires the amount in cents as a long.
      long finalAmountInCents = Math.round(grossAmount * 100);

      SessionCreateParams params =
          SessionCreateParams.builder()
              .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
              .setMode(SessionCreateParams.Mode.PAYMENT)
              .setSuccessUrl(tollProperties.getSuccessUrl())
              .setCancelUrl(tollProperties.getCancelUrl())
              .addLineItem(
                  SessionCreateParams.LineItem.builder()
                      .setPriceData(
                          SessionCreateParams.LineItem.PriceData.builder()
                              .setCurrency("usd")
                              .setUnitAmount(finalAmountInCents)
                              .setProductData(
                                  SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                      .setName("Inbox Toll" + " Balance" + " Top-up")
                                      .setDescription(
                                          String.format(
                                              "Add funds"
                                                  + " to your"
                                                  + " inbox"
                                                  + " toll"
                                                  + " balance"
                                                  + " (minimum:"
                                                  + " $%.2f)"
                                                  + " + Stripe"
                                                  + " fees",
                                              netAmount))
                                      .build())
                              .build())
                      .setQuantity(1L)
                      .setAdjustableQuantity(
                          SessionCreateParams.LineItem.AdjustableQuantity.builder()
                              .setEnabled(true)
                              .setMinimum(1L)
                              .setMaximum(1000L) // Allow up to $1000
                              // top-up
                              .build())
                      .build())
              .putAllMetadata(metadata)
              .build();

      Session session = Session.create(params);

      logger.info(
          "Created toll balance top-up link for message {} with session ID {} for sender"
              + " {} (min toll: ${})",
          messageId,
          session.getId(),
          senderEmail,
          netAmount);
      return session.getUrl();

    } catch (StripeException e) {
      logger.error(
          "Failed to create toll payment link for message {}: {}", messageId, e.getMessage(), e);
      return null;
    } catch (Exception e) {
      logger.error(
          "Failed to create toll payment link for message {}: {}", messageId, e.getMessage(), e);
      return null;
    }
  }
}
