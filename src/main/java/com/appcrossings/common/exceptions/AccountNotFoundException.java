package com.appcrossings.common.exceptions;

@SuppressWarnings("serial")
public class AccountNotFoundException extends EntityNotFoundException {

  public AccountNotFoundException(String accountId, String message) {
    super(accountId, message);
  }
}
