package com.appx;

import org.jasypt.encryption.pbe.PooledPBEStringEncryptor;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EncryptUtil {

	public static final Logger log = LoggerFactory.getLogger(EncryptUtil.class);

	private PooledPBEStringEncryptor encryptor = new PooledPBEStringEncryptor();

	@Before
	public final void init() {
		encryptor.setPassword(password());
		encryptor.setAlgorithm(algo());
		encryptor.setPoolSize(4);
	}

	public String algo() {
		return "PBEWithMD5AndTripleDES";
	}

	public String[] values() {
		return new String[]{"some text"};
	}

	public String password() {
		return "secret";
	}

	@Test
	public final void encrypt() {

		for (String s : values()) {
			log.info("Encrypted using algo " + algo() + " [ " + s + " ] : [ "
					+ encryptor.encrypt(s) + " ]");
		}

	}

}