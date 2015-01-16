package com.appcrossings.common.exceptions;

@SuppressWarnings("serial")
public class EntityNotFoundException extends BaseException {

  public String accountId;

  public EntityNotFoundException(String accountId, String message) {
    super(accountId, message);
  }

}
