package com.tollbooth.toll;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.tollbooth.persistence.AbstractCrudDaoTest;
import com.tollbooth.query.CrudDao;
import java.util.UUID;
import org.junit.jupiter.api.Test;

public class TollEmailMetaDaoTest extends AbstractCrudDaoTest<TollEmailMeta, UUID> {

  @Override
  protected CrudDao<TollEmailMeta, UUID> dao() {
    return tollEmailMetaDao;
  }

  @Override
  protected TollEmailMeta build() {
    return FAKER.tollEmailMeta();
  }

  @Override
  protected void mutate(TollEmailMeta entity) {
    entity.setSenderEmail(FAKER.internet().emailAddress());
    entity.setTollPaid(!entity.isTollPaid());
    entity.setStripeCustomerId(FAKER.internet().uuid());
  }

  @Test
  public void findByGmailId_Valid() {
    var entity = create(build());
    var found = tollEmailMetaDao.findByGmailId(entity.getGmailId());
    assertThat(found).isPresent();
    assertThat(found.get()).isEqualTo(entity);
  }

  @Test
  public void findByGmailId_NotFound() {
    var found = tollEmailMetaDao.findByGmailId(FAKER.internet().uuid());
    assertThat(found).isEmpty();
  }

  @Test
  public void isEmailAlreadyProcessed_True() {
    var entity = create(build());
    var isProcessed = tollEmailMetaDao.isEmailAlreadyProcessed(entity.getGmailId());
    assertThat(isProcessed).isTrue();
  }

  @Test
  public void isEmailAlreadyProcessed_False() {
    var isProcessed = tollEmailMetaDao.isEmailAlreadyProcessed(FAKER.internet().uuid());
    assertThat(isProcessed).isFalse();
  }
}
