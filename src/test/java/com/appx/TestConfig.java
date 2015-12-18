package com.appx;

import java.util.Properties;

import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class TestConfig {

  private String host = "http://static.ca.pixtulate.com";
  private HierarchicalPropertyPlaceholderConfigurer config;

  @BeforeClass
  public void env() {
    System.setProperty("env", "QA");
  }

  @AfterClass
  public void destroyEnv() {
    System.setProperty("env", "");
  }

  @BeforeMethod
  public void setUp() throws Exception {

    MockitoAnnotations.initMocks(this);

    config = new HierarchicalPropertyPlaceholderConfigurer("classpath:/env/hosts.properties");
    config.setPassword("secret");
    config.setHostName("michelangello");
    config.init();
  }

  @Test
  public void testDetectHost() {

    String hostName = config.detectHostName();
    Assert.assertNotNull(hostName);

  }

  @Test
  public void testLoadHosts() throws Exception {

    Properties p = config.loadHosts(config.getHostsFile());
    Assert.assertNotNull(p);
    Assert.assertTrue(p.containsKey("michelangello"));
    Assert.assertEquals(p.getProperty("michelangello"), "classpath:/env/dev/");

  }

  @Test
  public void overrideHostName() throws Exception {
    System.setProperty("hostname", "testhost");

    String hostName = config.detectHostName();
    Assert.assertNotNull(hostName);
    Assert.assertEquals(hostName, "testhost");
  }

  @Test
  public void loadProperties() throws Exception {

    Properties p = config.loadProperties("classpath:/env/dev/");
    Assert.assertNotNull(p);
    Assert.assertTrue(p.containsKey("property.1.name"));
    Assert.assertEquals(p.getProperty("property.1.name"), "value1");

  }

  @Test
  public void testLoadHostFile() {

    String value = config.getProperty("property.1.name", String.class);
    Assert.assertNotNull(value);
    Assert.assertEquals(value, "value1");

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

    config.setHostName("michelangello-custom");
    config.init();

    String value = config.getProperty("property.1.name", String.class);
    Assert.assertNotNull(value);
    Assert.assertEquals(value, "custom");

    value = config.getProperty("property.2.name", String.class);
    Assert.assertNotNull(value);
    Assert.assertEquals(value, "value2");

  }

  @Test
  public void testPropertiesCascadeOverride() throws Exception {

    config.setHostName("michelangello-custom");
    config.init();

    String value = config.getProperty("property.1.name", String.class);
    Assert.assertNotNull(value);
    Assert.assertEquals(value, "custom");

  }

  @Test
  public void testIncludeClasspathProperty() throws Exception {

    config.setHostName("michelangello-custom");
    config.setSearchClasspath(true);
    config.init();

    String value = config.getProperty("property.5.name", String.class);
    Assert.assertNotNull(value);
    Assert.assertEquals(value, "classpath");

  }

  @Test
  public void testDoNotIncludeClasspathProperty() throws Exception {

    config.setHostName("michelangello-custom");
    config.setSearchClasspath(false);
    config.init();

    String value = config.getProperty("property.5.name", String.class);
    Assert.assertNull(value);

  }

  @Test
  public void testGetNonExistingProperty() throws Exception {

    config.setHostName("michelangello-custom");
    config.init();

    String value = config.getProperty("property.not-exists", String.class);
    Assert.assertNull(value);

  }

  @Test
  public void testGetEncryptedProperty() throws Exception {
    config.setHostName("michelangello-custom");
    config.init();

    String value = config.getProperty("property.6.name", String.class);
    Assert.assertNotNull(value);
    Assert.assertNotEquals(value, "NvuRfrVnqL8yDunzmutaCa6imIzh6QFL");
    Assert.assertEquals(value, "password");
  }

  @AfterMethod
  public void tearDown() {

    System.setProperty("hostname", "");
  }

}
