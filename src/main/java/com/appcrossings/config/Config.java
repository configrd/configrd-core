package com.appcrossings.config;

/**
 * Interface for configuration implementations.
 * 
 * @author Krzysztof Karski
 *
 */
public interface Config {

  /**
   * 
   * @param key - key name of the property
   * @param clazz - the type of the property value for strong typing.
   * @return the property value case to the type provided. Returns null if no property is found
   */
  public <T> T getProperty(String key, Class<T> clazz);

  /**
   * 
   * @param key - key name of the property
   * @param clazz - the type of the property value for strong typing
   * @param defaultVal - a default value to return if a property value isn't found
   * @return the property value case to the type provided. Returns null if no property is found
   */
  public <T> T getProperty(String key, Class<T> clazz, T defaultVal);

  /**
   * Register a property change listener which will be triggered with any updated property values
   * when detected. Allows components to subscribe to property changes.
   * 
   * @param listener - the listener instance which will be triggered in the event of the property
   *        changing
   */
//  public void register(ConfigChangeListener listener);

  /**
   * Register a property change listener which will be triggered with any updated property values
   * when detected. Allows components to subscribe to property changes.
   * 
   * @param key - key name of the property to register the listener against
   * @param listener - the listener instance which will be triggered in the event of the property
   *        changing
   */
//  public void register(String key, ConfigChangeListener listener);

  /**
   * Stop listening to property change updated
   * 
   * @param key - key name of the property to register the listener against
   * @param listener - the listener instance which will be triggered in the event of the property
   *        changing
   */
//  public void deregister(String key, ConfigChangeListener listener);

}
