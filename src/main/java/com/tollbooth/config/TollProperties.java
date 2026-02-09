package com.tollbooth.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "toll")
public class TollProperties {

  private String gmailEmail;
  private double tollAmount = 0.25;
  private List<String> trustedDomains = new ArrayList<>();
  private int pollIntervalSeconds = 60;
  private String successUrl;
  private String cancelUrl;
  private String emailSubject = "Payment required to reach my inbox";
  private String emailBody;
  private String emailFromName;

  public String getGmailEmail() {
    return gmailEmail;
  }

  public void setGmailEmail(String gmailEmail) {
    this.gmailEmail = gmailEmail;
  }

  public double getTollAmount() {
    return tollAmount;
  }

  public void setTollAmount(double tollAmount) {
    this.tollAmount = tollAmount;
  }

  public List<String> getTrustedDomains() {
    return trustedDomains;
  }

  public void setTrustedDomains(List<String> trustedDomains) {
    this.trustedDomains = trustedDomains;
  }

  public void setTrustedDomains(String trustedDomains) {
    if (trustedDomains != null && !trustedDomains.trim().isEmpty()) {
      this.trustedDomains =
          Arrays.stream(trustedDomains.split(","))
              .map(String::trim)
              .filter(s -> !s.isEmpty())
              .toList();
    }
  }

  public int getPollIntervalSeconds() {
    return pollIntervalSeconds;
  }

  public void setPollIntervalSeconds(int pollIntervalSeconds) {
    this.pollIntervalSeconds = pollIntervalSeconds;
  }

  public String getSuccessUrl() {
    return successUrl;
  }

  public void setSuccessUrl(String successUrl) {
    this.successUrl = successUrl;
  }

  public String getCancelUrl() {
    return cancelUrl;
  }

  public void setCancelUrl(String cancelUrl) {
    this.cancelUrl = cancelUrl;
  }

  public String getEmailSubject() {
    return emailSubject;
  }

  public void setEmailSubject(String emailSubject) {
    this.emailSubject = emailSubject;
  }

  public String getEmailBody() {
    return emailBody;
  }

  public void setEmailBody(String emailBody) {
    this.emailBody = emailBody;
  }

  public String getEmailFromName() {
    return emailFromName;
  }

  public void setEmailFromName(String emailFromName) {
    this.emailFromName = emailFromName;
  }
}
