package io.configrd.core.source;

import java.util.Map;
import java.util.Set;

public interface ConfigSource<T extends StreamSource> {

  /**
   * Traverses a config tree per the underlying implementation's mechanism.
   * 
   * @param path Path extending the pre-configured uri in the repo definition
   * @return
   */
  public Map<String, Object> get(String path, Set<String> names);

  /**
   * Retrieves a single node of the config given by the properties path. No traversal.
   * 
   * @param path Path extending the pre-configured uri in the repo definition
   * @return
   */
  public Map<String, Object> getRaw(String path);

  public T getStreamSource();

  public boolean isCompatible(StreamSource source);

  public String getName();

  public Map<String, String> getNamedPaths();

}
