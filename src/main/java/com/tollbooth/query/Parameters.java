package com.tollbooth.query;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.AbstractSqlParameterSource;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

/**
 * A convenience class that wraps {@link BeanPropertySqlParameterSource} with a {@link
 * MapSqlParameterSource}. This allows you to create a set of parameters from an Dto, then add to or
 * override them.
 */
public class Parameters extends AbstractSqlParameterSource {

  private MapSqlParameterSource mapParameters;
  private BeanPropertySqlParameterSource beanParameters;

  /**
   * Create new Parameters from the given object and map.
   *
   * @param object the object
   * @param map the map
   */
  public Parameters(Object object, Map<String, Object> map) {
    this.beanParameters = new BeanPropertySqlParameterSource(object);
    this.mapParameters = new MapSqlParameterSource(map);
  }

  /** Create new empty Parameters. */
  public Parameters() {
    this(new Object(), Map.of());
  }

  /**
   * Create new Parameters from the given object.
   *
   * @param object the object
   */
  public Parameters(Object object) {
    this(object, Map.of());
  }

  /**
   * Create new Parameters from the given map.
   *
   * @param map the map
   */
  public Parameters(Map<String, Object> map) {
    this(new Object(), map);
  }

  /** Create new Parameters from the given map entry. */
  public Parameters(String k1, Object v1) {
    this(Map.of(k1, v1));
  }

  /** Create new Parameters from the given two map entries. */
  public Parameters(String k1, Object v1, String k2, Object v2) {
    this(Map.of(k1, v1, k2, v2));
  }

  /** Create new Parameters from the given three map entries. */
  public Parameters(String k1, Object v1, String k2, Object v2, String k3, Object v3) {
    this(Map.of(k1, v1, k2, v2, k3, v3));
  }

  /** Add the given entry to the parameters. */
  public Parameters add(String key, Object value) {
    mapParameters.addValue(key, value);
    return this;
  }

  @Override
  public boolean hasValue(String paramName) {
    return mapParameters.hasValue(paramName) || beanParameters.hasValue(paramName);
  }

  @Override
  public Object getValue(String paramName) throws IllegalArgumentException {
    Object value =
        mapParameters.hasValue(paramName)
            ? mapParameters.getValue(paramName)
            : beanParameters.getValue(paramName);
    if (value instanceof Instant) {
      return Timestamp.from((Instant) value);
    }
    return value;
  }

  @Override
  public int getSqlType(String paramName) {
    return mapParameters.hasValue(paramName)
        ? mapParameters.getSqlType(paramName)
        : beanParameters.getSqlType(paramName);
  }
}
