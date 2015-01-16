package com.appcrossings.common.exceptions;

@SuppressWarnings("serial")
public class BadRequestException extends BaseException {

  public BadRequestException(String accountId, Exception e) {
    super(accountId, e);    
  }
  
  public BadRequestException(String accountId, String errorCode, String errorMessage) {
    super(accountId, errorCode, errorMessage);
  }
  
  public BadRequestException(ErrorResponse resp){
    super(resp);
  }
  
  public BadRequestException(String accountId, String errorMessage) {
    super(accountId, errorMessage);
  }
  
  
  
}
