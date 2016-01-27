package com.appcrossings.config;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.beanutils.ConvertUtilsBean;
import org.jasypt.exceptions.AlreadyInitializedException;
import org.jasypt.properties.EncryptableProperties;
import org.jasypt.util.text.BasicTextEncryptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.Environment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.google.common.base.Throwables;

/**
 * 
 * @author Krzysztof Karski
 *
 */
@Service
public class HierarchicalPropertyPlaceholderConfigurer extends PropertySourcesPlaceholderConfigurer
    implements Config, EnvironmentAware {

  private class ReloadTask extends TimerTask {

    @Override
    public void run() {
      try {

        if (beanFactory != null)
          postProcessBeanFactory(beanFactory);

      } catch (Exception e) {
        logger.error("Error refreshing configs", e);
      }
    }
  }

  private final static Logger log = LoggerFactory
      .getLogger(HierarchicalPropertyPlaceholderConfigurer.class);

  @Autowired(required = false)
  protected Environment springProfiles;

  private final ConvertUtilsBean bean = new ConvertUtilsBean();

  protected final BasicTextEncryptor encryptor = new BasicTextEncryptor();

  private String environmentName;

  private String hostName;

  @Value("${properties.hostsFilePath}")
  private String hostsFilePath;

  private final ConcurrentHashMap<String, Set<ConfigChangeListener>> listeners =
      new ConcurrentHashMap<String, Set<ConfigChangeListener>>();

  protected String password = "secret";

  private final AtomicReference<EncryptableProperties> loadedProperties = new AtomicReference<>(
      new EncryptableProperties(encryptor));

  private String propertiesFileName = "default.properties";

  private boolean searchClasspath = true;

  private Timer timer = new Timer(true);

  private ConfigurableListableBeanFactory beanFactory;

  @Override
  public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory)
      throws BeansException {

    this.beanFactory = beanFactory;
    init();
    super.postProcessBeanFactory(beanFactory);

  }

  /**
   * Set path to hosts.properties file using the setter.
   * 
   * @throws Exception
   */
  public HierarchicalPropertyPlaceholderConfigurer() throws Exception {
    setLocalOverride(true);
  }

  /**
   * 
   * @param path The path of the hosts.properties file
   * @throws Exception
   */
  public HierarchicalPropertyPlaceholderConfigurer(String path) throws Exception {
    setLocalOverride(true);
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
    this.hostsFilePath = path;
    setLocalOverride(true);
    setRefreshRate(refresh);
  }

  /**
   * 
   * @param path The path of the hosts.properties file
   * @param fileName Name of the property files to search for (i.e. default.properties)
   * @throws Exception
   */
  public HierarchicalPropertyPlaceholderConfigurer(String path, String fileName) throws Exception {
    this.hostsFilePath = path;
    setLocalOverride(true);
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
    this.hostsFilePath = path;
    this.propertiesFileName = fileName;
    setRefreshRate(refresh);
    setLocalOverride(true);
  }

  @Override
  public void deregister(String key, ConfigChangeListener listener) {

    if (listeners.containsKey(key)) {
      listeners.get(key).remove(listener);
    }
  }

  /**
   * Attempt to detect environment of the application.
   * 
   * @return environment string
   */
  public String detectEnvironment() {

    String env = null;

    if (springProfiles != null && springProfiles.getActiveProfiles() != null
        && springProfiles.getActiveProfiles().length > 0) {
      env = springProfiles.getActiveProfiles()[0];
    }

    env = StringUtils.hasText(env) ? env : System.getProperty("env");
    env = StringUtils.hasText(env) ? env : System.getProperty("ENV");
    env = StringUtils.hasText(env) ? env : System.getProperty("environment");
    env = StringUtils.hasText(env) ? env : System.getProperty("ENVIRONMENT");

    if (!StringUtils.hasText(env)) {
      log.info("No environment variable detected under 'spring.profiles' or system properties 'env', 'ENV', 'environment', 'ENVIRONMENT'");
    } else {
      log.info("Detected environment: " + env);
    }

    return env;
  }

  /**
   * Reads the underlying host's name. This is used to match this host against its configuration.
   * You can programmatically override the hostname value by setting System.setProperty("hostname",
   * "value").
   * 
   * @return hostname
   */
  public String detectHostName() {

    String hostName = null;

    try {

      hostName =
          StringUtils.hasText(System.getProperty("hostname")) ? System.getProperty("hostname")
              : InetAddress.getLocalHost().getHostName();

      hostName = StringUtils.hasText(hostName) ? hostName : System.getProperty("HOSTNAME");

      if (!StringUtils.hasText(hostName)) {
        throw new UnknownHostException(
            "Unable to resolve host in order to resolve hosts file config. Searched system property 'hostname', 'HOSTNAME' and localhost.hostName");
      }

      if (hostName.contains(".")) {
        hostName = hostName.substring(0, hostName.indexOf("."));
      }

      log.info("Resolved hostname to: " + hostName);

    } catch (UnknownHostException ex) {
      log.error("Can't resolve hostname", ex);
      Throwables.propagate(ex);
    }

    return hostName;
  }

  private Properties fetchProperties(String propertiesPath) {

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

  public String getFileName() {
    return propertiesFileName;
  }

  public String getHostName() {
    return hostName;
  }

  public String getHostsFile() {
    return hostsFilePath;
  }

  protected EncryptableProperties getLoadedProperties() {
    return loadedProperties.get();
  }

  public String getPassword() {
    return password;
  }

  @Override
  public <T> T getProperty(String key, Class<T> clazz) {

    String property = loadedProperties.get().getProperty(key);

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

  private void init() {

    if (StringUtils.isEmpty(environmentName))
      environmentName = detectEnvironment();

    if (StringUtils.isEmpty(hostName))
      hostName = detectHostName();

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
    super.setProperties(getLoadedProperties());

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

  public boolean isSearchClasspath() {
    return searchClasspath;
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
  public void register(ConfigChangeListener listener) {
    // TODO Auto-generated method stub

  }

  @Override
  public void register(String key, ConfigChangeListener listener) {

    if (listeners.contains(key)) {
      listeners.get(key).add(listener);
    } else {
      synchronized (listeners) {
        Set<ConfigChangeListener> n = new ConcurrentSkipListSet<ConfigChangeListener>();
        n.add(listener);
        listeners.put(key, n);
      }

    }

  }

  @Override
  public void setEnvironment(Environment environment) {
    super.setEnvironment(environment);
    this.springProfiles = environment;
  }

  public void setFileName(String fileName) {
    this.propertiesFileName = fileName;
  }

  public void setHostName(String hostName) {
    this.hostName = hostName;
  }

  public void setHostsFile(String hostsFile) {
    this.hostsFilePath = hostsFile;
  }

  public void setPassword(String password) {
    this.password = password;
  }

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

  private String stripDir(String path) {

    int i = path.lastIndexOf("/");

    if (i > 0)
      return path.substring(0, i);

    return "";

  }

  public String getEnvironmentName() {
    return environmentName;
  }

  public void setEnvironmentName(String environmentName) {
    this.environmentName = environmentName;
  }
}
