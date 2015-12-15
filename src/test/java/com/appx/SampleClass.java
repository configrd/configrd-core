package com.appx;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import com.appx.Config;

public class SampleClass {

  @Value("${property.1.name}")
  public String someValue1;

  @Value("${property.2.name}")
  public String someValue2;

  @Value("${property.4.name}")
  public String someValue4;

  public String someOtherValue;

  public String getSomeValue4() {
    return someValue4;
  }


  public void setSomeValue4(String someValue4) {
    this.someValue4 = someValue4;
  }

  @Autowired
  public Config config;

  public String getSomeOtherValue() {
    return someOtherValue;
  }


  public String getSomeValue1() {
    return someValue1;
  }


  public void setSomeValue1(String someValue1) {
    this.someValue1 = someValue1;
  }


  public String getSomeValue2() {
    return someValue2;
  }


  public void setSomeValue2(String someValue2) {
    this.someValue2 = someValue2;
  }


  public void setSomeOtherValue(String someOtherValue) {
    this.someOtherValue = someOtherValue;
  }

  public <T> T getPropertyValue(String key, Class<T> clazz) {
    return config.getProperty(key, clazz);
  }


}
