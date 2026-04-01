package com.carddemo.account;

import com.carddemo.account.dto.AccountUpdateRequest;
import com.carddemo.account.dto.AccountUpdateResponse;
import com.carddemo.account.dto.AccountViewResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountViewService accountViewService;
    private final AccountUpdateService accountUpdateService;

    public AccountController(AccountViewService accountViewService, AccountUpdateService accountUpdateService) {
        this.accountViewService = accountViewService;
        this.accountUpdateService = accountUpdateService;
    }

    @GetMapping("/view")
    public AccountViewResponse view(@RequestParam(name = "accountId", required = false) String accountId) {
        return accountViewService.view(accountId);
    }

    @PutMapping
    public AccountUpdateResponse update(@Valid @RequestBody AccountUpdateRequest request) {
        return accountUpdateService.update(request);
    }
}
