package io.configrd.core.source;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import io.configrd.core.util.StringUtils;

@SuppressWarnings("serial")
public abstract class DefaultRepoDef implements Serializable, RepoDef {

  protected String name;
  protected String sourceName;
  protected String uri;
  protected String configrdFileName;
  protected Map<String, String> named = new HashMap<>();

  protected Map<String, String> vendor = new HashMap<>();

  Boolean trustCert = false;
  protected DefaultRepoDef() {
    super();
  }
  

  public DefaultRepoDef(String name) {

    super();
    this.name = name;
  }

  public String getConfigrdFileName() {
    return configrdFileName;
  }

  @Override
  public String getName() {
    return name;
  }

  public Map<String, String> getNamed() {
    return named;
  }

  public String getSourceName() {
    return sourceName;
  }

  public Boolean getTrustCert() {
    return trustCert;
  }

  public String getUri() {
    return uri;
  }

  protected Map<String, String> getVendor() {
    return vendor;
  }

  public void setConfigrdFileName(String configrdFileName) {
    this.configrdFileName = configrdFileName;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setNamed(Map<String, String> namedPaths) {
    this.named = namedPaths;
  }

  public void setSourceName(String sourceName) {
    this.sourceName = sourceName;
  }

  public void setTrustCert(Boolean trustCert) {
    this.trustCert = trustCert;
  }

  public void setUri(String uri) {
    this.uri = uri;
  }

  protected void setVendor(Map<String, String> vendor) {
    this.vendor = vendor;
  }

  public String[] valid() {
    
    Set<String> err = new HashSet<>();
    
    if(!StringUtils.hasText(getUri())) {
      err.add("Uri must have a value.");
    }

    if (!StringUtils.hasText(getSourceName())) {
      err.add("Repo's sourceName must be specified.");
    }

    return err.toArray(new String[] {});
  }


}
