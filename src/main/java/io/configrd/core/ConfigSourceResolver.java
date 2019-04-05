package io.configrd.core;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.stream.StreamSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;
import io.configrd.core.file.FileRepoDef;
import io.configrd.core.filter.DefaultFilterChain;
import io.configrd.core.filter.Filter;
import io.configrd.core.filter.FilterChain;
import io.configrd.core.filter.FilterFactory;
import io.configrd.core.filter.RequestFilter;
import io.configrd.core.filter.ResponseFilter;
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
  final ServiceLoader<FilterFactory> filterLoader;
  ConfigrdConfigLoader configLoader;

  final Map<String, ConfigSource> reposByName = new HashMap<>();
  final Map<String, DefaultFilterChain<ResponseFilter>> responseFilterChain = new HashMap<>();
  final Map<String, DefaultFilterChain<RequestFilter>> requestFilterChain = new HashMap<>();
  final Map<String, StreamSource> typedSources = new HashMap<>();

  private LinkedHashMap<String, Object> repos;

  public static final String DEFAULT_TRUST_CERTS = "false";
  public static final String DEFAULT_SOURCENAME = "file";
  public static final String DEFAULT_CONFIG_FILE = "configrd.yaml";
  public static final String DEFAULT_PROPERTIES_FILE_NAME = "default.properties";
  public static final String DEFAULT_CONFIG_URI =
      "file:/srv/configrd/" + ConfigSourceResolver.DEFAULT_CONFIG_FILE;

  public ConfigSourceResolver() {

    defaults.put(FileRepoDef.FILE_NAME_FIELD, DEFAULT_PROPERTIES_FILE_NAME);

    streamSourceLoader = ServiceLoader.load(ConfigSourceFactory.class, getClass().getClassLoader());
    filterLoader = ServiceLoader.load(FilterFactory.class, getClass().getClassLoader());

    for (ConfigSourceFactory f : streamSourceLoader) {
      logger.debug("Found " + f.getSourceName() + " config source on classpath.");
    }

    for (FilterFactory f : filterLoader) {
      logger.debug("Found " + f.getName() + " filters on classpath.");
    }

  }

  public ConfigSourceResolver(final Map<String, Object> vals) {
    this();
    configLoader = new ConfigrdConfigLoader(vals);
  }

  protected class ConfigrdConfigLoader {

    void handleDefaults(LinkedHashMap<String, Object> service) {
      if (service.containsKey("defaults")) {
        defaults.putAll((Map) service.get("defaults"));
      }
    }

    void handleRepos(LinkedHashMap<String, Object> service) {
      if (service.containsKey("repos")) {

        repos = (LinkedHashMap) service.get("repos");

        for (Entry<String, Object> entry : repos.entrySet()) {

          LinkedHashMap<String, Object> repo = (LinkedHashMap) entry.getValue();
          handleRepo(entry.getKey(), repo);
          handleEncrypt(entry.getKey(), repo);

        }
      } else {
        logger.error("Found no 'repos' definitions in configrd config file.");
      }
    }

    void handleRepo(String name, LinkedHashMap<String, Object> repo) {

      try {

        Optional<ConfigSource> cs = newConfigSource(name, repo);
        if (cs.isPresent()) {
          reposByName.put(cs.get().getName().toLowerCase(), cs.get());
          requestFilterChain.put(name, new DefaultFilterChain<RequestFilter>());
          responseFilterChain.put(name, new DefaultFilterChain<ResponseFilter>());
        }

      } catch (Exception e) {
        logger.error(e.getMessage());
        // allow to continue loading other repos
      }

    }

    void handleEncrypt(String name, LinkedHashMap<String, Object> repo) {

      if (repo.containsKey("encrypt")) {

        LinkedHashMap<String, Object> encrypt = (LinkedHashMap) repo.get("encrypt");
        if (!encrypt.isEmpty()) {

          Entry<String, Object> entry = encrypt.entrySet().iterator().next();

          final String providerName = entry.getKey();
          final LinkedHashMap<String, Object> props = (LinkedHashMap) entry.getValue();

          Optional<FilterFactory> factory = StreamSupport.stream(filterLoader.spliterator(), false)
              .filter(f -> f.getName().equalsIgnoreCase(providerName)).findFirst();

          if (factory.isPresent()) {

            responseFilterChain.get(name.toLowerCase())
                .isFirst(factory.get().build(props, ResponseFilter.class));

            requestFilterChain.get(name.toLowerCase())
                .isLast(factory.get().build(props, RequestFilter.class));

          } else {
            logger.warn("Unable to find matching enctyption provider with name " + name);
          }
        }
      }
    }

    protected ConfigrdConfigLoader(final Map<String, Object> vals) {

      final Map<String, Object> configrdConfigDefaults = new HashMap<>();
      configrdConfigDefaults.put(RepoDef.CONFIGRD_CONFIG_FILENAME_FIELD, DEFAULT_CONFIG_FILE);
      configrdConfigDefaults.put(RepoDef.SOURCE_NAME_FIELD, DEFAULT_SOURCENAME);
      configrdConfigDefaults.put(DEFAULT_TRUST_CERTS, DEFAULT_TRUST_CERTS);

      // Override defaults with what's passed
      vals.entrySet().stream().filter(e -> StringUtils.hasText((String) e.getValue()))
          .forEach(e -> {
            configrdConfigDefaults.put(e.getKey(), e.getValue());
          });

      logger.debug("Initialization params:" + configrdConfigDefaults);

      LinkedHashMap<String, Object> y = loadRepoDefFile(configrdConfigDefaults);

      if (y.containsKey("service")) {

        LinkedHashMap<String, Object> service = (LinkedHashMap) y.get("service");

        handleDefaults(service);
        handleRepos(service);

      }
    }

    protected Optional<LinkedHashMap<String, Object>> fetchConfigrdConfigFile(ConfigSource source,
        String fileName) throws FileNotFoundException {

      Optional<LinkedHashMap<String, Object>> y = Optional.empty();

      if (source instanceof FileConfigSource) {

        Optional<StreamPacket> stream = ((FileConfigSource) source).getFile(fileName);

        if (stream.isPresent()) {

          StreamPacket p = (StreamPacket) stream.get();
          try (InputStream s = p.getInputStream()) {

            Yaml yaml = new Yaml();
            y = Optional.of((LinkedHashMap) yaml.load(s));
            logger.info("Loaded configrd config file at " + p.getUri());

          } catch (IOException e2) {
            // nothing
          }

        } else {
          throw new FileNotFoundException();
        }
      }

      return y;
    }

    protected LinkedHashMap<String, Object> loadRepoDefFile(Map<String, Object> vals) {

      defaults.entrySet().forEach(e -> vals.putIfAbsent(e.getKey(), e.getValue()));

      String configUri = (String) vals.get(RepoDef.URI_FIELD);
      String configrdFileName = (String) vals.get(RepoDef.CONFIGRD_CONFIG_FILENAME_FIELD);
      String sourceName = (String) vals.get(RepoDef.SOURCE_NAME_FIELD);

      Optional<LinkedHashMap<String, Object>> y = Optional.empty();
      Optional<ConfigSourceFactory> cs = resolveFactoryBySourceName(sourceName);

      if (cs.isPresent()) {

        logger.debug("Found config source with name: " + cs.get().getSourceName());

        ConfigSource source = cs.get().newConfigSource("init", vals);

        try {

          Optional<String> f = UriUtil.getFileName(configUri);

          if (f.isPresent()) {

            logger.info("Attempting to load configrd config file from " + configUri
                + " using source " + sourceName);
            y = fetchConfigrdConfigFile(source, f.get());

          } else {

            try {

              logger.info("Attempting alternate configrd config file named " + configrdFileName);
              y = fetchConfigrdConfigFile(source, configrdFileName);

            } catch (FileNotFoundException e3) {

              throw new IllegalStateException("Couldn't find a configrd config file to load.");

            }
          }

        } catch (FileNotFoundException e) {

          try {

            logger.info("Attempting alternate configrd config file named " + configrdFileName);
            y = fetchConfigrdConfigFile(source, configrdFileName);

          } catch (FileNotFoundException e2) {

            throw new IllegalStateException("Couldn't find a configrd config file to load.");

          }
        }

      } else {
        throw new IllegalArgumentException("Unable to find a config source of " + sourceName);
      }

      return y.get();
    }

  }

  public Optional<ConfigSource> newConfigSource(String repoName, Map<String, Object> vals) {

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

  public Optional<ConfigSource> findConfigSourceByName(String repoName) {

    if (!StringUtils.hasText(repoName))
      repoName = ConfigSourceResolver.DEFAULT_REPO_NAME;

    return Optional.ofNullable(reposByName.get(repoName.toLowerCase()));
  }

  public FilterChain findFilterChainByName(String name, Class<? extends Filter> type) {

    if (RequestFilter.class.isAssignableFrom(type)) {

      return requestFilterChain.get(name.toLowerCase());

    } else if (ResponseFilter.class.isAssignableFrom(type)) {

      return responseFilterChain.get(name.toLowerCase());

    } else {
      throw new IllegalArgumentException();
    }
  }

  protected Optional<ConfigSourceFactory> resolveFactoryBySourceName(String sourceName) {

    Optional<ConfigSourceFactory> ocs =
        StreamSupport.stream(streamSourceLoader.spliterator(), false).filter(s -> {
          return sourceName.toLowerCase().contains(s.getSourceName().toLowerCase());
        }).findFirst();

    return ocs;

  }

}
