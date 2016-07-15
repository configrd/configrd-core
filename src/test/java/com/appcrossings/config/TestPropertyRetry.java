package com.appcrossings.config;


import org.testng.annotations.Test;

public class TestPropertyRetry {

  @Test(expectedExceptions={IllegalArgumentException.class})
  public void createConfigWithWrongPath() throws Exception {
    HierarchicalPropertyPlaceholderConfigurer config = new HierarchicalPropertyPlaceholderConfigurer("classpath:/env/wrong.properties");
    config.init();
  }
  
  @Test
  public void createConfigWithNonExistingHostOrEnvironment() throws Exception {
    HierarchicalPropertyPlaceholderConfigurer config = new HierarchicalPropertyPlaceholderConfigurer("classpath:/env/hosts.properties");
    config.setHostName("doesntexist");
    config.init();
  }
  
  @Test
  public void createConfigWithNonExistingProperties() throws Exception {
    HierarchicalPropertyPlaceholderConfigurer config = new HierarchicalPropertyPlaceholderConfigurer("classpath:/env/hosts.properties");
    config.setHostName("xyz");
    config.init();
  }

}
