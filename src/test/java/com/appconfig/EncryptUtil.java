package com.appconfig;

import org.jasypt.encryption.pbe.PooledPBEStringEncryptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class EncryptUtil {
	
	public static final Logger log = LoggerFactory.getLogger(EncryptUtil.class);

	private String[] values = {"password"};
	private PooledPBEStringEncryptor encryptor = new PooledPBEStringEncryptor();
	private String password = "secret";
	private String algo = "PBEWithMD5AndTripleDES";
	
	@BeforeMethod
	public void init(){
		encryptor.setPassword(password);
		encryptor.setAlgorithm(algo);
		encryptor.setPoolSize(4);
	}
	
	
	@Test
	public void encrypt() {
		
		for(String s : values){
			log.info("Value " + s + " encrypted as " + encryptor.encrypt(s));
		}
		
	}
	
}
