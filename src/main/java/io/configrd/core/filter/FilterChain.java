package io.configrd.core.filter;

import java.util.Map;

public interface FilterChain {

  public Map<String, Object> apply(Map<String, Object> props);
  
}
