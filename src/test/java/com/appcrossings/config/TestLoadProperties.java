package com.appcrossings.config;

import java.util.Properties;

import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class TestLoadProperties {

  private String host = "http://static.ca.pixtulate.com";
  private StandardPBEStringEncryptor encryptor = new StandardPBEStringEncryptor();

  @BeforeClass
  public void before() {
    encryptor.setPassword("secret");
    encryptor.setAlgorithm("PBEWithMD5AndDES");
  }

  @Test
  public void loadProperties() throws Exception {

    Properties p =
        ResourcesUtil.loadProperties("classpath:/env/dev/",
            HierarchicalPropertyPlaceholderConfigurer.DEFAULT_PROPERTIES_FILE_NAME,
            HierarchicalPropertyPlaceholderConfigurer.SEARCH_CLASSPATH, encryptor);
    Assert.assertNotNull(p);
    Assert.assertTrue(p.containsKey("property.1.name"));
    Assert.assertEquals(p.getProperty("property.1.name"), "value1");

  }

  @Test
  public void testPullHostFileFromAmazon() throws Exception {

    Properties p = ResourcesUtil.loadHosts(host + "/env/hosts.properties", encryptor);
    Assert.assertNotNull(p);
    Assert.assertTrue(p.containsKey("kkarski-ibm"));

  }

  @Test
  public void testLoadHosts() throws Exception {

    Properties p = ResourcesUtil.loadHosts("classpath:/env/hosts.properties", encryptor);
    Assert.assertNotNull(p);
    Assert.assertTrue(p.containsKey("michelangello"));
    Assert.assertEquals(p.getProperty("michelangello"), "classpath:/env/dev/");

  }

  @Test
  public void testPullPropertiesFileFromAmazon() throws Exception {

    Properties p =
        ResourcesUtil.loadProperties(host + "/env/dev",
            HierarchicalPropertyPlaceholderConfigurer.DEFAULT_PROPERTIES_FILE_NAME,
            HierarchicalPropertyPlaceholderConfigurer.SEARCH_CLASSPATH, encryptor);

    Assert.assertNotNull(p);
    Assert.assertTrue(p.containsKey("algo.im.path"));

  }

}
