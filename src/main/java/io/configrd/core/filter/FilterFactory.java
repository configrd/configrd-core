package io.configrd.core.filter;

import java.util.Map;

public interface FilterFactory {

  public <T> T build(Map<String, Object> vals, Class<? extends Filter> type);

  public String getName();

  public boolean capableOf(Class<? extends Filter> clazz);

}
