package com.tollbooth.query;

import com.google.common.base.Preconditions;
import java.util.List;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/** A subclass of Query used to build and execute SQL DELETE statements. */
public class Delete extends Query<Delete> {

  private String tableName;

  /**
   * Create a new Delete.
   *
   * @param jdbcTemplate a JdbcTemplate with which to execute the query.
   */
  public Delete(NamedParameterJdbcTemplate jdbcTemplate) {
    super(jdbcTemplate);
  }

  /**
   * Set the DELETE table name to the given tableName.
   *
   * @param tableName the table name
   * @return this
   */
  public Delete deleteFrom(String tableName) {
    this.tableName = tableName;
    return self();
  }

  @Override
  Delete self() {
    return this;
  }

  @Override
  public String toSql() {
    Preconditions.checkState(tableName != null, "Must call deleteFrom() in a Delete.");
    List<String> whereClauses = toSqlWhereHelper();
    String where =
        whereClauses.isEmpty() ? "" : String.join(lineSeparator, whereClauses) + lineSeparator;
    return String.format("DELETE FROM %s%n%s", tableName, where);
  }
}
