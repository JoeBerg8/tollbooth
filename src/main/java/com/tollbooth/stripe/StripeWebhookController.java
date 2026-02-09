package com.tollbooth.stripe;

import com.google.api.services.gmail.Gmail;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.tollbooth.config.GmailConfig;
import com.tollbooth.config.StripeConfig;
import com.tollbooth.toll.TollService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/webhook/stripe")
public class StripeWebhookController {

  private static final Logger logger = LogManager.getLogger(StripeWebhookController.class);

  @Autowired private HttpServletRequest httpServletRequest;

  @Autowired private StripeConfig stripeConfig;

  @Autowired private StripeService stripeService;

  @Autowired private TollService tollService;

  @Autowired private GmailConfig gmailConfig;

  /**
   * Webhook endpoint for Stripe events. Handles checkout.session.completed for toll top-ups.
   *
   * @param payload The raw webhook payload
   * @return HTTP response
   */
  @PostMapping
  public ResponseEntity<String> handleStripeWebhook(@RequestBody String payload) {
    try {
      String sigHeader = httpServletRequest.getHeader("Stripe-Signature");
      Event event = Webhook.constructEvent(payload, sigHeader, stripeConfig.getWebhookSecret());

      logger.info("Processing Stripe webhook event: {} ({})", event.getType(), event.getId());

      if ("checkout.session.completed".equals(event.getType())) {
        handleCheckoutSessionCompleted(event);
      } else {
        logger.debug("Unhandled Stripe event type: {}", event.getType());
      }

      return ResponseEntity.ok("Webhook processed successfully");

    } catch (SignatureVerificationException e) {
      logger.error("Stripe webhook signature verification failed: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature");
    } catch (Exception e) {
      logger.error("Error processing Stripe webhook: {}", e.getMessage(), e);
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Webhook error: " + e.getMessage());
    }
  }

  /**
   * Handles checkout session completed events for toll top-ups.
   *
   * @param event The Stripe event
   */
  private void handleCheckoutSessionCompleted(Event event) {
    StripeObject stripeObject = event.getDataObjectDeserializer().getObject().orElse(null);

    if (!(stripeObject instanceof Session)) {
      logger.error(
          "Expected Session object for checkout.session.completed event but got {}",
          stripeObject != null ? stripeObject.getClass().getName() : "null");
      return;
    }

    Session session = (Session) stripeObject;
    Map<String, String> metadata = session.getMetadata();

    if (metadata == null) {
      logger.warn("No metadata found in checkout session for session {}", session.getId());
      return;
    }

    // Check if this is a balance top-up
    if (!"inbox_toll_topup".equals(metadata.get("sessionType"))) {
      logger.debug("Not a toll top-up session, skipping");
      return;
    }

    try {
      String senderCustomerId = metadata.get("senderCustomerId");
      String senderEmail = metadata.get("senderEmail");
      String messageId = metadata.get("messageId");
      String tollAmountAtTopUp = metadata.get("tollAmountAtTopUp");

      if (senderCustomerId == null || messageId == null || tollAmountAtTopUp == null) {
        logger.warn("Missing required metadata for balance top-up session {}", session.getId());
        return;
      }

      // Get the top-up amount from the session (in cents)
      long grossAmountCents = session.getAmountTotal();

      // Calculate Stripe fee (2.9% + $0.30)
      long fee = Math.round(grossAmountCents * 0.029) + 30;

      // Calculate net amount in cents
      long netAmountCents = grossAmountCents - fee;

      // Credit the sender's balance with the net amount
      boolean creditSuccessful =
          stripeService.creditSenderBalance(
              senderCustomerId, netAmountCents, "Balance top-up from session " + session.getId());

      if (creditSuccessful) {
        double netAmountDollars = netAmountCents / 100.0;
        logger.info(
            "Successfully credited ${} (net) to sender {} balance from session {}",
            netAmountDollars,
            senderEmail,
            session.getId());

        // Now try to process the original toll payment
        double tollAmount = Double.parseDouble(tollAmountAtTopUp);
        Gmail gmailClient = gmailConfig.getGmailClient();
        tollService.processTollPaymentAfterTopUp(gmailClient, messageId, tollAmount);

      } else {
        logger.error(
            "Failed to credit balance for sender {} from session {}", senderEmail, session.getId());
      }

    } catch (Exception e) {
      logger.error("Error processing balance top-up completion: {}", e.getMessage(), e);
    }
  }
}
