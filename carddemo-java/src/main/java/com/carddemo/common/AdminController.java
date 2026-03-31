package com.carddemo.common;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Admin menu controller for admin users.
 * Migrated from COADM01C.cbl -- builds numbered option list from COADM02Y
 * and routes to the selected admin program via XCTL.
 * PGMIDERR handler maps to "feature not available" message.
 */
@Controller
@RequestMapping("/admin")
public class AdminController {

    @GetMapping("/dashboard")
    public String showDashboard(Model model) {
        List<MenuOption> options = MenuConfig.ADMIN_MENU_OPTIONS;
        model.addAttribute("menuOptions", options);
        model.addAttribute("menuTitle", "Admin Menu");
        return "admin/dashboard";
    }

    @PostMapping("/dashboard")
    public String processAdminSelection(
            @RequestParam(name = "option", required = false) Integer option,
            RedirectAttributes redirectAttributes) {

        List<MenuOption> options = MenuConfig.ADMIN_MENU_OPTIONS;

        if (option == null || option < 1 || option > options.size()) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Please enter a valid option number...");
            return "redirect:/admin/dashboard";
        }

        MenuOption selected = options.get(option - 1);
        return "redirect:" + selected.endpoint();
    }
}
