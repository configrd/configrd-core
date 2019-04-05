package io.configrd.core.http;

import java.util.Map;
import io.configrd.core.source.ConfigSourceFactory;
import io.configrd.core.source.StreamSource;

public class HttpConfigSourceFactory implements ConfigSourceFactory<DefaultHttpConfigSource> {

  @Override
  public DefaultHttpConfigSource newConfigSource(String name, Map<String, Object> values) {

    DefaultHttpStreamSource source = newStreamSource(name, values);

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

  public DefaultHttpStreamSource newStreamSource(String name, Map<String, Object> values) {

    HttpRepoDef def = new HttpRepoDef(name, values);

    if (def.valid().length > 0) {
      throw new IllegalArgumentException(String.join(",", def.valid()));
    }

    DefaultHttpStreamSource source = new DefaultHttpStreamSource(def);
    source.init();

    return source;
  }

}
