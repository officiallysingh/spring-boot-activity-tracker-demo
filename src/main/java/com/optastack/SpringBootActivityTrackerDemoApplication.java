package com.optastack;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@EnableAspectJAutoProxy
public class SpringBootActivityTrackerDemoApplication {

  public static void main(String[] args) {
    SpringApplication.run(SpringBootActivityTrackerDemoApplication.class, args);
  }
}
