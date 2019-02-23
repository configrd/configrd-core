package io.configrd.core.http;

import java.net.URI;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.commons.beanutils.BeanUtils;
import io.configrd.core.source.DefaultRepoDef;
import io.configrd.core.source.FileBasedRepo;
import io.configrd.core.source.SecuredRepo;
import io.configrd.core.util.StringUtils;

@SuppressWarnings("serial")
public class HttpRepoDef extends DefaultRepoDef implements FileBasedRepo, SecuredRepo {

  String fileName;

  String hostsName;

  String password;

  String username;


  /**
   * For testing convenience
   */
  protected HttpRepoDef() {
    super();
  }

  public HttpRepoDef(String name) {
    this.name = name;
  }

  public HttpRepoDef(String name, Map<String, Object> values) {
    super(name);

    try {
      if (values != null && !values.isEmpty())
        BeanUtils.populate(this, values);
    } catch (Exception e) {
      throw new IllegalArgumentException(e);
    }
  }

  public String getFileName() {
    return fileName;
  }

  public String getHostsName() {
    return hostsName;
  }

  @Override
  public String getPassword() {
    return password;
  }

  public String getUri() {
    return uri;
  }

  @Override
  public String getUsername() {
    return username;
  }

  public void setFileName(String fileName) {
    this.fileName = fileName;
  }

  public void setHostsName(String hostsName) {
    this.hostsName = hostsName;
  }

  public void setPassword(String passWord) {
    this.password = passWord;
  }

  public void setUri(String uri) {
    this.uri = uri;
  }

  public void setUsername(String userName) {
    this.username = userName;
  }

  @Override
  public String[] valid() {

    Set<String> errors = new HashSet<>();

    for (String s : super.valid()) {
      errors.add(s);
    }

    if (!(StringUtils.hasText(getUri()) && StringUtils.hasText(getFileName()))) {
      errors.add("Missing required values. Uri, configFileName are all required");
    } else {

      try {
        URI.create(getUri());
      } catch (IllegalArgumentException e) {
        errors.add("Uri is malformed or missing. Error:" + e.getMessage());
      }
    }

    return errors.toArray(new String[] {});
  }

  @Override
  public String getAuthMethod() {
    return "HttpBasicAuth";
  }

}
