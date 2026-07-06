package com.playstop.backend.controller;

import com.playstop.backend.dto.response.UserSearchResponse;
import com.playstop.backend.service.FriendService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/friends")
@RequiredArgsConstructor
public class FriendController {

    private final FriendService friendService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<UserSearchResponse>> getFriends() {
        return ResponseEntity.ok(friendService.getFriends());
    }

    @PostMapping("/request")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> sendRequest(@RequestBody Map<String, String> body) {
        friendService.sendFriendRequest(UUID.fromString(body.get("targetUserId")));
        return ResponseEntity.ok(Map.of("message", "Amigo agregado correctamente"));
    }

    @DeleteMapping("/{friendId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> removeFriend(@PathVariable UUID friendId) {
        friendService.removeFriend(friendId);
        return ResponseEntity.noContent().build();
    }
}
