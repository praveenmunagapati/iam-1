package it.infn.mw.iam.github;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.filter.OAuth2ClientAuthenticationProcessingFilter;
import org.springframework.security.web.savedrequest.DefaultSavedRequest;

public class GithubAuthFilter extends OAuth2ClientAuthenticationProcessingFilter {

  protected static final String REDIRECT_URI_SESSION_VARIABLE = "redirect_uri";
  protected static final String STATE_SESSION_VARIABLE = "state";
  protected static final String NONCE_SESSION_VARIABLE = "nonce";
  protected static final String ISSUER_SESSION_VARIABLE = "issuer";
  protected static final String TARGET_SESSION_VARIABLE = "target";
  protected static final int HTTP_SOCKET_TIMEOUT = 30000;

  protected static final String ORIGIN_AUTH_REQUEST_SESSION_VARIABLE = "origin_auth_request";

  public static final String FILTER_PROCESSES_URL = "/login/github";

  public GithubAuthFilter() {
    super(FILTER_PROCESSES_URL);
  }

  public GithubAuthFilter(String processingUrl) {
    super(processingUrl);
  }

  @Override
  public Authentication attemptAuthentication(final HttpServletRequest request,
      final HttpServletResponse response)
          throws IOException, ServletException {

    HttpSession session = request.getSession();

    // backup origin redirect uri and state
    DefaultSavedRequest savedRequest =
        (DefaultSavedRequest) session.getAttribute("SPRING_SECURITY_SAVED_REQUEST");
    session.setAttribute(ORIGIN_AUTH_REQUEST_SESSION_VARIABLE, savedRequest);

    return super.attemptAuthentication(request, response);

  }

}
