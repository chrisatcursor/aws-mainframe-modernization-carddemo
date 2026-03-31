package com.carddemo.account;

import com.carddemo.card.Card;
import com.carddemo.card.CardRepository;
import com.carddemo.card.CardXref;
import com.carddemo.card.CardXrefRepository;
import com.carddemo.common.DateValidator;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AccountService {

    /** CEEDAYS-style picture for stored {@code YYYY-MM-DD} strings. */
    private static final String DATE_PICTURE = "YYYY-MM-DD";

    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;
    private final CardRepository cardRepository;
    private final CardXrefRepository cardXrefRepository;

    public AccountService(
            AccountRepository accountRepository,
            CustomerRepository customerRepository,
            CardRepository cardRepository,
            CardXrefRepository cardXrefRepository) {
        this.accountRepository = accountRepository;
        this.customerRepository = customerRepository;
        this.cardRepository = cardRepository;
        this.cardXrefRepository = cardXrefRepository;
    }

    public Account findAccount(Long acctId) {
        return accountRepository
                .findById(acctId)
                .orElseThrow(() -> new EntityNotFoundException("Account not found: " + acctId));
    }

    public AccountDetailDto findAccountWithCustomer(Long acctId) {
        Account account = findAccount(acctId);
        Customer customer = findCustomerForAccount(acctId);
        return AccountDetailDto.from(account, customer);
    }

    public Customer findCustomerForAccount(Long acctId) {
        List<CardXref> xrefs = cardXrefRepository.findByAccountId(acctId);
        if (xrefs.isEmpty()) {
            return null;
        }
        Long customerId = xrefs.get(0).getCustomerId();
        return customerRepository.findById(customerId).orElse(null);
    }

    public List<Card> findCardsForAccount(Long acctId) {
        return cardRepository.findByAccountId(acctId);
    }

    @Transactional
    public void updateAccount(Long acctId, AccountUpdateRequest request) {
        if (!acctId.equals(request.getAcctId())) {
            throw new IllegalArgumentException("Account id mismatch");
        }

        Account account =
                accountRepository
                        .findById(acctId)
                        .orElseThrow(() -> new EntityNotFoundException("Account not found: " + acctId));

        Customer customer = findCustomerForAccount(acctId);
        if (customer == null) {
            throw new EntityNotFoundException("Customer not found for account: " + acctId);
        }

        validateOptionalDate(request.getOpenDate(), "Open date");
        validateOptionalDate(request.getExpirationDate(), "Expiration date");
        validateOptionalDate(request.getReissueDate(), "Reissue date");
        validateOptionalDate(request.getDateOfBirth(), "Date of birth");

        account.setVersion(request.getAccountVersion());
        account.setActiveStatus(trimToNull(request.getActiveStatus()));
        account.setCreditLimit(request.getCreditLimit());
        account.setCashCreditLimit(request.getCashCreditLimit());
        account.setOpenDate(trimToNull(request.getOpenDate()));
        account.setExpirationDate(trimToNull(request.getExpirationDate()));
        account.setReissueDate(trimToNull(request.getReissueDate()));
        account.setGroupId(trimToNull(request.getGroupId()));
        account.setAddressZip(trimToNull(request.getAddressZip()));

        customer.setVersion(request.getCustomerVersion());
        customer.setFirstName(trimToNull(request.getFirstName()));
        customer.setLastName(trimToNull(request.getLastName()));
        customer.setAddressLine1(trimToNull(request.getAddressLine1()));
        customer.setAddressLine2(trimToNull(request.getAddressLine2()));
        customer.setAddressLine3(trimToNull(request.getAddressLine3()));
        customer.setAddressStateCode(trimToNull(request.getAddressStateCode()));
        customer.setAddressCountryCode(trimToNull(request.getAddressCountryCode()));
        customer.setAddressZip(trimToNull(request.getCustomerAddressZip()));
        customer.setPhoneNumber1(trimToNull(request.getPhoneNumber1()));
        customer.setPhoneNumber2(trimToNull(request.getPhoneNumber2()));
        customer.setSsn(request.getSsn());
        customer.setGovtIssuedId(trimToNull(request.getGovtIssuedId()));
        customer.setDateOfBirth(trimToNull(request.getDateOfBirth()));
        customer.setEftAccountId(trimToNull(request.getEftAccountId()));
        customer.setFicoCreditScore(request.getFicoCreditScore());

        accountRepository.save(account);
        customerRepository.save(customer);
    }

    private static void validateOptionalDate(String value, String label) {
        if (value == null || value.isBlank()) {
            return;
        }
        DateValidator.ValidationResult result = DateValidator.validate(value.trim(), DATE_PICTURE);
        if (result.severity() != 0) {
            throw new IllegalArgumentException(label + ": " + result.message());
        }
    }

    private static String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
