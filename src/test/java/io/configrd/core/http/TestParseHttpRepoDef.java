package io.configrd.core.http;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.Assert;
import org.junit.Test;
import io.configrd.core.ConfigSourceResolver;
import io.configrd.core.source.ConfigSource;
import io.configrd.core.source.RepoDef;
import io.configrd.core.source.StreamSource;

public class TestParseHttpRepoDef {

  private ConfigSourceResolver resolver;
  private final String yamlFile = "classpath:/repos.yaml";

  @Test
  public void testParseHttpRepoDef() throws Exception {

    Map<String, Object> vals = new HashMap<>();
    vals.put(RepoDef.URI_FIELD, yamlFile);
    vals.put(RepoDef.SOURCE_NAME_FIELD, StreamSource.FILE_SYSTEM);

    resolver = new ConfigSourceResolver(vals);

    Optional<ConfigSource> def = resolver.findConfigSourceByName("git-master");
    Assert.assertFalse(def.isPresent());

    def = resolver.findConfigSourceByName("http-resource");
    Assert.assertTrue(def.isPresent());
    Assert.assertTrue(def.get().getStreamSource().getSourceConfig() instanceof HttpRepoDef);

    HttpRepoDef hrepo = (HttpRepoDef) def.get().getStreamSource().getSourceConfig();
    Assert.assertEquals("http-resource", hrepo.getName());
    Assert.assertEquals(ConfigSourceResolver.DEFAULT_PROPERTIES_FILE_NAME, hrepo.getFileName());
    Assert.assertEquals(
        "https://raw.githubusercontent.com/configrd/configrd-core/master/src/test/resources/",
        hrepo.getUri());
    Assert.assertEquals(2, hrepo.getNamed().size());

  }


}
