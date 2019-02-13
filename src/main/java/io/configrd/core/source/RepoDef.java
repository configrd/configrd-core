package io.configrd.core.source;

import java.util.Map;

public interface RepoDef {

  public static final String NAME_FIELD = "name";
  public static final String NAMED_PATHS_FIELD = "named";
  public static final String URI_FIELD = "uri";
  public static final String SOURCE_NAME_FIELD = "sourceName";
  public static final String TRUST_CERTS_FIELD = "trustCert";
  public static final String CONFIGRD_CONFIG_FILENAME_FIELD = "configrdFileName";

  public String getName();

  public String getSourceName();

  public Map<String, String> getNamed();

  public String[] valid();

  public Boolean getTrustCert();
  
  public String getConfigrdFileName();
  
  
}

