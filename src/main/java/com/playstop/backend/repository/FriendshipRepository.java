package com.playstop.backend.repository;

import com.playstop.backend.entity.Friendship;
import com.playstop.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface FriendshipRepository extends JpaRepository<Friendship, UUID> {
    @Query("SELECT f FROM Friendship f JOIN FETCH f.friend WHERE f.user = :user ORDER BY f.createdAt DESC")
    List<Friendship> findByUserOrderByCreatedAtDesc(@Param("user") User user);

    boolean existsByUserAndFriend(User user, User friend);
    void deleteByUserAndFriend(User user, User friend);
}
