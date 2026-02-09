package com.tollbooth.toll;

import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePartHeader;
import com.tollbooth.config.GmailConfig;
import com.tollbooth.config.TollProperties;
import java.io.IOException;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class WhitelistService {

  private static final Logger logger = LogManager.getLogger(WhitelistService.class);

  @Autowired private GmailConfig gmailConfig;

  @Autowired private TollProperties tollProperties;

  /**
   * Checks if a sender is whitelisted based on four exemption rules: 1. Hosted domain match (same
   * Google Workspace domain) 2. Trusted domains list 3. CC'd recipients from trusted domains 4.
   * Known sender via sent-folder check
   *
   * @param gmailClient The Gmail client instance
   * @param senderEmail The sender's email address
   * @param fullMessage The full message object
   * @return True if sender is whitelisted, false otherwise
   */
  public boolean isSenderWhitelisted(Gmail gmailClient, String senderEmail, Message fullMessage) {
    logger.debug("Checking if sender {} is whitelisted", senderEmail);
    try {
      // Rule 1: Check if the sender's domain matches the user's hosted domain
      if (checkHostedDomain(senderEmail)) {
        logger.debug("Sender {} is whitelisted (hosted domain match)", senderEmail);
        return true;
      }

      // Rule 2: Check if sender's domain is in trusted domains list
      if (isTrustedDomain(senderEmail)) {
        logger.debug("Sender {} is whitelisted (trusted domain)", senderEmail);
        return true;
      }

      // Rule 3: Check if any TO/CC recipients are from trusted domains
      if (hasRecipientsFromTrustedDomains(fullMessage)) {
        logger.debug("Sender {} is whitelisted (recipient from trusted domain)", senderEmail);
        return true;
      }

      // Rule 4: Check if user has previously sent an email to this sender
      if (isKnownSender(gmailClient, senderEmail)) {
        logger.debug("Sender {} is whitelisted (known sender)", senderEmail);
        return true;
      }

      return false;

    } catch (IOException e) {
      logger.error(
          "Error checking if sender {} is whitelisted: {}", senderEmail, e.getMessage(), e);
      // In case of error, assume sender is not whitelisted to be safe
      return false;
    }
  }

  /**
   * Rule 1: Checks if the sender's domain matches the user's email domain (same Google Workspace).
   *
   * @param senderEmail The sender's email address
   * @return True if domains match, false otherwise
   */
  private boolean checkHostedDomain(String senderEmail) {
    String userEmail = gmailConfig.getGmailEmail();
    String userDomain = extractDomain(userEmail);
    String senderDomain = extractDomain(senderEmail);

    if (userDomain != null && senderDomain != null) {
      return userDomain.equalsIgnoreCase(senderDomain);
    }
    return false;
  }

  /**
   * Rule 2: Checks if the sender's domain is in the trusted domains list.
   *
   * @param senderEmail The sender's email address
   * @return True if the sender's domain is trusted, false otherwise
   */
  public boolean isTrustedDomain(String senderEmail) {
    if (senderEmail == null || senderEmail.isEmpty()) {
      return false;
    }

    List<String> trustedDomains = tollProperties.getTrustedDomains();
    if (trustedDomains == null || trustedDomains.isEmpty()) {
      return false;
    }

    String senderDomain = extractDomain(senderEmail);
    if (senderDomain == null) {
      return false;
    }

    // Case-insensitive domain comparison
    return trustedDomains.stream()
        .anyMatch(trustedDomain -> trustedDomain.equalsIgnoreCase(senderDomain));
  }

  /**
   * Rule 3: Checks if any TO/CC recipients in the message are from trusted domains. This handles
   * the introduction use case where an unknown sender reaches out and includes a trusted contact in
   * the TO/CC fields.
   *
   * @param fullMessage The full message object
   * @return True if any TO/CC recipient is from a trusted domain, false otherwise
   */
  private boolean hasRecipientsFromTrustedDomains(Message fullMessage) {
    if (fullMessage.getPayload() == null || fullMessage.getPayload().getHeaders() == null) {
      return false;
    }

    for (MessagePartHeader header : fullMessage.getPayload().getHeaders()) {
      if ("To".equalsIgnoreCase(header.getName()) || "Cc".equalsIgnoreCase(header.getName())) {
        String headerValue = header.getValue();
        if (headerValue != null) {
          // Extract email addresses from "Name <email@domain.com>" or just
          // "email@domain.com"
          String[] addresses = headerValue.split(",");
          for (String address : addresses) {
            address = address.trim();
            String recipientEmail = null;

            if (address.contains("<") && address.contains(">")) {
              int startIndex = address.indexOf('<') + 1;
              int endIndex = address.indexOf('>');
              if (startIndex > 0 && endIndex > startIndex) {
                recipientEmail = address.substring(startIndex, endIndex).trim();
              }
            } else {
              recipientEmail = address.trim();
            }

            if (recipientEmail != null && isTrustedDomain(recipientEmail)) {
              logger.debug(
                  "Recipient {} is from a trusted domain, whitelisting sender", recipientEmail);
              return true;
            }
          }
        }
      }
    }
    return false;
  }

  /**
   * Rule 4: Checks if the user has previously sent an email to this sender by searching the sent
   * folder. Excludes automated emails with "[jmc]" in the subject.
   *
   * @param gmailClient The Gmail client instance
   * @param senderEmail The sender's email address
   * @return True if user has sent emails to this sender before, false otherwise
   * @throws IOException if there's an error querying Gmail
   */
  private boolean isKnownSender(Gmail gmailClient, String senderEmail) throws IOException {
    String userEmail = gmailConfig.getGmailEmail();

    // Search for messages in the sent folder that were sent to the sender's email address
    // Exclude automated emails with "[jmc]" in the subject
    String query = "in:sent to:" + senderEmail + " -subject:\"[jmc]\"";

    Gmail.Users.Messages.List request =
        gmailClient
            .users()
            .messages()
            .list(userEmail)
            .setQ(query)
            .setMaxResults(1L); // We only need to know if at least one exists

    ListMessagesResponse response = request.execute();

    // If we find any messages, it means the user has sent emails to this sender before
    boolean hasContact = response.getMessages() != null && !response.getMessages().isEmpty();

    logger.debug(
        "Checked if sender {} is a known sender (sent folder): {}", senderEmail, hasContact);

    return hasContact;
  }

  /**
   * Extracts the domain from an email address.
   *
   * @param email The email address to extract the domain from
   * @return The domain of the email address, or null if the email is invalid
   */
  public String extractDomain(String email) {
    if (StringUtils.isBlank(email)) {
      return null;
    }
    int atIndex = email.lastIndexOf('@');
    if (atIndex > 0 && atIndex < email.length() - 1) {
      return email.substring(atIndex + 1).toLowerCase();
    }
    return null;
  }
}
