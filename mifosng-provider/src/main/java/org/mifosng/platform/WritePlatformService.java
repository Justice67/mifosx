package org.mifosng.platform;

import org.mifosng.data.EntityIdentifier;
import org.mifosng.data.command.AdjustLoanTransactionCommand;
import org.mifosng.data.command.BranchMoneyTransferCommand;
import org.mifosng.data.command.ChangePasswordCommand;
import org.mifosng.data.command.CreateLoanProductCommand;
import org.mifosng.data.command.EnrollClientCommand;
import org.mifosng.data.command.LoanStateTransitionCommand;
import org.mifosng.data.command.LoanTransactionCommand;
import org.mifosng.data.command.NoteCommand;
import org.mifosng.data.command.OfficeCommand;
import org.mifosng.data.command.RoleCommand;
import org.mifosng.data.command.SignupCommand;
import org.mifosng.data.command.SubmitLoanApplicationCommand;
import org.mifosng.data.command.UndoLoanApprovalCommand;
import org.mifosng.data.command.UndoLoanDisbursalCommand;
import org.mifosng.data.command.UpdateLoanProductCommand;
import org.mifosng.data.command.UpdateOrganisationCurrencyCommand;
import org.mifosng.data.command.UpdateUsernamePasswordCommand;
import org.mifosng.data.command.UserCommand;
import org.mifosng.platform.exceptions.InvalidSignupException;
import org.springframework.security.access.prepost.PreAuthorize;

public interface WritePlatformService {

	Long createOffice(OfficeCommand command);

	Long updateOffice(OfficeCommand command);

	Long createUser(UserCommand command);
	
	Long updateUser(UserCommand command);
	
	Long updateCurrentUser(UserCommand command);

	Long updateCurrentUserPassword(ChangePasswordCommand command);
	
	void deleteUser(Long userId);

	Long createRole(RoleCommand command);
	
	Long updateRole(RoleCommand command);

	/**
	 * @throws InvalidSignupException when sign up fails
	 */
	Long signup(SignupCommand command);

	void updateUsernamePasswordOnFirstTimeLogin(UpdateUsernamePasswordCommand command);

	@PreAuthorize(value = "hasAnyRole('PORTFOLIO_MANAGEMENT_SUPER_USER_ROLE', 'CAN_ENROLL_NEW_CLIENT_ROLE')")
	Long enrollClient(EnrollClientCommand command);
	
	void updateOrganisationCurrencies(UpdateOrganisationCurrencyCommand command);

	EntityIdentifier createLoanProduct(CreateLoanProductCommand command);
	
	EntityIdentifier updateLoanProduct(UpdateLoanProductCommand command);

	@PreAuthorize(value = "hasAnyRole('PORTFOLIO_MANAGEMENT_SUPER_USER_ROLE', 'CAN_DELETE_LOAN_THAT_IS_SUBMITTED_AND_NOT_APPROVED')")
	EntityIdentifier deleteLoan(Long loanId);
	
	@PreAuthorize(value = "hasAnyRole('PORTFOLIO_MANAGEMENT_SUPER_USER_ROLE', 'CAN_SUBMIT_NEW_LOAN_APPLICATION_ROLE', 'CAN_SUBMIT_HISTORIC_LOAN_APPLICATION_ROLE')")
	EntityIdentifier submitLoanApplication(SubmitLoanApplicationCommand command);

	@PreAuthorize(value = "hasAnyRole('PORTFOLIO_MANAGEMENT_SUPER_USER_ROLE', 'CAN_APPROVE_LOAN_ROLE', 'CAN_APPROVE_LOAN_IN_THE_PAST_ROLE')")
	EntityIdentifier approveLoanApplication(LoanStateTransitionCommand command);

	@PreAuthorize(value = "hasAnyRole('PORTFOLIO_MANAGEMENT_SUPER_USER_ROLE', 'CAN_UNDO_LOAN_APPROVAL_ROLE')")
	EntityIdentifier undoLoanApproval(UndoLoanApprovalCommand command);

	@PreAuthorize(value = "hasAnyRole('PORTFOLIO_MANAGEMENT_SUPER_USER_ROLE', 'CAN_REJECT_LOAN_ROLE', 'CAN_REJECT_LOAN_IN_THE_PAST_ROLE')")
	EntityIdentifier rejectLoan(LoanStateTransitionCommand command);

	@PreAuthorize(value = "hasAnyRole('PORTFOLIO_MANAGEMENT_SUPER_USER_ROLE', 'CAN_WITHDRAW_LOAN_ROLE', 'CAN_WITHDRAW_LOAN_IN_THE_PAST_ROLE')")
	EntityIdentifier withdrawLoan(LoanStateTransitionCommand command);

	@PreAuthorize(value = "hasAnyRole('PORTFOLIO_MANAGEMENT_SUPER_USER_ROLE', 'CAN_DISBURSE_LOAN_ROLE', 'CAN_DISBURSE_LOAN_IN_THE_PAST_ROLE')")
	EntityIdentifier disburseLoan(LoanStateTransitionCommand command);

	@PreAuthorize(value = "hasAnyRole('PORTFOLIO_MANAGEMENT_SUPER_USER_ROLE', 'CAN_UNDO_LOAN_DISBURSAL_ROLE')")
	public EntityIdentifier undloLoanDisbursal(UndoLoanDisbursalCommand command);

	@PreAuthorize(value = "hasAnyRole('PORTFOLIO_MANAGEMENT_SUPER_USER_ROLE', 'CAN_MAKE_LOAN_REPAYMENT_ROLE', 'CAN_MAKE_LOAN_REPAYMENT_IN_THE_PAST_ROLE')")
	public EntityIdentifier makeLoanRepayment(LoanTransactionCommand command);

	EntityIdentifier adjustLoanTransaction(AdjustLoanTransactionCommand command);

	EntityIdentifier waiveLoanAmount(LoanTransactionCommand command);

	EntityIdentifier addClientNote(NoteCommand command);

	EntityIdentifier updateNote(NoteCommand command);

	Long branchMoneyTransfer(BranchMoneyTransferCommand command);
}