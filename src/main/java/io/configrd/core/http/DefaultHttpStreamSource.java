package io.configrd.core.http;

import java.io.IOException;
import java.net.URI;
import java.net.UnknownHostException;
import java.security.cert.CertificateException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.configrd.core.processor.ProcessorSelector;
import io.configrd.core.source.AdHocStreamSource;
import io.configrd.core.source.PropertyPacket;
import io.configrd.core.source.StreamPacket;
import io.configrd.core.source.StreamSource;
import io.configrd.core.util.StringUtils;
import io.configrd.core.util.URIBuilder;
import io.configrd.core.util.UriUtil;
import okhttp3.Credentials;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Request.Builder;
import okhttp3.Response;
import okhttp3.Route;

public class DefaultHttpStreamSource implements StreamSource, AdHocStreamSource {



  protected OkHttpClient client;
  public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
  private final static Logger log = LoggerFactory.getLogger(DefaultHttpStreamSource.class);
  private final HttpRepoDef def;
  private final URIBuilder builder;


  public DefaultHttpStreamSource(HttpRepoDef def) {
    client = new OkHttpClient();
    this.def = def;
    URI uri = def.toURI();
    builder = URIBuilder.create(uri);
  }

  public void init() {

    OkHttpClient.Builder builder = new OkHttpClient.Builder();

    if (def.getTrustCert()) {
      try {

        final SSLContext sslContext = SSLContext.getInstance("TSL");
        sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
        final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

        builder.sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts[0]);
        builder.hostnameVerifier(new HostnameVerifier() {
          @Override
          public boolean verify(String hostname, SSLSession session) {
            return true;
          }
        });

      } catch (Exception e) {
        // TODO: handle exception
      }
    }

    if (StringUtils.hasText(def.getPassword())) {
      final AtomicInteger result = new AtomicInteger(0);
      builder.authenticator(new okhttp3.Authenticator() {


        @Override
        public Request authenticate(Route route, Response response) throws IOException {

          while ((response = response.priorResponse()) != null) {
            if (result.incrementAndGet() >= 3) {
              return null;
            }
          }

          String credential = Credentials.basic(def.getUsername(), def.getPassword());
          return response.request().newBuilder().header("Authorization", credential).build();
        }
      });
    }

    builder.connectTimeout(10, TimeUnit.SECONDS);
    builder.writeTimeout(10, TimeUnit.SECONDS);
    builder.readTimeout(30, TimeUnit.SECONDS);

    client = builder.build();

  }

  public Optional<PropertyPacket> stream(URI uri) {

    Optional<PropertyPacket> stream = Optional.empty();
    Builder request = new Request.Builder().url(uri.toString()).get();
    log.debug(request.build().url().uri().toString());

    if (!validateURI(uri)) {
      throw new IllegalArgumentException("Uri " + uri + " is not valid");
    }

    try (Response call = client.newCall(request.build()).execute()) {

      if (call.isSuccessful() && !call.isRedirect() && call.body().contentLength() > 0) {

        StreamPacket packet = new StreamPacket(uri, call.body().byteStream());
        packet.setETag(call.header("ETag"));
        packet.putAll(ProcessorSelector.process(uri.toString(), packet.bytes()));
        stream = Optional.of(packet);

      } else if (call.isSuccessful() && call.isRedirect()) {

        log.error("Redirect handling not implemented. Server returned location "
            + call.header("location"));
      }

    } catch (UnknownHostException e) {

      log.error(e.getMessage(), e);
      throw new IllegalArgumentException(e.getMessage());

    } catch (Exception e) {
      log.debug(e.getMessage(), e);
      // nothing else
    }

    return stream;
  }

  @Override
  public String getSourceName() {
    return StreamSource.HTTPS;
  }

  @Override
  public HttpRepoDef getSourceConfig() {
    return def;
  }

  private boolean validateURI(URI uri) {
    return UriUtil.validate(uri).hasScheme().isScheme("http", "https").hasHost().hasPath().hasFile()
        .valid();
  }

  @Override
  public Optional<PropertyPacket> stream(String path) {
    Optional<PropertyPacket> is = Optional.empty();

    if (builder != null) {

      URI tempUri = prototypeURI(path);
      is = stream(tempUri);

    }

    return is;
  }


  @Override
  public URI prototypeURI(String path) {
    return builder.build(path);
  }

  @Override
  public void close() {
    // nothing
  }

  private static final TrustManager[] trustAllCerts = new TrustManager[] {new X509TrustManager() {
    @Override
    public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType)
        throws CertificateException {}

    @Override
    public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType)
        throws CertificateException {}

    @Override
    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
      return new java.security.cert.X509Certificate[] {};
    }
  }};

}
