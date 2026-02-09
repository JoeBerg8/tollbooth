package com.tollbooth.persistence;

import com.tollbooth.query.Delete;
import com.tollbooth.query.Insert;
import com.tollbooth.query.Select;
import com.tollbooth.query.Update;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public abstract class AbstractDao {

  @Autowired protected NamedParameterJdbcTemplate jdbcTemplate;

  protected Select select(String select) {
    return new Select(jdbcTemplate).select(select);
  }

  protected Delete deleteFrom(String tableName) {
    return new Delete(jdbcTemplate).deleteFrom(tableName);
  }

  protected Insert insertInto(String tableName) {
    return new Insert(jdbcTemplate).insertInto(tableName);
  }

  protected Update update(String tableName) {
    return new Update(jdbcTemplate).update(tableName);
  }
}
