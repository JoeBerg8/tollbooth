package com.tollbooth.query;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/** A subclass of Query used to build and execute SQL SELECT queries. */
public class Select extends Query<Select> {

  private String select;
  private String from;
  private List<String> joins = new ArrayList<>();
  private String orderBy;
  private String groupBy;
  private String having;
  private Integer limit;
  private Integer offset;

  /**
   * Create a new Select.
   *
   * @param jdbcTemplate a JdbcTemplate with which to execute the query.
   */
  public Select(NamedParameterJdbcTemplate jdbcTemplate) {
    super(jdbcTemplate);
  }

  /**
   * Set the SELECT clause to the given select.
   *
   * @param select the select columns, e.g. "object.foo, object.bar"
   * @return this
   */
  public Select select(String select) {
    this.select = select;
    return self();
  }

  /**
   * Set the FROM clause to the given from.
   *
   * @param from the from clause, e.g. "objects AS object"
   * @return this
   */
  public Select from(String from) {
    this.from = from;
    return self();
  }

  /**
   * Add the given joinCondition as a JOIN to the FROM clause.
   *
   * @param joinCondition the join condition, e.g. "foos as foo ON foo.id = bar.id"
   * @return this
   */
  public Select join(String joinCondition) {
    return joinHelper("JOIN", joinCondition);
  }

  /**
   * Add the given joinCondition as a LEFT JOIN to the FROM clause.
   *
   * @param joinCondition the join condition, e.g. "foos as foo ON foo.id = bar.id"
   * @return this
   */
  public Select leftJoin(String joinCondition) {
    return joinHelper("LEFT JOIN", joinCondition);
  }

  private Select joinHelper(String joinType, String joinCondition) {
    Preconditions.checkState(!StringUtils.isEmpty(from), "Calling join() before from().");
    this.joins.add(joinType + " " + joinCondition);
    return self();
  }

  /**
   * Set the GROUP BY clause to the given groupBy.
   *
   * @param groupBy - the group by clauses
   * @return this
   */
  public Select groupBy(String groupBy) {
    this.groupBy = groupBy;
    return self();
  }

  /**
   * Set the ORDER BY clause to the given orderBy.
   *
   * @param orderBy the order by clauses, e.g. "object.foo DESC"
   * @return this
   */
  public Select orderBy(String orderBy) {
    this.orderBy = orderBy;
    return self();
  }

  /**
   * Set the LIMIT clause to the given limit.
   *
   * @param limit the limit amount
   * @return this
   */
  public Select limit(Integer limit) {
    this.limit = limit;
    return self();
  }

  /**
   * Set the OFFSET clause to the given offset.
   *
   * @param offset the offset amount
   * @return this
   */
  public Select offset(Integer offset) {
    this.offset = offset;
    return self();
  }

  @Override
  public String toSql() {
    return toSqlHelper(false);
  }

  /**
   * Reply the Select as a COUNT query.
   *
   * @return the SQL count(*) query
   */
  public String toCountSql() {
    return toSqlHelper(true);
  }

  /**
   * Return the Select's SQL query string.
   *
   * @param count if true, generate a count(*) variation on this query
   * @return the query string
   */
  private String toSqlHelper(boolean count) {
    if (!count) {
      Preconditions.checkState(
          select != null, "Must call select() in a Select, unless you're making a count query.");
    }
    Preconditions.checkState(from != null, "Must call from() in a Select.");
    List<String> clauses = new ArrayList<>();
    clauses.add("SELECT " + (count ? "COUNT(*)" : select));
    clauses.add("FROM " + from);
    clauses.addAll(joins);
    clauses.addAll(toSqlWhereHelper());
    if (!count) {
      if (StringUtils.isNotEmpty(groupBy)) {
        clauses.add("GROUP BY " + groupBy);
      }
      if (StringUtils.isNotEmpty(having)) {
        clauses.add("HAVING " + having);
      }
      if (StringUtils.isNotEmpty(orderBy)) {
        clauses.add("ORDER BY " + orderBy);
      }
      if (limit != null) {
        clauses.add("LIMIT " + limit);
      }
      if (offset != null) {
        clauses.add("OFFSET " + offset);
      }
    }
    return String.join(lineSeparator, clauses) + lineSeparator;
  }

  @Override
  public void run() {
    throw new AssertionError("Select does not support the run() method");
  }

  @Override
  Select self() {
    return this;
  }

  /**
   * Execute this Select as a count query, i.e. one that just returns the count of the rows that
   * would be returned by the regular query.
   *
   * @return the count
   */
  public Integer queryForCount() {
    return jdbcTemplate.queryForObject(toCountSql(), parameters, Integer.class);
  }

  /**
   * Execute this Select query and return an Optional of a single result, mapped with the given
   * rowMapper.
   *
   * @param rowMapper the row mapper
   * @param <T> the resulting type
   * @return the optional result
   */
  public <T> Optional<T> queryForSingle(RowMapper<T> rowMapper) {
    var result = queryForList(rowMapper);
    return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
  }

  /**
   * Execute this Select query and return a list of result, mapped with the given rowMapper.
   *
   * @param rowMapper the row mapper
   * @param <T> the resulting type
   * @return the list of results
   */
  public <T> List<T> queryForList(RowMapper<T> rowMapper) {
    return jdbcTemplate.query(toSql(), parameters, rowMapper);
  }
}
