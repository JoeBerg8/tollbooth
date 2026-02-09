package com.tollbooth.persistence;

import com.tollbooth.AbstractCleanupTest;
import com.tollbooth.toll.TollEmailMetaDao;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith({SpringExtension.class, PostgresExtension.class})
@ContextConfiguration(classes = {PostgresTestConfig.class})
public abstract class AbstractDaoTest extends AbstractCleanupTest {

  @Autowired protected TollEmailMetaDao tollEmailMetaDao;
}
