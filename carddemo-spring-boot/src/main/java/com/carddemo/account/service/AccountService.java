package com.carddemo.account.service;

import com.carddemo.account.api.AccountUpdateRequest;
import com.carddemo.account.api.AccountUpdateResponse;
import com.carddemo.account.api.AccountViewResponse;
import com.carddemo.account.model.Account;
import com.carddemo.account.model.Customer;
import com.carddemo.account.repository.AccountRepository;
import com.carddemo.account.repository.CustomerRepository;
import com.carddemo.card.model.CardXref;
import com.carddemo.card.repository.CardXrefRepository;
import com.carddemo.common.error.ConflictException;
import com.carddemo.common.error.ResourceNotFoundException;
import com.carddemo.common.validation.LegacyValidation;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;
    private final CardXrefRepository cardXrefRepository;

    public AccountService(AccountRepository accountRepository,
                          CustomerRepository customerRepository,
                          CardXrefRepository cardXrefRepository) {
        this.accountRepository = accountRepository;
        this.customerRepository = customerRepository;
        this.cardXrefRepository = cardXrefRepository;
    }

    @Transactional(readOnly = true)
    public AccountViewResponse getAccountView(Long accountId) {
        validateAccountId(accountId);
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        CardXref xref = cardXrefRepository.findByAccountIdOrderByCardNumberAsc(accountId).stream()
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Account missing card xref"));

        Customer customer = customerRepository.findById(xref.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found for account"));

        return AccountViewResponse.from(account, customer, xref.getCardNumber());
    }

    @Transactional
    public AccountUpdateResponse updateAccount(Long accountId, AccountUpdateRequest request) {
        validateAccountId(accountId);
        LegacyValidation.requireNotNull(request, "Request body is required");
        LegacyValidation.requireYn(request.activeStatus(), "activeStatus");
        LegacyValidation.requirePhone(request.phone1(), "phone1");
        LegacyValidation.requirePhone(request.phone2(), "phone2");
        LegacyValidation.requireSsn(request.ssn(), "ssn");
        LegacyValidation.requireStateCode(request.stateCode(), "stateCode");
        LegacyValidation.requireFico(request.ficoScore(), "ficoScore");
        LegacyValidation.requirePositiveMoney(request.creditLimit(), "creditLimit");
        LegacyValidation.requirePositiveMoney(request.cashCreditLimit(), "cashCreditLimit");
        LegacyValidation.requireDate(request.expirationDate(), "expirationDate");
        LegacyValidation.requireDate(request.reissueDate(), "reissueDate");
        LegacyValidation.requireDate(request.dateOfBirth(), "dateOfBirth");

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        if (request.version() != null
                && account.getVersion() != null
                && !request.version().equals(account.getVersion())) {
            throw new ConflictException("Record changed by someone else. Please review");
        }

        CardXref xref = cardXrefRepository.findByAccountIdOrderByCardNumberAsc(accountId).stream()
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Account missing card xref"));
        Customer customer = customerRepository.findById(xref.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found for account"));

        account.setActiveStatus(request.activeStatus());
        account.setCreditLimit(request.creditLimit());
        account.setCashCreditLimit(request.cashCreditLimit());
        account.setExpirationDate(request.expirationDate());
        account.setReissueDate(request.reissueDate());
        account.setAddressZip(request.zip());
        accountRepository.save(account);

        customer.setFirstName(request.firstName());
        customer.setMiddleName(request.middleName());
        customer.setLastName(request.lastName());
        customer.setAddressLine1(request.addressLine1());
        customer.setAddressLine2(request.addressLine2());
        customer.setAddressLine3(request.addressLine3());
        customer.setStateCode(request.stateCode());
        customer.setCountryCode(request.countryCode());
        customer.setZip(request.zip());
        customer.setPhone1(request.phone1());
        customer.setPhone2(request.phone2());
        customer.setSsn(request.ssn());
        customer.setDateOfBirth(request.dateOfBirth());
        customer.setFicoScore(request.ficoScore());
        customerRepository.save(customer);

        return new AccountUpdateResponse(
                accountId,
                customer.getCustomerId(),
                account.getVersion(),
                "Account updated");
    }

    private static void validateAccountId(Long accountId) {
        if (accountId == null || accountId <= 0 || accountId > 99_999_999_999L) {
            throw new IllegalArgumentException("Account number must be a non-zero 11 digit number");
        }
    }
}
