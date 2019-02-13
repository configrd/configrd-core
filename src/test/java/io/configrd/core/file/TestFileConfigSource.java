package io.configrd.core.file;

import java.util.HashMap;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import com.google.common.base.Splitter;
import com.google.common.collect.Sets;
import io.configrd.core.source.ConfigSource;
import io.configrd.core.source.ConfigSourceFactory;
import io.configrd.core.source.FileBasedRepo;
import io.configrd.core.source.RepoDef;


public class TestFileConfigSource {

  private ConfigSource source;
  private ConfigSourceFactory factory = new FileConfigSourceFactory();

  Map<String, Object> defaults = null;

  @Rule
  public TemporaryFolder folder = new TemporaryFolder();

  @Before
  public void before() throws Exception {
    folder.create();
    System.out.println("from: " + FileUtils.toFile(this.getClass().getResource("/")));
    System.out.println("to: " + folder.getRoot().toPath());
    FileUtils.copyDirectory(FileUtils.toFile(getClass().getResource("/")), folder.getRoot());

    defaults = new HashMap<>();
    defaults.put(FileBasedRepo.FILE_NAME_FIELD, "default.properties");
    defaults.put(FileBasedRepo.HOSTS_FILE_NAME_FIELD, "hosts.properties");
    defaults.put(RepoDef.SOURCE_NAME_FIELD, "file");

  }

  @After
  public void cleanup() throws Exception {
    FileUtils.forceDelete(folder.getRoot());
    defaults.clear();
  }

  @Test
  public void loadClasspathProperties() throws Exception {

    defaults.putAll((Map) Splitter.on(",").omitEmptyStrings().trimResults()
        .withKeyValueSeparator("=").split("uri=classpath:/"));

    source = source = factory.newConfigSource("TestFileConfigSource", defaults);

    Map<String, Object> p = source.get("env/dev/default.properties", Sets.newHashSet());
    Assert.assertNotNull(p);
    Assert.assertTrue(p.containsKey("property.1.name"));
    Assert.assertEquals(p.get("property.1.name"), "value1");

  }

  @Test
  public void loadAppendDefaultFileName() throws Exception {

    defaults.putAll((Map) Splitter.on(",").omitEmptyStrings().trimResults()
        .withKeyValueSeparator("=").split("uri=classpath:/"));

    source = source = factory.newConfigSource("TestFileConfigSource", defaults);

    Map<String, Object> p = source.get("env/dev/", Sets.newHashSet());
    Assert.assertNotNull(p);
    Assert.assertTrue(p.containsKey("property.1.name"));
    Assert.assertEquals(p.get("property.1.name"), "value1");

  }

  @Test
  public void loadFileProperties() throws Exception {

    defaults.putAll((Map) Splitter.on(",").omitEmptyStrings().trimResults()
        .withKeyValueSeparator("=").split("uri=file:" + folder.getRoot()));

    source = source = factory.newConfigSource("TestFileConfigSource", defaults);

    Map<String, Object> p = source.get("/env/dev/default.properties", Sets.newHashSet());
    Assert.assertNotNull(p);
    Assert.assertTrue(p.containsKey("property.1.name"));
    Assert.assertEquals(p.get("property.1.name"), "value1");

  }

  @Test(expected = IllegalArgumentException.class)
  public void loadClasspathRelativePath() throws Exception {

    defaults.putAll((Map) Splitter.on(",").omitEmptyStrings().trimResults()
        .withKeyValueSeparator("=").split("uri=/env/dev/"));

    source = source = factory.newConfigSource("TestFileConfigSource", defaults);

    Map<String, Object> p = source.get("/", Sets.newHashSet());
    Assert.assertNotNull(p);
    Assert.assertTrue(p.containsKey("property.1.name"));
    Assert.assertEquals(p.get("property.1.name"), "value1");

  }

  @Test
  public void testLoadHosts() throws Exception {

    defaults.putAll((Map) Splitter.on(",").omitEmptyStrings().trimResults()
        .withKeyValueSeparator("=").split("uri=classpath:/"));

    source = source = factory.newConfigSource("TestFileConfigSource", defaults);

    Map<String, Object> p = source.getRaw("env/hosts.properties");
    Assert.assertNotNull(p);
    Assert.assertTrue(p.containsKey("michelangello"));
    Assert.assertEquals(p.get("michelangello"), "classpath:/env/dev/");

  }
}
