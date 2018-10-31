package io.configrd.core;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;
import io.configrd.core.file.FileRepoDef;
import io.configrd.core.source.ConfigSource;
import io.configrd.core.source.ConfigSourceFactory;
import io.configrd.core.source.PropertyPacket;
import io.configrd.core.source.RepoDef;
import io.configrd.core.source.StreamPacket;
import io.configrd.core.source.StreamSource;
import io.configrd.core.util.StringUtils;
import io.configrd.core.util.UriUtil;

public class ConfigSourceResolver {

  private final static Logger logger = LoggerFactory.getLogger(ConfigSourceResolver.class);

  public final static String DEFAULT_REPO_NAME = "default";

  final Map<String, Object> defaults = new HashMap<>();
  final ServiceLoader<ConfigSourceFactory> streamSourceLoader;

  final Map<String, ConfigSource> reposByName = new HashMap<>();
  final Map<String, StreamSource> typedSources = new HashMap<>();

  public static final String ADHOC_SOURCE = "configrd.source.adhoc";

  private static final String configrd_config_source =
      System.getProperty(SystemProperties.CONFIGRD_CONFIG_SOURCE);

  private LinkedHashMap<String, Object> repos;

  public ConfigSourceResolver() {
    this(System.getProperty(SystemProperties.CONFIGRD_CONFIG, "classpath:repo-defaults.yml"));
  }

  public ConfigSourceResolver(String repoDefPath) {

    defaults.put(FileRepoDef.HOSTS_FILE_NAME_FIELD, "hosts.properties");
    defaults.put(FileRepoDef.FILE_NAME_FIELD, "defaults.properties");

    streamSourceLoader = ServiceLoader.load(ConfigSourceFactory.class);

    if (repoDefPath.equalsIgnoreCase("classpath:repo-defaults.yml")) {
      logger.warn("Loading default configrd configuration file at " + repoDefPath);
    } else {
      logger.info("Loading configrd configuration file from " + repoDefPath);
    }


    LinkedHashMap<String, Object> y = loadRepoDefFile(URI.create(repoDefPath));
    loadRepoConfig(y);

    if (y.containsKey("service")) {

      LinkedHashMap<String, Object> service = (LinkedHashMap) y.get("service");

      if (service.containsKey("defaults")) {
        defaults.putAll((Map) service.get("defaults"));
      }

      if (service.containsKey("repos")) {

        repos = (LinkedHashMap) service.get("repos");
        for (Entry<String, Object> entry : repos.entrySet()) {

          try {

            LinkedHashMap<String, Object> repo = (LinkedHashMap) entry.getValue();

            // copy down defaults
            defaults.entrySet().stream().forEach(e -> repo.putIfAbsent(e.getKey(), e.getValue()));

            Optional<ConfigSource> cs = buildConfigSource(entry);
            if (cs.isPresent())
              reposByName.put(cs.get().getName().toLowerCase(), cs.get());

          } catch (Exception e) {
            logger.error(e.getMessage());
            // allow to continue loading other repos
          }

        }
      }
    }
  }

  public Map<String, Object> getDefaults() {
    return defaults;
  }

  protected LinkedHashMap<String, Object> loadRepoDefFile(URI repoDefPath) {

    Optional<ConfigSource> cs = buildAdHocConfigSource(repoDefPath);

    if (cs.isPresent()) {

      String path = UriUtil.getPath(repoDefPath);

      Optional<PropertyPacket> stream = cs.get().getStreamSource().stream(path);

      if (stream.isPresent() && stream.get() instanceof StreamPacket) {

        try (InputStream s = ((StreamPacket) stream.get()).getInputStream()) {

          Yaml yaml = new Yaml();
          LinkedHashMap<String, Object> y = (LinkedHashMap) yaml.load(s);
          return y;

        } catch (IOException e) {
          // TODO: handle exception
        }

      } else {
        throw new IllegalArgumentException(
            "Unable to fetch repo definitions from location " + repoDefPath);
      }
    }

    return new LinkedHashMap<String, Object>();
  }

  protected Optional<ConfigSource> buildConfigSource(Entry<String, Object> entry) {

    Optional<ConfigSource> oc = Optional.empty();

    if (entry.getValue() instanceof LinkedHashMap) {

      LinkedHashMap<String, Object> repo = (LinkedHashMap) entry.getValue();

      final String repoName = entry.getKey();

      URI uri = null;
      if (repo.containsKey(RepoDef.URI_FIELD)) {
        uri = URI.create((String) repo.get(RepoDef.URI_FIELD));
      } else {
        throw new IllegalArgumentException(
            "No " + RepoDef.URI_FIELD + " found for repo " + repoName + ". Is required.");
      }

      Optional<ConfigSourceFactory> factory = Optional.empty();
      if (repo.containsKey(RepoDef.STREAM_SOURCE_FIELD)) {
        factory = resolveFactorySourceName((String) repo.get(RepoDef.STREAM_SOURCE_FIELD));
      }

      if (!factory.isPresent()) {
        Set<ConfigSourceFactory> sources = resolveFactoryByUri(uri);

        if (!sources.isEmpty())
          factory = Optional.of(sources.iterator().next());
      }

      if (factory.isPresent()) {

        ConfigSource initializedSource =
            factory.get().newConfigSource(repoName.toLowerCase(), (Map) entry.getValue());

        oc = Optional.of(initializedSource);

      }
    }

    return oc;
  }

  protected void loadRepoConfig(LinkedHashMap<String, Object> y) {

    if (y.containsKey("service")) {

      LinkedHashMap<String, Object> service = (LinkedHashMap) y.get("service");

      if (service.containsKey("defaults")) {
        defaults.putAll((Map) service.get("defaults"));
      }
    }
  }

  public Optional<ConfigSource> buildAdHocConfigSource(final URI uri) {

    Set<ConfigSourceFactory> sources = new HashSet<>();

    if (StringUtils.hasText(configrd_config_source)) {

      Optional<ConfigSourceFactory> named = resolveFactorySourceName(configrd_config_source);
      if (named.isPresent()) {
        sources.add(named.get());
      }

    }

    if (sources.isEmpty()) {
      sources = resolveFactoryByUri(uri);
    }

    Optional<ConfigSource> source = Optional.empty();

    if (!sources.isEmpty()) {
      ConfigSourceFactory csf = sources.iterator().next();
      URI root = UriUtil.getRoot(uri);

      Map<String, Object> values = new HashMap<>();
      values.put("uri", root.toString());
      defaults.entrySet().forEach(e -> values.putIfAbsent(e.getKey(), e.getValue()));
      source = Optional.of(csf.newConfigSource("configrd.source.adhoc", values));
    }

    return source;
  }

  public Optional<ConfigSource> findByRepoName(String repoName) {

    if (!StringUtils.hasText(repoName))
      repoName = ConfigSourceResolver.DEFAULT_REPO_NAME;

    return Optional.ofNullable(reposByName.get(repoName.toLowerCase()));
  }

  public Optional<ConfigSourceFactory> resolveFactorySourceName(String sourceNames) {

    Optional<ConfigSourceFactory> ocs =
        StreamSupport.stream(streamSourceLoader.spliterator(), false).filter(s -> {
          return sourceNames.contains(s.getSourceName());
        }).findFirst();

    return ocs;

  }

  public Set<ConfigSourceFactory> resolveFactoryByUri(final URI uri) {

    Set<ConfigSourceFactory> ocs =
        StreamSupport.stream(streamSourceLoader.spliterator(), false).filter(s -> {
          return s.isCompatible(uri.toString());
        }).collect(Collectors.toSet());

    return ocs;

  }

}
