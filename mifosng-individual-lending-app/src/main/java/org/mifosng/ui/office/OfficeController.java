package org.mifosng.ui.office;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.mifosng.data.EntityIdentifier;
import org.mifosng.data.ErrorResponse;
import org.mifosng.data.OfficeData;
import org.mifosng.data.OfficeTransferData;
import org.mifosng.data.command.BranchMoneyTransferCommand;
import org.mifosng.data.command.OfficeCommand;
import org.mifosng.ui.CommonRestOperations;
import org.mifosng.ui.loanproduct.ClientValidationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@Controller
public class OfficeController {

	private final CommonRestOperations commonRestOperations;

	@Autowired
	public OfficeController(final CommonRestOperations commonRestOperations) {
		this.commonRestOperations = commonRestOperations;
	}
	
	@ExceptionHandler(AccessDeniedException.class)
	public String accessDeniedException() {
		return "unAuthorizedAction";
	}

	@ExceptionHandler(ClientValidationException.class)
	public @ResponseBody
	Collection<ErrorResponse> validationException(ClientValidationException ex,
			HttpServletResponse response) {

		response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
		response.setContentType("application/json");

		return ex.getValidationErrors();
	}
	
	private LocalDate parseStringToLocalDate(String eventDate, String dateFieldIdentifier) {
		LocalDate eventLocalDate = null;
		if (StringUtils.isNotBlank(eventDate)) {
			try {
				Locale locale = LocaleContextHolder.getLocale();
				eventLocalDate = DateTimeFormat.forPattern("dd MMMM yyyy").withLocale(locale).parseLocalDate(eventDate.toLowerCase(locale));
			} catch (IllegalArgumentException e) {
				List<ErrorResponse> validationErrors = new ArrayList<ErrorResponse>();
				validationErrors.add(new ErrorResponse("validation.msg.invalid.date.format", dateFieldIdentifier, eventDate));
				throw new ClientValidationException(validationErrors);
			}
		}
		
		return eventLocalDate;
	}
	
	@RequestMapping(consumes="application/json", produces="application/json", value = "/org/office/all", method = RequestMethod.GET)
	public @ResponseBody Collection<OfficeData> viewAllOffices() {

		return this.commonRestOperations.retrieveAllOffices();
	}
	
	@RequestMapping(consumes="application/json", produces="application/json", value = "/org/office/new", method = RequestMethod.GET)
	public @ResponseBody OfficeData viewNewOfficeForm() {

		OfficeData newOffice = new OfficeData();
		newOffice.setOpeningDate(new LocalDate());
		
		Collection<OfficeData> allOffices = this.commonRestOperations.retrieveAllOffices();
		List<OfficeData> allowedParents = new ArrayList<OfficeData>(allOffices);
		
		newOffice.setAllowedParents(allowedParents);
		
		return newOffice;
	}
	
	@RequestMapping(consumes="application/x-www-form-urlencoded", produces="application/json", value = "/org/office/new", method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.OK)
	public @ResponseBody EntityIdentifier createOffice(HttpServletRequest request,
			@RequestParam("name") String name, 
			@RequestParam(value="parentId", required=false) Long parentId,
			@RequestParam("openingDate") String openingDate, 
			@RequestParam(value="externalId", required=false) String externalId)  {
		
		LocalDate openingLocalDate = parseStringToLocalDate(openingDate, "openingDate");
		
		OfficeCommand command = new OfficeCommand(name, externalId, parentId, openingLocalDate);
		return this.commonRestOperations.createOffice(command);
	}
	
	@RequestMapping(consumes="application/json", produces="application/json", value = "/org/office/{officeId}", method = RequestMethod.GET)
	public @ResponseBody OfficeData viewOffice(@PathVariable("officeId") final Long officeId) {

		return this.commonRestOperations.retrieveOffice(officeId);
	}
	
	@RequestMapping(consumes="application/x-www-form-urlencoded", produces="application/json", value = "/org/office/{officeId}", method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.OK)
	public @ResponseBody EntityIdentifier updateOffice(HttpServletRequest request, @PathVariable("officeId") final Long officeId,
			@RequestParam("name") String name, 
			@RequestParam(value="parentId", required=false) Long parentId,
			@RequestParam("openingDate") String openingDate, 
			@RequestParam(value="externalId", required=false) String externalId)  {
		
		LocalDate openingLocalDate = parseStringToLocalDate(openingDate, "openingDate");
		
		OfficeCommand command = new OfficeCommand(officeId, name, externalId, parentId, openingLocalDate);
		return this.commonRestOperations.updateOffice(command);
	}
	
	@RequestMapping(consumes="application/json", produces="application/json", value = "/org/office/{officeId}/transfer", method = RequestMethod.GET)
	public @ResponseBody OfficeTransferData retrieveOfficeTransferFromDetails(@PathVariable("officeId") final Long officeId) {

		return this.commonRestOperations.retrieveOfficeTransferDetails(officeId);
	}
	
	@RequestMapping(consumes="application/x-www-form-urlencoded", produces="application/json", value = "/org/office/{officeId}/transfer", method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.OK)
	public @ResponseBody EntityIdentifier transferFundsToOffice(HttpServletRequest request, 
			@PathVariable("officeId") final Long fromOfficeId,
			@RequestParam(value="toOfficeId", required=false) final Long toOfficeId,
			@RequestParam(value="transactionDate", required=false) final String transactionDate,
			@RequestParam(value="transactionAmount", required=false) String transactionAmountNumber)  {
		
		LocalDate transactionLocalDate = parseStringToLocalDate(transactionDate, "transactionDate");
		BigDecimal transactionAmount = parseStringToBigDecimal(transactionAmountNumber);
		
		BranchMoneyTransferCommand command = new BranchMoneyTransferCommand(fromOfficeId, toOfficeId, transactionLocalDate, transactionAmount);
		
		return this.commonRestOperations.transferFunds(command);
	}
	
	private BigDecimal parseStringToBigDecimal(String source) {
		try {
			BigDecimal number = null;

			if (StringUtils.isNotBlank(source)) {
				String sourceWithoutSpaces = source.replaceAll(" ", "");
				Locale locale = LocaleContextHolder.getLocale();
				NumberFormat format = NumberFormat.getNumberInstance(locale);
				Number parsedNumber = format.parse(sourceWithoutSpaces);
				number = BigDecimal.valueOf(Double.valueOf(parsedNumber
						.doubleValue()));
			}
			
			return number;
		} catch (ParseException e) {
			List<ErrorResponse> validationErrors = new ArrayList<ErrorResponse>();
			validationErrors.add(new ErrorResponse("validation.msg.invalid.number.format", "amount", source));
			throw new ClientValidationException(validationErrors);
		}
	}
}