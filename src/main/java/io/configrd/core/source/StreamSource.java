package io.configrd.core.source;

import java.io.Closeable;
import java.net.URI;
import java.util.Optional;

public interface StreamSource extends Closeable{

  public static final String FILE_SYSTEM = "file";
  public static final String HTTPS = "http";

  /**
   * Leverages the stream source's source config uri as base and streams paths relative to the
   * configured base URI.
   * 
   * @param path
   * @return
   */
  public Optional<? extends PropertyPacket> stream(String path);

  /**
   * A unique name for this config source type
   * 
   * @return
   */
  public String getSourceName();

  /**
   * Returns the source config of this stream source.
   * 
   * @return
   */
  public RepoDef getSourceConfig();

  public URI prototypeURI(String path);

  public void init();


}
