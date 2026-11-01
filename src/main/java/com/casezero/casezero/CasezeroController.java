
package com.casezero.casezero;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

import java.security.Principal;
import java.util.Locale;
import java.util.UUID;

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
    private final ContactMessageRepository contactRepository;
    private final PasswordEncoder passwordEncoder;

    public CasezeroController(
            UserAccountRepository repository,
            ContactMessageRepository contactRepository,
            PasswordEncoder passwordEncoder) {

        this.repository = repository;
        this.contactRepository = contactRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // HOME PAGE: GUEST AND MEMBER DASHBOARD

    @GetMapping("/")
    public String home(
            Model model,
            Principal principal,
            HttpSession session) {

        boolean loggedIn = principal != null;

        model.addAttribute("loggedIn", loggedIn);

        if (loggedIn) {

            UserAccount account = repository
                    .findByEmailIgnoreCase(principal.getName())
                    .orElseThrow(() ->
                            new IllegalStateException(
                                    "Logged-in account was not found."));

            // Assign an ID to accounts created before this feature.
            if (account.getPublicId() == null
                    || account.getPublicId().isBlank()) {

                account.setPublicId(
                        "CZ-" + UUID.randomUUID()
                                .toString()
                                .replace("-", "")
                                .substring(0, 8)
                                .toUpperCase(Locale.ROOT)
                );

                repository.save(account);
            }

            model.addAttribute("profileName", account.getName());
            model.addAttribute("username", account.getEmail());
            model.addAttribute("publicId", account.getPublicId());
            model.addAttribute("profileType", "MEMBER");

        } else {

            // Keep the same guest ID throughout this browser session.
            String guestId = (String) session.getAttribute("guestId");

            if (guestId == null) {

                guestId = "GUEST-" + UUID.randomUUID()
                        .toString()
                        .replace("-", "")
                        .substring(0, 6)
                        .toUpperCase(Locale.ROOT);

                session.setAttribute("guestId", guestId);
            }

            model.addAttribute("profileName", "Guest Member");
            model.addAttribute("publicId", guestId);
            model.addAttribute("profileType", "GUEST");
        }

        return "home";
    }

    // REGISTRATION

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
                && repository.existsByEmailIgnoreCase(
                        form.getEmail().trim())) {

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

        account.setEmail(
                form.getEmail().trim().toLowerCase());

        account.setPassword(
                passwordEncoder.encode(form.getPassword()));

        // Public ID is generated automatically before database insertion.
        repository.save(account);

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Account created successfully. You can now sign in.");

        return "redirect:/login";
    }

    // LOGIN PAGE

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    // CONTACT PAGE

    @GetMapping("/contact")
    public String contact(Model model) {

        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new ContactForm());
        }

        return "contact";
    }

    // CONTACT FORM SUBMISSION

    @PostMapping("/contact")
    public String submitContact(
            @Valid @ModelAttribute("form") ContactForm form,
            BindingResult result,
            RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            return "contact";
        }

        ContactMessage contactMessage = new ContactMessage();

        contactMessage.setName(form.getName().trim());

        contactMessage.setEmail(
                form.getEmail().trim().toLowerCase());

        contactMessage.setSubject(form.getSubject().trim());

        contactMessage.setMessage(form.getMessage().trim());

        contactRepository.save(contactMessage);

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Your message has been sent successfully!");

        return "redirect:/contact";
    }
}
