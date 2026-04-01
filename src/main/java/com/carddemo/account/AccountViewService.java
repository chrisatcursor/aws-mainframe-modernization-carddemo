package com.carddemo.account;

import com.carddemo.account.dto.AccountViewResponse;
import com.carddemo.card.CardXref;
import com.carddemo.card.CardXrefRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class AccountViewService {

    private final CardXrefRepository cardXrefRepository;
    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;

    public AccountViewService(
            CardXrefRepository cardXrefRepository,
            AccountRepository accountRepository,
            CustomerRepository customerRepository) {
        this.cardXrefRepository = cardXrefRepository;
        this.accountRepository = accountRepository;
        this.customerRepository = customerRepository;
    }

    @Transactional(readOnly = true)
    public AccountViewResponse view(String accountIdRaw) {
        String trimmed = accountIdRaw == null ? "" : accountIdRaw.trim();
        if (trimmed.isEmpty() || "*".equals(trimmed)) {
            return AccountViewResponse.error(0, "Account number not provided");
        }
        if (!trimmed.chars().allMatch(Character::isDigit)) {
            return AccountViewResponse.error(0, "Account Filter must be a non-zero 11 digit number");
        }
        if (trimmed.length() != 11) {
            return AccountViewResponse.error(0, "Account Filter must be a non-zero 11 digit number");
        }
        long acctId = Long.parseLong(trimmed);
        if (acctId == 0) {
            return AccountViewResponse.error(0, "Account Filter must be a non-zero 11 digit number");
        }

        Optional<CardXref> xref = cardXrefRepository.findByAccountId(acctId);
        if (xref.isEmpty()) {
            return AccountViewResponse.error(acctId,
                    "Account:" + pad11(acctId) + " not found in Cross ref file.  Resp: 13 Reas: 0");
        }

        Optional<Account> accountOpt = accountRepository.findById(acctId);
        if (accountOpt.isEmpty()) {
            return AccountViewResponse.error(acctId,
                    "Account:" + pad11(acctId) + " not found in Acct Master file.Resp: 13 Reas: 0");
        }
        Account a = accountOpt.get();
        long custId = xref.get().getCustomerId();
        Optional<Customer> custOpt = customerRepository.findById(custId);
        if (custOpt.isEmpty()) {
            return AccountViewResponse.error(acctId,
                    "CustId:" + pad9(custId) + " not found in customer master.Resp: 13 REAS: 0");
        }
        Customer c = custOpt.get();
        String ssn = c.getSsn() == null ? "" : c.getSsn().replaceAll("\\D", "");
        String ssnFmt = ssn.length() == 9 ? ssn.substring(0, 3) + "-" + ssn.substring(3, 5) + "-" + ssn.substring(5, 9) : ssn;

        return new AccountViewResponse(
                acctId,
                null,
                a.getActiveStatus(),
                a.getCurrentBalance(),
                a.getCreditLimit(),
                a.getCashCreditLimit(),
                a.getCurrentCycleCredit(),
                a.getCurrentCycleDebit(),
                a.getOpenDate(),
                a.getExpirationDate(),
                a.getReissueDate(),
                a.getGroupId(),
                c.getId(),
                ssnFmt,
                c.getFicoCreditScore(),
                c.getDobYyyyMmDd(),
                c.getFirstName(),
                c.getMiddleName(),
                c.getLastName(),
                c.getAddrLine1(),
                c.getAddrLine2(),
                c.getAddrLine3(),
                c.getAddrStateCd(),
                c.getAddrZip(),
                c.getAddrCountryCd(),
                c.getPhoneNum1(),
                c.getPhoneNum2(),
                c.getGovtIssuedId(),
                c.getEftAccountId(),
                c.getPriCardHolderInd());
    }

    private static String pad11(long id) {
        return String.format("%011d", id);
    }

    private static String pad9(long id) {
        return String.format("%09d", id);
    }
}
