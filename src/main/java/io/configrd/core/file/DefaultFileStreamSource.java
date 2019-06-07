package io.configrd.core.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.configrd.core.processor.ProcessorSelector;
import io.configrd.core.source.FileStreamSource;
import io.configrd.core.source.RepoDef;
import io.configrd.core.source.StreamPacket;
import io.configrd.core.source.StreamSource;
import io.configrd.core.util.URIBuilder;
import io.configrd.core.util.UriUtil;

public class DefaultFileStreamSource implements StreamSource, FileStreamSource {

  private final static Logger log = LoggerFactory.getLogger(DefaultFileStreamSource.class);
  private final FileRepoDef def;
  private final URIBuilder builder;

  public DefaultFileStreamSource(FileRepoDef def) {
    this.def = def;
    URI uri = toURI();
    builder = URIBuilder.create(uri);
  }

  protected boolean validateURI(URI uri) {

    return UriUtil.validate(uri).isScheme("classpath", "file").hasPath().hasFile().valid();

  }

  private URI toURI() {
    URIBuilder builder = URIBuilder.create(URI.create(def.getUri()));
    builder.setFileNameIfMissing(def.getFileName());
    return builder.build();
  }

  @Override
  public String getSourceName() {
    return StreamSource.FILE_SYSTEM;
  }

  @Override
  public RepoDef getSourceConfig() {
    return this.def;
  }

  @Override
  public Optional<StreamPacket> stream(final String path) {

    Optional<StreamPacket> p = streamFile(path);
    URI uri = prototypeURI(path);

    try {
      if (p.isPresent()) {
        p.get().putAll(ProcessorSelector.process(uri.toString(), p.get().bytes()));
      }
    } catch (Exception e) {
      log.error(e.getMessage());
    }

    return p;
  }

  @Override
  public void init() {
    // nothing
  }

  @Override
  public URI prototypeURI(String path) {
    return builder.build(path);
  }

  @Override
  public void close() {
    // nothing

  }

  @Override
  public Optional<StreamPacket> streamFile(final String path) {

    StreamPacket p = null;
    final URI uri = prototypeURI(path);

    if (!validateURI(uri)) {
      throw new IllegalArgumentException("Uri " + uri + " is not valid");
    }

    if (uri.getScheme().equalsIgnoreCase("file")) {

      try (InputStream is = new FileInputStream(new File(uri))) {

        if (is != null) {
          p = new StreamPacket(uri, is);
        }

      } catch (FileNotFoundException e) {
        log.debug(e.getMessage());
        // nothing else
      } catch (IOException e) {

      }

    } else if (uri.getScheme().equalsIgnoreCase("classpath")) {

      String trimmed = uri.getSchemeSpecificPart();

      if (!trimmed.startsWith(File.separator))
        trimmed = File.separator + trimmed;

      try (InputStream is = this.getClass().getResourceAsStream(trimmed)) {

        if (is != null) {
          p = new StreamPacket(uri, is);
        }

      } catch (IOException e) {
        log.debug(e.getMessage());
        // nothing else
      }

    } else {
      throw new IllegalArgumentException("Incompatible stream uri " + uri);
    }

    return Optional.ofNullable(p);
  }

}
