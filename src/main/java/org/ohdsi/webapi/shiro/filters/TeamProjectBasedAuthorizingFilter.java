package org.ohdsi.webapi.shiro.filters;

import java.io.IOException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.web.servlet.AdviceFilter;
import org.apache.shiro.web.util.WebUtils;
import org.json.JSONObject;
import org.ohdsi.webapi.shiro.PermissionManager;
import org.ohdsi.webapi.shiro.Entities.RoleEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

/**
 * Filter class to dynamically assign a "team project" role to the
 * user, after validating with an external authorization service
 * whether the user has been granted access to this "team project".
 * 
 * @author Pieter Lukasse
 */
public class TeamProjectBasedAuthorizingFilter extends AdviceFilter {

  private final Logger logger = LoggerFactory.getLogger(TeamProjectBasedAuthorizingFilter.class);

  private final PermissionManager authorizer;
  private final Set<String> defaultRoles;
  private final String authorizationMode;
  private final String authorizationUrl;

  public TeamProjectBasedAuthorizingFilter(
          PermissionManager authorizer,
          Set<String> defaultRoles,
          String authorizationMode,
          String authorizationUrl) {
    this.authorizer = authorizer;
    this.defaultRoles = defaultRoles;
    this.authorizationMode = authorizationMode;
    this.authorizationUrl = authorizationUrl;
    logger.debug("AUTHORIZATION_MODE in TeamProjectBasedAuthorizingFilter constructor == '{}'", this.authorizationMode);
    logger.debug("AUTHORIZATION_URL in TeamProjectBasedAuthorizingFilter constructor == '{}'", this.authorizationUrl);
  }

  @Override
  protected boolean preHandle(ServletRequest request, ServletResponse response) throws Exception {

    try {
        logger.debug("preHandle in TeamProjectBasedAuthorizingFilter == '{}'", this.authorizationMode);
        if (this.authorizationMode.equals("teamproject") && SecurityUtils.getSubject().isAuthenticated()) {
          // in case of "teamproject" mode, we want all roles to be reset always, and
          // set to only the one requested/found in the request parameters (following lines below):
          String login = this.authorizer.getSubjectName();
          boolean foundValidTeamProject = extractAndValidateTeamProjectRoleAndUpdateUserIfNecessary(this, login, request, response);
          if (!foundValidTeamProject) {
            return false;
          }
        }

      } catch (Exception e) {
        WebUtils.toHttp(response).setHeader("x-auth-error", e.getMessage());
        throw new Exception(e);
      }

    return true;
  }

  /**
   * Tries to extract "team project" role found in the request.
   * If necessary, assigns the newly found "team project" role to the user,
   * after validating with a Gen3 authorization service if the user
   * has been granted access to this team.
   * 
   * @param self: reference to "this". Just a workaround to allow the method to be
   * globally synchronized by making it static.
   * @param login: the user login
   * @param request: the request passing through this filter
   * @param response: response object that can be used to write error status and
   * error message if needed.
   * 
   * @return: Returns true if the "team project" was found and passed validation
   * and DB update to user roles did not fail.
   * 
   * @throws IOException
   * @throws Exception
   */
  private static boolean extractAndValidateTeamProjectRoleAndUpdateUserIfNecessary(
      TeamProjectBasedAuthorizingFilter self,
      String login, ServletRequest request,
      ServletResponse response)  throws IOException, Exception {

    // synchronize on login to avoid race conditions (especially on DB updates) if
    // this filter receives parallel requests for any reason:
    synchronized (login.intern()) {
      // check if a teamproject parameter is found in the request:
      String teamProjectRole = self.extractTeamProjectFromRequestParameters(request);
      Set<String> newUserRoles = new HashSet<String>();

      // if found, add teamproject as a role in the newUserRoles list:
      if (teamProjectRole != null && !teamProjectRole.trim().isEmpty()) {
        // double check if this role has really been granted to the user:
        if (self.checkGen3Authorization(teamProjectRole, login) == false) {
          String errorMessage = "User is not authorized to access this team project's data";
          self.logger.error(errorMessage);
          WebUtils.toHttp(response).sendError(HttpServletResponse.SC_FORBIDDEN,
            errorMessage);
          return false;
        }
        // add teamproject role:
        newUserRoles.add(teamProjectRole);
        self.authorizer.setCurrentTeamProjectRoleForCurrentUser(teamProjectRole, login);
        self.authorizer.updateUser(login, self.defaultRoles, newUserRoles, true);
        return true;
      } else {
        String errorMessage = "The teamproject is compulsory when on authorizationMode==teamproject configuration";
        self.logger.error(errorMessage);
        WebUtils.toHttp(response).sendError(HttpServletResponse.SC_FORBIDDEN,
          errorMessage);
        return false;
      }
    }
  }


  private boolean checkGen3Authorization(String teamProjectRole, String login) throws Exception {
    logger.debug("Checking Gen3 Authorization for 'team project'={} and user={} using service={}", teamProjectRole, login, this.authorizationUrl);
    RestTemplate restTemplate = new RestTemplate();
    String arboristAuthorizationURL = this.authorizationUrl;
    String expectedMethod = "access";
    String expectedService = "atlas-argo-wrapper-and-cohort-middleware"; // TODO - make the service name configurable?
    String requestBody = String.format(
      "{\"user\": {" +
      "    \"user_id\": \"%s\"" +
      "  }," +
      "  \"request\": {\n" +
      "    \"resource\": \"%s\"," +
      "    \"action\": {" +
      "      \"service\": \"%s\"," +
      "      \"method\": \"%s\"" +
      "    }" +
      "  }" +
      "}", 
      login, teamProjectRole, expectedService, expectedMethod
    );
    ResponseEntity<String> responseEntity = restTemplate.postForEntity(arboristAuthorizationURL, requestBody, String.class);
    if (responseEntity.getStatusCode().value() == 200) {
      String responseBody = responseEntity.getBody();
      JSONObject jsonObject = new JSONObject(responseBody);
        if (jsonObject.optBoolean("auth", false)) { 
          // auth is true, handle success
          logger.debug("Authorization successful!");
          return true;
      } else {
          // auth response is missing or false
          logger.error("Authorization failed.");
          return false;
      }
    } else {
        // HTTP response status is not 200
        logger.error("Request failed with status: {} ", responseEntity.getStatusCode());
        return false;
    }
  }

  /**
   * Tries to find the team project information in the given request or as part of the
   * stored this.authorizer.getCurrentTeamProjectRoleForCurrentUser(). Returns null
   * if nothing can be found.
   */
  private String extractTeamProjectFromRequestParameters(ServletRequest request) throws Exception {
    // Get the url
    HttpServletRequest httpRequest = (HttpServletRequest) request;
    String url = httpRequest.getRequestURL().toString();

    RoleEntity currentTeamProjectRole = this.authorizer.getCurrentTeamProjectRoleForCurrentUser();
    String currentTeamProjectName = null; 
    if (currentTeamProjectRole != null) {
      currentTeamProjectName = this.authorizer.getCurrentTeamProjectRoleForCurrentUser().getName();
      if (currentTeamProjectName == null || currentTeamProjectName.trim().isEmpty()) {
        throw new Exception("The teamproject role was found but name was unexpectedly empty");
      }
    }
    logger.debug("Current teamproject: {}...", currentTeamProjectName);
    logger.debug("Checking if a teamproject has been specified in the request...");

    // try to find it in the redirectUrl parameter:
    logger.debug("Looking for redirectUrl in request: {}....", url);
    String[] redirectUrlParams = getParameterValues(request, "redirectUrl");
    if (redirectUrlParams != null) {
      logger.debug("Parameter redirectUrl found. Checking if it contains teamproject....");
      // teamProject will be in first one in this case...as only parameter:
      String firstParameter = redirectUrlParams[0];
      if (firstParameter.contains("teamproject=")) {
        String teamProject = firstParameter.split("teamproject=")[1];
        logger.debug("Found teamproject: {}", teamProject);
        return teamProject;
      }
    }

    // try to find "teamproject" param in url itself (there will be no redirectUrl if user session is still valid):
    logger.debug("Fallback1: Looking for teamproject in request: {}....", url);
    String[] teamProjectParams = getParameterValues(request, "teamproject");
    if (teamProjectParams != null) {
      logger.debug("Parameter teamproject found. Parsing....");
      String teamProject = teamProjectParams[0];
      logger.debug("Found teamproject: {}", teamProject);
      return teamProject;
    }

    logger.debug("Fallback2: Looking for teamproject in Action-Location header of request: {}....", url);
    String actionLocationUrl = httpRequest.getHeader("Action-Location");
    if (actionLocationUrl != null && actionLocationUrl.contains("teamproject=")) {
      String teamProject = actionLocationUrl.split("teamproject=")[1];
      logger.debug("Found teamproject: {}", teamProject);
      return teamProject;
    }

    logger.debug("Found NO teamproject explicitly set in the request, so keeping team project: {}.",
      currentTeamProjectName);
    return currentTeamProjectName;
  }

  private String[] getParameterValues(ServletRequest request, String parameterName) {
    // Get the parameters
    logger.debug("Looking for parameter with name: {} ...", parameterName);
    Enumeration<String> paramNames = request.getParameterNames();
    while(paramNames.hasMoreElements()) {
        String paramName = paramNames.nextElement();
        logger.debug("Parameter name: {}", paramName);
        if (paramName.equals(parameterName)) {
          String[] paramValues = request.getParameterValues(paramName);
          return paramValues;
        }
    }
    logger.debug("Found NO parameter with name: {}", parameterName);
    return null;
  }

}
