package com.tollbooth.gmail;

import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Label;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePartHeader;
import com.google.api.services.gmail.model.ModifyMessageRequest;
import com.tollbooth.config.GmailConfig;
import java.io.IOException;
import java.util.Base64;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class GmailService {

  private static final Logger logger = LogManager.getLogger(GmailService.class);

  @Autowired private GmailConfig gmailConfig;

  /**
   * Gets an authenticated Gmail client.
   *
   * @return Gmail client instance
   * @throws IOException if there's an error creating the client
   */
  public Gmail getGmailClient() throws IOException {
    return gmailConfig.getGmailClient();
  }

  /**
   * Ensures a label exists in Gmail, creating it if necessary.
   *
   * @param gmailClient The Gmail client instance
   * @param labelName The name of the label
   * @return The label ID, or null if creation failed
   * @throws IOException if there's an error with Gmail operations
   */
  public String ensureLabelExists(Gmail gmailClient, String labelName) throws IOException {
    String userEmail = gmailConfig.getGmailEmail();

    // Check if the label already exists
    List<Label> labels = gmailClient.users().labels().list(userEmail).execute().getLabels();

    for (Label label : labels) {
      if (labelName.equals(label.getName())) {
        return label.getId();
      }
    }

    // Label doesn't exist, create it
    logger.info("Creating label '{}' for user {}", labelName, userEmail);

    Label newLabel =
        new Label()
            .setName(labelName)
            .setLabelListVisibility("labelShow")
            .setMessageListVisibility("show");

    try {
      Label createdLabel = gmailClient.users().labels().create(userEmail, newLabel).execute();
      logger.info(
          "Successfully created label '{}' for user {} with ID {}",
          labelName,
          userEmail,
          createdLabel.getId());
      return createdLabel.getId();
    } catch (IOException e) {
      logger.error(
          "Failed to create label '{}' for user {}: {}", labelName, userEmail, e.getMessage(), e);
      return null;
    }
  }

  /**
   * Ensures the "Awaiting Toll" label exists.
   *
   * @param gmailClient The Gmail client instance
   * @return The label ID, or null if creation failed
   */
  public String ensureAwaitingTollLabelExists(Gmail gmailClient) {
    try {
      return ensureLabelExists(gmailClient, "Awaiting Toll");
    } catch (IOException e) {
      logger.error("Failed to ensure Awaiting Toll label exists: {}", e.getMessage(), e);
      return null;
    }
  }

  /**
   * Archives a message and adds a label to it.
   *
   * @param gmailClient The Gmail client instance
   * @param messageId The ID of the message
   * @param labelId The ID of the label to add
   * @throws IOException if there's an error modifying the message
   */
  public void archiveAndLabelMessage(Gmail gmailClient, String messageId, String labelId)
      throws IOException {
    String userEmail = gmailConfig.getGmailEmail();

    ModifyMessageRequest modifyRequest =
        new ModifyMessageRequest()
            .setAddLabelIds(List.of(labelId))
            .setRemoveLabelIds(List.of("INBOX"));

    gmailClient.users().messages().modify(userEmail, messageId, modifyRequest).execute();
  }

  /**
   * Adds a label to a message without archiving it.
   *
   * @param gmailClient The Gmail client instance
   * @param messageId The ID of the message to label
   * @param labelId The ID of the label to add
   * @throws IOException if there's an error modifying the message
   */
  public void labelMessage(Gmail gmailClient, String messageId, String labelId) throws IOException {
    String userEmail = gmailConfig.getGmailEmail();

    ModifyMessageRequest modifyRequest =
        new ModifyMessageRequest().setAddLabelIds(List.of(labelId));

    gmailClient.users().messages().modify(userEmail, messageId, modifyRequest).execute();
  }

  /**
   * Removes a label from a Gmail message.
   *
   * @param gmailClient The Gmail client instance
   * @param messageId The ID of the message
   * @param labelId The ID of the label to remove
   * @throws IOException if there's an error modifying the message
   */
  public void removeLabelFromMessage(Gmail gmailClient, String messageId, String labelId)
      throws IOException {
    String userEmail = gmailConfig.getGmailEmail();

    ModifyMessageRequest modifyRequest =
        new ModifyMessageRequest().setRemoveLabelIds(List.of(labelId));

    gmailClient.users().messages().modify(userEmail, messageId, modifyRequest).execute();
  }

  /**
   * Unarchives a message by adding it back to the inbox.
   *
   * @param gmailClient The Gmail client instance
   * @param messageId The ID of the message
   * @throws IOException if there's an error modifying the message
   */
  public void unarchiveMessage(Gmail gmailClient, String messageId) throws IOException {
    String userEmail = gmailConfig.getGmailEmail();

    ModifyMessageRequest modifyRequest =
        new ModifyMessageRequest().setAddLabelIds(List.of("INBOX"));

    gmailClient.users().messages().modify(userEmail, messageId, modifyRequest).execute();
  }

  /**
   * Moves an email to the inbox, removes the awaiting toll label, and adds the toll paid label.
   *
   * @param gmailClient The Gmail client instance
   * @param messageId The ID of the message
   * @param awaitingPaymentLabelId The ID of the "Awaiting Toll" label
   */
  public void moveAndUnlabelMessage(
      Gmail gmailClient, String messageId, String awaitingPaymentLabelId) {
    try {
      unarchiveMessage(gmailClient, messageId);
      removeLabelFromMessage(gmailClient, messageId, awaitingPaymentLabelId);
      String tollPaidLabelId = ensureLabelExists(gmailClient, "Toll Paid");
      if (tollPaidLabelId != null) {
        labelMessage(gmailClient, messageId, tollPaidLabelId);
      }
    } catch (IOException e) {
      logger.error("Error moving and unlabeling message {}: {}", messageId, e.getMessage(), e);
    }
  }

  /**
   * Extracts the sender email address from a Gmail message.
   *
   * @param message The Gmail message
   * @return The sender email address, or null if not found
   */
  public String extractSenderEmail(Message message) {
    if (message.getPayload() == null || message.getPayload().getHeaders() == null) {
      return null;
    }

    for (MessagePartHeader header : message.getPayload().getHeaders()) {
      if ("From".equalsIgnoreCase(header.getName())) {
        String fromValue = header.getValue();
        if (fromValue != null) {
          // Extract email from "Name <email@domain.com>" format
          if (fromValue.contains("<") && fromValue.contains(">")) {
            int startIndex = fromValue.indexOf('<') + 1;
            int endIndex = fromValue.indexOf('>');
            if (startIndex > 0 && endIndex > startIndex) {
              return fromValue.substring(startIndex, endIndex).trim();
            }
          } else {
            // Handle case where it's just the email address
            return fromValue.trim();
          }
        }
      }
    }
    return null;
  }

  /**
   * Sends an email via Gmail API.
   *
   * @param toEmail The recipient's email address
   * @param subject The email subject
   * @param body The email body (HTML)
   * @throws IOException if there's an error sending the email
   */
  public void sendEmail(Gmail gmailClient, String toEmail, String subject, String body)
      throws IOException {
    String fromEmail = gmailConfig.getGmailEmail();

    // Create raw email message in RFC 2822 format
    String rawEmail = createRawEmailMessage(fromEmail, toEmail, subject, body);

    // Encode the raw email message to base64url
    String encodedEmail = Base64.getUrlEncoder().encodeToString(rawEmail.getBytes());

    // Create Gmail Message object
    Message message = new Message();
    message.setRaw(encodedEmail);

    // Send the message via Gmail API
    gmailClient.users().messages().send(fromEmail, message).execute();

    logger.info("Successfully sent email via Gmail API from {} to {}", fromEmail, toEmail);
  }

  /**
   * Creates a raw email message in RFC 2822 format.
   *
   * @param fromEmail The sender's email address
   * @param toEmail The recipient's email address
   * @param subject The email subject
   * @param body The email body (HTML)
   * @return The raw email message as a string
   */
  private String createRawEmailMessage(
      String fromEmail, String toEmail, String subject, String body) {
    StringBuilder rawMessage = new StringBuilder();

    rawMessage.append("From: ").append(fromEmail).append("\r\n");
    rawMessage.append("To: ").append(toEmail).append("\r\n");
    rawMessage.append("Subject: ").append(subject).append("\r\n");
    rawMessage.append("MIME-Version: 1.0\r\n");
    rawMessage.append("Content-Type: text/html; charset=utf-8\r\n");
    rawMessage.append("\r\n");
    rawMessage.append(body);

    return rawMessage.toString();
  }

  /**
   * Fetches a full message by ID.
   *
   * @param gmailClient The Gmail client instance
   * @param messageId The message ID
   * @return The full message
   * @throws IOException if there's an error fetching the message
   */
  public Message getMessage(Gmail gmailClient, String messageId) throws IOException {
    String userEmail = gmailConfig.getGmailEmail();
    return gmailClient.users().messages().get(userEmail, messageId).execute();
  }
}
