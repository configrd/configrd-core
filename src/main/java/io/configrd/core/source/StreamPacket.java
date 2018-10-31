package io.configrd.core.source;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Collection;
import eu.medsea.mimeutil.MimeType;
import eu.medsea.mimeutil.MimeUtil2;
import io.configrd.core.util.UriUtil;

@SuppressWarnings("serial")
public class StreamPacket extends PropertyPacket {

  private final static MimeUtil2 mimes = new MimeUtil2();
  private byte[] bytes = new byte[0];
  private String contentType;
  private String encoding = "UTF-8";

  public StreamPacket(URI uri, InputStream stream) throws FileNotFoundException {
    super(uri);
    readBytes(stream, 1024);
    detectMediaType();

  }

  public StreamPacket(URI uri, InputStream stream, long bytes) throws FileNotFoundException {
    super(uri);
    readBytes(stream, bytes);
    detectMediaType();

  }

  public StreamPacket(URI uri, InputStream stream, String contentType)
      throws FileNotFoundException {
    this(uri, stream);
    this.contentType = contentType;
  }

  public byte[] bytes() throws IOException {
    return this.bytes;
  }

  private void detectMediaType() {

    String fileName = UriUtil.getFileName(getUri()).orElse(null);

    if (fileName != null) {

      Collection<MimeType> types = mimes.getMimeTypes(fileName);

      if (types.isEmpty()) {

      }

    } else {

      contentType = (String) mimes.getMimeTypes(bytes).stream().map(m -> {
        return ((MimeType) m).toString();
      }).findFirst().orElse("");

    }

  }

  public String getContentType() {
    return contentType;
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

    try(ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {

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
