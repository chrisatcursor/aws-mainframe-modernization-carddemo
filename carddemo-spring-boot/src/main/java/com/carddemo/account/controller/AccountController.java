package com.carddemo.account.controller;

import com.carddemo.account.api.AccountUpdateRequest;
import com.carddemo.account.api.AccountUpdateResponse;
import com.carddemo.account.api.AccountViewResponse;
import com.carddemo.account.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/{accountId}")
    public AccountViewResponse getAccount(@PathVariable Long accountId) {
        return accountService.getAccountView(accountId);
    }

    @PutMapping("/{accountId}")
    public AccountUpdateResponse updateAccount(@PathVariable Long accountId, @Valid @RequestBody AccountUpdateRequest request) {
        return accountService.updateAccount(accountId, request);
    }
}
