package com.tollbooth.toll;

import com.tollbooth.config.TollProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TollEmailTemplateService {

  @Autowired private TollProperties tollProperties;

  /**
   * Renders the email subject template with placeholders replaced.
   *
   * @param tollAmount The toll amount
   * @return The rendered subject
   */
  public String renderSubject(double tollAmount) {
    String subject = tollProperties.getEmailSubject();
    if (subject == null || subject.isEmpty()) {
      subject = "Payment required to reach my inbox";
    }
    return subject.replace("{tollAmount}", String.format("%.2f", tollAmount));
  }

  /**
   * Renders the email body template with placeholders replaced.
   *
   * @param tollAmount The toll amount
   * @param paymentLink The Stripe payment link
   * @param senderEmail The sender's email address
   * @return The rendered body (HTML)
   */
  public String renderBody(double tollAmount, String paymentLink, String senderEmail) {
    String body = tollProperties.getEmailBody();
    if (body == null || body.isEmpty()) {
      // Default simple template
      body =
          "<p>A $"
              + String.format("%.2f", tollAmount)
              + " fee is required to deliver your message to my inbox.</p>"
              + "<p><a href=\""
              + paymentLink
              + "\">Add funds</a> to deliver this message.</p>";
    } else {
      // Replace placeholders
      body =
          body.replace("{tollAmount}", String.format("%.2f", tollAmount))
              .replace("{paymentLink}", paymentLink)
              .replace("{senderEmail}", senderEmail != null ? senderEmail : "");
    }
    return body;
  }

  /**
   * Gets the display name for sent emails.
   *
   * @return The display name, or the email address if not configured
   */
  public String getFromName() {
    String fromName = tollProperties.getEmailFromName();
    if (fromName == null || fromName.isEmpty()) {
      return tollProperties.getGmailEmail();
    }
    return fromName;
  }
}
