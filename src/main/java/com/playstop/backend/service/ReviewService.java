package com.playstop.backend.service;

import com.playstop.backend.dto.request.ReviewRequest;
import com.playstop.backend.dto.response.ReviewResponse;
import com.playstop.backend.entity.Court;
import com.playstop.backend.entity.Review;
import com.playstop.backend.entity.User;
import com.playstop.backend.repository.CourtRepository;
import com.playstop.backend.repository.ReviewRepository;
import com.playstop.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final CourtRepository courtRepository;
    private final UserRepository userRepository;

    @Lazy
    @Autowired
    private GamificationService gamificationService;

    public List<ReviewResponse> getReviewsByCourt(UUID courtId) {
        Court court = courtRepository.findById(courtId)
                .orElseThrow(() -> new RuntimeException("Cancha no encontrada"));
        return reviewRepository.findByCourtOrderByCreatedAtDesc(court)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<ReviewResponse> getMyReviews() {
        User user = getCurrentUser();
        return reviewRepository.findByUserOrderByCreatedAtDesc(user)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public ReviewResponse createReview(ReviewRequest request) {
        User user = getCurrentUser();
        Court court = courtRepository.findById(request.courtId())
                .orElseThrow(() -> new RuntimeException("Cancha no encontrada"));

        if (reviewRepository.existsByUserAndCourt(user, court)) {
            throw new RuntimeException("Ya dejaste una reseña para esta cancha");
        }

        String photoUrlsStr = (request.photoUrls() != null && !request.photoUrls().isEmpty())
                ? String.join(",", request.photoUrls())
                : null;

        Review review = Review.builder()
                .user(user)
                .court(court)
                .rating(request.rating())
                .comment(request.comment())
                .photoUrls(photoUrlsStr)
                .build();

        ReviewResponse response = toResponse(reviewRepository.save(review));
        gamificationService.onReviewCreated(user);
        return response;
    }

    public void deleteReview(UUID reviewId) {
        User user = getCurrentUser();
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Reseña no encontrada"));

        boolean isOwner = review.getUser().getId().equals(user.getId());
        boolean isAdmin = user.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isOwner && !isAdmin) {
            throw new RuntimeException("No tienes permiso para eliminar esta reseña");
        }

        reviewRepository.delete(review);
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
    }

    private ReviewResponse toResponse(Review review) {
        String avatar = review.getUser().getProfileImageUrl() != null
                ? review.getUser().getProfileImageUrl()
                : "https://ui-avatars.com/api/?name=" +
                  review.getUser().getName().replace(" ", "+") +
                  "&background=0f172a&color=fff&size=80";
        List<String> photos = (review.getPhotoUrls() != null && !review.getPhotoUrls().isBlank())
                ? Arrays.asList(review.getPhotoUrls().split(","))
                : Collections.emptyList();

        return new ReviewResponse(
                review.getId(),
                review.getUser().getName(),
                avatar,
                review.getRating(),
                review.getComment(),
                photos,
                review.getCreatedAt()
        );
    }
}
