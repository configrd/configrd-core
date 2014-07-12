package com.appconfig;

public interface Config {
    
    public <T>T getProperty(String key, Class<T> clazz);
    public <T>T getProperty(String key, Class<T> clazz, T defaultVal);

}
