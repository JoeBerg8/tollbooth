package com.tollbooth.toll;

import com.tollbooth.persistence.AbstractCrudDao;
import com.tollbooth.query.AbstractRowMapper;
import com.tollbooth.query.Dao;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Component;

@Component
@Dao
public class TollEmailMetaDao extends AbstractCrudDao<TollEmailMeta, UUID> {

  @Override
  protected String tableName() {
    return "toll_email_meta";
  }

  @Override
  protected String tableAlias() {
    return null;
  }

  @Override
  protected String selectColumns() {
    return "id, gmail_id, sender_email, toll_paid, stripe_customer_id, created_at";
  }

  @Override
  protected RowMapper<TollEmailMeta> rowMapper() {
    return new Mapper();
  }

  static class Mapper extends AbstractRowMapper<TollEmailMeta> {
    @Override
    public TollEmailMeta mapRow(SqlRowSet rs) {
      return TollEmailMeta.builder()
          .id(getUuid(rs, "id"))
          .gmailId(rs.getString("gmail_id"))
          .senderEmail(rs.getString("sender_email"))
          .tollPaid(rs.getBoolean("toll_paid"))
          .stripeCustomerId(rs.getString("stripe_customer_id"))
          .createdAt(getInstant(rs, "created_at"))
          .build();
    }
  }

  @Override
  protected List<Pair<String, String>> updateCols() {
    return List.of(
        Pair.of("gmail_id", ":gmailId"),
        Pair.of("sender_email", ":senderEmail"),
        Pair.of("toll_paid", ":tollPaid"),
        Pair.of("stripe_customer_id", ":stripeCustomerId"),
        Pair.of("created_at", ":createdAt"));
  }

  @Override
  protected List<Pair<String, String>> insertCols() {
    return List.of(
        Pair.of("id", ":id"),
        Pair.of("gmail_id", ":gmailId"),
        Pair.of("sender_email", ":senderEmail"),
        Pair.of("toll_paid", ":tollPaid"),
        Pair.of("stripe_customer_id", ":stripeCustomerId"),
        Pair.of("created_at", ":createdAt"));
  }

  /**
   * Checks if an email has already been processed by looking up its Gmail ID.
   *
   * @param gmailId The Gmail message ID
   * @return True if the email has been processed, false otherwise
   */
  public boolean isEmailAlreadyProcessed(String gmailId) {
    Optional<TollEmailMeta> existing =
        select(selectColumns())
            .from(tableName())
            .where("gmail_id = :gmailId")
            .parameters("gmailId", gmailId)
            .queryForSingle(rowMapper());

    return existing.isPresent();
  }

  /**
   * Finds an email meta record by Gmail ID.
   *
   * @param gmailId The Gmail message ID
   * @return Optional of TollEmailMeta if found
   */
  public Optional<TollEmailMeta> findByGmailId(String gmailId) {
    return select(selectColumns())
        .from(tableName())
        .where("gmail_id = :gmailId")
        .parameters("gmailId", gmailId)
        .queryForSingle(rowMapper());
  }
}
