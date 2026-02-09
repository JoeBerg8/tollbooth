package com.tollbooth.gmail;

import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;
import com.tollbooth.config.GmailConfig;
import com.tollbooth.toll.TollService;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class GmailPollingTask {

  private static final Logger logger = LogManager.getLogger(GmailPollingTask.class);

  private final AtomicBoolean isProcessing = new AtomicBoolean(false);
  private Instant lastRunAt = null;

  @Autowired private GmailConfig gmailConfig;

  @Autowired private GmailService gmailService;

  @Autowired private TollService tollService;

  /**
   * Scheduled task that runs every minute to poll Gmail for new emails and process them for toll
   * payment. Uses a flag to prevent concurrent executions.
   */
  @Scheduled(cron = "0 * * * * *") // Run every minute at the start of the minute
  public void pollGmail() {
    // Check if already processing
    if (!isProcessing.compareAndSet(false, true)) {
      logger.warn("Gmail polling is already running, skipping this execution");
      return;
    }

    try {
      logger.debug("Starting Gmail polling task");

      Gmail gmailClient = gmailConfig.getGmailClient();
      String userEmail = gmailConfig.getGmailEmail();

      // Build date query
      String dateQuery;
      if (lastRunAt == null) {
        // If no previous run, get emails from the last 48 hours
        Instant fortyEightHoursAgo = Instant.now().minus(48, java.time.temporal.ChronoUnit.HOURS);
        long epochSeconds = fortyEightHoursAgo.getEpochSecond();
        dateQuery = "after:" + epochSeconds;
      } else {
        // Use lastRunAt minus a 5-minute buffer to account for race conditions and Gmail
        // API
        // delays
        Instant queryTime = lastRunAt.minus(5, java.time.temporal.ChronoUnit.MINUTES);
        dateQuery = "after:" + queryTime.getEpochSecond();
      }

      // Query for new messages (exclude sent folder)
      String finalQuery = String.format("-in:sent %s", dateQuery);
      Gmail.Users.Messages.List request =
          gmailClient.users().messages().list(userEmail).setMaxResults(500L).setQ(finalQuery);

      ListMessagesResponse response = request.execute();
      List<Message> messages = response.getMessages();

      if (messages == null || messages.isEmpty()) {
        logger.debug("No new messages found");
        lastRunAt = Instant.now();
        return;
      }

      logger.info("Found {} new messages to process", messages.size());

      // Process each message
      for (Message messageSummary : messages) {
        try {
          String messageId = messageSummary.getId();
          Message fullMessage = gmailService.getMessage(gmailClient, messageId);
          tollService.processEmail(gmailClient, messageId, fullMessage);
        } catch (Exception e) {
          logger.error(
              "Error processing message {}: {}", messageSummary.getId(), e.getMessage(), e);
          // Continue processing other messages even if one fails
        }
      }

      lastRunAt = Instant.now();
      logger.info("Completed Gmail polling task, processed {} messages", messages.size());

    } catch (IOException e) {
      logger.error("Error polling Gmail: {}", e.getMessage(), e);
    } catch (Exception e) {
      logger.error("Unexpected error in Gmail polling task", e);
    } finally {
      // Always reset the flag
      isProcessing.set(false);
    }
  }
}
