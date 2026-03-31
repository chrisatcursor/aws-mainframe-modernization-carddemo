package com.carddemo.transaction;

import com.carddemo.transaction.TransactionCategory.TransactionCategoryKey;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Comparator;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * COTRTLIC / COTRTUPC — transaction type and category maintenance.
 */
@Controller
@RequestMapping("/admin/trantype")
@PreAuthorize("hasRole('ADMIN')")
public class TransactionTypeAdminController {

    private final TransactionTypeRepository typeRepository;
    private final TransactionCategoryRepository categoryRepository;

    public TransactionTypeAdminController(
            TransactionTypeRepository typeRepository,
            TransactionCategoryRepository categoryRepository) {
        this.typeRepository = typeRepository;
        this.categoryRepository = categoryRepository;
    }

    @GetMapping("/list")
    public String list(Model model) {
        List<TransactionType> types = typeRepository.findAll().stream()
                .sorted(Comparator.comparing(TransactionType::getTypeCode))
                .toList();
        model.addAttribute("types", types);
        model.addAttribute("categories", categoryRepository.findAll().stream()
                .sorted(Comparator.comparing(c -> c.getId().getTypeCode() + c.getId().getCategoryCode()))
                .toList());
        return "admin/trantype-list";
    }

    @GetMapping("/update")
    public String typeForm(@RequestParam(required = false) String typeCode, Model model) {
        TypeForm form = new TypeForm();
        if (typeCode != null && typeRepository.existsById(typeCode)) {
            TransactionType t = typeRepository.findById(typeCode).orElseThrow();
            form.setTypeCode(t.getTypeCode());
            form.setTypeDescription(t.getTypeDescription());
        }
        model.addAttribute("form", form);
        return "admin/trantype-update";
    }

    @PostMapping("/update")
    public String saveType(
            @ModelAttribute("form") TypeForm form,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "admin/trantype-update";
        }
        String code = form.getTypeCode() != null ? form.getTypeCode().trim() : "";
        if (code.length() != 2) {
            redirectAttributes.addFlashAttribute("errorMessage", "Type code must be exactly 2 characters");
            return "redirect:/admin/trantype/update";
        }
        TransactionType t = typeRepository.findById(code).orElseGet(() -> TransactionType.of(code, ""));
        t.setTypeDescription(form.getTypeDescription() != null ? form.getTypeDescription().trim() : "");
        typeRepository.save(t);
        redirectAttributes.addFlashAttribute("successMessage", "Transaction type saved");
        return "redirect:/admin/trantype/list";
    }

    @GetMapping("/category")
    public String categoryForm(
            @RequestParam(required = false) String typeCode,
            @RequestParam(required = false) Integer catCd,
            Model model) {
        List<TransactionType> types = typeRepository.findAll();
        if (types.isEmpty()) {
            throw new ResponseStatusException(NOT_FOUND, "Create a transaction type first");
        }
        String tc = typeCode != null && typeCode.length() == 2 ? typeCode : types.getFirst().getTypeCode();
        CategoryForm form = new CategoryForm();
        form.setTypeCode(tc);
        if (catCd != null) {
            TransactionCategory c = categoryRepository.findById(new TransactionCategoryKey(tc, catCd))
                    .orElseThrow(() -> new ResponseStatusException(NOT_FOUND));
            form.setCatCd(c.getId().getCategoryCode());
            form.setCategoryCode(c.getCategoryCode());
            form.setDescription(c.getDescription());
        }
        model.addAttribute("form", form);
        model.addAttribute("types", types);
        return "admin/trantype-category";
    }

    @PostMapping("/category")
    public String saveCategory(@ModelAttribute CategoryForm form, RedirectAttributes redirectAttributes) {
        String tc = form.getTypeCode() != null ? form.getTypeCode().trim() : "";
        if (tc.length() != 2 || !typeRepository.existsById(tc)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Invalid type code");
            return "redirect:/admin/trantype/list";
        }
        int catCd = form.getCatCd() != null ? form.getCatCd() : nextCatCd(tc);
        String catCodeStr = normalizeCategoryCode(form.getCategoryCode(), catCd);
        TransactionCategory c = categoryRepository.findById(new TransactionCategoryKey(tc, catCd))
                .orElseGet(() -> TransactionCategory.of(tc, catCd, ""));
        c.setCategoryCode(catCodeStr);
        c.setDescription(form.getDescription() != null ? form.getDescription().trim() : "");
        categoryRepository.save(c);
        redirectAttributes.addFlashAttribute("successMessage", "Category saved");
        return "redirect:/admin/trantype/list";
    }

    private int nextCatCd(String typeCode) {
        return categoryRepository.findAll().stream()
                .filter(x -> typeCode.equals(x.getId().getTypeCode()))
                .mapToInt(x -> x.getId().getCategoryCode())
                .max()
                .orElse(0) + 1;
    }

    private static String normalizeCategoryCode(String input, int catCd) {
        if (input != null && !input.isBlank()) {
            String t = input.trim();
            if (t.length() > 4) {
                t = t.substring(0, 4);
            }
            return String.format("%4s", t).replace(' ', '0');
        }
        return String.format("%04d", catCd);
    }

    public static class TypeForm {
        @NotBlank
        @Size(max = 2)
        private String typeCode;
        @Size(max = 50)
        private String typeDescription;

        public String getTypeCode() { return typeCode; }
        public void setTypeCode(String typeCode) { this.typeCode = typeCode; }
        public String getTypeDescription() { return typeDescription; }
        public void setTypeDescription(String typeDescription) { this.typeDescription = typeDescription; }
    }

    public static class CategoryForm {
        private String typeCode;
        private Integer catCd;
        @Size(max = 4)
        private String categoryCode;
        @Size(max = 50)
        private String description;

        public String getTypeCode() { return typeCode; }
        public void setTypeCode(String typeCode) { this.typeCode = typeCode; }
        public Integer getCatCd() { return catCd; }
        public void setCatCd(Integer catCd) { this.catCd = catCd; }
        public String getCategoryCode() { return categoryCode; }
        public void setCategoryCode(String categoryCode) { this.categoryCode = categoryCode; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }
}
