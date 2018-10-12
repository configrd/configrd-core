package io.configrd.core.http;

import java.util.HashMap;
import java.util.Map;
import io.configrd.core.source.ConfigSource;
import io.configrd.core.source.ConfigSourceFactory;
import io.configrd.core.source.StreamSource;

public class HttpConfigSourceFactory implements ConfigSourceFactory {

  @Override
  public ConfigSource newConfigSource(String name, Map<String, Object> values,
      Map<String, Object> defaults) {

    StreamSource source = newStreamSource(name, values, defaults);

    DefaultHttpConfigSource configSource = new DefaultHttpConfigSource(source, values);
    return configSource;
  }

  @Override
  public String getSourceName() {
    return StreamSource.HTTPS;
  }

  @Override
  public boolean isCompatible(String path) {
    return path.trim().startsWith("http");
  }

  public StreamSource newStreamSource(String name, Map<String, Object> values,
      Map<String, Object> defaults) {

    final Map<String, Object> merged = new HashMap<>(defaults);
    merged.putAll((Map) values);

    HttpRepoDef def = new HttpRepoDef(name, merged);

    if (def.valid().length > 0) {
      throw new IllegalArgumentException(String.join(",", def.valid()));
    }

    DefaultHttpStreamSource source = new DefaultHttpStreamSource(def);
    source.init();

    return source;
  }

}
