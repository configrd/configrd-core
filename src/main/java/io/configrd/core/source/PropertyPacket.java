package io.configrd.core.source;

import java.net.URI;
import java.util.HashMap;
import java.util.Properties;
import org.apache.commons.beanutils.ConvertUtils;
import io.configrd.core.Config;

@SuppressWarnings("serial")
public class PropertyPacket extends HashMap<String, Object> implements Config {

  private String eTag;
  private final URI uri;

  public PropertyPacket(URI uri) {
    super();
    this.uri = uri;
  }

  public String getETag() {
    return eTag;
  }

  public URI getUri() {
    return uri;
  }

  public void setETag(String eTag) {
    this.eTag = eTag;
  }

  @Override
  public Properties getProperties() {
    Properties p = new Properties();
    p.putAll(this);
    return p;
  }

  @Override
  public <T> T getProperty(String key, Class<T> clazz) {
    return (T) ConvertUtils.convert(this.get(key), clazz);
  }

  @Override
  public <T> T getProperty(String key, Class<T> clazz, T defaultVal) {
    return (T) ConvertUtils.convert(this.getOrDefault(key, defaultVal), clazz);
  }

}
