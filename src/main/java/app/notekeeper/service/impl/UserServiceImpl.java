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

    public JSendResponse<UserProfileResponse> getUserProfile() {
        // Lấy userId từ người dùng hiện tại
        UUID currentUserId = SecurityUtils.getCurrentUserId();

        // Kiểm tra xem người dùng có đăng nhập hay không
        if (currentUserId == null) {
            throw ServiceException.businessRuleViolation("User not authenticated");
        }

        // Truy vấn người dùng từ database
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> ServiceException.resourceNotFound("User not found"));

        // Xây dựng đối tượng phản hồi UserProfileResponse
        UserProfileResponse response = UserProfileResponse.builder()
                .id(user.getId())
                .displayName(user.getDisplayName())
                .email(user.getEmail())
                .gender(user.getGender())
                .dob(user.getDob())
                .avatarUrl(user.getAvatarUrl())
                .build();

        // Trả về kết quả trong dạng JSendResponse
        return JSendResponse.success(response, "View profile successfully");
    }

    @Override
    public JSendResponse<UserProfileResponse> updateUserProfile(UserProfileUpdateRequest request) {
        // Lấy userId của người dùng hiện tại từ hệ thống xác thực
        UUID currentUserId = SecurityUtils.getCurrentUserId();

        // Kiểm tra nếu người dùng chưa đăng nhập
        if (currentUserId == null) {
            throw ServiceException.businessRuleViolation("User not authenticated");
        }

        // Tìm người dùng trong cơ sở dữ liệu
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> ServiceException.resourceNotFound("User not found"));

        // Cập nhật thông tin người dùng từ request
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

        // Lưu người dùng đã được cập nhật
        userRepository.save(user);

        // Tạo và trả về phản hồi với thông tin người dùng mới
        UserProfileResponse response = UserProfileResponse.builder()
                .id(user.getId())
                .displayName(user.getDisplayName())
                .email(user.getEmail())
                .gender(user.getGender())
                .dob(user.getDob())
                .avatarUrl(user.getAvatarUrl())
                .build();

        return JSendResponse.success(response, "Profile updated successfully");
    }
}
