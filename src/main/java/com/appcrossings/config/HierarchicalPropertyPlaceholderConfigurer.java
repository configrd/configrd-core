package com.appcrossings.config;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.beanutils.ConvertUtilsBean;
import org.jasypt.exceptions.AlreadyInitializedException;
import org.jasypt.properties.EncryptableProperties;
import org.jasypt.util.text.BasicTextEncryptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.Environment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.PropertyPlaceholderHelper;
import org.springframework.util.StringUtils;

import com.google.common.base.Throwables;

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

  private final static Logger log = LoggerFactory
      .getLogger(HierarchicalPropertyPlaceholderConfigurer.class);

  private ValueInjector injector;

  private final ConvertUtilsBean bean = new ConvertUtilsBean();

  private ConfigurableListableBeanFactory beanFactory;

  private final BasicTextEncryptor encryptor = new BasicTextEncryptor();

  protected EnvironmentUtil envUtil = new EnvironmentUtil();

  private PropertyPlaceholderHelper helper;


  @Value("${properties.hostsFilePath}")
  protected String hostsFilePath;

  private final AtomicReference<EncryptableProperties> loadedProperties = new AtomicReference<>(
      new EncryptableProperties(encryptor));

  protected String password = "secret";

  protected String propertiesFileName = "default.properties";

  protected boolean searchClasspath = true;

  private Timer timer = new Timer(true);

  /**
   * Set path to hosts.properties file using the setter.
   * 
   * @throws Exception
   */
  public HierarchicalPropertyPlaceholderConfigurer() throws Exception {
    setLocalOverride(true);
    helper =
        new PropertyPlaceholderHelper(this.placeholderPrefix, this.placeholderSuffix,
            this.valueSeparator, this.ignoreUnresolvablePlaceholders);
  }

  /**
   * 
   * @param path The path of the hosts.properties file
   * @throws Exception
   */
  public HierarchicalPropertyPlaceholderConfigurer(String path) throws Exception {
    this();
    this.hostsFilePath = path;
  }

  /**
   * 
   * @param path - The path of the hosts.properties file
   * @param refresh - The period in seconds at which the config properties should be refreshed.
   *        Defaults to 10 minutes.
   * @throws Exception
   */
  public HierarchicalPropertyPlaceholderConfigurer(String path, int refresh) throws Exception {
    this(path);
    setRefreshRate(refresh);
  }

  /**
   * 
   * @param path The path of the hosts.properties file
   * @param fileName Name of the property files to search for (i.e. default.properties)
   * @throws Exception
   */
  public HierarchicalPropertyPlaceholderConfigurer(String path, String fileName) throws Exception {
    this(path);
    this.propertiesFileName = fileName;
  }

  /**
   * 
   * @param path - The path of the hosts.properties file
   * @param fileName Name of the property files to search for (i.e. default.properties)
   * @param refresh - The period in seconds at which the config properties should be refreshed.
   *        Defaults to 10 minutes.
   * @throws Exception
   */
  public HierarchicalPropertyPlaceholderConfigurer(String path, String fileName, int refresh)
      throws Exception {
    this(path, fileName);
    setRefreshRate(refresh);
  }

  protected Properties fetchProperties(String propertiesPath) {

    Properties p = new Properties();
    ResourceLoader resourceLoader = new DefaultResourceLoader();

    Resource resource = resourceLoader.getResource(propertiesPath + "/" + propertiesFileName);

    // Check if a spring properties file exists
    if (!resource.exists()) {
      resource = resourceLoader.getResource(propertiesPath + "/application.properties");
    }

    // Search for default.properties file in parent folders
    if (resource.exists()) {

      try (InputStream stream2 = resource.getInputStream()) {

        log.info("Found properties file: " + propertiesPath + "/" + resource.getFilename());
        p.load(stream2);

      } catch (IOException e) {

        // file not found...no issue, keep going
        if (StringUtils.hasText(e.getMessage())
            && (e.getMessage().contains("code: 403") || e.getMessage().contains("code: 404"))) {

          // Do nothing here since we'll just keep looping with parent
          // path anyway

        } else {
          Throwables.propagate(e);
        }
      }
    }

    return p;
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

    try {
      encryptor.setPassword(password);
    } catch (AlreadyInitializedException e) {
      // skip
    }

    logger.info("Loading property files...");

    Properties hosts = loadHosts(hostsFilePath);

    String propertiesFile = hosts.getProperty(hostName);

    // Attempt environment as a backup
    if (!StringUtils.hasText(propertiesFile) && StringUtils.hasText(environmentName)) {

      propertiesFile = hosts.getProperty(environmentName);

    } else if (!StringUtils.hasText(propertiesFile)) {

      propertiesFile = hosts.getProperty("*");

    }

    EncryptableProperties ps = loadProperties(propertiesFile);

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

    String ttl = (String) ps.get("config.ttl");
    if (!StringUtils.isEmpty(ttl)) {

      Integer lttl = Integer.valueOf(ttl);

      log.info("Setting config refresh rate to: " + lttl + " seconds.");
      setRefreshRate(lttl);
    }

  }

  protected Properties loadHosts(String hostsFile) throws IllegalArgumentException {

    log.info("Fetching hosts file from path: " + hostsFile);

    Resource resource = new DefaultResourceLoader().getResource(hostsFile);

    if (!resource.exists()) {
      throw new IllegalArgumentException("Properties file " + hostsFile
          + " couldn't be found at location " + hostsFile);
    }

    Properties hosts = new EncryptableProperties(encryptor);
    try (InputStream stream = resource.getInputStream()) {

      hosts.load(stream);

    } catch (IOException e) {
      log.error("Can't load hosts file", e);
    }

    return hosts;

  }

  protected EncryptableProperties loadProperties(String propertiesPath) {

    List<Properties> all = new ArrayList<>();

    if (StringUtils.hasText(propertiesPath)) {

      do {

        all.add(fetchProperties(propertiesPath));
        propertiesPath = stripDir(propertiesPath);

      } while (new File(propertiesPath).getParent() != null);
    }

    // Finally, check classpath
    if (searchClasspath) {
      all.add(fetchProperties("classpath:/config/"));
      all.add(fetchProperties("classpath:"));
    }

    Collections.reverse(all); // sort from root to highest

    EncryptableProperties ps = new EncryptableProperties(encryptor);

    for (Properties p : all) {
      ps.putAll(p); // merge down and replace lower properties with override properties
    }

    return ps;

  }

  @Override
  public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory)
      throws BeansException {

    this.beanFactory = beanFactory;
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

  public void setHostsFileLocation(String hostsFile) {
    this.hostsFilePath = hostsFile;
  }

  public void setPassword(String password) {
    this.password = password;
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

  protected String stripDir(String path) {

    int i = path.lastIndexOf("/");

    if (i > 0)
      return path.substring(0, i);

    return "";

  }
}
