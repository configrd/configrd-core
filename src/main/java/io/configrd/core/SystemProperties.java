package io.configrd.core;

public interface SystemProperties {
  
  /**
   * Should HTTP config streams trust all certificates? True/False
   */
  public static final String HTTP_TRUST_CERTS = "configrd.source.http.cert.trust";
  
  /**
   * Absolute location of the configrd config yaml file
   */
  public static final String CONFIGRD_CONFIG = "configrd.config.location";
  
  /**
   * Which config source should be used to fetch the configrd config yaml file?
   */
  public static final String CONFIGRD_CONFIG_SOURCE = "configrd.config.source";

}
