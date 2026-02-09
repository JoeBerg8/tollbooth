package com.tollbooth.query;

import com.google.common.base.Preconditions;
import java.util.List;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/** A subclass of Query used to build and execute SQL UPDATE statements. */
public class Update extends Query<Update> {

  private String tableName;
  private String set;

  /**
   * Create a new Update.
   *
   * @param jdbcTemplate a JdbcTemplate with which to execute the query.
   */
  public Update(NamedParameterJdbcTemplate jdbcTemplate) {
    super(jdbcTemplate);
  }

  public Update update(String tableName) {
    this.tableName = tableName;
    return self();
  }

  public Update set(String set) {
    this.set = set;
    return self();
  }

  @Override
  public String toSql() {
    Preconditions.checkState(
        tableName != null && set != null, "Must call update() and set() in an Update.");
    List<String> whereClauses = toSqlWhereHelper();
    String where =
        whereClauses.isEmpty() ? "" : String.join(lineSeparator, whereClauses) + lineSeparator;
    return String.format("UPDATE %s%nSET %s%n%s", tableName, set, where);
  }

  @Override
  Update self() {
    return this;
  }
}
