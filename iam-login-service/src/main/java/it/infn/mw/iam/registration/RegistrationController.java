package it.infn.mw.iam.registration;

import static it.infn.mw.iam.api.scim.controller.utils.ValidationHelper.handleValidationError;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import it.infn.mw.iam.api.scim.exception.IllegalArgumentException;
import it.infn.mw.iam.api.scim.exception.ScimResourceNotFoundException;
import it.infn.mw.iam.api.scim.model.ScimConstants;
import it.infn.mw.iam.api.scim.model.ScimUser;
import it.infn.mw.iam.core.IamRegistrationRequestStatus;

@RestController
@Transactional
@Profile("registration")
public class RegistrationController {

  private RegistrationRequestService service;

  @Autowired
  public RegistrationController(final RegistrationRequestService registrationService) {
    service = registrationService;
  }

  @RequestMapping(value = "/registration/manage", method = RequestMethod.GET)
  public ModelAndView showManagement() {

    return new ModelAndView("iam/requestsManagement");
  }

  @RequestMapping(value = "/registration/username-available/{username}", method = RequestMethod.GET)
  public Boolean usernameAvailable(@PathVariable("username") final String username) {
    return service.usernameAvailable(username);
  }

  @PreAuthorize("#oauth2.hasScope('registration:read') or hasRole('ADMIN')")
  @RequestMapping(value = "/registration", method = RequestMethod.GET)
  @ResponseBody
  public List<RegistrationRequestDto> listRequests(
      @RequestParam(value = "status", required = false) final IamRegistrationRequestStatus status) {

    return service.listRequests(status);
  }

  @RequestMapping(value = "/registration", method = RequestMethod.POST,
      consumes = ScimConstants.SCIM_CONTENT_TYPE)
  public RegistrationRequestDto createRegistrationRequest(
      @RequestBody @Validated(ScimUser.NewUserValidation.class) final ScimUser user,
      final BindingResult validationResult) {

    handleValidationError("Invalid user", validationResult);

    return service.createRequest(user);
  }

  @PreAuthorize("#oauth2.hasScope('registration:write') or hasRole('ADMIN')")
  @RequestMapping(value = "/registration/{uuid}/{decision}", method = RequestMethod.POST)
  public RegistrationRequestDto changeStatus(@PathVariable("uuid") final String uuid,
      @PathVariable("decision") final String decision) {

    IamRegistrationRequestStatus status = null;
    try {
      status = IamRegistrationRequestStatus.valueOf(decision);
    } catch (Exception e) {
      throw new IllegalArgumentException(String.format("Operation [%s] not found", decision));
    }

    return service.updateStatus(uuid, status);
  }

  @RequestMapping(value = "/registration/confirm/{token}", method = RequestMethod.GET)
  public RegistrationRequestDto confirmEmail(@PathVariable("token") final String token) {

    return service.confirmRequest(token);
  }

  @RequestMapping(value = "/registration/verify/{token}", method = RequestMethod.GET)
  public ModelAndView verify(final Model model, @PathVariable("token") final String token) {
    try {
      service.confirmRequest(token);
      model.addAttribute("verificationSuccess", true);
    } catch (ScimResourceNotFoundException srnfe) {
      String message = "Activation failed: " + srnfe.getMessage();
      model.addAttribute("verificationMessage", message);
      model.addAttribute("verificationFailure", true);
    }

    return new ModelAndView("iam/verify");
  }

  @RequestMapping(value = "/registration/submitted", method = RequestMethod.GET)
  public ModelAndView submissionSuccess() {
    return new ModelAndView("iam/requestSubmitted");
  }

}
