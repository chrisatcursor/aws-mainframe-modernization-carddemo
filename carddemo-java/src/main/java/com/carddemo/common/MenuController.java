package com.carddemo.common;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Main menu controller for regular users.
 * Migrated from COMEN01C.cbl -- builds numbered option list from COMEN02Y
 * and routes to the selected program via XCTL.
 */
@Controller
public class MenuController {

    @GetMapping("/menu")
    public String showMenu(Model model, Authentication authentication) {
        List<MenuOption> options = MenuConfig.MAIN_MENU_OPTIONS;
        model.addAttribute("menuOptions", options);
        model.addAttribute("menuTitle", "Main Menu");
        return "menu";
    }

    @PostMapping("/menu")
    public String processMenuSelection(
            @RequestParam(name = "option", required = false) Integer option,
            RedirectAttributes redirectAttributes) {

        List<MenuOption> options = MenuConfig.MAIN_MENU_OPTIONS;

        if (option == null || option < 1 || option > options.size()) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Please enter a valid option number...");
            return "redirect:/menu";
        }

        MenuOption selected = options.get(option - 1);
        return "redirect:" + selected.endpoint();
    }
}
