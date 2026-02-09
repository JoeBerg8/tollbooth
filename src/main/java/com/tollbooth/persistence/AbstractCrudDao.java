package com.tollbooth.persistence;

import static java.util.stream.Collectors.joining;

import com.google.common.base.Preconditions;
import com.tollbooth.dto.Identifiable;
import com.tollbooth.query.CrudDao;
import com.tollbooth.query.Insert;
import com.tollbooth.query.Parameters;
import com.tollbooth.query.Select;
import com.tollbooth.query.Update;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.jdbc.support.rowset.SqlRowSetMetaData;

/**
 * Simple implementation of {@link CrudDao}.
 *
 * @param <T> the type
 * @param <K> the key type
 */
public abstract class AbstractCrudDao<T extends Identifiable<K>, K> extends AbstractDao
    implements CrudDao<T, K> {

  // Abstract methods

  /**
   * The name of the table being operated on. E.g. "foos" -> "SELECT foo.a, foo.b FROM foos AS foo
   * ...".
   *
   * @return the table name
   */
  protected abstract String tableName();

  /**
   * The output name of the table operated on. E.g. "foo" -> "SELECT foo.a, foo.b FROM foos AS foo
   * ...".
   *
   * @return the output name, null if alias not used
   */
  protected abstract String tableAlias();

  /**
   * The column selection string used to {@link Select}. E.g. "foo.a, foo.b" -> "SELECT foo.a, foo.b
   * FROM foos AS foo ...". z
   *
   * @return the selection string
   */
  protected abstract String selectColumns();

  /**
   * Return a {@link RowMapper} to map records.
   *
   * @return the row mapper
   */
  protected abstract RowMapper<T> rowMapper();

  /**
   * Return a list of pairs used to set the columns and their values during {@link Update}. E.g.
   * List.of(Pair.of("id", ":id"), Pair.of("code", ":code")) -> "UPDATE foos as foo SET id = :id,
   * code = :code ..."
   *
   * @return a list of update columns and values
   */
  protected abstract List<Pair<String, String>> updateCols();

  /**
   * Return a list of pairs used to set the columns and their values during {@link Insert}. E.g.
   * List.of(Pair.of("id", ":id"), Pair.of("code", ":code")) -> "INSERT INTO foos as foo (id, code)
   * VALUES (:id, :code)".
   *
   * @return a list of insert columns and values
   */
  protected abstract List<Pair<String, String>> insertCols();

  // CrudDao impl

  @Override
  public Optional<T> find(K key) {
    var keyCondition = keyCondition(key);
    return select(selectColumns())
        .from(tableAs())
        .where(keyCondition.getLeft())
        .parameters(keyCondition.getRight())
        .queryForSingle(rowMapper());
  }

  @Override
  public void create(T entity) {
    var insertCols = insertCols();
    var columns = insertCols.stream().map(Pair::getLeft).collect(joining(","));
    var values = insertCols.stream().map(Pair::getRight).collect(joining(","));
    insertInto(tableAs())
        .columns(columns)
        .values(values)
        .parameters(createAndUpdateParameters(entity))
        .run();
  }

  @Override
  public void update(T entity) {
    var keyCondition = keyCondition(entity.getId());

    var updateColumns = getNonNullUpdateCols(entity);

    if (updateColumns.isEmpty()) {
      return;
    }

    var setClause =
        updateColumns.stream()
            .map(colPair -> String.format("%s = %s", colPair.getLeft(), colPair.getRight()))
            .collect(joining(","));
    update(tableAs())
        .set(setClause)
        .where(keyCondition.getLeft())
        .parameters(createAndUpdateParameters(entity))
        .parameters(keyCondition.getRight())
        .run();
  }

  @Override
  public void delete(K key) {
    var keyCondition = keyCondition(key);
    deleteFrom(tableAs()).where(keyCondition.getLeft()).parameters(keyCondition.getRight()).run();
  }

  // Helper methods

  /**
   * Filter update columns to only include non-null properties. Replaces leading colon or trailing
   * colons when checking parameter names.
   *
   * @param entity the entity with properties to update
   * @return a list of update columns and values for non-null properties
   */
  protected List<Pair<String, String>> getNonNullUpdateCols(T entity) {
    // Get all potential update columns
    List<Pair<String, String>> allCols = updateCols();

    // Create a Parameters object from the entity to check for property values
    BeanPropertySqlParameterSource beanParams = new BeanPropertySqlParameterSource(entity);

    return allCols.stream()
        .filter(
            colPair -> {
              String paramValue = colPair.getRight();
              // Explicitly check for now() function
              if (paramValue.equals("now()")) {
                return true;
              }
              String paramName = colPair.getRight().replaceAll("^:", "").replaceAll("::.*$", "");
              try {
                // Check if the property exists and is not null
                return beanParams.hasValue(paramName) && beanParams.getValue(paramName) != null;
              } catch (IllegalArgumentException e) {
                // Parameter doesn't exist in bean, skip it
                return false;
              }
            })
        .collect(Collectors.toList());
  }

  /**
   * Create the parameters bound to {@link #create(Identifiable)} and {@link #update(Identifiable)}
   * queries. Override if custom parameters are needed.
   *
   * @param entity the entity
   * @return the parameters
   */
  protected Parameters createAndUpdateParameters(T entity) {
    return new Parameters(entity);
  }

  /**
   * Given the {@code key}, return a pair of a condition and a parameter map corresponding to its
   * record. This default implementation assumes UUID keys and must be overridden for other key
   * types.
   *
   * @param key the key
   * @return a pair of the condition and parameters
   */
  protected Pair<String, Map<String, Object>> keyCondition(K key) {
    Preconditions.checkState(
        key instanceof UUID, "keyCondition() must be overridden for non-UUID keys.");
    var alias = tableAlias();
    var columnName = alias == null ? "id" : alias + ".id";
    var condition = String.format("%s = %s", columnName, ":id");
    return Pair.of(condition, Map.of("id", key));
  }

  /**
   * Return the table and alias SQL clause given {@link #tableName()} and {@link #tableAlias()}.
   * E.g. "foos AS foo".
   *
   * @return the table as clause
   */
  protected String tableAs() {
    var tableAs = new StringBuilder(tableName());
    Optional.ofNullable(tableAlias()).ifPresent(alias -> tableAs.append(" AS ").append(alias));
    return tableAs.toString();
  }

  /**
   * Determines if a result set contains specific column.
   *
   * <p><b>Usage Example:</b>
   *
   * <pre>
   *   SqlRowSet rowSet = jdbcTemplate.queryForRowSet("SELECT * FROM users");
   *   if (AbstractCrudDao.hasColumn(rowSet, "email")) {
   *     String email = rowSet.getString("email");
   *     // process email
   *   }
   * </pre>
   *
   * @param rs the {@link SqlRowSet} to check
   * @param columnName the name of the column to look for (case-sensitive)
   * @return {@code true} if the column exists in the result set; {@code false} otherwise
   */
  public static boolean hasColumn(SqlRowSet rs, String columnName) {
    SqlRowSetMetaData sqlRowSetMetaData = rs.getMetaData();
    int columns = sqlRowSetMetaData.getColumnCount();
    for (int x = 1; x <= columns; x++) {
      if (columnName.equals(sqlRowSetMetaData.getColumnName(x))) {
        return true;
      }
    }
    return false;
  }
}
