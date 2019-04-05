package io.configrd.core.source;

import java.util.Map;

public interface ConfigSourceFactory<T extends ConfigSource<? extends StreamSource>> {

  public T newConfigSource(String name, final Map<String, Object> values);

  /**
   * Method resolving to true or false depending if the given paths are compatible with this
   * source's capabilities. I.e. the method could look at the path prefix (file://, classpath:) and
   * determine if it can handle this path or not.
   * 
   * @param paths
   * @return
   */
  public boolean isCompatible(String path);

  /**
   * A unique name for this config source type
   * 
   * @return
   */
  public String getSourceName();

}
