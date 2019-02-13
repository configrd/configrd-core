package io.configrd.core.source;

import java.util.Optional;

public interface FileConfigSource {

  /**
   * Get file per path
   * 
   * @param path
   * @return
   */
  public Optional<StreamPacket> getFile(String path);

}
