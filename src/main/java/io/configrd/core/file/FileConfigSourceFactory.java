package io.configrd.core.file;

import java.io.File;
import java.util.Map;
import io.configrd.core.source.ConfigSourceFactory;
import io.configrd.core.source.StreamSource;

public class FileConfigSourceFactory implements ConfigSourceFactory<DefaultFileConfigSource> {

  @Override
  public DefaultFileConfigSource newConfigSource(String name, final Map<String, Object> values) {

    DefaultFileStreamSource source = newStreamSource(name, values);

    DefaultFileConfigSource configSource = new DefaultFileConfigSource(source, values);
    return configSource;
  }


  @Override
  public boolean isCompatible(String paths) {

    return (paths == "" || paths.toLowerCase().startsWith(File.separator + File.separator)
        || paths.toLowerCase().startsWith("file:") || paths.toLowerCase().startsWith("classpath"));
  }

  @Override
  public String getSourceName() {
    return StreamSource.FILE_SYSTEM;
  }

  public DefaultFileStreamSource newStreamSource(String name, Map<String, Object> values) {

    FileRepoDef def = new FileRepoDef(name, values);

    if (def.valid().length > 0) {
      throw new IllegalArgumentException(String.join(",", def.valid()));
    }

    DefaultFileStreamSource source = new DefaultFileStreamSource(def);
    source.init();

    return source;
  }



}
