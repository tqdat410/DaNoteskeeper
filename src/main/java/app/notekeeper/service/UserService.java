package app.notekeeper.service;

import app.notekeeper.model.dto.request.UserProfileUpdateRequest;
import app.notekeeper.model.dto.response.JSendResponse;
import app.notekeeper.model.dto.response.UserProfileResponse;

import java.util.UUID;

public interface UserService {

    JSendResponse<UserProfileResponse> getUserProfile(UUID userId);
    JSendResponse<UserProfileResponse> updateUserProfile(UUID userId, UserProfileUpdateRequest request);
}

