package io.configrd.core.source;

import java.util.Optional;

public interface FileStreamSource {

  /**
   * Get byte representation of a file with path relative to stream source configuration.
   * 
   * @param uri
   * @return
   */
  public Optional<StreamPacket> streamFile(String uri);

}
