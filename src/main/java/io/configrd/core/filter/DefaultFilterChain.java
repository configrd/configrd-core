package io.configrd.core.filter;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class DefaultFilterChain<T extends Filter> implements FilterChain {

  protected final LinkedList<T> chain = new LinkedList<>();

  public DefaultFilterChain() {}

  @Override
  public Map<String, Object> apply(Map<String, Object> props) {

    if (props != null && !props.isEmpty()) {
      Map<String, Object> copy = new HashMap<String, Object>(props);

      for (Filter f : chain) {
        copy = f.apply(copy);
      }
      
      return copy;
    }

    return props;
  }

  public void isFirst(T filter) {
    this.chain.addFirst(filter);
  }

  public void isLast(T filter) {
    this.chain.addLast(filter);
  }

  public void addFilter(T filter) {

    if (filter != null)
      this.chain.add(filter);
  }

}
