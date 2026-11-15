package com.casezero.casezero;

import jakarta.persistence.*;
import java.util.Locale;
import java.util.UUID;

@Entity
@Table(name = "app_users")
public class UserAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String name;

    // Username is normalized to lowercase by the registration controller.
    // A database migration adds a case-insensitive unique index.
    @Column(name = "username", length = 24)
    private String username;

    @Column(nullable = false, unique = true, length = 254)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 20)
    private String role = "USER";

    // Public member ID. This is separate from the internal database ID.
    @Column(name = "public_id", unique = true, length = 20)
    private String publicId;

    @Column(length = 300)
    private String bio;

    @Column(length = 100)
    private String location;

    // Key for a built-in CASEZERO avatar; never stores an uploaded image or URL.
    @Column(name = "avatar_key", nullable = false, length = 40)
    private String avatarKey = "case-file";

    public UserAccount() {
    }

    @PrePersist
    public void generatePublicId() {
        if (publicId == null || publicId.isBlank()) {
            publicId = "CZ-" + UUID.randomUUID()
                    .toString()
                    .replace("-", "")
                    .substring(0, 8)
                    .toUpperCase(Locale.ROOT);
        }
        if (avatarKey == null || avatarKey.isBlank()) {
            avatarKey = "case-file";
        }
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public String getPublicId() {
        return publicId;
    }

    public void setPublicId(String publicId) {
        this.publicId = publicId;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getAvatarKey() {
        return avatarKey;
    }

    public void setAvatarKey(String avatarKey) {
        this.avatarKey = avatarKey;
    }
}
