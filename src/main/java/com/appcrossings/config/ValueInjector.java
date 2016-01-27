package com.appcrossings.config;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

public class ValueInjector implements ConfigChangeListener {

  @Autowired
  private ApplicationContext context;

  @Autowired
  private Config config;

  @PostConstruct
  private void register() {

  }

  @Override
  public void propertyChanged(String key, Object value) {

//    Map<String, Object> beans = context.getBeansWithAnnotation(Value.class);
//
//    for (Object o : beans.values()) {
//
//      Field[] values = o.getClass().getFields();
//
//      for (Field f : values) {
//        if (f.isAnnotationPresent(Value.class)
//            && f.getAnnotation(Value.class).value().equalsIgnoreCase(key)) {
//          try {
//            f.set(o, value);
//          } catch (Exception e) {
//            // nothing
//          }
//
//        }
//      }
//
//      Method[] methods = o.getClass().getMethods();
//
//      for (Method f : methods) {
//        if (f.isAnnotationPresent(Value.class)
//            && f.getAnnotation(Value.class).value().equalsIgnoreCase(key)
//            && f.getParameterCount() == 1) {
//          try {
//            f.invoke(o, value);
//          } catch (Exception e) {
//
//          }
//        }
//      }
//    }
  }

}
