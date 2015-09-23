package com.appconfig;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.PostConstruct;

import org.apache.commons.beanutils.ConvertUtilsBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;

import com.google.common.base.Throwables;

/**
 * 
 * 
 * 
 * @author Krzysztof Karski
 *
 */
@Service
public class ConfigImpl extends PropertyPlaceholderConfigurer implements
		Config, EnvironmentAware {

	private class ReloadTask extends TimerTask {

		@Override
		public void run() {
			try {
				init();
			} catch (Exception e) {
				logger.error("Error refreshing configs", e);
			}
		}
	}

	private final static Logger log = LoggerFactory.getLogger(ConfigImpl.class);
	private final ConvertUtilsBean bean = new ConvertUtilsBean();

	private String fileName = "default.properties";

	@Value("${properties.hostsFilePath}")
	protected String hostsFile;

	protected InetAddress inet = InetAddress.getLocalHost();

	private final ConcurrentHashMap<String, Set<ConfigChangeListener>> listeners = new ConcurrentHashMap<String, Set<ConfigChangeListener>>();

	private final AtomicReference<Properties> properties = new AtomicReference<>(
			new Properties());

	@Autowired(required = false)
	protected Environment springProfiles;

	private Timer timer = new Timer(true);

	/**
	 * Set path to hosts.properties file using the setter.
	 * 
	 * @throws Exception
	 */
	public ConfigImpl() throws Exception {
	}

	/**
	 * 
	 * @param path
	 *            The path of the hosts.properties file
	 * @throws Exception
	 */
	public ConfigImpl(String path) throws Exception {
		this.hostsFile = path;
	}

	/**
	 * 
	 * @param path
	 *            - The path of the hosts.properties file
	 * @param refresh
	 *            - The period in seconds at which the config properties should
	 *            be refreshed. Defaults to 10 minutes.
	 * @throws Exception
	 */
	public ConfigImpl(String path, int refresh) throws Exception {
		this.hostsFile = path;
		setRefreshRate(refresh);
	}

	/**
	 * 
	 * @param path
	 *            The path of the hosts.properties file
	 * @throws Exception
	 */
	public ConfigImpl(String path, String fileName) throws Exception {
		this.hostsFile = path;
		this.fileName = fileName;
	}

	/**
	 * 
	 * @param path
	 *            - The path of the hosts.properties file
	 * @param refresh
	 *            - The period in seconds at which the config properties should
	 *            be refreshed. Defaults to 10 minutes.
	 * @throws Exception
	 */
	public ConfigImpl(String path, String fileName, int refresh)
			throws Exception {
		this.hostsFile = path;
		this.fileName = fileName;
		setRefreshRate(refresh);
	}

	@Override
	public void deregister(String key, ConfigChangeListener listener) {

		if (listeners.containsKey(key)) {
			listeners.get(key).remove(listener);
		}
	}

	/**
	 * Attempt to detect environment of the application.
	 * 
	 * @return environment string
	 */
	protected String detectEnvironment() {

		String env = null;

		if (springProfiles != null
				&& springProfiles.getActiveProfiles() != null
				&& springProfiles.getActiveProfiles().length > 0) {
			env = springProfiles.getActiveProfiles()[0];
		}

		try {

			env = StringUtils.hasText(env) ? env : System.getProperty("env");

			if (!StringUtils.hasText(env)) {
				log.info("No environment variable detected under 'spring.profiles' or system property 'env'");
			} else {
				log.info("Detected environment: " + env);
			}

		} catch (Exception e) {
			log.error("Error while detecting environment", e);
		}

		return env;
	}

	/**
	 * Reads the underlying host's name. This is used to match this host against
	 * its configuration. You can programmatically override the hostname value
	 * by setting System.setProperty("hostname", "value").
	 * 
	 * @return hostname
	 */
	protected String detectHostName() {

		String hostName = null;

		try {

			hostName = StringUtils.hasText(System.getProperty("hostname")) ? System
					.getProperty("hostname") : inet.getHostName();

			if (!StringUtils.hasText(hostName)) {
				throw new UnknownHostException(
						"Unable to resolve host in order to resolve hosts file config");
			}

			if (hostName.contains(".")) {
				hostName = hostName.substring(0, hostName.indexOf("."));
			}

			log.info("Resolved hostname to: " + hostName);

		} catch (UnknownHostException ex) {
			log.error("Can't resolve hostname", ex);
			Throwables.propagate(ex);
		}

		return hostName;
	}

	@Override
	public <T> T getDecryptedProperty(String key, Class<T> clazz) {
		// TODO Auto-generated method stub
		return null;
	}

	public String getHostsFile() {
		return hostsFile;
	}

	protected Properties getLoadedProperties() {
		return properties.get();
	}

	@Override
	public <T> T getProperty(String key, Class<T> clazz) {

		// long now = System.currentTimeMillis();

		// boolean sync = false;
		// synchronized (lastRefresh) {
		// if ((now - lastRefresh) > (ttl.get() * 1000)) {
		// sync = true;
		// lastRefresh = System.currentTimeMillis();
		// }
		// }
		//
		// if (sync) {
		//
		// try {
		// init();
		// } catch (Exception e) {
		// log.error("Attempting to reload config has failed", e);
		// }
		//
		// sync = false;
		// }

		String property = properties.get().getProperty(key);

		if (clazz.equals(String.class))
			return (T) property;
		else if (property != null)
			return (T) bean.convert(property, clazz);
		else
			return null;

	}

	public <T> T getProperty(String key, Class<T> clazz, T value) {

		T val = getProperty(key, clazz);

		if (val != null)
			return val;

		return value;

	}

	@PostConstruct
	protected void init() throws Exception {

		Properties hosts = loadHosts(hostsFile);

		String hostName = detectHostName();
		String environment = detectEnvironment();

		String propertiesFile = hosts.getProperty(hostName);

		// Attempt environment as a backup
		if (!StringUtils.hasText(propertiesFile)
				&& StringUtils.hasText(environment)) {

			propertiesFile = hosts.getProperty(environment);

		} else if (!StringUtils.hasText(propertiesFile)) {

			propertiesFile = hosts.getProperty("*");

		}

		Properties ps = loadProperties(propertiesFile);

		if (ps.isEmpty()) {
			throw new FileNotFoundException(
					"Counldn't find any properties for host " + hostName);
		}

		properties.set(ps);
		
		String ttl = (String) properties.get().get("config.ttl");
		if (ttl != null) {

			Integer lttl = Integer.valueOf(ttl);

			log.info("Setting config refresh rate to: " + lttl + " seconds.");
			setRefreshRate(lttl);
		}
		
		// bootstrap setting log level here
		if (StringUtils.hasText(properties.get().getProperty("log.root.level"))) {
			LoggerContext lc = (LoggerContext) LoggerFactory
					.getILoggerFactory();
			ch.qos.logback.classic.Logger logger = lc
					.getLogger("com.appconfig");
			logger.setLevel(Level.valueOf(properties.get().getProperty(
					"log.root.level")));
			log.info("Configuring log level: " + logger.getLevel());
		}

		super.setProperties(getLoadedProperties());

	}

	protected Properties loadHosts(String hostsFile)
			throws FileNotFoundException {

		log.info("Fetching hosts file from path: " + hostsFile);

		Resource resource = new DefaultResourceLoader().getResource(hostsFile);

		if (!resource.exists()) {
			throw new FileNotFoundException("Properties file " + hostsFile
					+ " couldn't be found");
		}

		Properties hosts = new Properties();
		try (InputStream stream = resource.getInputStream()) {

			hosts.load(stream);

		} catch (IOException e) {
			log.error("Can't load hosts file", e);
		}

		return hosts;

	}

	protected Properties loadProperties(String propertiesPath) throws Exception {

		List<Properties> all = new ArrayList<>();

		if (StringUtils.hasText(propertiesPath)) {

			do {

				Resource resource = new DefaultResourceLoader()
						.getResource(propertiesPath + "/" + fileName);

				// Check if a spring properties file exists
				if (!resource.exists()) {
					resource = new DefaultResourceLoader()
							.getResource(propertiesPath
									+ "/application.properties");
				}

				// Search for default.properties file in parent folders
				if (!resource.exists()) {
					propertiesPath = stripDir(propertiesPath);
					continue;
				}

				Properties p = new Properties();

				try (InputStream stream2 = resource.getInputStream()) {

					log.info("Found properties file: " + propertiesPath + "/"
							+ resource.getFilename());
					p.load(stream2);
					all.add(p);

				} catch (IOException e) {

					// file not found...no issue, keep going
					if (StringUtils.hasText(e.getMessage())
							&& (e.getMessage().contains("code: 403") || e
									.getMessage().contains("code: 404"))) {

						propertiesPath = stripDir(propertiesPath);
						continue;

					} else {
						Throwables.propagate(e);
					}
				}

				propertiesPath = stripDir(propertiesPath);

			} while (new File(propertiesPath).getParent() != null);
		}

		Collections.reverse(all); // sort from root to highest

		Properties ps = new Properties();

		for (Properties p : all) {
			ps.putAll(p); // replace root properties with higher level
							// properties
		}

		return ps;

	}

	@Override
	public void register(String key, ConfigChangeListener listener) {

		if (listeners.contains(key)) {
			listeners.get(key).add(listener);
		} else {
			synchronized (listeners) {
				Set<ConfigChangeListener> n = new ConcurrentSkipListSet<ConfigChangeListener>();
				n.add(listener);
				listeners.put(key, n);
			}

		}

	}

	@Override
	public void setEnvironment(Environment environment) {
		this.springProfiles = environment;
	}

	public void setHostsFile(String hostsFile) {
		this.hostsFile = hostsFile;
	}

	private void setRefreshRate(Integer refresh) {
		
		if (refresh == 0L || refresh == null) {
			timer.cancel();
			return;
		}		

		synchronized (timer) {
			try {
				timer.cancel();
				timer = new Timer(true);
			} catch (IllegalStateException e) {
				// Nothing
			}

			timer.schedule(new ReloadTask(), refresh * 1000, refresh * 1000);
		}
	}

	private String stripDir(String path) {

		int i = path.lastIndexOf("/");

		if (i > 0)
			return path.substring(0, i);

		return "";

	}
}
