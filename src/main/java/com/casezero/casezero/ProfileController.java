package com.casezero.casezero;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ProfileController {

    private static final Map<String, String> AVATARS = Map.of(
            "case-file", "📁",
            "magnifier", "🔎",
            "owl", "🦉",
            "fox", "🦊",
            "raven", "🐦‍⬛",
            "shield", "🛡️",
            "compass", "🧭",
            "archive", "🗃️"
    );

    private final UserAccountRepository userRepository;
    private final UserFollowRepository followRepository;
    private final PasswordEncoder passwordEncoder;

    public ProfileController(UserAccountRepository userRepository,
                             UserFollowRepository followRepository,
                             PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.followRepository = followRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/profile")
    public String myProfile(Principal principal) {
        UserAccount member = currentMember(principal);
        return "redirect:/members/" + member.getPublicId();
    }

    @GetMapping("/members/{publicId}")
    public String viewProfile(@PathVariable String publicId, Principal principal, Model model) {
        UserAccount profile = findMember(publicId);
        UserAccount viewer = optionalCurrentMember(principal);
        boolean ownProfile = viewer != null && viewer.getId().equals(profile.getId());

        model.addAttribute("profile", profile);
        model.addAttribute("ownProfile", ownProfile);
        model.addAttribute("viewerAuthenticated", viewer != null);
        model.addAttribute("avatarSymbol", AVATARS.getOrDefault(profile.getAvatarKey(), AVATARS.get("case-file")));
        model.addAttribute("followerCount", followRepository.countByFollowed_Id(profile.getId()));
        model.addAttribute("followingCount", followRepository.countByFollower_Id(profile.getId()));
        model.addAttribute("isFollowing", viewer != null && !ownProfile
                && followRepository.existsByFollower_IdAndFollowed_Id(viewer.getId(), profile.getId()));
        model.addAttribute("followers", followRepository.findAllByFollowed_IdOrderByCreatedAtDesc(profile.getId())
                .stream().map(UserFollow::getFollower).toList());
        model.addAttribute("following", followRepository.findAllByFollower_IdOrderByCreatedAtDesc(profile.getId())
                .stream().map(UserFollow::getFollowed).toList());

        if (ownProfile) {
            model.addAttribute("avatarOptions", List.of(
                    Map.of("key", "case-file", "label", "Case File", "symbol", "📁"),
                    Map.of("key", "magnifier", "label", "Investigator", "symbol", "🔎"),
                    Map.of("key", "owl", "label", "Night Owl", "symbol", "🦉"),
                    Map.of("key", "fox", "label", "Fox", "symbol", "🦊"),
                    Map.of("key", "raven", "label", "Raven", "symbol", "🐦‍⬛"),
                    Map.of("key", "shield", "label", "Guardian", "symbol", "🛡️"),
                    Map.of("key", "compass", "label", "Explorer", "symbol", "🧭"),
                    Map.of("key", "archive", "label", "Archivist", "symbol", "🗃️")
            ));
        }
        return "profile";
    }

    @GetMapping("/community")
    public String community(Principal principal, Model model) {
        UserAccount viewer = optionalCurrentMember(principal);
        List<UserAccount> members = userRepository.findAll().stream()
                .filter(member -> viewer == null || !member.getId().equals(viewer.getId()))
                .toList();
        Set<Long> followingIds = viewer == null ? Set.of()
                : followRepository.findAllByFollower_IdOrderByCreatedAtDesc(viewer.getId())
                    .stream().map(follow -> follow.getFollowed().getId()).collect(Collectors.toSet());
        model.addAttribute("members", members);
        model.addAttribute("followingIds", followingIds);
        return "community";
    }

    @PostMapping("/profile/update")
    public String updateProfile(@RequestParam String name,
                                @RequestParam(required = false) String bio,
                                @RequestParam(required = false) String location,
                                Principal principal,
                                RedirectAttributes redirectAttributes) {
        UserAccount member = currentMember(principal);
        String cleanName = name == null ? "" : name.trim();
        String cleanBio = bio == null ? "" : bio.trim();
        String cleanLocation = location == null ? "" : location.trim();

        if (cleanName.isBlank() || cleanName.length() > 80
                || cleanBio.length() > 300 || cleanLocation.length() > 100) {
            redirectAttributes.addFlashAttribute("profileError",
                    "Please enter a display name (1–80 characters), a bio up to 300 characters, and a location up to 100 characters.");
            return "redirect:/profile";
        }

        member.setName(cleanName);
        member.setBio(cleanBio);
        member.setLocation(cleanLocation);
        userRepository.save(member);
        redirectAttributes.addFlashAttribute("profileSuccess", "Your profile has been updated.");
        return "redirect:/profile";
    }

    @PostMapping("/profile/avatar")
    public String updateAvatar(@RequestParam String avatarKey,
                               Principal principal,
                               RedirectAttributes redirectAttributes) {
        UserAccount member = currentMember(principal);
        if (!AVATARS.containsKey(avatarKey)) {
            redirectAttributes.addFlashAttribute("profileError", "Please choose one of the available CASEZERO avatars.");
            return "redirect:/profile";
        }
        member.setAvatarKey(avatarKey);
        userRepository.save(member);
        redirectAttributes.addFlashAttribute("profileSuccess", "Your avatar has been updated.");
        return "redirect:/profile";
    }

    @PostMapping("/profile/password")
    public String changePassword(@RequestParam String currentPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 Principal principal,
                                 RedirectAttributes redirectAttributes) {
        UserAccount member = currentMember(principal);

        if (currentPassword == null || currentPassword.isBlank()
                || !passwordEncoder.matches(currentPassword, member.getPassword())) {
            redirectAttributes.addFlashAttribute("passwordError", "Your current password is incorrect.");
            return "redirect:/profile";
        }

        if (newPassword == null || newPassword.length() < 8 || newPassword.length() > 72
                || !newPassword.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@#$%^&+=!]).*$")) {
            redirectAttributes.addFlashAttribute("passwordError",
                    "New password must be 8–72 characters and include uppercase, lowercase, a number, and a special character.");
            return "redirect:/profile";
        }

        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("passwordError", "The new passwords do not match.");
            return "redirect:/profile";
        }

        if (passwordEncoder.matches(newPassword, member.getPassword())) {
            redirectAttributes.addFlashAttribute("passwordError", "Choose a password different from your current password.");
            return "redirect:/profile";
        }

        member.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(member);
        redirectAttributes.addFlashAttribute("passwordSuccess", "Your password has been changed successfully.");
        return "redirect:/profile";
    }

    @PostMapping("/members/{publicId}/follow")
    @Transactional
    public String follow(@PathVariable String publicId, Principal principal,
                         RedirectAttributes redirectAttributes) {
        UserAccount follower = currentMember(principal);
        UserAccount followed = findMember(publicId);
        if (follower.getId().equals(followed.getId())) {
            redirectAttributes.addFlashAttribute("profileError", "You cannot follow your own profile.");
            return "redirect:/members/" + publicId;
        }
        if (!followRepository.existsByFollower_IdAndFollowed_Id(follower.getId(), followed.getId())) {
            followRepository.save(new UserFollow(follower, followed));
        }
        return "redirect:/members/" + publicId;
    }

    @PostMapping("/members/{publicId}/unfollow")
    @Transactional
    public String unfollow(@PathVariable String publicId, Principal principal) {
        UserAccount follower = currentMember(principal);
        UserAccount followed = findMember(publicId);
        followRepository.findByFollower_IdAndFollowed_Id(follower.getId(), followed.getId())
                .ifPresent(followRepository::delete);
        return "redirect:/members/" + publicId;
    }

    private UserAccount currentMember(Principal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please sign in.");
        }
        return userRepository.findByEmailIgnoreCase(principal.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Account not found."));
    }

    private UserAccount optionalCurrentMember(Principal principal) {
        return principal == null ? null
                : userRepository.findByEmailIgnoreCase(principal.getName()).orElse(null);
    }

    private UserAccount findMember(String publicId) {
        return userRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member profile not found."));
    }
}
