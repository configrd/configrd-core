package com.appcrossings.config;

import java.util.Properties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.appcrossings.config.TestCustomConfig.SampleApplicationContext;

@DirtiesContext
@ContextConfiguration(classes = SampleApplicationContext.class)
public class TestCustomConfig extends AbstractTestNGSpringContextTests {

  private String host = "http://static.ca.pixtulate.com";

  @Autowired
  private HierarchicalPropertyPlaceholderConfigurer config;

  @Test
  public void testDetectHost() {
    String hostName = config.envUtil.detectHostName();
    Assert.assertNotNull(hostName);
  }

  @Test
  public void testLoadHosts() throws Exception {

    Properties p = config.loadHosts(config.hostsFilePath);
    Assert.assertNotNull(p);
    Assert.assertTrue(p.containsKey("michelangello"));
    Assert.assertEquals(p.getProperty("michelangello"), "classpath:/env/dev/");

  }

  @Test
  public void overrideHostName() throws Exception {
    System.setProperty("hostname", "testhost");

    String hostName = config.envUtil.detectHostName();
    Assert.assertNotNull(hostName);
    Assert.assertEquals(hostName, "testhost");

    System.setProperty("hostname", "michelangello-custom");
  }

  @Test
  public void loadProperties() throws Exception {

    Properties p = config.loadProperties("classpath:/env/dev/");
    Assert.assertNotNull(p);
    Assert.assertTrue(p.containsKey("property.1.name"));
    Assert.assertEquals(p.getProperty("property.1.name"), "value1");

  }

  @Test
  public void testPullHostFileFromAmazon() throws Exception {

    Properties p = config.loadHosts(host + "/env/hosts.properties");
    Assert.assertNotNull(p);
    Assert.assertTrue(p.containsKey("kkarski-ibm"));

  }

  @Test
  public void testPullPropertiesFileFromAmazon() throws Exception {

    Properties p = config.loadProperties(host + "/env/dev");

    Assert.assertNotNull(p);
    Assert.assertTrue(p.containsKey("algo.im.path"));

  }

  @Test
  public void testPropertiesCascade() throws Exception {

    String value = config.getProperty("property.1.name", String.class);
    Assert.assertNotNull(value);
    Assert.assertEquals(value, "custom");

    value = config.getProperty("property.2.name", String.class);
    Assert.assertNotNull(value);
    Assert.assertEquals(value, "value2");

  }

  @Test
  public void testPropertiesCascadeOverride() throws Exception {

    String value = config.getProperty("property.1.name", String.class);
    Assert.assertNotNull(value);
    Assert.assertEquals(value, "custom");

  }

  @Test
  public void testIncludeClasspathProperty() throws Exception {

    String value = config.getProperty("property.5.name", String.class);
    Assert.assertNotNull(value);
    Assert.assertEquals(value, "classpath");

  }

  @Test
  public void testGetNonExistingProperty() throws Exception {

    String value = config.getProperty("property.not-exists", String.class);
    Assert.assertNull(value);

  }

  @Test
  public void testGetEncryptedProperty() throws Exception {

    String value = config.getProperty("property.6.name", String.class);
    Assert.assertNotNull(value);
    Assert.assertNotEquals(value, "NvuRfrVnqL8yDunzmutaCa6imIzh6QFL");
    Assert.assertEquals(value, "password");
  }

  @Configuration
  public static class SampleApplicationContext {

    static {
      System.setProperty("env", "QA");
      System.setProperty("hostname", "michelangello-custom");
    }

    @Bean
    public static HierarchicalPropertyPlaceholderConfigurer createConfig() throws Exception {
      HierarchicalPropertyPlaceholderConfigurer c =
          new HierarchicalPropertyPlaceholderConfigurer("classpath:/env/hosts.properties");
      c.setPassword("secret");
      return c;
    }

  }


}
