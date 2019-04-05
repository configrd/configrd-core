package io.configrd.core.filter;

import java.util.Map;

public interface Filter {

  public Map<String, Object> apply(Map<String, Object> vals);
  
  public String getName();
  
}
