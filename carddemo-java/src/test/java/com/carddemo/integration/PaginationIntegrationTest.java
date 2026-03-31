package com.carddemo.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.carddemo.card.Card;
import com.carddemo.card.CardRepository;
import com.carddemo.card.CardSearchCriteria;
import com.carddemo.card.CardService;
import com.carddemo.common.PaginationConstants;
import com.carddemo.transaction.Transaction;
import com.carddemo.transaction.TransactionRepository;
import com.carddemo.transaction.TransactionService;
import com.carddemo.transaction.TransactionTestFactory;
import com.carddemo.user.User;
import com.carddemo.user.UserRepository;
import com.carddemo.user.UserService;
import com.carddemo.user.UserTestFactory;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PaginationIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private CardService cardService;

    @Autowired
    private CardRepository cardRepository;

    @BeforeEach
    void clearUsers() {
        userRepository.deleteAll();
    }

    @Test
    void userList_fifteenUsers_pageSizeTen_firstPageTenSecondPageFive() {
        for (int i = 0; i < 15; i++) {
            userRepository.save(newUser("U" + String.format("%02d", i), "First" + i, "Last" + i));
        }

        var p0 = userService.findAll(PageRequest.of(0, PaginationConstants.DEFAULT_PAGE_SIZE));
        var p1 = userService.findAll(PageRequest.of(1, PaginationConstants.DEFAULT_PAGE_SIZE));

        assertThat(p0.getTotalElements()).isEqualTo(15);
        assertThat(p0.getContent()).hasSize(10);
        assertThat(p1.getContent()).hasSize(5);
    }

    @Test
    void transactionList_startIdFilter_respectsPaging() {
        transactionRepository.deleteAll();
        cardRepository.deleteAll();

        String cardNum = "5666666666666666";
        persistCard(cardNum, 74001L);

        for (int i = 1; i <= 10; i++) {
            String id = String.format("%016d", i);
            Transaction t = TransactionTestFactory.minimal(id, cardNum);
            t.setAmount(new BigDecimal(i + ".00"));
            transactionRepository.save(t);
        }

        var page0 =
                transactionService.findByTransactionIdGreaterThanEqual(
                        "0000000000000005", PageRequest.of(0, 3));
        var page1 =
                transactionService.findByTransactionIdGreaterThanEqual(
                        "0000000000000005", PageRequest.of(1, 3));

        assertThat(page0.getTotalElements()).isEqualTo(6);
        assertThat(page0.getContent()).hasSize(3);
        assertThat(page0.getContent().get(0).transactionId()).isEqualTo("0000000000000005");
        assertThat(page0.getContent().get(2).transactionId()).isEqualTo("0000000000000007");

        assertThat(page1.getContent()).hasSize(3);
        assertThat(page1.getContent().get(0).transactionId()).isEqualTo("0000000000000008");
        assertThat(page1.getContent().get(2).transactionId()).isEqualTo("0000000000000010");
    }

    @Test
    void cardList_accountSpecification_filtersAndPages() {
        cardRepository.deleteAll();

        for (int i = 0; i < 3; i++) {
            persistCard("577777777777777" + i, 75001L);
        }
        for (int i = 0; i < 2; i++) {
            persistCard("588888888888888" + i, 75002L);
        }

        CardSearchCriteria criteria = new CardSearchCriteria();
        criteria.setAccountId(75001L);

        var page0 = cardService.listCards(criteria, PageRequest.of(0, 2));
        var page1 = cardService.listCards(criteria, PageRequest.of(1, 2));

        assertThat(page0.getTotalElements()).isEqualTo(3);
        assertThat(page0.getContent()).hasSize(2);
        assertThat(page0.getContent())
                .allMatch(dto -> dto.accountId() != null && dto.accountId().equals(75001L));

        assertThat(page1.getContent()).hasSize(1);
        assertThat(page1.getContent().get(0).accountId()).isEqualTo(75001L);
    }

    private void persistCard(String cardNum, Long acctId) {
        Card c = Card.forImport();
        c.setCardNumber(cardNum);
        c.setAccountId(acctId);
        c.setActiveStatus("Y");
        c.setExpirationDate("2030-01-01");
        cardRepository.save(c);
    }

    private User newUser(String userId, String first, String last) {
        User u = UserTestFactory.newUser();
        u.setUserId(userId);
        u.setFirstName(first);
        u.setLastName(last);
        u.setPassword(passwordEncoder.encode("secret"));
        u.setUserType("U");
        return u;
    }
}
