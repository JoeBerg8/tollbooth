package com.tollbooth.persistence;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import com.tollbooth.dto.Copyable;
import com.tollbooth.dto.Identifiable;
import com.tollbooth.query.CrudDao;
import java.util.Optional;
import org.junit.jupiter.api.Test;

public abstract class AbstractCrudDaoTest<T extends Identifiable<K> & Copyable<T>, K>
    extends AbstractDaoTest {

  /**
   * Return the DAO to be tested.
   *
   * @return the dao
   */
  protected abstract CrudDao<T, K> dao();

  /**
   * Build an entity without persisting it to the database.
   *
   * @return the entity
   */
  protected abstract T build();

  /**
   * Mutate the given entity in such a way that original.equals(entity) returns false.
   *
   * @param entity the entity to mutate
   */
  protected abstract void mutate(T entity);

  /**
   * Persist the given entity and add a cleanup task.
   *
   * @param entity the entity
   * @return the entity
   */
  protected T create(T entity) {
    cleanup(() -> dao().delete(entity.getId()));
    dao().create(entity);
    return entity;
  }

  /**
   * Allow children to situationally disable update testing.
   *
   * @return true if update is enabled
   */
  protected boolean updateEnabled() {
    return true;
  }

  @Test
  public void crud_Valid() {
    // Validate entity does not exist (if it has a key)
    var entity = build();
    if (entity.getId() != null) {
      assertThat(dao().find(entity.getId()).isPresent()).isFalse();
    }

    // Create and validate
    create(entity);
    var response = dao().find(entity.getId());
    assertThat(response.isPresent()).isTrue();
    assertThat(entity).isEqualTo(response.get());

    if (updateEnabled()) {
      // Mutate and validate
      var originalEntity = entity.deepCopy();
      mutate(entity);
      assertNotEquals(originalEntity, entity);
      dao().update(entity);
      assertThat(Optional.of(entity)).isEqualTo(dao().find(entity.getId()));
    }

    // Delete and validate
    dao().delete(entity.getId());
    assertThat(dao().find(entity.getId()).isPresent()).isFalse();
  }
}
