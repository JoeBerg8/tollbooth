package com.tollbooth.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "toll")
@Getter
@Setter
public class TollProperties {

  private String gmailEmail;
  private double tollAmount = 0.25;

  @Setter(lombok.AccessLevel.NONE)
  private List<String> trustedDomains = new ArrayList<>();

  private int pollIntervalSeconds = 60;
  private String successUrl;
  private String cancelUrl;
  private String emailSubject = "Payment required to reach my inbox";
  private String emailBody;
  private String emailFromName;
  private boolean dryRun = true;

  public void setTrustedDomains(List<String> trustedDomains) {
    this.trustedDomains = trustedDomains != null ? trustedDomains : new ArrayList<>();
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
}
