package io.configrd.core.file;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.configrd.core.source.DefaultConfigSource;
import io.configrd.core.source.FileConfigSource;
import io.configrd.core.source.FileStreamSource;
import io.configrd.core.source.PropertyPacket;
import io.configrd.core.source.StreamPacket;
import io.configrd.core.source.StreamSource;

public class DefaultFileConfigSource extends DefaultConfigSource implements FileConfigSource {

  private final static Logger log = LoggerFactory.getLogger(DefaultFileConfigSource.class);

  protected DefaultFileConfigSource(StreamSource source, Map<String, Object> values) {
    super(source, values);
  }

  @Override
  public Map<String, Object> getRaw(String path) {

    Optional<? extends PropertyPacket> stream = streamSource.stream(path);

    if (!stream.isPresent())
      return new HashMap<>();

    return stream.get();
  }


  @Override
  public boolean isCompatible(StreamSource source) {
    return source instanceof DefaultFileStreamSource;
  }

  @Override
  public Optional<StreamPacket> getFile(String path) {
    return ((FileStreamSource) streamSource).streamFile(path);
  }

}
