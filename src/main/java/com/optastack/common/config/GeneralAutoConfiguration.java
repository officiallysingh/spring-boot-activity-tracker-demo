package com.optastack.common.config;

import com.ksoot.activity.model.AuthorProvider;
import com.ksoot.activity.model.MockAuthorProvider;
import com.optastack.common.util.MessageProvider;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;

@Configuration
@EnableConfigurationProperties
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
public class GeneralAutoConfiguration {

  @Bean
  MessageProvider messageProvider(final MessageSource messageSource) {
    return new MessageProvider(messageSource);
  }

  @Bean
  SpringProfiles springProfiles(final Environment environment) {
    return new SpringProfiles(environment);
  }

  @Bean
  BeanRegistry defaultBeanRegistry(final ApplicationContext applicationContext) {
    BeanRegistry beanRegistry = new DefaultBeanRegistry();
    beanRegistry.setApplicationContext(applicationContext);
    return beanRegistry;
  }

  // Uncomment if you want to use just a hardcoded author name always.
    @Bean
    AuthorProvider authorProvider() {
      return new MockAuthorProvider();
    }

  //  @Bean
  //  AuthorProvider authorProvider(@Value("#{systemProperties['user.name']}") String
  // systemUserName) {
  //    return new SystemAuthorProvider(systemUserName);
  //  }

  // Uncomment if you have spring-boot-starter-security dependency
  //  @Bean
  //  AuthorProvider authorProvider() {
  //    return new SpringSecurityAuthorProvider();
  //  }
}
