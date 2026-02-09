package com.tollbooth.query;

import com.tollbooth.validation.ErrorCode;
import com.tollbooth.validation.Validation;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.rowset.ResultSetWrappingSqlRowSet;
import org.springframework.jdbc.support.rowset.SqlRowSet;

public abstract class AbstractRowMapper<T> implements RowMapper<T> {

  @Override
  public T mapRow(ResultSet rs, int rowNum) {
    return mapRow(new ResultSetWrappingSqlRowSet(rs));
  }

  public abstract T mapRow(SqlRowSet rs);

  // Helpers

  protected static UUID getUuid(SqlRowSet rs, String field) {
    if (StringUtils.isNotBlank(rs.getString(field))) {
      return UUID.fromString(Objects.requireNonNull(rs.getString(field)));
    }
    return null;
  }

  protected static Instant getInstant(SqlRowSet rs, String field) {
    return Optional.ofNullable(rs.getTimestamp(field)).map(Timestamp::toInstant).orElse(null);
  }

  public static LocalDateTime toLocalDateTime(Instant instant) {
    return Optional.ofNullable(instant)
        .map(instant1 -> LocalDateTime.ofInstant(instant, ZoneOffset.UTC))
        .orElse(null);
  }

  protected static byte[] getByteArray(SqlRowSet rs, String field) {
    Object obj = rs.getObject(field);
    if (obj == null) {
      return null;
    }
    if (obj instanceof byte[]) {
      return (byte[]) obj;
    }
    throw Validation.serviceException(
        ErrorCode.INTERNAL_SERVER_ERROR,
        "Error reading field %s as byte array, was type %s",
        field,
        obj.getClass().getName());
  }

  protected <R> R getOptionalColumn(SqlRowSet rs, String columnName, ColumnExtractor<R> extractor) {
    try {
      rs.findColumn(columnName);
      return extractor.extract(rs, columnName);
    } catch (Exception e) {
      return null;
    }
  }

  @FunctionalInterface
  protected interface ColumnExtractor<R> {
    R extract(SqlRowSet rs, String columnName);
  }
}
