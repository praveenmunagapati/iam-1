package it.infn.mw.iam.api.scim.model;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.base.Preconditions;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ScimMeta {

  private final String resourceType;
  private final Date created;
  private final Date lastModified;

  private final String location;
  private final String version;

  private ScimMeta(Builder b) {
    this.resourceType = b.resourceType;
    this.created = b.created;
    this.lastModified = b.lastModified;
    this.location = b.location;
    this.version = b.version;
  }

  public String getResourceType() {

    return resourceType;
  }

  public Date getCreated() {

    return created;
  }

  public Date getLastModified() {

    return lastModified;
  }

  public String getLocation() {

    return location;
  }

  public String getVersion() {

    return version;
  }

  public static class Builder {

    private final Date created;
    private final Date lastModified;
    private String location;
    private String version;
    private String resourceType;

    public Builder(Date created, Date lastModified) {
      this.created = created;
      this.lastModified = lastModified;
    }

    public Builder location(String location) {

      this.location = location;
      return this;
    }

    public Builder version(String version) {

      this.version = version;
      return this;
    }

    public Builder resourceType(String resourceType) {

      this.resourceType = resourceType;
      return this;
    }

    public ScimMeta build() {

      Preconditions.checkNotNull(resourceType, "resourceType must be non-null");
      Preconditions.checkNotNull(location, "location must be non-null");

      return new ScimMeta(this);
    }
  }
}
