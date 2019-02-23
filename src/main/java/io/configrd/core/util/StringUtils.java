package io.configrd.core.util;

import java.util.HashMap;
import java.util.Map;
import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.text.StringSubstitutor;


public class StringUtils {

  /** Default placeholder prefix: {@value} */
  public static final String DEFAULT_PLACEHOLDER_PREFIX = "${";

  /** Default placeholder suffix: {@value} */
  public static final String DEFAULT_PLACEHOLDER_SUFFIX = "}";

  public static boolean hasText(String string) {
    return (string != null && !string.trim().equals(""));
  }

  protected boolean ignoreUnresolvablePlaceholders = false;

  protected final StringSubstitutor sub;
  protected final Map<String, Object> mapped;

  public StringUtils(Map<String, Object> vals) {

    sub = new StringSubstitutor(vals);
    sub.setVariablePrefix(DEFAULT_PLACEHOLDER_PREFIX);
    sub.setVariableSuffix(DEFAULT_PLACEHOLDER_SUFFIX);
    this.mapped = fillAll(vals);
  }

  public String fill(final Object value) {

    String v = String.valueOf(value);

    if (v.contains(DEFAULT_PLACEHOLDER_PREFIX))
      v = sub.replace(v);

    return v;
  }

  public Map<String, Object> filled() {
    return this.mapped;
  }

  protected Map<String, Object> fillAll(Map<String, Object> vals) {

    Map<String, Object> filled = new HashMap<>();

    vals.entrySet().stream().forEach(e -> {
      filled.put(e.getKey(), fill(e.getValue()));
    });

    return filled;

  }

}
