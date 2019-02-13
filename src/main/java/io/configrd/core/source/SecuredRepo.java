package io.configrd.core.source;

public interface SecuredRepo {
  
  public static final String PASSWORD_FIELD = "password";
  public static final String USERNAME_FIELD = "username";
  public static final String AUTH_METHOD_FIELD = "authMethod";
 

  public String getPassword();

  public String getUsername();
  
  public String getAuthMethod();

}
