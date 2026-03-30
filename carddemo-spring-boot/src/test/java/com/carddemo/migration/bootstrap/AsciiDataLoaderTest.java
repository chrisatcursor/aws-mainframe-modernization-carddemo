package com.carddemo.migration.bootstrap;

import com.carddemo.account.repository.AccountRepository;
import com.carddemo.card.repository.CardRepository;
import com.carddemo.card.repository.CardXrefRepository;
import com.carddemo.transaction.repository.DailyTransactionRepository;
import com.carddemo.transaction.repository.DisclosureGroupRepository;
import com.carddemo.transaction.repository.TransactionCategoryBalanceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = {
        "carddemo.bootstrap.enabled=true",
        "spring.datasource.url=jdbc:h2:mem:carddemo_ascii_loader;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class AsciiDataLoaderTest {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private CardXrefRepository cardXrefRepository;

    @Autowired
    private DailyTransactionRepository dailyTransactionRepository;

    @Autowired
    private TransactionCategoryBalanceRepository transactionCategoryBalanceRepository;

    @Autowired
    private DisclosureGroupRepository disclosureGroupRepository;

    @Test
    void loadsSeedDataFromAsciiFiles() {
        assertThat(accountRepository.count()).isEqualTo(50);
        assertThat(cardRepository.count()).isEqualTo(50);
        assertThat(cardXrefRepository.count()).isEqualTo(50);
        assertThat(transactionCategoryBalanceRepository.count()).isEqualTo(50);
        assertThat(disclosureGroupRepository.count()).isEqualTo(51);
        assertThat(dailyTransactionRepository.count()).isEqualTo(300);
    }
}
