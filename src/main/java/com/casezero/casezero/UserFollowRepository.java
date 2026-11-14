package com.casezero.casezero;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserFollowRepository extends JpaRepository<UserFollow, Long> {

    boolean existsByFollower_IdAndFollowed_Id(Long followerId, Long followedId);

    Optional<UserFollow> findByFollower_IdAndFollowed_Id(Long followerId, Long followedId);

    long countByFollowed_Id(Long followedId);

    long countByFollower_Id(Long followerId);

    List<UserFollow> findAllByFollower_IdOrderByCreatedAtDesc(Long followerId);

    List<UserFollow> findAllByFollowed_IdOrderByCreatedAtDesc(Long followedId);
}