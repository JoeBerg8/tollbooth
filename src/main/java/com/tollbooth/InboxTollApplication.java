package com.tollbooth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class InboxTollApplication {

  public static void main(String[] args) {
    SpringApplication.run(InboxTollApplication.class, args);
  }
}
