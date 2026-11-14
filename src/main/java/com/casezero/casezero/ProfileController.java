package com.casezero.casezero;

import java.security.Principal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.transaction.annotation.Transactional;

@Controller
public class ProfileController {

    private final UserAccountRepository userRepository;
    private final UserFollowRepository followRepository;

    public ProfileController(UserAccountRepository userRepository,
                              UserFollowRepository followRepository) {
        this.userRepository = userRepository;
        this.followRepository = followRepository;
    }

    @GetMapping("/profile")
    public String myProfile(Principal principal) {
        UserAccount member = currentMember(principal);
        return "redirect:/members/" + member.getPublicId();
    }

    @GetMapping("/members/{publicId}")
    public String viewProfile(@PathVariable String publicId, Principal principal, Model model) {
        UserAccount profile = userRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member profile not found."));
        UserAccount viewer = optionalCurrentMember(principal);
        boolean ownProfile = viewer != null && viewer.getId().equals(profile.getId());

        model.addAttribute("profile", profile);
        model.addAttribute("ownProfile", ownProfile);\n        model.addAttribute("viewerAuthenticated", viewer != null);
        model.addAttribute("followerCount", followRepository.countByFollowed_Id(profile.getId()));
        model.addAttribute("followingCount", followRepository.countByFollower_Id(profile.getId()));
        model.addAttribute("isFollowing", viewer != null && !ownProfile
                && followRepository.existsByFollower_IdAndFollowed_Id(viewer.getId(), profile.getId()));
        model.addAttribute("followers", followRepository.findAllByFollowed_IdOrderByCreatedAtDesc(profile.getId())
                .stream().map(UserFollow::getFollower).toList());
        model.addAttribute("following", followRepository.findAllByFollower_IdOrderByCreatedAtDesc(profile.getId())
                .stream().map(UserFollow::getFollowed).toList());
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
