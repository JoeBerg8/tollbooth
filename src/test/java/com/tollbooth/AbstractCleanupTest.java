package com.tollbooth;

import java.util.ArrayDeque;
import java.util.Deque;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

public abstract class AbstractCleanupTest {

  public static final Faker FAKER = new Faker();

  private Deque<Runnable> cleanupTasks = new ArrayDeque<>();

  @BeforeEach
  void setupCleanup() {
    cleanupTasks = new ArrayDeque<>();
  }

  /** Cleanup the test. */
  @AfterEach
  void cleanup() {
    cleanupTasks
        .descendingIterator()
        .forEachRemaining(
            task -> {
              try {
                task.run();
              } catch (Exception e) {
                throw new RuntimeException("Exception thrown during cleanup task.", e);
              }
            });
  }

  /**
   * Add a runnable to the deque of tasks that will be executed when cleaning up after each task.
   * Tasks will be executed in LIFO (last in first out) order.
   *
   * @param cleanupTask the task
   */
  protected void cleanup(Runnable cleanupTask) {
    cleanupTasks.add(cleanupTask);
  }
}
