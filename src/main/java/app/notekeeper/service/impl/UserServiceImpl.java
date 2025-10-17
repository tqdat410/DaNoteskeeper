package app.notekeeper.service.impl;

import app.notekeeper.common.exception.ServiceException;
import app.notekeeper.model.dto.request.UserProfileUpdateRequest;
import app.notekeeper.model.dto.response.JSendResponse;
import app.notekeeper.model.dto.response.UserProfileResponse;
import app.notekeeper.model.entity.User;
import app.notekeeper.repository.UserRepository;
import app.notekeeper.security.SecurityUtils;
import app.notekeeper.service.UserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    public JSendResponse<UserProfileResponse> getUserProfile(UUID userId) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw ServiceException.businessRuleViolation("User not authenticated");
        }
        if (!currentUserId.equals(userId)) {
            throw ServiceException.businessRuleViolation("You are not allowed to view this profile");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> ServiceException.resourceNotFound("User not found"));

        UserProfileResponse response = UserProfileResponse.builder()
                .id(user.getId())
                .displayName(user.getDisplayName())
                .email(user.getEmail())
                .gender(user.getGender())
                .dob(user.getDob())
                .avatarUrl(user.getAvatarUrl())
                .build();

        return JSendResponse.success(response,"View profile successfully");
    }

    @Override
    public JSendResponse<UserProfileResponse> updateUserProfile(UUID userId, UserProfileUpdateRequest request) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw ServiceException.businessRuleViolation("User not authenticated");
        }
        if (!currentUserId.equals(userId)) {
            throw ServiceException.businessRuleViolation("You are not allowed to update this profile");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> ServiceException.resourceNotFound("User not found"));

        if (request.getDisplayName() != null) {
            user.setDisplayName(request.getDisplayName());
        }
        if (request.getGender() != null) {
            user.setGender(request.getGender());
        }
        if (request.getDob() != null) {
            user.setDob(request.getDob());
        }
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }

        userRepository.save(user);

        UserProfileResponse response = UserProfileResponse.builder()
                .id(user.getId())
                .displayName(user.getDisplayName())
                .email(user.getEmail())
                .gender(user.getGender())
                .dob(user.getDob())
                .avatarUrl(user.getAvatarUrl())
                .build();

        return JSendResponse.success(response,"Profile updated successfully");
    }
}
