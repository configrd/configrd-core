package io.configrd.core;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
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
import io.configrd.core.source.FileConfigSource;
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

  private LinkedHashMap<String, Object> repos;

  public final static String DEFAULT_HOSTS_FILE_NAME = "hosts.properties";
  public static final String DEFAULT_CONFIG_FILE = "configrd.yaml";
  public final static String DEFAULT_PROPERTIES_FILE_NAME = "default.properties";

  public ConfigSourceResolver() {

    defaults.put(FileRepoDef.FILE_NAME_FIELD, DEFAULT_PROPERTIES_FILE_NAME);

    streamSourceLoader = ServiceLoader.load(ConfigSourceFactory.class, getClass().getClassLoader());

    for (ConfigSourceFactory f : streamSourceLoader) {
      logger.debug("Found " + f.getSourceName() + " config source on classpath.");
    }

  }

  public ConfigSourceResolver(final Map<String, Object> vals) {

    this();

    LinkedHashMap<String, Object> y = loadRepoDefFile(vals);

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

            Optional<ConfigSource> cs = buildConfigSource(entry);
            if (cs.isPresent()) {
              reposByName.put(cs.get().getName().toLowerCase(), cs.get());
            }

          } catch (Exception e) {
            logger.error(e.getMessage());
            // allow to continue loading other repos
          }

        }
      } else {
        logger.error("Found no 'repos' definitions in configrd config file.");
      }
    }
  }

  public Map<String, Object> getDefaults() {
    return defaults;
  }

  protected LinkedHashMap<String, Object> loadRepoDefFile(Map<String, Object> vals) {

    defaults.entrySet().forEach(e -> vals.putIfAbsent(e.getKey(), e.getValue()));

    String configUri = (String) vals.get(RepoDef.URI_FIELD);
    String fileName = (String) vals.get(RepoDef.CONFIGRD_CONFIG_FILENAME_FIELD);
    String sourceName = (String) vals.get(RepoDef.SOURCE_NAME_FIELD);

    if (UriUtil.hasFile(configUri) && !StringUtils.hasText(fileName)) {

      Optional<String> name = UriUtil.getFileName(configUri);
      if (name.isPresent()) {
        fileName = name.get();
      }

    } else if (!UriUtil.hasFile(configUri) && !StringUtils.hasText(fileName)) {
      fileName = DEFAULT_CONFIG_FILE;
    }

    logger.info("Loading configrd configuration file from " + configUri + " using stream source: "
        + sourceName);

    Optional<ConfigSourceFactory> cs = resolveFactoryBySourceName(sourceName);

    LinkedHashMap<String, Object> y = new LinkedHashMap<>();

    if (cs.isPresent()) {

      ConfigSource source = cs.get().newConfigSource("init", vals);

      logger.debug("Found stream source of type: " + source.getStreamSource().getSourceName());

      if (source instanceof FileConfigSource) {

        Optional<StreamPacket> stream = ((FileConfigSource) source).getFile(fileName);

        if (stream.isPresent()) {

          try (InputStream s = ((StreamPacket) stream.get()).getInputStream()) {

            Yaml yaml = new Yaml();
            y = (LinkedHashMap) yaml.load(s);

          } catch (IOException e2) {
            // nothing
          }

        } else {
          throw new IllegalArgumentException(
              "Unable to fetch repo definitions from location " + configUri);
        }

      } else {

        throw new IllegalStateException(
            "Unable to find compatible config stream source to bootstrap with.");

      }

    } else {
      throw new IllegalStateException(
          "Unable to find config stream source '" + sourceName + "' to load " + fileName);
    }

    return y;
  }

  protected Optional<ConfigSource> buildConfigSource(Entry<String, Object> entry) {

    Optional<ConfigSource> oc = Optional.empty();

    if (entry.getValue() instanceof LinkedHashMap) {

      LinkedHashMap<String, Object> repo = (LinkedHashMap) entry.getValue();
      final String repoName = entry.getKey();

      oc = buildConfigSource(repoName, repo);
    }

    return oc;
  }

  public Optional<ConfigSource> buildConfigSource(String repoName, Map<String, Object> vals) {

    Optional<ConfigSource> oc = Optional.empty();

    // copy down defaults
    defaults.entrySet().stream().forEach(e -> vals.putIfAbsent(e.getKey(), e.getValue()));

    Optional<ConfigSourceFactory> factory = Optional.empty();
    if (vals.containsKey(RepoDef.SOURCE_NAME_FIELD)) {
      factory = resolveFactoryBySourceName((String) vals.get(RepoDef.SOURCE_NAME_FIELD));
    }

    if (factory.isPresent()) {

      ConfigSource initializedSource = factory.get().newConfigSource(repoName.toLowerCase(), vals);

      oc = Optional.of(initializedSource);

    } else {
      logger.warn("Unable to find a config source matching '" + vals.get(RepoDef.SOURCE_NAME_FIELD)
          + "' for repo " + repoName + ".");
    }

    return oc;
  }

  public Optional<ConfigSource> findByRepoName(String repoName) {

    if (!StringUtils.hasText(repoName))
      repoName = ConfigSourceResolver.DEFAULT_REPO_NAME;

    return Optional.ofNullable(reposByName.get(repoName.toLowerCase()));
  }

  protected Optional<ConfigSourceFactory> resolveFactoryBySourceName(String sourceName) {

    Optional<ConfigSourceFactory> ocs =
        StreamSupport.stream(streamSourceLoader.spliterator(), false).filter(s -> {
          return sourceName.toLowerCase().contains(s.getSourceName().toLowerCase());
        }).findFirst();

    return ocs;

  }

  protected Set<ConfigSourceFactory> resolveFactoryByUri(final URI uri) {

    Set<ConfigSourceFactory> ocs =
        StreamSupport.stream(streamSourceLoader.spliterator(), false).filter(s -> {
          return s.isCompatible(uri.toString());
        }).collect(Collectors.toSet());

    return ocs;

  }

}
