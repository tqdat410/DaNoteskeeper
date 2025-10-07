package app.notekeeper.service.impl;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import app.notekeeper.common.exception.ServiceException;
import app.notekeeper.common.exception.SystemException;
import app.notekeeper.model.dto.request.RefreshTokenRequest;
import app.notekeeper.model.dto.response.AuthTokenResponse;
import app.notekeeper.model.dto.response.JSendResponse;
import app.notekeeper.model.entity.RefreshToken;
import app.notekeeper.model.entity.User;
import app.notekeeper.repository.RefreshTokenRepository;
import app.notekeeper.security.CustomUserDetails;
import app.notekeeper.security.jwt.JwtProperties;
import app.notekeeper.security.jwt.JwtProvider;
import app.notekeeper.service.SessionService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
public class SessionServiceImpl implements SessionService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProvider jwtProvider;
    private final JwtProperties jwtProperties;

    @Override
    public JSendResponse<Void> logout(RefreshTokenRequest request) {
        try {
            log.info("Logout attempt");

            // Validate refresh token
            if (!jwtProvider.validateRefreshToken(request.getRefreshToken())) {
                log.warn("Invalid refresh token provided for logout");
                throw ServiceException.businessRuleViolation("Invalid refresh token");
            }

            // Extract user ID from refresh token
            String userIdString = jwtProvider.getSubjectFromRefreshToken(request.getRefreshToken());
            UUID userId = UUID.fromString(userIdString);

            // Find and delete refresh token
            Optional<RefreshToken> refreshTokenOpt = refreshTokenRepository.findByUserId(userId);
            if (refreshTokenOpt.isPresent()) {
                RefreshToken refreshToken = refreshTokenOpt.get();

                // Verify token matches
                if (!refreshToken.getTokenHash().equals(request.getRefreshToken())) {
                    log.warn("Refresh token mismatch for logout");
                    throw ServiceException.businessRuleViolation("Invalid refresh token");
                }

                // Delete refresh token (invalidate session)
                refreshTokenRepository.delete(refreshToken);

                log.info("User logged out successfully: {}", refreshToken.getUser().getEmail());
            } else {
                log.warn("Refresh token not found in database for logout");
                // Don't throw error, just treat as already logged out
            }

            return JSendResponse.success(null, "Logged out successfully");

        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("Logout failed", e);
            throw SystemException.systemError("Logout failed due to system error");
        }
    }

    @Override
    public JSendResponse<AuthTokenResponse> refreshToken(RefreshTokenRequest request) {
        try {
            log.info("Refresh token attempt");

            // Validate refresh token
            if (!jwtProvider.validateRefreshToken(request.getRefreshToken())) {
                log.warn("Invalid refresh token provided");
                throw ServiceException.businessRuleViolation("Invalid or expired refresh token");
            }

            // Extract user ID from refresh token
            String userIdString = jwtProvider.getSubjectFromRefreshToken(request.getRefreshToken());
            UUID userId = UUID.fromString(userIdString);

            // Find refresh token in database
            Optional<RefreshToken> refreshTokenOpt = refreshTokenRepository.findByUserId(userId);
            if (refreshTokenOpt.isEmpty()) {
                log.warn("Refresh token not found in database for user: {}", userId);
                throw ServiceException.businessRuleViolation("Invalid refresh token");
            }

            RefreshToken refreshToken = refreshTokenOpt.get();

            // Check if refresh token matches
            if (!refreshToken.getTokenHash().equals(request.getRefreshToken())) {
                log.warn("Refresh token mismatch for user: {}", userId);
                throw ServiceException.businessRuleViolation("Invalid refresh token");
            }

            // Check if refresh token is expired
            if (refreshToken.getExpiresAt().isBefore(ZonedDateTime.now())) {
                log.warn("Refresh token expired for user: {}", userId);
                refreshTokenRepository.delete(refreshToken);
                throw ServiceException.businessRuleViolation("Refresh token has expired");
            }

            // Get user
            User user = refreshToken.getUser();
            CustomUserDetails userDetails = CustomUserDetails.fromUser(user);

            // Generate new tokens
            String newAccessToken = jwtProvider.generateAccessToken(userDetails, user.getId());
            String newRefreshToken = jwtProvider.generateRefreshToken(userDetails, user.getId());

            // Update refresh token in database (ensure one refresh token per user)
            refreshToken.setTokenHash(newRefreshToken);
            refreshToken
                    .setExpiresAt(ZonedDateTime.now().plusSeconds(jwtProperties.getRefreshTokenExpiration() / 1000));
            refreshTokenRepository.save(refreshToken);

            AuthTokenResponse response = AuthTokenResponse.builder()
                    .accessToken(newAccessToken)
                    .refreshToken(newRefreshToken)
                    .tokenType("Bearer")
                    .expiresIn(jwtProperties.getAccessTokenExpiration() / 1000)
                    .email(user.getEmail())
                    .displayName(user.getDisplayName())
                    .avatarUrl(user.getAvatarUrl())
                    .role("USER")
                    .build();

            log.info("Token refreshed successfully for user: {}", user.getEmail());
            return JSendResponse.success(response, "Token refreshed successfully");

        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("Refresh token failed", e);
            throw SystemException.systemError("Token refresh failed due to system error");
        }
    }
}
