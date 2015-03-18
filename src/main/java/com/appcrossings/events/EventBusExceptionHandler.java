package com.appcrossings.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Throwables;
import com.google.common.eventbus.SubscriberExceptionContext;
import com.google.common.eventbus.SubscriberExceptionHandler;

public class EventBusExceptionHandler implements SubscriberExceptionHandler {

  private final static Logger logger = LoggerFactory.getLogger(EventBusExceptionHandler.class);

  @Override
  public void handleException(Throwable exception, SubscriberExceptionContext context) {
       
    Throwable th = Throwables.getRootCause(exception);
    logger.error(th.getMessage(), th);

  }

}
