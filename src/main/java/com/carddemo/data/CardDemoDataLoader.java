package com.carddemo.data;

import com.carddemo.account.Account;
import com.carddemo.account.AccountRepository;
import com.carddemo.account.Customer;
import com.carddemo.account.CustomerRepository;
import com.carddemo.card.Card;
import com.carddemo.card.CardRepository;
import com.carddemo.card.CardXref;
import com.carddemo.card.CardXrefRepository;
import com.carddemo.transaction.DisclosureGroup;
import com.carddemo.transaction.DisclosureGroupRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Component
@Profile("!test")
public class CardDemoDataLoader implements ApplicationRunner {

    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;
    private final CardRepository cardRepository;
    private final CardXrefRepository cardXrefRepository;
    private final DisclosureGroupRepository disclosureGroupRepository;

    public CardDemoDataLoader(
            AccountRepository accountRepository,
            CustomerRepository customerRepository,
            CardRepository cardRepository,
            CardXrefRepository cardXrefRepository,
            DisclosureGroupRepository disclosureGroupRepository) {
        this.accountRepository = accountRepository;
        this.customerRepository = customerRepository;
        this.cardRepository = cardRepository;
        this.cardXrefRepository = cardXrefRepository;
        this.disclosureGroupRepository = disclosureGroupRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (accountRepository.count() > 0) {
            return;
        }

        Customer c = new Customer();
        c.setId(91L);
        c.setFirstName("Jane");
        c.setMiddleName("Q");
        c.setLastName("Cardholder");
        c.setAddrLine1("410 Terry Ave N");
        c.setAddrLine2("");
        c.setAddrLine3("Seattle");
        c.setAddrStateCd("WA");
        c.setAddrCountryCd("USA");
        c.setAddrZip("98109");
        c.setPhoneNum1("(206)555-0100");
        c.setPhoneNum2("");
        c.setSsn("123456789");
        c.setGovtIssuedId("WA-DL-999");
        c.setDobYyyyMmDd("1985-06-15");
        c.setEftAccountId("EFT0000001");
        c.setPriCardHolderInd("Y");
        c.setFicoCreditScore(720);
        customerRepository.save(c);

        Account a = new Account();
        a.setId(1L);
        a.setActiveStatus("Y");
        a.setCurrentBalance(new BigDecimal("1000.00"));
        a.setCreditLimit(new BigDecimal("5000.00"));
        a.setCashCreditLimit(new BigDecimal("1000.00"));
        a.setOpenDate("2020-01-01");
        a.setExpirationDate("2099-12-31");
        a.setReissueDate("2025-01-01");
        a.setCurrentCycleCredit(new BigDecimal("500.00"));
        a.setCurrentCycleDebit(new BigDecimal("200.00"));
        a.setAddrZip("98109");
        a.setGroupId("DEFAULT");
        accountRepository.save(a);

        Card card = new Card();
        card.setCardNumber("4111111111111111");
        card.setAccountId(1L);
        card.setCvvCode(123);
        card.setEmbossedName("JANE Q CARDHOLDER");
        card.setExpirationDate("2028-12-31");
        card.setActiveStatus("Y");
        cardRepository.save(card);

        CardXref xref = new CardXref();
        xref.setCardNumber("4111111111111111");
        xref.setCustomerId(91L);
        xref.setAccountId(1L);
        cardXrefRepository.save(xref);

        DisclosureGroup dg = new DisclosureGroup();
        DisclosureGroup.DisclosureGroupKey key = new DisclosureGroup.DisclosureGroupKey();
        key.setAccountGroupId("DEFAULT");
        key.setTranTypeCode("01");
        key.setTranCategoryCode(5);
        dg.setId(key);
        dg.setInterestRate(new BigDecimal("12.00"));
        disclosureGroupRepository.save(dg);
    }
}
