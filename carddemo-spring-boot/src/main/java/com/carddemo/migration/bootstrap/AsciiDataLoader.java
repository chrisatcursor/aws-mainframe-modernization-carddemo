package com.carddemo.migration.bootstrap;

import com.carddemo.account.model.Account;
import com.carddemo.account.model.Customer;
import com.carddemo.account.repository.AccountRepository;
import com.carddemo.account.repository.CustomerRepository;
import com.carddemo.card.repository.CardRepository;
import com.carddemo.card.repository.CardXrefRepository;
import com.carddemo.migration.fixedwidth.AccountFixedWidthCodec;
import com.carddemo.migration.fixedwidth.CardFixedWidthCodec;
import com.carddemo.migration.fixedwidth.CardXrefFixedWidthCodec;
import com.carddemo.migration.fixedwidth.CustomerFixedWidthCodec;
import com.carddemo.migration.fixedwidth.DailyTransactionFixedWidthCodec;
import com.carddemo.migration.fixedwidth.DisclosureGroupFixedWidthCodec;
import com.carddemo.migration.fixedwidth.TransactionCategoryBalanceFixedWidthCodec;
import com.carddemo.transaction.repository.DailyTransactionRepository;
import com.carddemo.transaction.repository.DisclosureGroupRepository;
import com.carddemo.transaction.repository.TransactionCategoryBalanceRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

@Component
public class AsciiDataLoader implements CommandLineRunner {

    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;
    private final CardRepository cardRepository;
    private final CardXrefRepository cardXrefRepository;
    private final DailyTransactionRepository dailyTransactionRepository;
    private final TransactionCategoryBalanceRepository transactionCategoryBalanceRepository;
    private final DisclosureGroupRepository disclosureGroupRepository;

    @Value("${carddemo.bootstrap.enabled:true}")
    private boolean bootstrapEnabled;

    public AsciiDataLoader(AccountRepository accountRepository,
                           CustomerRepository customerRepository,
                           CardRepository cardRepository,
                           CardXrefRepository cardXrefRepository,
                           DailyTransactionRepository dailyTransactionRepository,
                           TransactionCategoryBalanceRepository transactionCategoryBalanceRepository,
                           DisclosureGroupRepository disclosureGroupRepository) {
        this.accountRepository = accountRepository;
        this.customerRepository = customerRepository;
        this.cardRepository = cardRepository;
        this.cardXrefRepository = cardXrefRepository;
        this.dailyTransactionRepository = dailyTransactionRepository;
        this.transactionCategoryBalanceRepository = transactionCategoryBalanceRepository;
        this.disclosureGroupRepository = disclosureGroupRepository;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        if (!bootstrapEnabled) {
            return;
        }
        if (accountRepository.count() > 0) {
            return;
        }

        accountRepository.saveAll(load("/workspace/app/data/ASCII/acctdata.txt", AccountFixedWidthCodec::decode));
        customerRepository.saveAll(load("/workspace/app/data/ASCII/custdata.txt", CustomerFixedWidthCodec::decode));
        cardRepository.saveAll(load("/workspace/app/data/ASCII/carddata.txt", CardFixedWidthCodec::decode));
        cardXrefRepository.saveAll(load("/workspace/app/data/ASCII/cardxref.txt", CardXrefFixedWidthCodec::decode));
        transactionCategoryBalanceRepository.saveAll(
                load("/workspace/app/data/ASCII/tcatbal.txt", TransactionCategoryBalanceFixedWidthCodec::decode));
        disclosureGroupRepository.saveAll(load("/workspace/app/data/ASCII/discgrp.txt", DisclosureGroupFixedWidthCodec::decode));
        dailyTransactionRepository.saveAll(load("/workspace/app/data/ASCII/dailytran.txt", DailyTransactionFixedWidthCodec::decode));
    }

    private <T> List<T> load(String absolutePath, Function<String, T> decoder) throws IOException {
        Resource[] resources = new PathMatchingResourcePatternResolver().getResources("file:" + absolutePath);
        if (resources.length == 0 || !resources[0].exists()) {
            return List.of();
        }
        Resource resource = resources[0];
        List<T> records = new ArrayList<>();
        try (Stream<String> lines = Files.lines(resource.getFile().toPath(), StandardCharsets.UTF_8)) {
            lines.filter(line -> !line.isBlank())
                    .map(decoder)
                    .forEach(records::add);
        }
        return records;
    }
}
