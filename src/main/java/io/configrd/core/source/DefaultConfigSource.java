package io.configrd.core.source;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import io.configrd.core.DefaultMergeStrategy;
import io.configrd.core.MergeStrategy;
import io.configrd.core.filter.FilterChain;
import io.configrd.core.util.DirectoryTraverse;
import io.configrd.core.util.StringUtils;

public abstract class DefaultConfigSource<T extends StreamSource> implements ConfigSource<T> {

  protected final Map<String, String> namedPaths;
  protected final T streamSource;

  public DefaultConfigSource(T source, Map<String, Object> values) {
    this.streamSource = source;

    if (values.containsKey(RepoDef.NAMED_PATHS_FIELD)) {
      this.namedPaths = (Map) values.get(RepoDef.NAMED_PATHS_FIELD);
    } else {
      this.namedPaths = new HashMap<>();
    }
  }

  public Map<String, Object> get(String path, Set<String> names) {

    final MergeStrategy merge = new DefaultMergeStrategy();

    if (!names.isEmpty()) {

      for (String name : names) {

        path = namedPaths.get(name);

        if (!StringUtils.hasText(path))
          continue;

        final DirectoryTraverse traverse = new DirectoryTraverse(path);

        do {

          Map<String, Object> props = getRaw(traverse.decend().toString());
          merge.addConfig(props);

        } while (traverse.hasNextDown());

      }

    } else if (StringUtils.hasText(path)) {

      final DirectoryTraverse traverse = new DirectoryTraverse(path);

      do {

        Map<String, Object> props = getRaw(traverse.decend().toString());
        merge.addConfig(props);

      } while (traverse.hasNextDown());

    }

    return merge.merge();

  }

  @Override
  public String getName() {
    return streamSource.getSourceConfig().getName();
  }

  protected Map<String, String> getNamedPaths() {
    return namedPaths;
  }

  @Override
  public T getStreamSource() {
    return streamSource;
  }
}
