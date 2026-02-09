package com.tollbooth.query;

import com.tollbooth.dto.Identifiable;
import java.util.Optional;

public interface CrudDao<T extends Identifiable<K>, K> {

  /**
   * Return an Optional of an entity given its key.
   *
   * @param key the key
   * @return the entity
   */
  Optional<T> find(K key);

  /**
   * Create a record for the given entity.
   *
   * @param entity the entity
   */
  void create(T entity);

  /**
   * Update the record for the given entity.
   *
   * @param entity the entity
   */
  void update(T entity);

  /**
   * Delete the entity with the given key, idempotently.
   *
   * @param key the key
   */
  void delete(K key);
}
