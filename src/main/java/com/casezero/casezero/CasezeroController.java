
package com.casezero.casezero;

import jakarta.validation.Valid;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class CasezeroController {

    private final UserAccountRepository repository;
    private final PasswordEncoder passwordEncoder;

    public CasezeroController(
            UserAccountRepository repository,
            PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/")
    public String home() {
        return "home";
    }

    @GetMapping("/signup")
    public String signup(Model model) {
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new RegistrationForm());
        }
        return "signup";
    }

    @PostMapping("/signup")
    public String register(
            @Valid @ModelAttribute("form") RegistrationForm form,
            BindingResult result,
            RedirectAttributes redirectAttributes) {

        if (form.getPassword() != null
                && !form.getPassword().equals(form.getConfirmPassword())) {
            result.rejectValue(
                    "confirmPassword",
                    "password.mismatch",
                    "Passwords do not match.");
        }

        if (form.getEmail() != null
                && repository.existsByEmailIgnoreCase(form.getEmail().trim())) {
            result.rejectValue(
                    "email",
                    "email.exists",
                    "An account with this email already exists.");
        }

        if (result.hasErrors()) {
            return "signup";
        }

        UserAccount account = new UserAccount();
        account.setName(form.getName().trim());
        account.setEmail(form.getEmail().trim().toLowerCase());
        account.setPassword(passwordEncoder.encode(form.getPassword()));

        repository.save(account);

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Account created successfully. You can now sign in.");

        return "redirect:/login";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/contact")
    public String contact() {
        return "contact";
    }
}
