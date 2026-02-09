package com.tollbooth.config;

import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StripeConfig {

  @Value("${stripe.api-key}")
  private String apiKey;

  @Value("${stripe.webhook-secret}")
  private String webhookSecret;

  @PostConstruct
  public void init() {
    Stripe.apiKey = apiKey;
  }

  public String getWebhookSecret() {
    return webhookSecret;
  }
}
