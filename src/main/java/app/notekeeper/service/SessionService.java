package app.notekeeper.service;

import app.notekeeper.model.dto.request.RefreshTokenRequest;
import app.notekeeper.model.dto.response.AuthTokenResponse;
import app.notekeeper.model.dto.response.JSendResponse;

public interface SessionService {

    JSendResponse<Void> logout(RefreshTokenRequest request);

    JSendResponse<AuthTokenResponse> refreshToken(RefreshTokenRequest request);
}
