package com.appcrossings.common.exceptions;

import java.io.Serializable;
import java.util.Map;

import org.apache.commons.beanutils.BeanUtils;

@SuppressWarnings("serial")
public class ErrorResponse implements Serializable {

  public String errorCode;

  public String eventType;

  public String extErrorCode;

  public String extErrorMessage;

  public String id;

  public String message;

  public Long timestamp;

  public String accountId;

  public String userProfileId;

  public Map<String, Object> properties = new java.util.HashMap<>();

  public Map<String, Object> getProperties() {
    return properties;
  }

  public void setProperties(Map<String, Object> properties) {
    this.properties = properties;
  }

  public String getUserProfileId() {
    return userProfileId;
  }

  public void setUserProfileId(String userProfileId) {
    this.userProfileId = userProfileId;
  }

  public ErrorResponse() {
  }

  public ErrorResponse(BaseException event) {

    try {
      BeanUtils.copyProperties(this, event);
    } catch (Exception e) {
      throw new IllegalArgumentException("Unable to copy event properties");
    }
  }

  public String getAccountId() {
    return accountId;
  }

  public void setAccountId(String accountId) {
    this.accountId = accountId;
  }

  public String getErrorCode() {
    return errorCode;
  }

  public String getEventType() {
    return eventType;
  }

  public String getExtErrorCode() {
    return extErrorCode;
  }

  public String getExtErrorMessage() {
    return extErrorMessage;
  }

  public String getId() {
    return id;
  }

  public String getMessage() {

    return message;
  }

  public Long getTimestamp() {
    return timestamp;
  }

  public void setErrorCode(String errorCode) {
    this.errorCode = errorCode;
  }

  public void setEventType(String eventType) {
    this.eventType = eventType;
  }

  public void setExtErrorCode(String extErrorCode) {
    this.extErrorCode = extErrorCode;
  }

  public void setExtErrorMessage(String extErrorMessage) {
    this.extErrorMessage = extErrorMessage;
  }

  public void setId(String id) {
    this.id = id;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public void setTimestamp(Long timestamp) {
    this.timestamp = timestamp;
  }

}
