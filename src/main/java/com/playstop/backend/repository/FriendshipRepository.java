package com.playstop.backend.repository;

import com.playstop.backend.entity.Friendship;
import com.playstop.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FriendshipRepository extends JpaRepository<Friendship, UUID> {
    List<Friendship> findByUserOrderByCreatedAtDesc(User user);
    boolean existsByUserAndFriend(User user, User friend);
    void deleteByUserAndFriend(User user, User friend);
}
