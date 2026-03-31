package com.carddemo.authorization;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Controller
@RequestMapping("/auth")
public class PendingAuthController {

    private static final Pattern DEMO_USER_ACCOUNT_PATTERN = Pattern.compile("^USER0*(\\d+)$", Pattern.CASE_INSENSITIVE);

    private final PendingAuthorizationService pendingAuthorizationService;

    public PendingAuthController(PendingAuthorizationService pendingAuthorizationService) {
        this.pendingAuthorizationService = pendingAuthorizationService;
    }

    @GetMapping("/pending")
    public String list(Authentication authentication, Model model) {
        List<PendingAuthSummary> all = pendingAuthorizationService.listSummaries();
        if (isAdmin(authentication)) {
            model.addAttribute("summaries", all);
        } else {
            Long acctId = parseDemoAccountFromUsername(authentication != null ? authentication.getName() : null).orElse(null);
            if (acctId == null) {
                model.addAttribute("summaries", List.of());
            } else {
                model.addAttribute("summaries", all.stream().filter(s -> s.getAcctId().equals(acctId)).toList());
            }
        }
        return "auth/pending-list";
    }

    @GetMapping("/pending/{acctId}")
    public String detailList(@PathVariable long acctId, Authentication authentication, Model model) {
        ensureAccountAccess(authentication, acctId);
        PendingAuthSummary summary = pendingAuthorizationService.getSummary(acctId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "No pending authorizations for account"));
        model.addAttribute("summary", summary);
        model.addAttribute("details", pendingAuthorizationService.listDetails(acctId));
        return "auth/pending-detail-list";
    }

    @GetMapping("/pending/detail")
    public String detailForm(
            @RequestParam long acctId,
            @RequestParam int authDate9c,
            @RequestParam int authTime9c,
            @RequestParam String cardNum,
            Authentication authentication,
            Model model) {
        ensureAccountAccess(authentication, acctId);
        PendingAuthDetail d = pendingAuthorizationService.findDetail(acctId, authDate9c, authTime9c, cardNum)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Authorization not found"));
        model.addAttribute("detail", d);
        model.addAttribute("acctId", acctId);
        return "auth/pending-detail";
    }

    @PostMapping("/pending/fraud")
    public String submitFraud(
            @RequestParam long acctId,
            @RequestParam int authDate9c,
            @RequestParam int authTime9c,
            @RequestParam String cardNum,
            @RequestParam String action,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        ensureAccountAccess(authentication, acctId);
        char a = action.length() == 1 ? action.charAt(0) : ' ';
        if (a != 'F' && a != 'R' && a != 'f' && a != 'r') {
            redirectAttributes.addFlashAttribute("errorMessage", "Invalid fraud action");
            return "redirect:/auth/pending/" + acctId;
        }
        try {
            FraudReportingService.FraudUpdateResult r = pendingAuthorizationService.toggleFraud(
                    acctId, authDate9c, authTime9c, cardNum, Character.toUpperCase(a));
            redirectAttributes.addFlashAttribute("successMessage", r.message());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/auth/pending/detail?acctId=" + acctId
                + "&authDate9c=" + authDate9c
                + "&authTime9c=" + authTime9c
                + "&cardNum=" + cardNum;
    }

    private static void ensureAccountAccess(Authentication authentication, long acctId) {
        if (isAdmin(authentication)) {
            return;
        }
        Long allowed = parseDemoAccountFromUsername(authentication != null ? authentication.getName() : null).orElse(null);
        if (allowed == null || !allowed.equals(acctId)) {
            throw new ResponseStatusException(FORBIDDEN, "Not allowed to view this account");
        }
    }

    private static boolean isAdmin(Authentication authentication) {
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }

    private static java.util.Optional<Long> parseDemoAccountFromUsername(String username) {
        if (username == null) {
            return java.util.Optional.empty();
        }
        Matcher m = DEMO_USER_ACCOUNT_PATTERN.matcher(username.trim());
        if (!m.matches()) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(Long.parseLong(m.group(1)));
    }
}
