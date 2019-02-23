package io.configrd.core.util;

import java.net.URI;
import org.junit.Assert;
import org.junit.Test;

public class TestUriUtil {

  @Test
  public void testSomeURIs() {

    URI uri = URI.create("file://username:password/first/second/third");
    String string = uri.toString();

    uri = URI.create("file://:password@host/first/second/third");
    string = uri.toString();

  }

  @Test
  public void testGetFileName() {
    Assert.assertEquals("file.properties",
        UriUtil
            .getFileName(
                URI.create("file://username:password@host/first/second/third/file.properties"))
            .get());

    Assert.assertEquals("file.properties",
        UriUtil.getFileName(URI.create("file:first/second/third/file.properties")).get());

    Assert.assertFalse(UriUtil.getFileName(URI.create("file:first/second/third/")).isPresent());
    Assert.assertFalse(UriUtil.getFileName(URI.create("file:first/second/third")).isPresent());

    Assert.assertEquals("file.properties", UriUtil
        .getFileName(URI.create("http://host:port/first/second/third/file.properties")).get());

    Assert.assertEquals("file.properties",
        UriUtil.getFileName(URI.create("file:/first/second/third/file.properties")).get());

    Assert
        .assertFalse(UriUtil.getFileName(URI.create("http://config.appcrossings.com")).isPresent());
    Assert.assertFalse(UriUtil.getFileName("http://config.appcrossings.com").isPresent());
    Assert.assertFalse(UriUtil.getFileName("/first/second/third").isPresent());
    Assert.assertTrue(UriUtil.getFileName("/first/second/third/file.properties").isPresent());
  }

  @Test
  public void testGetDirSegments() {

    Assert.assertEquals(new String[] {"first", "second", "third"}, UriUtil.getDirSegements(
        URI.create("file://username:password@host/first/second/third/file.proeprties")));

    Assert.assertEquals(new String[] {"first", "second", "third"},
        UriUtil.getDirSegements(URI.create("file:first/second/third/file.proeprties")));

    Assert.assertEquals(new String[] {"first", "second", "third"},
        UriUtil.getDirSegements(URI.create("file:first/second/third/")));

    Assert.assertEquals(new String[] {"first", "second", "third"},
        UriUtil.getDirSegements(URI.create("http://host:port/first/second/third/file.proeprties")));

    Assert.assertEquals(new String[] {"first", "second", "third"},
        UriUtil.getDirSegements(URI.create("file:/first/second/third/file.proeprties")));

  }

  @Test
  public void testStripFile() throws Exception {

    Assert.assertEquals(UriUtil.stripFile(URI.create("http://host:1234/dir1/dir2/dir3")),
        URI.create("http://host:1234/dir1/dir2/dir3"));

    Assert.assertEquals(UriUtil.stripFile(URI.create("http://host:1234/dir1/dir2/dir3/file.yaml")),
        URI.create("http://host:1234/dir1/dir2/dir3/"));

    Assert.assertEquals(UriUtil.stripFile(URI.create("/dir1/dir2/dir3/file.yaml")),
        URI.create("/dir1/dir2/dir3/"));

    Assert.assertEquals(UriUtil.stripFile(URI.create("/dir1/dir2/dir3/")),
        URI.create("/dir1/dir2/dir3/"));

  }

  @Test
  public void testGetPath() throws Exception {

    Assert.assertEquals(UriUtil.getPath(URI.create("http://host:1234/dir1/dir2/dir3")),
        "/dir1/dir2/dir3");

    Assert.assertEquals(UriUtil.getPath(URI.create("http://host:1234/dir1/dir2/")), "/dir1/dir2/");

    Assert.assertEquals(UriUtil.getPath(URI.create("http://host:1234/dir1/dir2/dir3"),
        URI.create("http://host:1234")), "/dir1/dir2/dir3");

    Assert.assertEquals(UriUtil.getPath(URI.create("http://host:1234/dir1/dir2/dir3"),
        URI.create("http://host:1234/dir1")), "/dir2/dir3");

    try {
      Assert.assertEquals(
          UriUtil.getPath(URI.create("http://host:1234/dir1/dir2/dir3"), URI.create("blah")),
          "/dir2/dir3");
      Assert.fail();
    } catch (Exception e) {
      // TODO: handle exception
    }
  }

}
