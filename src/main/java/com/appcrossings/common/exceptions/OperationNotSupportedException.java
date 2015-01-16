package com.appcrossings.common.exceptions;

@SuppressWarnings("serial")
public class OperationNotSupportedException extends BaseException {

  public String accountId;

  public OperationNotSupportedException(String accountId, Exception e) {
    super(accountId, e);

  }

  public OperationNotSupportedException(String accountId, String message) {
    super(accountId, message);

  }

}
