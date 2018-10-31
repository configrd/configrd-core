package io.configrd.core.http;

import java.util.Map;
import io.configrd.core.SystemProperties;
import io.configrd.core.source.ConfigSource;
import io.configrd.core.source.ConfigSourceFactory;
import io.configrd.core.source.StreamSource;
import io.configrd.core.util.StringUtils;

public class HttpConfigSourceFactory implements ConfigSourceFactory {

  @Override
  public ConfigSource newConfigSource(String name, Map<String, Object> values) {

    StreamSource source = newStreamSource(name, values);

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

  public StreamSource newStreamSource(String name, Map<String, Object> values) {

    HttpRepoDef def = new HttpRepoDef(name, values);

    if (def.getTrustCert() == null
        && StringUtils.hasText(System.getProperty(SystemProperties.HTTP_TRUST_CERTS))) {
      def.setTrustCert(Boolean.valueOf(System.getProperty(SystemProperties.HTTP_TRUST_CERTS)));
    }

    if (def.valid().length > 0) {
      throw new IllegalArgumentException(String.join(",", def.valid()));
    }

    DefaultHttpStreamSource source = new DefaultHttpStreamSource(def);
    source.init();

    return source;
  }

}
