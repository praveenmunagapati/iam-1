package it.infn.mw.iam.core.oauth;

import java.util.Date;

import org.mitre.oauth2.exception.AuthorizationPendingException;
import org.mitre.oauth2.exception.DeviceCodeExpiredException;
import org.mitre.oauth2.model.DeviceCode;
import org.mitre.oauth2.service.DeviceCodeService;
import org.springframework.security.oauth2.common.exceptions.InvalidGrantException;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2RequestFactory;
import org.springframework.security.oauth2.provider.TokenRequest;
import org.springframework.security.oauth2.provider.token.AbstractTokenGranter;
import org.springframework.security.oauth2.provider.token.AuthorizationServerTokenServices;

public class IamDeviceCodeTokenGranter extends AbstractTokenGranter {

  public static final String GRANT_TYPE = "urn:ietf:params:oauth:grant-type:device_code";

  private final DeviceCodeService deviceCodeService;

  public IamDeviceCodeTokenGranter(AuthorizationServerTokenServices tokenServices,
      ClientDetailsService clientDetailsService, OAuth2RequestFactory requestFactory,
      DeviceCodeService deviceCodeService) {
    super(tokenServices, clientDetailsService, requestFactory, GRANT_TYPE);
    this.deviceCodeService = deviceCodeService;
  }


  // FIXME: revert back to mitre implementation as soon as they have fixed how
  // they manage the granter creation (use proper constructor injection)
  @Override
  protected OAuth2Authentication getOAuth2Authentication(ClientDetails client,
      TokenRequest tokenRequest) {

    String deviceCode = tokenRequest.getRequestParameters().get("device_code");

    // look up the device code and consume it
    DeviceCode dc = deviceCodeService.findDeviceCode(deviceCode, client);

    if (dc == null) {
      throw new InvalidGrantException("Invalid device code: " + deviceCode);
    }

    final Date now = new Date();

    // dc expiration checks
    if (dc.getExpiration() != null && dc.getExpiration().before(now)) {
      deviceCodeService.clearDeviceCode(deviceCode, client);
      throw new DeviceCodeExpiredException("Device code has expired: " + deviceCode);
    }

    if (!dc.isApproved()) {
      throw new AuthorizationPendingException("Authorization pending for code: " + deviceCode);
    }

    // inherit the (approved) scopes from the original request
    tokenRequest.setScope(dc.getScope());

    OAuth2Authentication auth =
        new OAuth2Authentication(getRequestFactory().createOAuth2Request(client, tokenRequest),
            dc.getAuthenticationHolder().getUserAuth());

    deviceCodeService.clearDeviceCode(deviceCode, client);

    return auth;
  }
}