package com.casezero.casezero;

import java.security.Principal;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class CurrentMemberModelAdvice {

    private final UserAccountRepository userRepository;

    public CurrentMemberModelAdvice(UserAccountRepository userRepository) {
        this.userRepository = userRepository;
    }

    @ModelAttribute
    public void addCurrentMember(Model model, Principal principal) {
        if (principal == null) {
            model.addAttribute("currentMember", null);
            return;
        }
        model.addAttribute("currentMember",
                userRepository.findByEmailIgnoreCase(principal.getName()).orElse(null));
    }
}