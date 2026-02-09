package com.tollbooth.query;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/**
 * A class that assists in performing common queries. It provides a builder pattern that allows for
 * constructing queries from their core components: select, from, where, orderBy, limit, offset, and
 * parameters.
 *
 * @param <T> the query type
 */
public abstract class Query<T extends Query<T>> {

  static final String lineSeparator = System.getProperty("line.separator");

  final NamedParameterJdbcTemplate jdbcTemplate;
  Parameters parameters = new Parameters();
  private boolean parametersInitialized = false;
  private String where;
  private List<String> ands = new ArrayList<>();
  private List<String> ors = new ArrayList<>();

  /**
   * Create a new Query.
   *
   * @param jdbcTemplate a JdbcTemplate to execute the query with.
   */
  public Query(NamedParameterJdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  /**
   * Set the WHERE clause to the given String-like thing.
   *
   * @param where the where clause, e.g. "object.foo = :foo"
   * @return this
   */
  public T where(Object where) {
    this.where = where.toString();
    return self();
  }

  /**
   * Extend the WHERE clause with an AND condition.
   *
   * @param and the and condition, e.g. "object.bar = :bar"
   * @return this
   */
  public T and(Object and) {
    this.ands.add(and.toString());
    return self();
  }

  /**
   * Extend the WHERE clause with an OR condition.
   *
   * @param or the or condition, e.g. "object.bar = :bar"
   * @return this
   */
  public T or(Object or) {
    this.ors.add(or.toString());
    return self();
  }

  /**
   * Set the SQL query parameters to the given parameters.
   *
   * @param parameters the parameters to be injected into prepared statement
   * @return this
   */
  public T parameters(Parameters parameters) {
    Preconditions.checkState(!parametersInitialized, "Clobbering previously set parameters.");
    this.parameters = parameters;
    parametersInitialized = true;
    return self();
  }

  /**
   * Set the SQL query parameters to the given object.
   *
   * @param object the object
   * @return this
   */
  public T parameters(Object object) {
    return parameters(new Parameters(object));
  }

  /**
   * Add the given entry to the SQL query parameters.
   *
   * @param k1 the entry key
   * @param v1 the entry value
   * @return this
   */
  public T parameters(String k1, Object v1) {
    this.parameters.add(k1, v1);
    parametersInitialized = true;
    return self();
  }

  /**
   * Add the two given entries to the SQL query parameters.
   *
   * @param k1 the first entry key
   * @param v1 the first entry value
   * @param k2 the second entry key
   * @param v2 the second entry value
   * @return this
   */
  public T parameters(String k1, Object v1, String k2, Object v2) {
    parameters(k1, v1);
    parameters(k2, v2);
    return self();
  }

  /**
   * Add the three given entries to the SQL query parameters.
   *
   * @param k1 the first entry key
   * @param v1 the first entry value
   * @param k2 the second entry key
   * @param v2 the second entry value
   * @param k3 the third entry key
   * @param v3 the third entry value
   * @return this
   */
  public T parameters(String k1, Object v1, String k2, Object v2, String k3, Object v3) {
    parameters(k1, v1);
    parameters(k2, v2);
    parameters(k3, v3);
    return self();
  }

  /**
   * Add the four given entries to the SQL query parameters.
   *
   * @param k1 the first entry key
   * @param v1 the first entry value
   * @param k2 the second entry key
   * @param v2 the second entry value
   * @param k3 the third entry key
   * @param v3 the third entry value
   * @param k4 the fourth entry key
   * @param v4 the fourth entry value
   * @return this
   */
  public T parameters(
      String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4) {
    parameters(k1, v1);
    parameters(k2, v2);
    parameters(k3, v3);
    parameters(k4, v4);
    return self();
  }

  /**
   * Add the entries of the given map to the SQL query parameters.
   *
   * @param map the map
   * @return this
   */
  public T parameters(Map<String, Object> map) {
    map.forEach(this::parameters);
    return self();
  }

  /**
   * Return the current set of Parameters.
   *
   * @return the Parameters
   */
  public Parameters getParameters() {
    return this.parameters;
  }

  /**
   * Return this Query's SQL query string.
   *
   * @return a query string representative of this instance of Query
   */
  public abstract String toSql();

  /**
   * Return this Query's SQL query.
   *
   * @return the query
   */
  @Override
  public String toString() {
    return toSql();
  }

  /**
   * Return the Query's where clause as a List of String, each containing one SQL link.
   *
   * @return the where clause, with the WHERE and each AND on a separate line.
   */
  List<String> toSqlWhereHelper() {
    String where = this.where;
    List<String> ands = new ArrayList<>(this.ands);
    List<String> ors = new ArrayList<>(this.ors);
    List<String> clauses = new ArrayList<>();
    if (StringUtils.isEmpty(where) && !ands.isEmpty()) {
      where = ands.get(0);
      ands.remove(0);
    }
    if (StringUtils.isNotEmpty(where)) {
      clauses.add("WHERE " + (ands.isEmpty() ? where : "(" + where + ")"));
    }
    ands.forEach(and -> clauses.add("AND (" + and + ")"));
    ors.forEach(and -> clauses.add("OR (" + and + ")"));
    return clauses;
  }

  /**
   * Reply the current object.
   *
   * @return the current object
   */
  abstract T self();

  /** Run the query (if its an {@link Insert}, {@link Update}, or {@link Delete}). */
  public void run() {
    jdbcTemplate.update(toSql(), parameters);
  }
}
