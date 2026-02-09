package com.tollbooth.query;

import com.google.common.base.Preconditions;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/** A subclass of Query used to build and execute SQL INSERT statements. */
public class Insert extends Query<Insert> {

  private String tableName;
  private String columns;
  private String values;

  /**
   * Create a new Insert.
   *
   * @param jdbcTemplate a JdbcTemplate with which to execute the query.
   */
  public Insert(NamedParameterJdbcTemplate jdbcTemplate) {
    super(jdbcTemplate);
  }

  /**
   * Set the INSERT INTO table name to the given tableName.
   *
   * @param tableName the table name
   * @return this
   */
  public Insert insertInto(String tableName) {
    this.tableName = tableName;
    return self();
  }

  /**
   * Set the INSERT columns to the given columns.
   *
   * @param columns the comma-seperated columns string (without the parentheses)
   * @return this
   */
  public Insert columns(String columns) {
    this.columns = columns;
    return self();
  }

  /**
   * Set the INSERT values to the given values.
   *
   * @param values the comma-separated values string (without the parentheses)
   * @return this
   */
  public Insert values(String values) {
    this.values = values;
    return self();
  }

  @Override
  public String toSql() {
    Preconditions.checkState(
        tableName != null && columns != null && values != null,
        "Must call insertInto(), columns(), and values() in an Insert.");
    Preconditions.checkState(
        toSqlWhereHelper().isEmpty(), "Cannot call where() or and() in an Insert.");
    return String.format("INSERT INTO %s (%s)%nVALUES (%s)%n", tableName, columns, values);
  }

  @Override
  Insert self() {
    return this;
  }
}
