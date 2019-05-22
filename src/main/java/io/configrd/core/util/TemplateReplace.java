package io.configrd.core.util;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class TemplateReplace {

  public String replace(String fileName, Map<String, Object> vals) throws Exception {

    URL url = getClass().getResource(URI.create(fileName).getPath());

    String content = new String(Files.readAllBytes(Paths.get(url.toURI())));

    final AtomicReference<String> ref = new AtomicReference<String>(content);

    vals.entrySet().stream()
        .forEach(e -> ref.set(ref.get().replace("${" + e.getKey() + "}", e.getValue() + "")));

    File temp = File.createTempFile("test-", ".yaml");
    Files.write(Paths.get(temp.toURI()), ref.get().getBytes());
    temp.deleteOnExit();

    return temp.getAbsolutePath();

  }

}
