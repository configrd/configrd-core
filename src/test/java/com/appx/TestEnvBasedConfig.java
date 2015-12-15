package com.appx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@DirtiesContext
@ContextConfiguration("classpath:META-INF/spring/test-spring-configurer.xml")
public class TestEnvBasedConfig extends AbstractTestNGSpringContextTests {

  @BeforeClass
  public void init() {
    System.setProperty("env", "QA");
  }

  @Autowired
  public SampleClass clazz;

  @Test
  public void testProperties() throws Exception {

    assertNotNull(clazz.getSomeValue1());
    assertNotNull(clazz.getSomeValue2());
    assertNotNull(clazz.getSomeValue4());
    assertNotNull(clazz.getSomeOtherValue());

    assertEquals(clazz.getSomeValue1(), "custom");
    assertEquals(clazz.getSomeValue2(), "value2");
    assertEquals(clazz.getSomeValue4(), "custom-custom2");
    assertEquals(clazz.getSomeOtherValue(), "custom2");

    assertNotNull(clazz.getPropertyValue("property.1.name", String.class));
    assertEquals(clazz.getPropertyValue("property.1.name", String.class), "custom");
  }

  @AfterClass
  public void tearDown() {
    System.setProperty("env", "");
  }

}
