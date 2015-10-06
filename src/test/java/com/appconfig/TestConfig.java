package com.appconfig;

import java.net.InetAddress;
import java.util.Properties;

import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class TestConfig {

	@Mock
	private InetAddress inet;
	private String host = "http://static.ca.pixtulate.com";
	private ConfigImpl config;

	@BeforeMethod
	public void setUp() throws Exception {

		MockitoAnnotations.initMocks(this);

		Mockito.doReturn("michelangello").when(inet).getHostName();

		config = new ConfigImpl("classpath:/env/hosts.properties");
		config.setPassword("secret");
		config.inet = inet;
		config.init();
	}

	@Test
	public void testDetectHost() {

		String hostName = config.detectHostName();
		Assert.assertNotNull(hostName);
		Assert.assertEquals(hostName, "michelangello");

		Mockito.doReturn("michelangello.my.domain.com").when(inet)
				.getHostName();

		Assert.assertEquals(inet.getHostName(), "michelangello.my.domain.com");

		hostName = config.detectHostName();
		Assert.assertNotNull(hostName);
		Assert.assertEquals(hostName, "michelangello");
	}

	@Test
	public void testLoadHosts() throws Exception {

		Properties p = config.loadHosts(config.hostsFile);
		Assert.assertNotNull(p);
		Assert.assertTrue(p.containsKey("michelangello"));
		Assert.assertEquals(p.getProperty("michelangello"),
				"classpath:/env/dev/");

	}

	@Test
	public void overrideHostName() throws Exception {
		System.setProperty("hostname", "testhost");

		String hostName = config.detectHostName();
		Assert.assertNotNull(hostName);
		Assert.assertEquals(hostName, "testhost");
	}

	@Test
	public void loadProperties() throws Exception {

		Properties p = config.loadProperties("classpath:/env/dev/");
		Assert.assertNotNull(p);
		Assert.assertTrue(p.containsKey("property.1.name"));
		Assert.assertEquals(p.getProperty("property.1.name"), "value1");

	}

	@Test
	public void testLoadHostFile() {

		String value = config.getProperty("property.1.name", String.class);
		Assert.assertNotNull(value);
		Assert.assertEquals(value, "value1");

	}

	@Test
	public void testPullHostFileFromAmazon() throws Exception {

		Properties p = config.loadHosts(host + "/env/hosts.properties");
		Assert.assertNotNull(p);
		Assert.assertTrue(p.containsKey("kkarski-ibm"));

	}

	@Test
	public void testPullPropertiesFileFromAmazon() throws Exception {

		Properties p = config.loadProperties(host + "/env/dev");

		Assert.assertNotNull(p);
		Assert.assertTrue(p.containsKey("algo.im.path"));

	}

	@Test
	public void testPropertiesCascade() throws Exception {

		Mockito.doReturn("michelangello-custom").when(inet).getHostName();
		config.init();

		String value = config.getProperty("property.1.name", String.class);
		Assert.assertNotNull(value);
		Assert.assertEquals(value, "custom");

		value = config.getProperty("property.2.name", String.class);
		Assert.assertNotNull(value);
		Assert.assertEquals(value, "value2");

	}

	@Test
	public void testPropertiesCascadeOverride() throws Exception {

		Mockito.doReturn("michelangello-custom").when(inet).getHostName();
		config.init();

		String value = config.getProperty("property.1.name", String.class);
		Assert.assertNotNull(value);
		Assert.assertEquals(value, "custom");

	}

	@Test
	public void testIncludeClasspathProperty() throws Exception {

		Mockito.doReturn("michelangello-custom").when(inet).getHostName();
		config.setSearchClasspath(true);
		config.init();

		String value = config.getProperty("property.5.name", String.class);
		Assert.assertNotNull(value);
		Assert.assertEquals(value, "classpath");

	}

	@Test
	public void testDoNotIncludeClasspathProperty() throws Exception {

		Mockito.doReturn("michelangello-custom").when(inet).getHostName();
		config.setSearchClasspath(false);
		config.init();

		String value = config.getProperty("property.5.name", String.class);
		Assert.assertNull(value);

	}

	@Test
	public void testGetNonExistingProperty() throws Exception {

		Mockito.doReturn("michelangello-custom").when(inet).getHostName();
		config.init();

		String value = config.getProperty("property.not-exists", String.class);
		Assert.assertNull(value);

	}

	@Test
	public void testGetEncryptedProperty() throws Exception {
		Mockito.doReturn("michelangello-custom").when(inet).getHostName();
		config.init();

		String value = config.getProperty("property.6.name", String.class);
		Assert.assertNotNull(value);
		Assert.assertNotEquals(value, "92W4NeYZYtuFamXo0mtlWq2VFL5AOfTI");
		Assert.assertEquals(value, "password");
	}

	@AfterMethod
	public void tearDown() {
		Mockito.reset(inet);
		System.setProperty("hostname", "");
	}

}
