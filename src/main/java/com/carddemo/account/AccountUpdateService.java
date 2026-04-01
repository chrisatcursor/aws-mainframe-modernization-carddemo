package com.carddemo.account;

import com.carddemo.account.dto.AccountUpdateRequest;
import com.carddemo.account.dto.AccountUpdateResponse;
import com.carddemo.card.CardXref;
import com.carddemo.card.CardXrefRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class AccountUpdateService {

    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;
    private final CardXrefRepository cardXrefRepository;

    public AccountUpdateService(
            AccountRepository accountRepository,
            CustomerRepository customerRepository,
            CardXrefRepository cardXrefRepository) {
        this.accountRepository = accountRepository;
        this.customerRepository = customerRepository;
        this.cardXrefRepository = cardXrefRepository;
    }

    @Transactional
    public AccountUpdateResponse update(AccountUpdateRequest req) {
        Optional<CardXref> xref = cardXrefRepository.findByAccountId(req.accountId());
        if (xref.isEmpty()) {
            return new AccountUpdateResponse(false, "Account not found in card xref", null, null);
        }
        if (!xref.get().getCustomerId().equals(req.customerId())) {
            return new AccountUpdateResponse(false, "Customer id does not match xref for account", null, null);
        }

        Optional<Account> accOpt = accountRepository.findById(req.accountId());
        Optional<Customer> custOpt = customerRepository.findById(req.customerId());
        if (accOpt.isEmpty() || custOpt.isEmpty()) {
            return new AccountUpdateResponse(false, "Account or customer not found", null, null);
        }
        Account a = accOpt.get();
        Customer c = custOpt.get();
        if (!a.getVersion().equals(req.expectedAccountVersion())) {
            return new AccountUpdateResponse(false, "Record was updated by another user; refresh and retry", null, null);
        }
        if (!c.getVersion().equals(req.expectedCustomerVersion())) {
            return new AccountUpdateResponse(false, "Record was updated by another user; refresh and retry", null, null);
        }

        if (req.activeStatus() != null) {
            a.setActiveStatus(req.activeStatus());
        }
        if (req.currentBalance() != null) {
            a.setCurrentBalance(req.currentBalance());
        }
        if (req.creditLimit() != null) {
            a.setCreditLimit(req.creditLimit());
        }
        if (req.cashCreditLimit() != null) {
            a.setCashCreditLimit(req.cashCreditLimit());
        }
        if (req.currentCycleCredit() != null) {
            a.setCurrentCycleCredit(req.currentCycleCredit());
        }
        if (req.currentCycleDebit() != null) {
            a.setCurrentCycleDebit(req.currentCycleDebit());
        }
        if (req.openDate() != null) {
            a.setOpenDate(req.openDate());
        }
        if (req.expirationDate() != null) {
            a.setExpirationDate(req.expirationDate());
        }
        if (req.reissueDate() != null) {
            a.setReissueDate(req.reissueDate());
        }
        if (req.groupId() != null) {
            a.setGroupId(req.groupId());
        }

        if (req.firstName() != null) {
            c.setFirstName(req.firstName());
        }
        if (req.middleName() != null) {
            c.setMiddleName(req.middleName());
        }
        if (req.lastName() != null) {
            c.setLastName(req.lastName());
        }
        if (req.addrLine1() != null) {
            c.setAddrLine1(req.addrLine1());
        }
        if (req.addrLine2() != null) {
            c.setAddrLine2(req.addrLine2());
        }
        if (req.addrLine3() != null) {
            c.setAddrLine3(req.addrLine3());
        }
        if (req.addrStateCd() != null) {
            c.setAddrStateCd(req.addrStateCd());
        }
        if (req.addrCountryCd() != null) {
            c.setAddrCountryCd(req.addrCountryCd());
        }
        if (req.addrZip() != null) {
            c.setAddrZip(req.addrZip());
        }
        if (req.phoneNum1() != null) {
            c.setPhoneNum1(req.phoneNum1());
        }
        if (req.phoneNum2() != null) {
            c.setPhoneNum2(req.phoneNum2());
        }
        if (req.ssn() != null) {
            c.setSsn(req.ssn().replaceAll("\\D", ""));
        }
        if (req.govtIssuedId() != null) {
            c.setGovtIssuedId(req.govtIssuedId());
        }
        if (req.dobYyyyMmDd() != null) {
            c.setDobYyyyMmDd(req.dobYyyyMmDd());
        }
        if (req.eftAccountId() != null) {
            c.setEftAccountId(req.eftAccountId());
        }
        if (req.priCardHolderInd() != null) {
            c.setPriCardHolderInd(req.priCardHolderInd());
        }
        if (req.ficoCreditScore() != null) {
            c.setFicoCreditScore(req.ficoCreditScore());
        }

        accountRepository.save(a);
        customerRepository.save(c);
        return new AccountUpdateResponse(true, "OK", a.getVersion(), c.getVersion());
    }
}
