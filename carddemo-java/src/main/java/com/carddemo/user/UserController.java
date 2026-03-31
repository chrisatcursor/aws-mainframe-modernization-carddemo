package com.carddemo.user;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.carddemo.common.PageResponse;
import com.carddemo.common.PaginationConstants;

@Controller
@RequestMapping("/admin/user")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/list")
    public String listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "" + PaginationConstants.DEFAULT_PAGE_SIZE) int size,
            Model model) {

        Pageable pageable = PageRequest.of(page, size);
        model.addAttribute("page", PageResponse.from(userService.findAll(pageable)));
        return "admin/user/list";
    }

    @GetMapping("/add")
    public String showAddForm() {
        return "admin/user/add";
    }

    @PostMapping("/add")
    public String createUser(
            @RequestParam String userId,
            @RequestParam(required = false) String firstName,
            @RequestParam(required = false) String lastName,
            @RequestParam String password,
            @RequestParam String userType,
            RedirectAttributes redirectAttributes) {

        try {
            User created = userService.createUser(userId, firstName, lastName, password, userType);
            redirectAttributes.addFlashAttribute("successMessage", "User created: " + created.getUserId());
            return "redirect:/admin/user/list";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/admin/user/add";
        }
    }

    @GetMapping("/update")
    public String showUpdateForm(
            @RequestParam(required = false) String userId,
            RedirectAttributes redirectAttributes,
            Model model) {

        if (userId == null || userId.isBlank()) {
            redirectAttributes.addFlashAttribute("errorMessage", "User ID is required. Open update from the user list.");
            return "redirect:/admin/user/list";
        }
        model.addAttribute("user", userService.findByUserId(userId.trim().toUpperCase()));
        return "admin/user/edit";
    }

    @PostMapping("/update")
    public String updateUser(
            @RequestParam String userId,
            @RequestParam(required = false) String firstName,
            @RequestParam(required = false) String lastName,
            @RequestParam(required = false) String password,
            @RequestParam String userType,
            RedirectAttributes redirectAttributes) {

        userService.updateUser(userId, firstName, lastName, password, userType);
        redirectAttributes.addFlashAttribute("successMessage", "User updated: " + userId);
        return "redirect:/admin/user/list";
    }

    @GetMapping("/delete")
    public String showDeleteConfirm(
            @RequestParam(required = false) String userId,
            RedirectAttributes redirectAttributes,
            Model model) {

        if (userId == null || userId.isBlank()) {
            redirectAttributes.addFlashAttribute("errorMessage", "User ID is required. Open delete from the user list.");
            return "redirect:/admin/user/list";
        }
        model.addAttribute("user", userService.findByUserId(userId.trim().toUpperCase()));
        return "admin/user/delete";
    }

    @PostMapping("/delete")
    public String deleteUser(
            @RequestParam String userId,
            RedirectAttributes redirectAttributes) {

        userService.deleteUser(userId);
        redirectAttributes.addFlashAttribute("successMessage", "User deleted: " + userId);
        return "redirect:/admin/user/list";
    }
}
