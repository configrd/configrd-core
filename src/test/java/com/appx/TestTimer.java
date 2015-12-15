package com.appx;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import com.appx.Config;

@DirtiesContext
@ActiveProfiles("QA")
@ContextConfiguration("classpath:META-INF/spring/test-spring-configurer-timer.xml")
public class TestTimer extends AbstractTestNGSpringContextTests {

  @Autowired
  private Config config;

  static {
    System.setProperty("hostname", "michelangello-custom");
  }

  @Test
  public void testTimerReload() throws Exception {

    String value = config.getProperty("property.5.name", String.class);
    Assert.assertNotNull(value);
    Assert.assertEquals(value, "classpath");

    Thread.sleep(4000);
  }

  @AfterClass
  public void tearDown() {
    System.setProperty("env", "");
  }

}
