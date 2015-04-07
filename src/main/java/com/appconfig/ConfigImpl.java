package com.appconfig;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.PostConstruct;

import org.apache.commons.beanutils.ConvertUtilsBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;

import com.google.common.base.Throwables;

@Service
public class ConfigImpl implements Config {

  private final static Logger log = LoggerFactory.getLogger(ConfigImpl.class);
  private final AtomicReference<Properties> properties = new AtomicReference<>(new Properties());
  private final ConvertUtilsBean bean = new ConvertUtilsBean();
  protected InetAddress inet = InetAddress.getLocalHost();
  private Long lastRefresh = Long.MIN_VALUE;
  private final AtomicLong ttl = new AtomicLong(600);

  private static final String DEFAULT_FILE = "default.properties";

  @Value("${properties.hostsFilePath}")
  protected String hostsFile;

  @Override
  public <T> T getProperty(String key, Class<T> clazz) {

    long now = System.currentTimeMillis();

    boolean sync = false;
    synchronized (lastRefresh) {
      if((now - lastRefresh) > (ttl.get() * 1000)){
        sync = true;
        lastRefresh = System.currentTimeMillis(); 
      }
    }
    
    if (sync) {

      try {
        init();
      } catch (Exception e) {
        log.error("Attempting to reload config has failed", e);
      }

      sync = false;
    }

    String property = properties.get().getProperty(key);

    if (clazz.equals(String.class))
      return (T) property;
    else if(property != null)
      return (T) bean.convert(property, clazz);
    else
      return null;

  }
  
  public <T> T getProperty(String key, Class<T> clazz, T value){
    
    T val = getProperty(key, clazz);
    
    if(val != null)
      return val;
    
    return value;
    
  }

  public ConfigImpl() throws Exception {
  }

  @PostConstruct
  protected void init() throws Exception {

    Properties hosts = loadHosts(hostsFile);

    String hostName = detectHostName();
    String propertiesFile = hosts.getProperty(hostName);

    // Special wildchar match
    if (!StringUtils.hasText(propertiesFile)) {
      propertiesFile = hosts.getProperty("*");
    }

    Properties ps = loadProperties(propertiesFile);

    if (ps.isEmpty()) {
      throw new FileNotFoundException("Counldn't find any properties for host " + hostName);
    }

    properties.set(ps);

    // bootstrap setting log level here
    if (StringUtils.hasText(properties.get().getProperty("log.root.level"))) {
      LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
      ch.qos.logback.classic.Logger logger = lc.getLogger("com.appconfig");
      logger.setLevel(Level.valueOf(properties.get().getProperty("log.root.level")));
      log.info("Configuring log level: " + logger.getLevel());
    }

    String ttl = (String) properties.get().get("config.ttl");
    if (ttl != null) {

      long lttl = Long.valueOf(ttl);

      if (lttl > 0 && lttl != this.ttl.get()) {

        log.info("Setting config time to live (config.ttl) to: " + ttl + " seconds.");
        this.ttl.set(lttl * 1000);
      }
    } else {
      log.info("Config time to live (config.ttl) set to: " + this.ttl.get() + " seconds.");
    }

  }

  protected String detectHostName() {

    String hostName = null;

    try {

      hostName = inet.getHostName();

      if (!StringUtils.hasText(hostName)) {
        throw new UnknownHostException("Unable to resolve host in order to resolve hosts file config");
      }

      log.info("Resolved hostname to: " + hostName);

      if (hostName.contains(".")) {
        hostName = hostName.substring(0, hostName.indexOf("."));
      }

    } catch (UnknownHostException ex) {
      log.error("Can't resolve hostname", ex);
      Throwables.propagate(ex);
    }

    return hostName;
  }

  protected Properties loadHosts(String hostsFile) throws FileNotFoundException {

    log.info("Fetching hosts file from path: " + hostsFile);

    Resource resource = new DefaultResourceLoader().getResource(hostsFile);

    if (!resource.exists()) {
      throw new FileNotFoundException("Properties file " + hostsFile + " couldn't be found");
    }

    Properties hosts = new Properties();
    try (InputStream stream = resource.getInputStream()) {

      hosts.load(stream);

    } catch (IOException e) {
      log.error("Can't load hosts file", e);
    }

    return hosts;

  }

  protected Properties loadProperties(String propertiesPath) throws Exception {

    List<Properties> all = new ArrayList<>();

    if (StringUtils.hasText(propertiesPath)) {

      do {

        Resource resource = new DefaultResourceLoader().getResource(propertiesPath + "/" + DEFAULT_FILE);

        if (!resource.exists()) {
          propertiesPath = stripDir(propertiesPath);
          continue;
        }

        Properties p = new Properties();

        try (InputStream stream2 = resource.getInputStream()) {

          log.info("Found properties file: " + propertiesPath + "/" + DEFAULT_FILE);
          p.load(stream2);
          all.add(p);

        } catch (IOException e) {

          // file not found...no issue, keep going
          if (StringUtils.hasText(e.getMessage()) && (e.getMessage().contains("code: 403") || e.getMessage().contains("code: 404"))) {

            propertiesPath = stripDir(propertiesPath);
            continue;

          } else {
            Throwables.propagate(e);
          }
        }

        propertiesPath = stripDir(propertiesPath);

      } while (new File(propertiesPath).getParent() != null);
    }

    Collections.reverse(all); // sort from root to highest

    Properties ps = new Properties();

    for (Properties p : all) {
      ps.putAll(p); // replace root properties with higher level properties
    }

    return ps;

  }

  private String stripDir(String path) {

    int i = path.lastIndexOf("/");

    if (i > 0)
      return path.substring(0, i);

    return "";

  }

}