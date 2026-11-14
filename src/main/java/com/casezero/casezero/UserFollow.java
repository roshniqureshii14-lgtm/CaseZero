package com.casezero.casezero;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_follows",
       uniqueConstraints = @UniqueConstraint(
               name = "uk_user_follow_pair",
               columnNames = {"follower_id", "followed_id"}))
public class UserFollow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "follower_id", nullable = false)
    private UserAccount follower;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "followed_id", nullable = false)
    private UserAccount followed;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected UserFollow() {
    }

    public UserFollow(UserAccount follower, UserAccount followed) {
        this.follower = follower;
        this.followed = followed;
    }

    @PrePersist
    public void setCreatedAt() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public UserAccount getFollower() { return follower; }
    public UserAccount getFollowed() { return followed; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}