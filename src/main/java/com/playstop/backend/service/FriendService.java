package com.playstop.backend.service;

import com.playstop.backend.exception.BusinessException;

import com.playstop.backend.dto.response.UserSearchResponse;
import com.playstop.backend.entity.Friendship;
import com.playstop.backend.entity.User;
import com.playstop.backend.repository.FriendshipRepository;
import com.playstop.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class FriendService {

    private final FriendshipRepository friendshipRepository;
    private final UserRepository userRepository;

    public Optional<UserSearchResponse> searchByEmail(String email) {
        User currentUser = getCurrentUser();
        return userRepository.findByEmail(email.trim().toLowerCase())
                .filter(u -> !u.getId().equals(currentUser.getId()))
                .map(this::toSearchResponse);
    }

    public List<UserSearchResponse> getFriends() {
        User currentUser = getCurrentUser();
        return friendshipRepository.findByUserOrderByCreatedAtDesc(currentUser).stream()
                .map(f -> toSearchResponse(f.getFriend()))
                .toList();
    }

    public void sendFriendRequest(UUID targetUserId) {
        User currentUser = getCurrentUser();

        if (currentUser.getId().equals(targetUserId)) {
            throw new BusinessException("No puedes agregarte a ti mismo como amigo");
        }

        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new BusinessException("Usuario no encontrado"));

        if (friendshipRepository.existsByUserAndFriend(currentUser, target)) {
            return;
        }

        friendshipRepository.save(Friendship.builder().user(currentUser).friend(target).build());
        friendshipRepository.save(Friendship.builder().user(target).friend(currentUser).build());
    }

    public void removeFriend(UUID friendId) {
        User currentUser = getCurrentUser();
        User friend = userRepository.findById(friendId)
                .orElseThrow(() -> new BusinessException("Usuario no encontrado"));
        friendshipRepository.deleteByUserAndFriend(currentUser, friend);
        friendshipRepository.deleteByUserAndFriend(friend, currentUser);
    }

    private UserSearchResponse toSearchResponse(User user) {
        return new UserSearchResponse(user.getId(), user.getName(), user.getEmail(), user.getProfileImageUrl());
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("Usuario no encontrado"));
    }
}
