package com.appcrossings.common.exceptions;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonTypeName;
import org.springframework.beans.BeanUtils;

@JsonTypeName("Error")
@JsonIgnoreProperties(ignoreUnknown = true)
@SuppressWarnings("serial")
public class BaseException extends RuntimeException {

  public static final String EVENT_TYPE = "error";

  public String accountId;
  public String errorCode;
  public String extErrorCode;
  public String extErrorMessage;

  public String userProfileId;
  public Object metadata;

  public Object getMetaData() {
    return metadata;
  }

  public void setMetadata(Object metadata) {
    this.metadata = metadata;
  }

  private BaseException() {
  }

  public BaseException(ErrorResponse resp) {
    super(resp.getMessage());
    BeanUtils.copyProperties(this, resp);
  }

  public BaseException(String accountId, Exception e) {
    super(e);
    this.accountId = accountId;
  }

  public BaseException(String accountId, String errorMessage) {
    super(errorMessage);
    this.accountId = accountId;
  }

  public BaseException(String accountId, String message, Exception e) {
    super(message, e);
    this.accountId = accountId;
  }

  public BaseException(String accountId, String errorCode, String errorMessage) {
    super(errorMessage);
    this.errorCode = errorCode;
    this.accountId = accountId;
  }

  public BaseException(String accountId, String userId, String errorCode, String errorMessage) {
    super(errorMessage);
    this.errorCode = errorCode;
    this.accountId = accountId;
    this.userProfileId = userId;
  };

  public String getAccountId() {
    return accountId;
  }

  public String getErrorCode() {
    return errorCode;
  }

  public String getExtErrorCode() {
    return extErrorCode;
  }

  public String getExtErrorMessage() {
    return extErrorMessage;
  }

  public String getUserProfileId() {
    return userProfileId;
  }

}
