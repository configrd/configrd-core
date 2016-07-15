package com.appcrossings.config;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.properties.EncryptableProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.StringUtils;

import com.google.common.base.Throwables;

public class ResourcesUtil {

  private final static Logger log = LoggerFactory.getLogger(ResourcesUtil.class);

  public static Properties fetchProperties(String propertiesPath, String propertiesFileName) {

    Properties p = new Properties();
    ResourceLoader resourceLoader = new DefaultResourceLoader();

    Resource resource = resourceLoader.getResource(propertiesPath + "/" + propertiesFileName);

    log.info("Attempting " + resource.getDescription());

    boolean exists = false;
    for (int retry = 0; retry < 3; retry++) {
      if (resource.exists()) {
        exists = true;
        break;
      }

      log.info("Not found. Retrying...");
    }

    if (!exists) {
      // OK, no custom properties file, maybe a spring application.properties file?
      resource = resourceLoader.getResource(propertiesPath + "/application.properties");

      // ok, last chance effort...
      if (!resource.exists())
        return p;
    }

    // Search for default.properties file in parent folders
    log.info("Found " + resource.getDescription());

    p = downloadFile(resource);

    return p;
  }

  public static Properties downloadFile(Resource resource) {

    Properties p = new Properties();
    Throwable ex = null;
    for (int retry = 0; retry < 3; retry++) {

      try (InputStream stream2 = resource.getInputStream()) {

        p.load(stream2);
        break;

      } catch (IOException e) {
        ex = e;
        continue;

      }
    }

    // we've retried, file not found...no issue, keep going
    if (ex != null && StringUtils.hasText(ex.getMessage()) && (ex.getMessage().contains("code: 403") || ex.getMessage().contains("code: 404"))) {

      // Do nothing here since we'll just keep looping with parent
      // path anyway

    } else if (ex != null) {
      Throwables.propagate(ex);
    }

    return p;

  }

  public static Properties loadHosts(String hostsFile, StandardPBEStringEncryptor encryptor) throws IllegalArgumentException {

    log.info("Fetching hosts file from path: " + hostsFile);

    Resource resource = new DefaultResourceLoader().getResource(hostsFile);

    boolean exists = false;
    for (int retry = 0; retry < 3; retry++) {
      if (resource.exists()) {
        exists = true;
        break;
      }

      log.info("Not found. Retrying...");
    }

    if (!exists) {
      throw new IllegalArgumentException("Properties file " + hostsFile + " couldn't be found at location " + hostsFile);
    }

    Properties hosts = new Properties();

    if (encryptor != null)
      hosts = new EncryptableProperties(encryptor);

    try (InputStream stream = resource.getInputStream()) {

      hosts.load(stream);

    } catch (IOException e) {
      log.error("Can't load hosts file", e);
    }

    return hosts;

  }

  public static Properties loadProperties(String propertiesPath, String propertiesFileName, boolean searchClasspath, StandardPBEStringEncryptor encryptor) {

    List<Properties> all = new ArrayList<>();

    if (StringUtils.hasText(propertiesPath)) {

      do {

        all.add(fetchProperties(propertiesPath, propertiesFileName));
        propertiesPath = stripDir(propertiesPath);

      } while (new File(propertiesPath).getParent() != null);
    }

    // Finally, check classpath
    if (searchClasspath) {
      all.add(fetchProperties("classpath:/config/", propertiesFileName));
      all.add(fetchProperties("classpath:", propertiesFileName));
    }

    Collections.reverse(all); // sort from root to highest

    Properties ps = new Properties();

    if (encryptor != null)
      ps = new EncryptableProperties(encryptor);

    for (Properties p : all) {
      ps.putAll(p); // merge down and replace lower properties with override properties
    }

    return ps;

  }

  protected static String stripDir(String path) {

    int i = path.lastIndexOf("/");

    if (i > 0)
      return path.substring(0, i);

    return "";

  }

}
