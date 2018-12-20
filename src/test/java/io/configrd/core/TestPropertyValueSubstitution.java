package io.configrd.core;

import org.junit.Assert;
import org.junit.Test;
import io.configrd.core.ConfigClient;
import io.configrd.core.ConfigClient.Method;

public class TestPropertyValueSubstitution {
  
  public static final String DEFAULT_CONFIGRD_CONFIG_URI = "classpath:repo-defaults.yml";

  @Test
  public void testPropertyValueSubstitution() throws Exception {
    ConfigClient client = new ConfigClient(DEFAULT_CONFIGRD_CONFIG_URI, "classpath:env/dev/michelangello-custom2", Method.ABSOLUTE_URI);
    client.init();

    Assert.assertNotNull(client.getProperty("property.4.name", String.class));
    Assert.assertEquals("simple-michelangello",
        client.getProperty("property.4.name", String.class));
  }

  @Test
  public void testPropertyValueSubstitutionWithMissingValue() throws Exception {
    ConfigClient client = new ConfigClient(DEFAULT_CONFIGRD_CONFIG_URI, "classpath:env/dev/michelangello-custom2", Method.ABSOLUTE_URI);
    client.init();

    Assert.assertNotNull(client.getProperty("property.5.name", String.class));
    Assert.assertEquals("${property.1.notexsts}-michelangello",
        client.getProperty("property.5.name", String.class));
  }

}
