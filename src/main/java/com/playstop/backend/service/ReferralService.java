package com.playstop.backend.service;

import com.playstop.backend.entity.Referral;
import com.playstop.backend.entity.User;
import com.playstop.backend.repository.ReferralRepository;
import com.playstop.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@Transactional
@RequiredArgsConstructor
public class ReferralService {

    private final ReferralRepository referralRepository;
    private final UserRepository userRepository;

    public Map<String, Object> getMyReferralInfo() {
        User user = getCurrentUser();
        if (user.getReferralCode() == null) {
            user.setReferralCode(User.generateReferralCode());
            userRepository.save(user);
        }
        long totalReferrals = referralRepository.countByReferrer(user);
        return Map.of(
                "referralCode", user.getReferralCode(),
                "totalReferrals", totalReferrals,
                "creditsEarned", totalReferrals * 10
        );
    }

    public void applyReferralCode(String code) {
        User referred = getCurrentUser();

        if (referralRepository.existsByReferred(referred)) {
            throw new RuntimeException("Ya usaste un código de referido");
        }

        User referrer = userRepository.findByReferralCode(code)
                .orElseThrow(() -> new RuntimeException("Código de referido inválido"));

        if (referrer.getId().equals(referred.getId())) {
            throw new RuntimeException("No puedes usar tu propio código");
        }

        referralRepository.save(Referral.builder()
                .referrer(referrer)
                .referred(referred)
                .build());
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
    }
}
