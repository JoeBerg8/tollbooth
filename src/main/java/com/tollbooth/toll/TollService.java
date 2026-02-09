package com.tollbooth.toll;

import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;
import com.tollbooth.config.TollProperties;
import com.tollbooth.gmail.GmailService;
import com.tollbooth.stripe.StripeService;
import java.time.Instant;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TollService {

  private static final Logger logger = LogManager.getLogger(TollService.class);

  @Autowired private GmailService gmailService;

  @Autowired private WhitelistService whitelistService;

  @Autowired private StripeService stripeService;

  @Autowired private TollEmailMetaDao tollEmailMetaDao;

  @Autowired private TollProperties tollProperties;

  @Autowired private TollEmailTemplateService emailTemplateService;

  /**
   * Processes an email for toll payment. Checks whitelist, balance, and either debits immediately
   * or creates a top-up link.
   *
   * @param gmailClient The Gmail client instance
   * @param messageId The Gmail message ID
   * @param fullMessage The full message object
   * @return True if toll was processed successfully, false otherwise
   */
  public boolean processEmail(Gmail gmailClient, String messageId, Message fullMessage) {
    try {
      // Check if email has already been processed
      if (tollEmailMetaDao.isEmailAlreadyProcessed(messageId)) {
        logger.debug("Email {} already processed, skipping", messageId);
        return true;
      }

      // Extract sender email
      String senderEmail = gmailService.extractSenderEmail(fullMessage);
      if (senderEmail == null || senderEmail.isEmpty()) {
        logger.warn("Could not extract sender email from message {}, skipping", messageId);
        return false;
      }

      // Check if sender is whitelisted
      if (whitelistService.isSenderWhitelisted(gmailClient, senderEmail, fullMessage)) {
        logger.debug(
            "Sender {} is whitelisted, skipping toll for message {}", senderEmail, messageId);
        // Still record that we processed this email
        recordEmailProcessed(messageId, senderEmail, null, false);
        return true;
      }

      // Ensure "Awaiting Toll" label exists
      String awaitingTollLabelId = gmailService.ensureAwaitingTollLabelExists(gmailClient);
      if (awaitingTollLabelId == null) {
        logger.error("Failed to create Awaiting Toll label, skipping toll processing");
        return false;
      }

      // Get or create sender Stripe customer
      String senderCustomerId = stripeService.getOrCreateSenderCustomer(senderEmail);

      // Check sender balance
      double tollAmount = tollProperties.getTollAmount();
      boolean hasSufficientBalance = stripeService.checkSenderBalance(senderCustomerId, tollAmount);

      if (hasSufficientBalance) {
        // Debit immediately and move email to inbox
        UUID emailMetaId = UUID.randomUUID();
        boolean debitSuccessful =
            stripeService.debitSenderBalance(senderCustomerId, tollAmount, emailMetaId);

        if (debitSuccessful) {
          // Move email to inbox and label as "Toll Paid"
          gmailService.moveAndUnlabelMessage(gmailClient, messageId, awaitingTollLabelId);
          recordEmailProcessed(messageId, senderEmail, senderCustomerId, true);

          logger.info(
              "Successfully processed toll payment (${}) for message {} from {} using" + " balance",
              tollAmount,
              messageId,
              senderEmail);
          return true;
        } else {
          logger.error("Failed to debit sender balance for message {}", messageId);
          return false;
        }
      } else {
        // Insufficient balance - create top-up link and archive email
        UUID emailMetaId = UUID.randomUUID();
        String topUpLink =
            stripeService.createTopUpCheckoutSession(
                senderEmail, senderCustomerId, messageId, emailMetaId);

        if (topUpLink != null) {
          // Archive and label the message
          gmailService.archiveAndLabelMessage(gmailClient, messageId, awaitingTollLabelId);

          // Send top-up email to sender
          String subject = emailTemplateService.renderSubject(tollAmount);
          String body = emailTemplateService.renderBody(tollAmount, topUpLink, senderEmail);
          gmailService.sendEmail(gmailClient, senderEmail, subject, body);

          // Record email as processed but not paid
          recordEmailProcessed(messageId, senderEmail, senderCustomerId, false);

          logger.info(
              "Insufficient balance for sender {}, sent top-up link for message {}",
              senderEmail,
              messageId);
          return true;
        } else {
          logger.error("Failed to create top-up link for sender {}", senderEmail);
          return false;
        }
      }

    } catch (Exception e) {
      logger.error("Error processing toll for message {}: {}", messageId, e.getMessage(), e);
      return false;
    }
  }

  /**
   * Processes toll payment after a balance top-up has been completed. Called from webhook handler.
   *
   * @param gmailClient The Gmail client instance
   * @param messageId The Gmail message ID
   * @param tollAmount The toll amount to process
   * @return True if payment was processed successfully, false otherwise
   */
  public boolean processTollPaymentAfterTopUp(
      Gmail gmailClient, String messageId, double tollAmount) {
    try {
      // Get the email meta record
      java.util.Optional<TollEmailMeta> emailMetaOpt = tollEmailMetaDao.findByGmailId(messageId);
      if (emailMetaOpt.isEmpty()) {
        logger.warn("No email meta found for message {} during post-topup processing", messageId);
        return false;
      }

      TollEmailMeta emailMeta = emailMetaOpt.get();
      String senderCustomerId = emailMeta.getStripeCustomerId();

      if (senderCustomerId == null) {
        logger.warn("No Stripe customer ID found for message {}", messageId);
        return false;
      }

      // Check if sender now has sufficient balance
      boolean hasSufficientBalance = stripeService.checkSenderBalance(senderCustomerId, tollAmount);

      if (hasSufficientBalance) {
        // Process the toll payment
        boolean debitSuccessful =
            stripeService.debitSenderBalance(senderCustomerId, tollAmount, emailMeta.getId());

        if (debitSuccessful) {
          // Move email to inbox
          String awaitingTollLabelId = gmailService.ensureAwaitingTollLabelExists(gmailClient);
          if (awaitingTollLabelId != null) {
            gmailService.moveAndUnlabelMessage(gmailClient, messageId, awaitingTollLabelId);
          }

          // Update email meta to mark as paid
          emailMeta.setTollPaid(true);
          tollEmailMetaDao.update(emailMeta);

          logger.info("Successfully processed toll payment after top-up for message {}", messageId);
          return true;
        } else {
          logger.error("Failed to debit sender balance after top-up for message {}", messageId);
          return false;
        }
      } else {
        logger.warn("Sender still has insufficient balance after top-up for message {}", messageId);
        return false;
      }

    } catch (Exception e) {
      logger.error(
          "Error processing toll payment after top-up for message {}: {}",
          messageId,
          e.getMessage(),
          e);
      return false;
    }
  }

  /**
   * Records that an email has been processed.
   *
   * @param gmailId The Gmail message ID
   * @param senderEmail The sender's email address
   * @param stripeCustomerId The sender's Stripe customer ID (may be null)
   * @param tollPaid Whether the toll was paid
   */
  private void recordEmailProcessed(
      String gmailId, String senderEmail, String stripeCustomerId, boolean tollPaid) {
    TollEmailMeta meta = new TollEmailMeta();
    meta.setId(UUID.randomUUID());
    meta.setGmailId(gmailId);
    meta.setSenderEmail(senderEmail);
    meta.setStripeCustomerId(stripeCustomerId);
    meta.setTollPaid(tollPaid);
    meta.setCreatedAt(Instant.now());
    tollEmailMetaDao.create(meta);
  }
}
