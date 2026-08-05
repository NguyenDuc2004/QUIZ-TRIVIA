package com.datn.quizai.user.service;

import com.datn.quizai.user.domain.User;
import com.datn.quizai.user.repository.UserRepository;
import com.datn.quizai.common.exception.BusinessException;
import com.datn.quizai.user.dto.UpdateProfileRequest;
import com.datn.quizai.user.dto.UserResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public UserResponse getProfile(UUID userId) {
        return UserResponse.from(findOrThrow(userId));
    }

    @Transactional
    public UserResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = findOrThrow(userId);
        user.setDisplayName(request.displayName().trim());
        user.setAvatarUrl(request.avatarUrl());
        return UserResponse.from(user);
    }

    private User findOrThrow(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy người dùng"));
    }
}
