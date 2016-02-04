package com.appcrossings.config;

import java.io.IOException;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.beanutils.ConvertUtilsBean;
import org.jasypt.properties.EncryptableProperties;
import org.jasypt.util.text.BasicTextEncryptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.Environment;
import org.springframework.util.PropertyPlaceholderHelper;
import org.springframework.util.StringUtils;

/**
 * 
 * @author Krzysztof Karski
 *
 */
public class HierarchicalPropertyPlaceholderConfigurer extends PropertySourcesPlaceholderConfigurer
    implements Config, EnvironmentAware, ApplicationContextAware {

  private class ReloadTask extends TimerTask {

    @Override
    public void run() {
      try {

        reload();

      } catch (Exception e) {
        logger.error("Error refreshing configs", e);
      }
    }
  }
  
  public final static String DEFAULT_PASSWORD = "secret";
  public final static String DEFAULT_PROPERTIES_FILE_NAME = "default.properties";
  private final static Logger log = LoggerFactory
      .getLogger(HierarchicalPropertyPlaceholderConfigurer.class);

  public final static boolean SEARCH_CLASSPATH = true;

  private final ConvertUtilsBean bean = new ConvertUtilsBean();

  private BasicTextEncryptor encryptor;

  protected EnvironmentUtil envUtil = new EnvironmentUtil();

  private PropertyPlaceholderHelper helper;

  protected final String hostsFilePath;

  private ValueInjector injector;

  private final AtomicReference<EncryptableProperties> loadedProperties = new AtomicReference<>();

  protected String propertiesFileName = DEFAULT_PROPERTIES_FILE_NAME;

  protected boolean searchClasspath = SEARCH_CLASSPATH;

  private Timer timer = new Timer(true);

  /**
   * 
   * @param path The path of the hosts.properties file
   * @throws Exception
   */
  public HierarchicalPropertyPlaceholderConfigurer(String path) throws Exception {
    setLocalOverride(true);
    helper =
        new PropertyPlaceholderHelper(this.placeholderPrefix, this.placeholderSuffix,
            this.valueSeparator, this.ignoreUnresolvablePlaceholders);
    this.hostsFilePath = path;
    setPassword(DEFAULT_PASSWORD);
  }

  /**
   * 
   * @param path - The path of the hosts.properties file
   * @param refresh - The period in seconds at which the config properties should be refreshed. 0
   *        indicates no automated timer
   * @throws Exception
   */
  public HierarchicalPropertyPlaceholderConfigurer(String path, int refresh) throws Exception {
    this(path);
    setRefreshRate(refresh);
  }



  @Override
  public <T> T getProperty(String key, Class<T> clazz) {

    String property;
    try {
      property =
          helper.replacePlaceholders(placeholderPrefix + key + placeholderSuffix,
              loadedProperties.get());

      if (property.equals(key))
        return null;

    } catch (IllegalArgumentException e) {
      return null;
    }

    if (clazz.equals(String.class))
      return (T) property;
    else if (property != null)
      return (T) bean.convert(property, clazz);
    else
      return null;

  }

  public <T> T getProperty(String key, Class<T> clazz, T value) {

    T val = getProperty(key, clazz);

    if (val != null)
      return val;

    return value;

  }

  protected void init() {

    String environmentName = envUtil.detectEnvironment();
    String hostName = envUtil.detectHostName();

    logger.info("Loading property files...");

    Properties hosts = ResourcesUtil.loadHosts(hostsFilePath, encryptor);

    String propertiesFile = hosts.getProperty(hostName);

    // Attempt environment as a backup
    if (!StringUtils.hasText(propertiesFile) && StringUtils.hasText(environmentName)) {

      propertiesFile = hosts.getProperty(environmentName);

    } else if (!StringUtils.hasText(propertiesFile)) {

      propertiesFile = hosts.getProperty("*");// catch all

    }

    EncryptableProperties ps =
        ResourcesUtil.loadProperties(propertiesFile, propertiesFileName, searchClasspath, encryptor);

    if (ps.isEmpty()) {
      log.warn("Counldn't find any properties for host " + hostName + " or environment "
          + environmentName);
    }

    // Finally, propagate properties to PropertyPlaceholderConfigurer
    loadedProperties.set(ps);
    super.setProperties(loadedProperties.get());

    try {
      mergeProperties();
    } catch (IOException io) {
      // nothing
    }
  }

  @Override
  public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory)
      throws BeansException {

    init();
    super.postProcessBeanFactory(beanFactory);

  }

  public void reload() {

    init();
    injector.reloadBeans(loadedProperties.get(), this.placeholderPrefix, this.placeholderSuffix);
  }

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    if (injector == null)
      this.injector = new ValueInjector(applicationContext);
  }

  @Override
  public void setEnvironment(Environment environment) {
    super.setEnvironment(environment);
    envUtil.setEnvironment(environment);
  }

  public void setEnvironment(String environmentName) {
    envUtil.setEnvironmentName(environmentName);
  }

  public void setFileName(String fileName) {
    this.propertiesFileName = fileName;
  }

  public void setHostName(String hostName) {
    envUtil.setHostName(hostName);
  }

  public void setPassword(String password) {
    encryptor = new BasicTextEncryptor();
    encryptor.setPassword(password);
  };

  public void setRefreshRate(Integer refresh) {

    if (refresh == 0L || refresh == null) {
      timer.cancel();
      return;
    }

    synchronized (timer) {
      timer.cancel();
      timer = new Timer(true);
      timer.schedule(new ReloadTask(), refresh * 1000, refresh * 1000);
    }
  }

  public void setSearchClasspath(boolean searchClasspath) {
    this.searchClasspath = searchClasspath;
  }
}
