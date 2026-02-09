package com.tollbooth;

import com.tollbooth.toll.TollEmailMeta;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

public class Faker extends net.datafaker.Faker {

  public TollEmailMeta tollEmailMeta() {
    return TollEmailMeta.builder()
        .id(UUID.randomUUID())
        .gmailId(this.internet().uuid())
        .senderEmail(this.internet().emailAddress())
        .tollPaid(this.bool().bool())
        .stripeCustomerId(this.internet().uuid())
        .createdAt(Instant.now().truncatedTo(ChronoUnit.MILLIS))
        .build();
  }
}
