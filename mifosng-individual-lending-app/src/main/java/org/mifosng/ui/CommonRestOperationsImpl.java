package org.mifosng.ui;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.mifosng.configuration.ApplicationConfigurationService;
import org.mifosng.data.AppUserData;
import org.mifosng.data.ClientData;
import org.mifosng.data.ClientDataWithAccountsData;
import org.mifosng.data.ClientList;
import org.mifosng.data.CurrencyData;
import org.mifosng.data.CurrencyList;
import org.mifosng.data.EntityIdentifier;
import org.mifosng.data.EnumOptionList;
import org.mifosng.data.EnumOptionReadModel;
import org.mifosng.data.ErrorResponse;
import org.mifosng.data.ErrorResponseList;
import org.mifosng.data.ExtraDatasets;
import org.mifosng.data.LoanAccountData;
import org.mifosng.data.LoanProductData;
import org.mifosng.data.LoanProductList;
import org.mifosng.data.LoanRepaymentData;
import org.mifosng.data.LoanSchedule;
import org.mifosng.data.NewLoanWorkflowStepOneData;
import org.mifosng.data.NoteData;
import org.mifosng.data.NoteDataList;
import org.mifosng.data.OfficeData;
import org.mifosng.data.OfficeList;
import org.mifosng.data.OfficeTransferData;
import org.mifosng.data.PermissionData;
import org.mifosng.data.PermissionList;
import org.mifosng.data.RoleData;
import org.mifosng.data.RoleList;
import org.mifosng.data.UserList;
import org.mifosng.data.command.AdjustLoanTransactionCommand;
import org.mifosng.data.command.BranchMoneyTransferCommand;
import org.mifosng.data.command.CalculateLoanScheduleCommand;
import org.mifosng.data.command.ChangePasswordCommand;
import org.mifosng.data.command.CreateLoanProductCommand;
import org.mifosng.data.command.EnrollClientCommand;
import org.mifosng.data.command.LoanStateTransitionCommand;
import org.mifosng.data.command.LoanTransactionCommand;
import org.mifosng.data.command.NoteCommand;
import org.mifosng.data.command.OfficeCommand;
import org.mifosng.data.command.RoleCommand;
import org.mifosng.data.command.SubmitLoanApplicationCommand;
import org.mifosng.data.command.UndoLoanApprovalCommand;
import org.mifosng.data.command.UndoLoanDisbursalCommand;
import org.mifosng.data.command.UpdateLoanProductCommand;
import org.mifosng.data.command.UpdateOrganisationCurrencyCommand;
import org.mifosng.data.command.UserCommand;
import org.mifosng.data.reports.GenericResultset;
import org.mifosng.ui.loanproduct.ClientValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth.consumer.ProtectedResourceDetails;
import org.springframework.security.oauth.consumer.client.OAuthRestTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;

import com.thoughtworks.xstream.XStream;

@Service(value="commonRestOperations")
public class CommonRestOperationsImpl implements CommonRestOperations {

	private final static Logger logger = LoggerFactory
			.getLogger(CommonRestOperationsImpl.class);
	
	private OAuthRestTemplate oauthRestServiceTemplate;
	private final ApplicationConfigurationService applicationConfigurationService;

	@Autowired
	public CommonRestOperationsImpl(
			final OAuthRestTemplate oauthRestServiceTemplate, final ApplicationConfigurationService applicationConfigurationService) {
		this.oauthRestServiceTemplate = oauthRestServiceTemplate;
		this.applicationConfigurationService = applicationConfigurationService;
	}
	
	private String getBaseServerUrl() {
		return this.applicationConfigurationService.retrieveOAuthProviderDetails().getProviderBaseUrl();
	}
	
	@Override
	public void logout(String accessToken) {
		URI restUri = URI.create(getBaseServerUrl().concat("api/protected/user/").concat(accessToken).concat("/signout"));

		oauthRestServiceTemplate.exchange(restUri, HttpMethod.GET, emptyRequest(), null);
	}

	@Override
	public void updateProtectedResource(ProtectedResourceDetails resource) {

		this.oauthRestServiceTemplate = new OAuthRestTemplate(resource);
	}

	private HttpEntity<ClientList> emptyRequest() {
		HttpHeaders requestHeaders = new HttpHeaders();
		requestHeaders.set("Accept", "application/xml");
		requestHeaders.set("Content-Type", "application/xml");

		HttpEntity<ClientList> requestEntity = new HttpEntity<ClientList>(
				requestHeaders);
		return requestEntity;
	}

	@Override
	public Collection<ClientData> retrieveAllIndividualClients() {
		URI restUri = URI
				.create(getBaseServerUrl().concat("api/protected/client/all"));

		ResponseEntity<ClientList> s = this.oauthRestServiceTemplate.exchange(
				restUri, HttpMethod.GET, emptyRequest(), ClientList.class);

		return s.getBody().getClients();
	}

	@Override
	public Collection<LoanProductData> retrieveAllLoanProducts() {
		URI restUri = URI
				.create(getBaseServerUrl().concat("api/protected/product/loan/all"));

		ResponseEntity<LoanProductList> s = this.oauthRestServiceTemplate
				.exchange(restUri, HttpMethod.GET, emptyRequest(),
						LoanProductList.class);

		return s.getBody().getProducts();
	}

	@Override
	public EntityIdentifier createLoanProduct(final CreateLoanProductCommand command) {
		try {
			URI restUri = URI
					.create(getBaseServerUrl().concat("api/protected/product/loan/new"));

			ResponseEntity<EntityIdentifier> s = this.oauthRestServiceTemplate.postForEntity(restUri,
					createLoanProductRequest(command), EntityIdentifier.class);

			return s.getBody();
		} catch (HttpStatusCodeException e) {
			ErrorResponseList errorList = parseErrors(e);
			throw new ClientValidationException(errorList.getErrors());
		}
	}
	
	@Override
	public EntityIdentifier updateLoanProduct(UpdateLoanProductCommand command) {
		try {
			URI restUri = URI
					.create(getBaseServerUrl().concat("api/protected/product/loan/update"));

			ResponseEntity<EntityIdentifier> s = this.oauthRestServiceTemplate
					.postForEntity(restUri, updateLoanProductRequest(command),
							EntityIdentifier.class);

			return s.getBody();
		} catch (HttpStatusCodeException e) {
			ErrorResponseList errorList = parseErrors(e);
			throw new ClientValidationException(errorList.getErrors());
		}
	}

	private ErrorResponseList parseErrors(HttpStatusCodeException e) {
		
		XStream xstream = new XStream();
		xstream.alias("errorResponseList", ErrorResponseList.class);
		xstream.alias("errorResponse", ErrorResponse.class);
		
		ErrorResponseList errorList = new ErrorResponseList();
		if (HttpStatus.BAD_REQUEST.equals(e.getStatusCode())) {
			errorList = (ErrorResponseList)xstream.fromXML(e.getResponseBodyAsString());
		} else if (HttpStatus.NOT_FOUND.equals(e.getStatusCode())){
			
			List<ErrorResponse> errors = new ArrayList<ErrorResponse>();
			errors.add(new ErrorResponse(e.getMessage(), "error", e.getMessage()));
			
			errorList = new ErrorResponseList(errors);
		} else if (HttpStatus.UNAUTHORIZED.equals(e.getStatusCode())) {
			throw new AccessDeniedException(e.getMessage());
		} else {
			throw e;
		}
		return errorList;
	}
	
	private HttpEntity<UpdateLoanProductCommand> updateLoanProductRequest(
			final UpdateLoanProductCommand command) {
		HttpHeaders requestHeaders = new HttpHeaders();
		requestHeaders.set("Accept", "application/xml");
		requestHeaders.set("Content-Type", "application/xml");

		HttpEntity<UpdateLoanProductCommand> requestEntity = new HttpEntity<UpdateLoanProductCommand>(
				command, requestHeaders);
		return requestEntity;
	}

	private HttpEntity<CreateLoanProductCommand> createLoanProductRequest(
			final CreateLoanProductCommand command) {
		HttpHeaders requestHeaders = new HttpHeaders();
		requestHeaders.set("Accept", "application/xml");
		requestHeaders.set("Content-Type", "application/xml");

		HttpEntity<CreateLoanProductCommand> requestEntity = new HttpEntity<CreateLoanProductCommand>(
				command, requestHeaders);
		return requestEntity;
	}

	@Override
	public LoanProductData retrieveLoanProductDetails(
			final Long selectedLoanProductOption) {
		try {
			URI restUri = URI
					.create(getBaseServerUrl().concat("api/protected/product/loan/"
							+ selectedLoanProductOption));

			ResponseEntity<LoanProductData> s = this.oauthRestServiceTemplate
					.exchange(restUri, HttpMethod.GET, emptyRequest(),
							LoanProductData.class);

			return s.getBody();
		} catch (HttpStatusCodeException e) {
			ErrorResponseList errorList = parseErrors(e);
			throw new ClientValidationException(errorList.getErrors());
		}
	}
	
	@Override
	public LoanProductData retrieveNewLoanProductDetails() {
		try {
			URI restUri = URI
					.create(getBaseServerUrl().concat("api/protected/product/loan/empty"));

			ResponseEntity<LoanProductData> s = this.oauthRestServiceTemplate
					.exchange(restUri, HttpMethod.GET, emptyRequest(),
							LoanProductData.class);

			return s.getBody();
		} catch (HttpStatusCodeException e) {
			ErrorResponseList errorList = parseErrors(e);
			throw new ClientValidationException(errorList.getErrors());
		}
	}

	@Override
	public LoanSchedule calculateLoanSchedule(
			final CalculateLoanScheduleCommand command) {
		try {
			URI restUri = URI
					.create(getBaseServerUrl().concat("api/protected/loan/calculate"));

			ResponseEntity<LoanSchedule> s = this.oauthRestServiceTemplate
					.exchange(restUri, HttpMethod.POST,
							calculateLoanRepaymentScheduleRequest(command),
							LoanSchedule.class);

			return s.getBody();
		} catch (HttpStatusCodeException e) {
			ErrorResponseList errorList = parseErrors(e);
			throw new ClientValidationException(errorList.getErrors());
		}
	}
	
	@Override
	public NewLoanWorkflowStepOneData retrieveNewLoanApplicationStepOneDetails(final Long clientId) {
		try {
			URI restUri = URI.create(getBaseServerUrl().concat("api/protected/loan/new/" + clientId.toString() + "/workflow/one"));

			ResponseEntity<NewLoanWorkflowStepOneData> s = this.oauthRestServiceTemplate
					.exchange(restUri, HttpMethod.GET, emptyRequest(), NewLoanWorkflowStepOneData.class);
			
			// for now ensure user must select product on step one (even if there is only one product!)
			NewLoanWorkflowStepOneData data = s.getBody();
			data.setProductId(null);
			
			return s.getBody();
		} catch (HttpStatusCodeException e) {
			ErrorResponseList errorList = parseErrors(e);
			throw new ClientValidationException(errorList.getErrors());
		}
	}
	

	@Override
	public NewLoanWorkflowStepOneData retrieveNewLoanApplicationDetails(Long clientId, Long productId) {
		try {
			URI restUri = URI.create(getBaseServerUrl().concat("api/protected/loan/new/" + clientId.toString() + "/product/" + productId.toString()));

			ResponseEntity<NewLoanWorkflowStepOneData> s = this.oauthRestServiceTemplate.exchange(restUri, HttpMethod.GET, emptyRequest(), NewLoanWorkflowStepOneData.class);
			
			return s.getBody();
		} catch (HttpStatusCodeException e) {
			ErrorResponseList errorList = parseErrors(e);
			throw new ClientValidationException(errorList.getErrors());
		}
	}

	@Override
	public Long submitLoanApplication(final SubmitLoanApplicationCommand command) {
		try {
			URI restUri = URI
					.create(getBaseServerUrl().concat("api/protected/loan/new"));

			ResponseEntity<EntityIdentifier> s = this.oauthRestServiceTemplate
					.postForEntity(restUri,
							submitLoanApplicationRequest(command),
							EntityIdentifier.class);

			return s.getBody().getEntityId();
		} catch (HttpStatusCodeException e) {
			ErrorResponseList errorList = parseErrors(e);
			throw new ClientValidationException(errorList.getErrors());
		}
	}
	
	@Override
	public EntityIdentifier deleteLoan(Long loanId) {
		try {
			URI restUri = URI.create(getBaseServerUrl().concat("api/protected/loan/").concat(loanId.toString()));

			ResponseEntity<EntityIdentifier> s = this.oauthRestServiceTemplate.exchange(restUri, HttpMethod.DELETE, null, EntityIdentifier.class);
			
			return s.getBody();
		} catch (HttpStatusCodeException e) {
			ErrorResponseList errorList = parseErrors(e);
			throw new ClientValidationException(errorList.getErrors());
		}
	}

	@Override
	public EntityIdentifier approveLoan(final LoanStateTransitionCommand command) {
		try {
			URI restUri = URI
					.create(getBaseServerUrl().concat("api/protected/loan/approve"));

			ResponseEntity<EntityIdentifier> s = this.oauthRestServiceTemplate
					.postForEntity(restUri,
							loanStateTransitionApplicationRequest(command),
							EntityIdentifier.class);

			return s.getBody();
		} catch (HttpStatusCodeException e) {
			ErrorResponseList errorList = parseErrors(e);
			throw new ClientValidationException(errorList.getErrors());
		}
	}

	@Override
	public EntityIdentifier undoLoanApproval(
			final UndoLoanApprovalCommand command) {
		try {
			URI restUri = URI
					.create(getBaseServerUrl().concat("api/protected/loan/undoapproval"));

			ResponseEntity<EntityIdentifier> s = this.oauthRestServiceTemplate
					.postForEntity(restUri, undoLoanApprovalRequest(command),
							EntityIdentifier.class);

			return s.getBody();
		} catch (HttpStatusCodeException e) {
			ErrorResponseList errorList = parseErrors(e);
			throw new ClientValidationException(errorList.getErrors());
		}
	}

	@Override
	public EntityIdentifier rejectLoan(final LoanStateTransitionCommand command) {
		try {
			URI restUri = URI
					.create(getBaseServerUrl().concat("api/protected/loan/reject"));

			ResponseEntity<EntityIdentifier> s = this.oauthRestServiceTemplate
					.postForEntity(restUri,
							loanStateTransitionApplicationRequest(command),
							EntityIdentifier.class);

			return s.getBody();
		} catch (HttpStatusCodeException e) {
			ErrorResponseList errorList = parseErrors(e);
			throw new ClientValidationException(errorList.getErrors());
		}
	}

	@Override
	public EntityIdentifier withdrawLoan(final LoanStateTransitionCommand command) {
		try {
			URI restUri = URI
					.create(getBaseServerUrl().concat("api/protected/loan/withdraw"));

			ResponseEntity<EntityIdentifier> s = this.oauthRestServiceTemplate
					.postForEntity(restUri,
							loanStateTransitionApplicationRequest(command),
							EntityIdentifier.class);

			return s.getBody();
		} catch (HttpStatusCodeException e) {
			ErrorResponseList errorList = parseErrors(e);
			throw new ClientValidationException(errorList.getErrors());
		}
	}

	@Override
	public EntityIdentifier disburseLoan(final LoanStateTransitionCommand command) {
		try {
			URI restUri = URI
					.create(getBaseServerUrl().concat("api/protected/loan/disburse"));

			ResponseEntity<EntityIdentifier> s = this.oauthRestServiceTemplate
					.postForEntity(restUri,
							loanStateTransitionApplicationRequest(command),
							EntityIdentifier.class);

			return s.getBody();
		} catch (HttpStatusCodeException e) {
			ErrorResponseList errorList = parseErrors(e);
			throw new ClientValidationException(errorList.getErrors());
		}
	}

	@Override
	public EntityIdentifier undloLoanDisbursal(
			final UndoLoanDisbursalCommand command) {
		try {
			URI restUri = URI
					.create(getBaseServerUrl().concat("api/protected/loan/undodisbursal"));

			ResponseEntity<EntityIdentifier> s = this.oauthRestServiceTemplate
					.postForEntity(restUri, undoLoanDisbursalRequest(command),
							EntityIdentifier.class);

			return s.getBody();
		} catch (HttpStatusCodeException e) {
			ErrorResponseList errorList = parseErrors(e);
			throw new ClientValidationException(errorList.getErrors());
		}
	}
	
	@Override
	public LoanRepaymentData retrieveNewLoanRepaymentDetails(Long loanId) {
		try {
			URI restUri = URI.create(getBaseServerUrl().concat("api/protected/loan/").concat(loanId.toString()).concat("/repayment/"));

			ResponseEntity<LoanRepaymentData> s = this.oauthRestServiceTemplate
					.exchange(restUri, HttpMethod.GET, emptyRequest(),
							LoanRepaymentData.class);

			return s.getBody();
		} catch (HttpStatusCodeException e) {
			ErrorResponseList errorList = parseErrors(e);
			throw new ClientValidationException(errorList.getErrors());
		}
	}

	@Override
	public LoanRepaymentData retrieveLoanRepaymentDetails(Long loanId, Long repaymentId) {
		try {
			URI restUri = URI.create(getBaseServerUrl().concat("api/protected/loan/").concat(loanId.toString()).concat("/repayment/").concat(repaymentId.toString()));

			ResponseEntity<LoanRepaymentData> s = this.oauthRestServiceTemplate
					.exchange(restUri, HttpMethod.GET, emptyRequest(),
							LoanRepaymentData.class);

			return s.getBody();
		} catch (HttpStatusCodeException e) {
			ErrorResponseList errorList = parseErrors(e);
			throw new ClientValidationException(errorList.getErrors());
		}
	}

	@Override
	public EntityIdentifier makeLoanRepayment(final LoanTransactionCommand command) {
		try {
			URI restUri = URI
					.create(getBaseServerUrl().concat("api/protected/loan/repayment"));

			ResponseEntity<EntityIdentifier> s = this.oauthRestServiceTemplate
					.postForEntity(restUri, loanTransactionRequest(command),
							EntityIdentifier.class);

			return s.getBody();
		} catch (HttpStatusCodeException e) {
			ErrorResponseList errorList = parseErrors(e);
			throw new ClientValidationException(errorList.getErrors());
		}
	}
	
	@Override
	public EntityIdentifier adjustLoanRepayment(AdjustLoanTransactionCommand command) {
		try {
			URI restUri = URI
					.create(getBaseServerUrl().concat("api/protected/loan/repayment/adjust"));

			ResponseEntity<EntityIdentifier> s = this.oauthRestServiceTemplate
					.postForEntity(restUri, adjustLoanRepaymentRequest(command),
							EntityIdentifier.class);

			return s.getBody();
		} catch (HttpStatusCodeException e) {
			ErrorResponseList errorList = parseErrors(e);
			throw new ClientValidationException(errorList.getErrors());
		}
	}
	
	@Override
	public LoanRepaymentData retrieveNewLoanWaiverDetails(Long loanId) {
		try {
			URI restUri = URI.create(getBaseServerUrl().concat("api/protected/loan/").concat(loanId.toString()).concat("/waive/"));

			ResponseEntity<LoanRepaymentData> s = this.oauthRestServiceTemplate
					.exchange(restUri, HttpMethod.GET, emptyRequest(),
							LoanRepaymentData.class);

			return s.getBody();
		} catch (HttpStatusCodeException e) {
			ErrorResponseList errorList = parseErrors(e);
			throw new ClientValidationException(errorList.getErrors());
		}
	}

	@Override
	public EntityIdentifier waiveLoanAmount(LoanTransactionCommand command) {
		try {
			URI restUri = URI
					.create(getBaseServerUrl().concat("api/protected/loan/waive"));

			ResponseEntity<EntityIdentifier> s = this.oauthRestServiceTemplate
					.postForEntity(restUri, loanTransactionRequest(command),
							EntityIdentifier.class);

			return s.getBody();
		} catch (HttpStatusCodeException e) {
			ErrorResponseList errorList = parseErrors(e);
			throw new ClientValidationException(errorList.getErrors());
		}
	}
	
	private HttpEntity<LoanTransactionCommand> loanTransactionRequest(final LoanTransactionCommand command) {
		HttpHeaders requestHeaders = new HttpHeaders();
		requestHeaders.set("Accept", "application/xml");
		requestHeaders.set("Content-Type", "application/xml");

		return new HttpEntity<LoanTransactionCommand>(command, requestHeaders);
	}
	
	private HttpEntity<AdjustLoanTransactionCommand> adjustLoanRepaymentRequest(
			final AdjustLoanTransactionCommand command) {
		HttpHeaders requestHeaders = new HttpHeaders();
		requestHeaders.set("Accept", "application/xml");
		requestHeaders.set("Content-Type", "application/xml");

		return new HttpEntity<AdjustLoanTransactionCommand>(command, requestHeaders);
	}

	private HttpEntity<UndoLoanDisbursalCommand> undoLoanDisbursalRequest(
			final UndoLoanDisbursalCommand command) {
		HttpHeaders requestHeaders = new HttpHeaders();
		requestHeaders.set("Accept", "application/xml");
		requestHeaders.set("Content-Type", "application/xml");

		return new HttpEntity<UndoLoanDisbursalCommand>(command, requestHeaders);
	}

	private HttpEntity<UndoLoanApprovalCommand> undoLoanApprovalRequest(
			final UndoLoanApprovalCommand command) {
		HttpHeaders requestHeaders = new HttpHeaders();
		requestHeaders.set("Accept", "application/xml");
		requestHeaders.set("Content-Type", "application/xml");

		return new HttpEntity<UndoLoanApprovalCommand>(command, requestHeaders);
	}

	private HttpEntity<LoanStateTransitionCommand> loanStateTransitionApplicationRequest(
			final LoanStateTransitionCommand command) {
		HttpHeaders requestHeaders = new HttpHeaders();
		requestHeaders.set("Accept", "application/xml");
		requestHeaders.set("Content-Type", "application/xml");

		return new HttpEntity<LoanStateTransitionCommand>(command, requestHeaders);
	}

	private HttpEntity<SubmitLoanApplicationCommand> submitLoanApplicationRequest(
			final SubmitLoanApplicationCommand command) {
		HttpHeaders requestHeaders = new HttpHeaders();
		requestHeaders.set("Accept", "application/xml");
		requestHeaders.set("Content-Type", "application/xml");

		return new HttpEntity<SubmitLoanApplicationCommand>(command,
				requestHeaders);
	}

	private HttpEntity<CalculateLoanScheduleCommand> calculateLoanRepaymentScheduleRequest(
			final CalculateLoanScheduleCommand command) {
		HttpHeaders requestHeaders = new HttpHeaders();
		requestHeaders.set("Accept", "application/xml");
		requestHeaders.set("Content-Type", "application/xml");

		return new HttpEntity<CalculateLoanScheduleCommand>(command,
				requestHeaders);
	}

	@Override
	public Collection<CurrencyData> retrieveAllowedCurrencies() {
		try {
			URI restUri = URI
					.create(getBaseServerUrl().concat("api/protected/config/currency/allowed"));

			ResponseEntity<CurrencyList> s = this.oauthRestServiceTemplate
					.exchange(restUri, HttpMethod.GET, emptyRequest(),
							CurrencyList.class);

			return s.getBody().getCurrencies();
		} catch (HttpStatusCodeException e) {
			ErrorResponseList errorList = parseErrors(e);
			throw new ClientValidationException(errorList.getErrors());
		}
	}
	
	@Override
	public Collection<EnumOptionReadModel> retrieveAllowedLoanAmortizationMethodOptions() {
		URI restUri = URI
				.create(getBaseServerUrl().concat("api/protected/config/loan/amortization/allowed"));

		ResponseEntity<EnumOptionList> s = this.oauthRestServiceTemplate
				.exchange(restUri, HttpMethod.GET, emptyRequest(),
						EnumOptionList.class);

		return s.getBody().getOptions();
	}

	@Override
	public Collection<EnumOptionReadModel> retrieveAllowedLoanInterestMethodOptions() {
		URI restUri = URI
				.create(getBaseServerUrl().concat("api/protected/config/loan/interestcalculation/allowed"));

		ResponseEntity<EnumOptionList> s = this.oauthRestServiceTemplate
				.exchange(restUri, HttpMethod.GET, emptyRequest(),
						EnumOptionList.class);

		return s.getBody().getOptions();
	}

	@Override
	public Collection<EnumOptionReadModel> retrieveAllowedRepaymentFrequencyOptions() {
		URI restUri = URI
				.create(getBaseServerUrl().concat("api/protected/config/loan/repaymentfrequency/allowed"));

		ResponseEntity<EnumOptionList> s = this.oauthRestServiceTemplate
				.exchange(restUri, HttpMethod.GET, emptyRequest(),
						EnumOptionList.class);

		return s.getBody().getOptions();
	}
	

	@Override
	public Collection<EnumOptionReadModel> retrieveAllowedNominalInterestFrequencyOptions() {
		URI restUri = URI
				.create(getBaseServerUrl().concat("api/protected/config/loan/interestfrequency/allowed"));

		ResponseEntity<EnumOptionList> s = this.oauthRestServiceTemplate
				.exchange(restUri, HttpMethod.GET, emptyRequest(),
						EnumOptionList.class);

		return s.getBody().getOptions();
	}

	@Override
	public Collection<CurrencyData> retrieveAllPlatformCurrencies() {
		try {
			URI restUri = URI
					.create(getBaseServerUrl().concat("api/protected/config/currency/all"));

			ResponseEntity<CurrencyList> s = this.oauthRestServiceTemplate
					.exchange(restUri, HttpMethod.GET, emptyRequest(),
							CurrencyList.class);

			return s.getBody().getCurrencies();
		} catch (HttpStatusCodeException e) {
			ErrorResponseList errorList = parseErrors(e);
			throw new ClientValidationException(errorList.getErrors());
		}
	}

	@Override
	public void updateOrganisationCurrencies(UpdateOrganisationCurrencyCommand command) {
		try {
			URI restUri = URI
					.create(getBaseServerUrl().concat("api/protected/config/currency/update"));

			this.oauthRestServiceTemplate.exchange(restUri, HttpMethod.PUT,
					updateCurrencyRequest(command),
					UpdateOrganisationCurrencyCommand.class);
		} catch (HttpStatusCodeException e) {
			ErrorResponseList errorList = parseErrors(e);
			throw new ClientValidationException(errorList.getErrors());
		}
	}

	private HttpEntity<UpdateOrganisationCurrencyCommand> updateCurrencyRequest(
			final UpdateOrganisationCurrencyCommand command) {
		HttpHeaders requestHeaders = new HttpHeaders();
		requestHeaders.set("Accept", "application/xml");
		requestHeaders.set("Content-Type", "application/xml");

		HttpEntity<UpdateOrganisationCurrencyCommand> requestEntity = new HttpEntity<UpdateOrganisationCurrencyCommand>(
				command, requestHeaders);
		return requestEntity;
	}

	@Override
	public EntityIdentifier createUser(final UserCommand command) {
		try {
			URI restUri = URI
					.create(getBaseServerUrl().concat("api/protected/admin/user/new"));

			ResponseEntity<EntityIdentifier> s = this.oauthRestServiceTemplate
					.postForEntity(restUri, createUserRequest(command),
							EntityIdentifier.class);

			return s.getBody();
		} catch (HttpStatusCodeException e) {
			ErrorResponseList errorList = parseErrors(e);
			throw new ClientValidationException(errorList.getErrors());
		}
	}
	
	@Override
	public EntityIdentifier updateUser(UserCommand command) {
		try {
			URI restUri = URI
					.create(getBaseServerUrl().concat("api/protected/admin/user/").concat(command.getId().toString()));

			ResponseEntity<EntityIdentifier> s = this.oauthRestServiceTemplate
					.postForEntity(restUri, createUserRequest(command),
							EntityIdentifier.class);

			return s.getBody();
		} catch (HttpStatusCodeException e) {
			ErrorResponseList errorList = parseErrors(e);
			throw new ClientValidationException(errorList.getErrors());
		}
	}
	
	@Override
	public EntityIdentifier updateCurrentUserDetails(UserCommand command) {
		try {
			URI restUri = URI
					.create(getBaseServerUrl().concat("api/protected/admin/user/current"));

			ResponseEntity<EntityIdentifier> s = this.oauthRestServiceTemplate
					.postForEntity(restUri, createUserRequest(command),
							EntityIdentifier.class);

			return s.getBody();
		} catch (HttpStatusCodeException e) {
			ErrorResponseList errorList = parseErrors(e);
			throw new ClientValidationException(errorList.getErrors());
		}
	}
	
	@Override
	public EntityIdentifier updateCurrentUserPassword(ChangePasswordCommand command) {
		try {
			URI restUri = URI
					.create(getBaseServerUrl().concat("api/protected/admin/user/current/password"));

			ResponseEntity<EntityIdentifier> s = this.oauthRestServiceTemplate
					.postForEntity(restUri, createUpdateCurrentUserPasswordRequest(command),
							EntityIdentifier.class);

			return s.getBody();
		} catch (HttpStatusCodeException e) {
			ErrorResponseList errorList = parseErrors(e);
			throw new ClientValidationException(errorList.getErrors());
		}
	}
	
	@Override
	public void deleteUser(Long userId) {
		URI restUri = URI.create(getBaseServerUrl().concat("api/protected/admin/user/").concat(userId.toString()));

		this.oauthRestServiceTemplate.delete(restUri);
	}

	private HttpEntity<UserCommand> createUserRequest(
			final UserCommand command) {

		HttpHeaders requestHeaders = new HttpHeaders();
		requestHeaders.set("Accept", "application/xml");
		requestHeaders.set("Content-Type", "application/xml");

		HttpEntity<UserCommand> requestEntity = new HttpEntity<UserCommand>(
				command, requestHeaders);
		return requestEntity;
	}
	
	private HttpEntity<ChangePasswordCommand> createUpdateCurrentUserPasswordRequest(final ChangePasswordCommand command) {

		HttpHeaders requestHeaders = new HttpHeaders();
		requestHeaders.set("Accept", "application/xml");
		requestHeaders.set("Content-Type", "application/xml");

		HttpEntity<ChangePasswordCommand> requestEntity = new HttpEntity<ChangePasswordCommand>(command, requestHeaders);
		return requestEntity;
	}

	private HttpEntity<RoleCommand> roleRequest(
			final RoleCommand command) {

		HttpHeaders requestHeaders = new HttpHeaders();
		requestHeaders.set("Accept", "application/xml");
		requestHeaders.set("Content-Type", "application/xml");

		HttpEntity<RoleCommand> requestEntity = new HttpEntity<RoleCommand>(
				command, requestHeaders);
		return requestEntity;
	}

	@Override
	public Collection<AppUserData> retrieveAllUsers() {
		try {
			URI restUri = URI
					.create(getBaseServerUrl().concat("api/protected/admin/user/all"));

			ResponseEntity<UserList> s = this.oauthRestServiceTemplate
					.exchange(restUri, HttpMethod.GET, emptyRequest(),
							UserList.class);

			return s.getBody().getUsers();
		} catch (HttpStatusCodeException e) {
			ErrorResponseList errorList = parseErrors(e);
			throw new ClientValidationException(errorList.getErrors());
		}
	}
	
	@Override
	public AppUserData retrieveNewUserDetails() {
		
		URI restUri = URI.create(getBaseServerUrl().concat("api/protected/admin/user/new"));

		ResponseEntity<AppUserData> s = this.oauthRestServiceTemplate.exchange(restUri, HttpMethod.GET, emptyRequest(), AppUserData.class);

		return s.getBody();
	}
	
	@Override
	public AppUserData retrieveUser(Long userId) {
		URI restUri = URI.create(getBaseServerUrl().concat("api/protected/admin/user/").concat(userId.toString()));

		ResponseEntity<AppUserData> s = this.oauthRestServiceTemplate.exchange(restUri, HttpMethod.GET, emptyRequest(), AppUserData.class);

		return s.getBody();
	}
	
	@Override
	public AppUserData retrieveCurrentUser() {
		URI restUri = URI.create(getBaseServerUrl().concat("api/protected/admin/user/current"));

		ResponseEntity<AppUserData> s = this.oauthRestServiceTemplate.exchange(restUri, HttpMethod.GET, emptyRequest(), AppUserData.class);

		return s.getBody();
	}

	@Override
	public Collection<RoleData> retrieveAllRoles() {
		try {
			URI restUri = URI
					.create(getBaseServerUrl().concat("api/protected/admin/role/all"));

			ResponseEntity<RoleList> s = this.oauthRestServiceTemplate
					.exchange(restUri, HttpMethod.GET, emptyRequest(),
							RoleList.class);

			return s.getBody().getRoles();
		} catch (HttpStatusCodeException e) {
			ErrorResponseList errorList = parseErrors(e);
			throw new ClientValidationException(errorList.getErrors());
		}
	}

	@Override
	public RoleData retrieveRole(final Long roleId) {
		
		URI restUri = URI.create(getBaseServerUrl().concat("api/protected/admin/role/").concat(roleId.toString()));

		ResponseEntity<RoleData> s = this.oauthRestServiceTemplate.exchange(restUri, HttpMethod.GET, emptyRequest(), RoleData.class);
	
		return s.getBody();
	}
	
	@Override
	public RoleData retrieveNewRoleDetails() {
		URI restUri = URI.create(getBaseServerUrl().concat("api/protected/admin/role/new"));

		ResponseEntity<RoleData> s = this.oauthRestServiceTemplate.exchange(restUri, HttpMethod.GET, emptyRequest(), RoleData.class);
	
		return s.getBody();
	}

	@Override
	public EntityIdentifier createRole(final RoleCommand command) {
		try {
			URI restUri = URI
					.create(getBaseServerUrl().concat("api/protected/admin/role/new"));

			ResponseEntity<EntityIdentifier> s = this.oauthRestServiceTemplate
					.postForEntity(restUri, roleRequest(command),
							EntityIdentifier.class);

			return s.getBody();
		} catch (HttpStatusCodeException e) {
			ErrorResponseList errorList = parseErrors(e);
			throw new ClientValidationException(errorList.getErrors());
		}
	}
	
	@Override
	public EntityIdentifier updateRole(RoleCommand command) {
		try {
			URI restUri = URI
					.create(getBaseServerUrl().concat("api/protected/admin/role/").concat(command.getId().toString()));

			ResponseEntity<EntityIdentifier> s = this.oauthRestServiceTemplate
					.postForEntity(restUri, roleRequest(command),
							EntityIdentifier.class);

			return s.getBody();
		} catch (HttpStatusCodeException e) {
			ErrorResponseList errorList = parseErrors(e);
			throw new ClientValidationException(errorList.getErrors());
		}
	}

	@Override
	public Collection<PermissionData> retrieveAllPermissions() {
		
			URI restUri = URI.create(getBaseServerUrl().concat("api/protected/admin/permissions/all"));

			ResponseEntity<PermissionList> s = this.oauthRestServiceTemplate
					.exchange(restUri, HttpMethod.GET, emptyRequest(),
							PermissionList.class);

			return s.getBody().getPermissions();
	}

	@Override
	public Collection<EnumOptionReadModel> retrieveAllPermissionGroups() {
		try {
			URI restUri = URI
					.create(getBaseServerUrl().concat("api/protected/admin/permissiongroup/all"));

			ResponseEntity<EnumOptionList> s = this.oauthRestServiceTemplate
					.exchange(restUri, HttpMethod.GET, emptyRequest(),
							EnumOptionList.class);

			return s.getBody().getOptions();
		} catch (HttpStatusCodeException e) {
			ErrorResponseList errorList = parseErrors(e);
			throw new ClientValidationException(errorList.getErrors());
		}
	}

	@Override
	public Collection<OfficeData> retrieveAllOffices() {
		try {
			URI restUri = URI
					.create(getBaseServerUrl().concat("api/protected/office/all"));

			ResponseEntity<OfficeList> s = this.oauthRestServiceTemplate
					.exchange(restUri, HttpMethod.GET, emptyRequest(),
							OfficeList.class);

			return s.getBody().getOffices();
		} catch (HttpStatusCodeException e) {
			ErrorResponseList errorList = parseErrors(e);
			throw new ClientValidationException(errorList.getErrors());
		}
	}

	@Override
	public OfficeData retrieveOffice(final Long officeId) {
		try {
			URI restUri = URI
					.create(getBaseServerUrl().concat("api/protected/office/"
							+ officeId));

			ResponseEntity<OfficeData> s = this.oauthRestServiceTemplate
					.exchange(restUri, HttpMethod.GET, emptyRequest(),
							OfficeData.class);

			return s.getBody();
		} catch (HttpStatusCodeException e) {
			ErrorResponseList errorList = parseErrors(e);
			throw new ClientValidationException(errorList.getErrors());
		}
	}

	@Override
	public EntityIdentifier createOffice(final OfficeCommand command) {
		try {
			URI restUri = URI
					.create(getBaseServerUrl().concat("api/protected/office/new"));

			ResponseEntity<EntityIdentifier> s = this.oauthRestServiceTemplate
					.postForEntity(restUri, createOfficeRequest(command),
							EntityIdentifier.class);

			return s.getBody();
		} catch (HttpStatusCodeException e) {
			ErrorResponseList errorList = parseErrors(e);
			throw new ClientValidationException(errorList.getErrors());
		}
	}
	

	@Override
	public EntityIdentifier transferFunds(BranchMoneyTransferCommand command) {
		try {
			URI restUri = URI
					.create(getBaseServerUrl().concat("api/protected/office/").concat(command.getFromOfficeId().toString()).concat("/transfer"));

			ResponseEntity<EntityIdentifier> s = this.oauthRestServiceTemplate
					.postForEntity(restUri, createOfficeFundTransferRequest(command),
							EntityIdentifier.class);

			return s.getBody();
		} catch (HttpStatusCodeException e) {
			ErrorResponseList errorList = parseErrors(e);
			throw new ClientValidationException(errorList.getErrors());
		}
	}

	@Override
	public EntityIdentifier updateOffice(final OfficeCommand command) {
		try {
			URI restUri = URI
					.create(getBaseServerUrl().concat("api/protected/office/update"));

			ResponseEntity<EntityIdentifier> s = this.oauthRestServiceTemplate
					.postForEntity(restUri, updateOfficeRequest(command),
							EntityIdentifier.class);

			return s.getBody();
		} catch (HttpStatusCodeException e) {
			ErrorResponseList errorList = parseErrors(e);
			throw new ClientValidationException(errorList.getErrors());
		}
	}

	private HttpEntity<BranchMoneyTransferCommand> createOfficeFundTransferRequest(
			final BranchMoneyTransferCommand command) {

		HttpHeaders requestHeaders = new HttpHeaders();
		requestHeaders.set("Accept", "application/xml");
		requestHeaders.set("Content-Type", "application/xml");

		HttpEntity<BranchMoneyTransferCommand> requestEntity = new HttpEntity<BranchMoneyTransferCommand>(
				command, requestHeaders);
		return requestEntity;
	}
	
	private HttpEntity<OfficeCommand> createOfficeRequest(
			final OfficeCommand command) {

		HttpHeaders requestHeaders = new HttpHeaders();
		requestHeaders.set("Accept", "application/xml");
		requestHeaders.set("Content-Type", "application/xml");

		HttpEntity<OfficeCommand> requestEntity = new HttpEntity<OfficeCommand>(
				command, requestHeaders);
		return requestEntity;
	}

	private HttpEntity<OfficeCommand> updateOfficeRequest(
			final OfficeCommand command) {

		HttpHeaders requestHeaders = new HttpHeaders();
		requestHeaders.set("Accept", "application/xml");
		requestHeaders.set("Content-Type", "application/xml");

		HttpEntity<OfficeCommand> requestEntity = new HttpEntity<OfficeCommand>(
				command, requestHeaders);
		return requestEntity;
	}

	@Override
	public ClientData retrieveNewIndividualClient() {
		URI restUri = URI.create(getBaseServerUrl().concat("api/protected/client/new"));

		ResponseEntity<ClientData> s = this.oauthRestServiceTemplate.exchange(restUri, HttpMethod.GET, emptyRequest(), ClientData.class);
	
		return s.getBody();
	}
	

	@Override
	public ClientData retrieveClientDetails(Long clientId) {
		try {
			URI restUri = URI.create(getBaseServerUrl().concat(
					"api/protected/client/").concat(clientId.toString()));

			ResponseEntity<ClientData> s = this.oauthRestServiceTemplate
					.exchange(restUri, HttpMethod.GET, emptyRequest(),
							ClientData.class);

			return s.getBody();
		} catch (HttpStatusCodeException e) {
			ErrorResponseList errorList = parseErrors(e);
			throw new ClientValidationException(errorList.getErrors());
		}
	}

	@Override
	public EntityIdentifier enrollClient(EnrollClientCommand command) {
		try {
			
			URI restUri = URI.create(getBaseServerUrl().concat("api/protected/client/new"));

			ResponseEntity<EntityIdentifier> s = this.oauthRestServiceTemplate.postForEntity(restUri, enrollClientRequest(command), EntityIdentifier.class);

			return s.getBody();
		} catch (HttpStatusCodeException e) {
			ErrorResponseList errorList = parseErrors(e);
			throw new ClientValidationException(errorList.getErrors());
		}
	}
	
	private HttpEntity<EnrollClientCommand> enrollClientRequest(
			final EnrollClientCommand command) {
		HttpHeaders requestHeaders = new HttpHeaders();
		requestHeaders.set("Accept", "application/xml");
		requestHeaders.set("Content-Type", "application/xml");

		return new HttpEntity<EnrollClientCommand>(command, requestHeaders);
	}
	
	@Override
	public EntityIdentifier addNote(NoteCommand command) {
		try {
			URI restUri = URI.create(getBaseServerUrl().concat("api/protected/client/").concat(command.getClientId().toString()).concat("/note/new"));

			ResponseEntity<EntityIdentifier> s = this.oauthRestServiceTemplate.postForEntity(restUri, noteRequest(command), EntityIdentifier.class);

			return s.getBody();
		} catch (HttpStatusCodeException e) {
			ErrorResponseList errorList = parseErrors(e);
			throw new ClientValidationException(errorList.getErrors());
		}
	}
	

	@Override
	public EntityIdentifier updateNote(NoteCommand command) {
		try {
			URI restUri = URI.create(getBaseServerUrl().concat("api/protected/note/").concat(command.getId().toString()));

			ResponseEntity<EntityIdentifier> s = this.oauthRestServiceTemplate.postForEntity(restUri, noteRequest(command), EntityIdentifier.class);

			return s.getBody();
		} catch (HttpStatusCodeException e) {
			ErrorResponseList errorList = parseErrors(e);
			throw new ClientValidationException(errorList.getErrors());
		}
	}

	@Override
	public NoteData retrieveClientNote(Long clientId, Long noteId) {
		try {
			URI restUri = URI.create(getBaseServerUrl().concat("api/protected/client/").concat(clientId.toString()).concat("/note/").concat(noteId.toString()));

			ResponseEntity<NoteData> s = this.oauthRestServiceTemplate.exchange(
					restUri, HttpMethod.GET, emptyRequest(), NoteData.class);

			return s.getBody();
		} catch (HttpStatusCodeException e) {
			ErrorResponseList errorList = parseErrors(e);
			throw new ClientValidationException(errorList.getErrors());
		}
	}
	
	@Override
	public Collection<NoteData> retrieveClientNotes(Long clientId) {
		try {
			URI restUri = URI.create(getBaseServerUrl().concat("api/protected/client/").concat(clientId.toString()).concat("/note/all"));

			ResponseEntity<NoteDataList> s = this.oauthRestServiceTemplate.exchange(
					restUri, HttpMethod.GET, emptyRequest(), NoteDataList.class);

			return s.getBody().getNotes();
		} catch (HttpStatusCodeException e) {
			ErrorResponseList errorList = parseErrors(e);
			throw new ClientValidationException(errorList.getErrors());
		}
	}
	
	private HttpEntity<NoteCommand> noteRequest(final NoteCommand command) {
		HttpHeaders requestHeaders = new HttpHeaders();
		requestHeaders.set("Accept", "application/xml");
		requestHeaders.set("Content-Type", "application/xml");

		return new HttpEntity<NoteCommand>(command, requestHeaders);
	}

	@Override
	public ClientDataWithAccountsData retrieveClientAccount(Long clientId) {
		try {
			URI restUri = URI
					.create(getBaseServerUrl().concat("api/protected/client/").concat(clientId.toString()).concat("/withaccounts"));

			ResponseEntity<ClientDataWithAccountsData> s = this.oauthRestServiceTemplate
					.exchange(restUri, HttpMethod.GET, emptyRequest(),
							ClientDataWithAccountsData.class);
			
			return s.getBody();
		} catch (HttpStatusCodeException e) {
			ErrorResponseList errorList = parseErrors(e);
			throw new ClientValidationException(errorList.getErrors());
		}
	}

	@Override
	public LoanAccountData retrieveLoanAccount(Long loanId) {
		try {
			URI restUri = URI
					.create(getBaseServerUrl().concat("api/protected/loan/"
							+ loanId));

			ResponseEntity<LoanAccountData> s = this.oauthRestServiceTemplate
					.exchange(restUri, HttpMethod.GET, emptyRequest(),
							LoanAccountData.class);
			
			return s.getBody();
		} catch (HttpStatusCodeException e) {
			ErrorResponseList errorList = parseErrors(e);
			throw new ClientValidationException(errorList.getErrors());
		}
	}
	
	@Override
	public OfficeTransferData retrieveOfficeTransferDetails(final Long officeId) {
		try {
			URI restUri = URI.create(getBaseServerUrl().concat("api/protected/office/" + officeId).concat("/transfer"));

			ResponseEntity<OfficeTransferData> s = this.oauthRestServiceTemplate.exchange(restUri, HttpMethod.GET, emptyRequest(), OfficeTransferData.class);
			
			return s.getBody();
		} catch (HttpStatusCodeException e) {
			ErrorResponseList errorList = parseErrors(e);
			throw new ClientValidationException(errorList.getErrors());
		}
	}

	@Override
	public GenericResultset retrieveReportingData(String rptDB, String name, String type, Map<String, String> extractedQueryParams) {
		try {
			
			StringBuilder uriAsString = new StringBuilder(getBaseServerUrl())
												.append("api/protected/reporting/exportcsv/")
												.append(encodeURIComponent(rptDB))
												.append('/')
												.append(encodeURIComponent(name))
												.append('/')
												.append(encodeURIComponent(type))
												.append('/');
			
			String officeIdValue = "0";
			String currencyIdValue = "-1"; // represents all currencies
			
			if (extractedQueryParams.size() > 3) {
				for (String key : extractedQueryParams.keySet()) {
					if (key.equalsIgnoreCase("${officeId}")) {
						officeIdValue = extractedQueryParams.get(key);
					}
					
					if (key.equalsIgnoreCase("${currencyId}")) {
						currencyIdValue = extractedQueryParams.get(key);
					}
				}
			}
			
			uriAsString.append("office/").append(encodeURIComponent(officeIdValue)).append('/');
			uriAsString.append("currency/").append(encodeURIComponent(currencyIdValue)).append('/');
					
			URI restUri = URI.create(uriAsString.toString());

			ResponseEntity<GenericResultset> s = this.oauthRestServiceTemplate
					.exchange(restUri, HttpMethod.GET, emptyRequest(), GenericResultset.class);
			
			return s.getBody();
		} catch (HttpStatusCodeException e) {
			ErrorResponseList errorList = parseErrors(e);
			throw new ClientValidationException(errorList.getErrors());
		}
	}


	@Override
	public ExtraDatasets retrieveExtraDatasets(String datasetType) {
		logger.info("In CommonRestOperationsImpl - retrieveExtraDatasets");
		try {
			URI restUri = URI.create(getBaseServerUrl().concat("api/protected/extradata/datasets/").concat(encodeURIComponent(datasetType)));

			ResponseEntity<ExtraDatasets> s = this.oauthRestServiceTemplate
					.exchange(restUri, HttpMethod.GET, emptyRequest(), ExtraDatasets.class);
			return s.getBody();
		} catch (HttpStatusCodeException e) {
			logger.info(e.toString());
			ErrorResponseList errorList = parseErrors(e);
			throw new ClientValidationException(errorList.getErrors());
		}
	}
	
	
	@Override
	public GenericResultset retrieveExtraData(String datasetType, String datasetName,
			String datasetPKValue) {
		try {
			URI restUri = URI.create(getBaseServerUrl().concat("api/protected/extradata/").concat(encodeURIComponent(datasetType)).concat("/").concat(encodeURIComponent(datasetName))
					.concat("/").concat(encodeURIComponent(datasetPKValue))
			);
			
			
			ResponseEntity<GenericResultset> s = this.oauthRestServiceTemplate
					.exchange(restUri, HttpMethod.GET, emptyRequest(),
							GenericResultset.class);
			
			return s.getBody();
		} catch (HttpStatusCodeException e) {
			ErrorResponseList errorList = parseErrors(e);
			throw new ClientValidationException(errorList.getErrors());
		}
	}

	@SuppressWarnings("rawtypes")
	@Override
	public EntityIdentifier saveExtraData(String datasetType, String datasetName, String datasetPKValue, Map requestMap) {

		String UrlString = getBaseServerUrl().concat("api/protected/extradata/").concat(encodeURIComponent(datasetType)).concat("/").concat(encodeURIComponent(datasetName))
				.concat("/").concat(encodeURIComponent(datasetPKValue));

		String value;
		boolean firstParameter = true;
		for (Object key : requestMap.keySet()) {
			if (firstParameter) {
				UrlString += "?";
				firstParameter = false;
			}
			else {
				UrlString += "&";				
			}	
			
			String[] valueArray = (String[]) requestMap.get(key);
			value = valueArray[0];
			if (value == null || value.equals("")) {
				value = "";
			}
			UrlString += encodeURIComponent(key.toString()) + "=" + encodeURIComponent(value);
		}
		logger.info("Url: " + UrlString);
		
		try {
			URI restUri = URI.create(UrlString); 	
			ResponseEntity<EntityIdentifier> s = this.oauthRestServiceTemplate.postForEntity(restUri, emptyRequest(), EntityIdentifier.class);
			return s.getBody();
		} catch (HttpStatusCodeException e) {
			ErrorResponseList errorList = parseErrors(e);
			throw new ClientValidationException(errorList.getErrors());
		}
	}
	
	private static String encodeURIComponent(String component)   {
		String result = null;      

		try {
			result = URLEncoder.encode(component, "UTF-8")
				   .replaceAll("\\%28", "(")
				   .replaceAll("\\%29", ")")
				   .replaceAll("\\+", "%20")
				   .replaceAll("\\%27", "'")
				   .replaceAll("\\%21", "!")
				   .replaceAll("\\%7E", "~");
		} catch (UnsupportedEncodingException e) {
			result = component;
		}      

		return result;
	}
}