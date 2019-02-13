package io.configrd.core.file;

import java.net.URI;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.commons.beanutils.BeanUtils;
import io.configrd.core.source.DefaultRepoDef;
import io.configrd.core.source.FileBasedRepo;
import io.configrd.core.util.UriUtil;

@SuppressWarnings("serial")
public class FileRepoDef extends DefaultRepoDef implements FileBasedRepo {

  String fileName;
  String hostsName;

  /**
   * For testing purposes
   */
  public FileRepoDef(String name) {
    super(name);
  }

  public FileRepoDef(String name, Map<String, Object> values) {
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

  public void setFileName(String fileName) {
    this.fileName = fileName;
  }

  public String getHostsName() {
    return hostsName;
  }

  public void setHostsName(String hostsName) {
    this.hostsName = hostsName;
  }

  @Override
  public String[] valid() {

    Set<String> errors = new HashSet<>();

    for (String s : super.valid()) {
      errors.add(s);
    }

    URI uri = URI.create(getUri());

    if (UriUtil.validate(uri).isAbsolute().invalid()) {
      errors.add("Uri must be absolute");
    }

    return errors.toArray(new String[] {});
  }
}
