package it.infn.mw.iam.api.scim.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ScimOidcId {

  public final String issuer;
  public final String subject;

  private ScimOidcId(Builder b) {
    this.issuer = b.issuer;
    this.subject = b.subject;
  }

  public String getIssuer() {

    return issuer;
  }

  public String getSubject() {

    return subject;
  }

  public static class Builder {

    public String issuer;
    public String subject;

    public Builder issuer(String issuer) {

      this.issuer = issuer;
      return this;
    }

    public Builder subject(String subject) {

      this.subject = subject;
      return this;
    }

    public ScimOidcId build() {

      return new ScimOidcId(this);
    }
  }
}
