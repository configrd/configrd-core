package io.configrd.core.source;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

@SuppressWarnings("serial")
public class StreamPacket extends PropertyPacket {

  private byte[] bytes = new byte[0];
  private String encoding = "UTF-8";

  public StreamPacket(URI uri) {
    super(uri);
  }

  public StreamPacket(URI uri, InputStream stream) throws FileNotFoundException {
    super(uri);
    readBytes(stream, 1024);
  }

  public StreamPacket(URI uri, InputStream stream, long bytes) throws FileNotFoundException {
    super(uri);
    readBytes(stream, bytes);
  }

  public byte[] bytes() throws IOException {
    return this.bytes;
  }

  public String getEncoding() {
    return encoding;
  }

  public InputStream getInputStream() {
    return new ByteArrayInputStream(this.bytes);
  }

  public boolean hasContent() {
    return bytes.length > 0;
  }

  private void readBytes(final InputStream stream, long bytes) throws FileNotFoundException {

    if (stream == null) {
      return;
    }

    int nRead;

    if (bytes > Integer.MAX_VALUE)
      bytes = Integer.MAX_VALUE;

    byte[] data = new byte[Math.toIntExact(bytes)];

    try (ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {

      while ((nRead = stream.read(data, 0, data.length)) != -1) {
        buffer.write(data, 0, nRead);
      }

      buffer.flush();
      this.bytes = buffer.toByteArray();

    } catch (FileNotFoundException e) {

      throw e;

    } catch (IOException e) {

      // nothing

    } finally {

      try {
        stream.close();
      } catch (Exception e) {
        // TODO: handle exception
      }
    }

  }

  public void setEncoding(String encoding) {
    this.encoding = encoding;
  }
}
