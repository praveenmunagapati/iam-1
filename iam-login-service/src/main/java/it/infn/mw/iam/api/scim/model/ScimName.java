package it.infn.mw.iam.api.scim.model;

import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.NotBlank;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ScimName {
  
  interface NewUserValidation {};
  interface UpdateUserValidation {};

  private final String formatted;

  @Length(groups={NewUserValidation.class, UpdateUserValidation.class}, max=64)
  private final String familyName;

  @NotBlank(groups={NewUserValidation.class})
  @Length(groups={NewUserValidation.class, UpdateUserValidation.class}, max=64)
  private final String givenName;

  @Length(groups={NewUserValidation.class, UpdateUserValidation.class}, max=64)
  private final String middleName;
  
  private final String honorificPrefix;
  private final String honorificSuffix;

  private ScimName(Builder b) {
    this.formatted = b.formatted;
    this.familyName = b.familyName;
    this.givenName = b.givenName;
    this.middleName = b.middleName;
    this.honorificPrefix = b.honorificPrefix;
    this.honorificSuffix = b.honorificSuffix;
  }

  public String getFormatted() {

    return formatted;
  }

  public String getFamilyName() {

    return familyName;
  }

  public String getGivenName() {

    return givenName;
  }

  public String getMiddleName() {

    return middleName;
  }

  public String getHonorificPrefix() {

    return honorificPrefix;
  }

  public String getHonorificSuffix() {

    return honorificSuffix;
  }

  public static class Builder {

    private String formatted;
    private String familyName;
    private String givenName;
    private String middleName;
    private String honorificPrefix;
    private String honorificSuffix;

    public Builder formatted(String formatted) {

      this.formatted = formatted;
      return this;
    }

    public Builder givenName(String givenName) {

      this.givenName = givenName;
      return this;
    }

    public Builder middleName(String middleName) {

      this.middleName = middleName;
      return this;
    }

    public Builder honorificPrefix(String honorificPrefix) {

      this.honorificPrefix = honorificPrefix;
      return this;
    }

    public Builder honorificSuffix(String honorificSuffix) {

      this.honorificSuffix = honorificSuffix;
      return this;
    }

    public Builder familyName(String familyName) {

      this.familyName = familyName;
      return this;
    }

    public ScimName build() {

      return new ScimName(this);
    }
  }
}