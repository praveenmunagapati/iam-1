package it.infn.mw.iam.api.account.password_reset;

import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.NotEmpty;

public class PasswordDTO {

  @NotEmpty
  private String currentPassword;
  
  @NotEmpty(message = "The password cannot be empty")
  @Length(min = 5, message = "The password must be at least 5 characters")
  private String updatedPassword;

  public String getCurrentPassword() {
    return currentPassword;
  }

  public void setCurrentPassword(String currentPassword) {
    this.currentPassword = currentPassword;
  }

  public String getUpdatedPassword() {
    return updatedPassword;
  }

  public void setUpdatedPassword(String updatedPassword) {
    this.updatedPassword = updatedPassword;
  }

}
