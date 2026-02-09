package com.tollbooth.toll;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.tollbooth.dto.Copyable;
import com.tollbooth.dto.Identifiable;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"createdAt"})
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties({"createdAt"})
public class TollEmailMeta implements Identifiable<UUID>, Copyable<TollEmailMeta> {

  private UUID id;
  private String gmailId;
  private String senderEmail;
  private boolean tollPaid;
  private String stripeCustomerId;
  private Instant createdAt;

  @Override
  public TollEmailMeta deepCopy() {
    return this.toBuilder().build();
  }
}
