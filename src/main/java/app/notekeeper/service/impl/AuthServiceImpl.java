package app.notekeeper.service.impl;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.google.api.client.auth.openidconnect.IdToken.Payload;

import app.notekeeper.common.exception.ServiceException;
import app.notekeeper.common.exception.SystemException;
import app.notekeeper.external.oauth.GoogleOAuthService;
import app.notekeeper.external.email.EmailService;
import app.notekeeper.model.dto.request.EmailLoginRequest;
import app.notekeeper.model.dto.request.EmailRegisterRequest;
import app.notekeeper.model.dto.request.GoogleLoginRequest;
import app.notekeeper.model.dto.request.ResendEmailRequest;
import app.notekeeper.model.dto.response.AuthTokenResponse;
import app.notekeeper.model.dto.response.JSendResponse;
import app.notekeeper.model.entity.EmailVerification;
import app.notekeeper.model.entity.RefreshToken;
import app.notekeeper.model.entity.User;
import app.notekeeper.model.enums.OAuthProvider;
import app.notekeeper.repository.EmailVerificationRepository;
import app.notekeeper.repository.RefreshTokenRepository;
import app.notekeeper.repository.UserRepository;
import app.notekeeper.service.AuthService;
import app.notekeeper.security.CustomUserDetails;
import app.notekeeper.security.jwt.JwtProperties;
import app.notekeeper.security.jwt.JwtProvider;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final EmailVerificationRepository emailVerificationRepository;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final JwtProperties jwtProperties;
    private final GoogleOAuthService googleOAuthService;
    private final EmailService emailService;

    @Override
    public JSendResponse<AuthTokenResponse> loginWithEmail(EmailLoginRequest request) {
        try {
            log.info("Email login attempt for: {}", request.getEmail());

            // Authenticate user
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

            // Generate tokens
            String accessToken = jwtProvider.generateAccessToken(userDetails, userDetails.getId());
            String refreshToken = jwtProvider.generateRefreshToken(userDetails, userDetails.getId());

            // Save or update refresh token
            saveRefreshToken(userDetails.getId(), refreshToken);

            AuthTokenResponse response = AuthTokenResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .expiresIn(jwtProperties.getAccessTokenExpiration() / 1000)
                    .email(userDetails.getEmail())
                    .displayName(userDetails.getDisplayName())
                    .role("USER")
                    .build();

            log.info("Email login successful for: {}", request.getEmail());
            return JSendResponse.success(response, "Login successful");

        } catch (AuthenticationException e) {
            log.warn("Email login failed for: {} - Invalid credentials", request.getEmail());
            throw ServiceException.businessRuleViolation("Invalid email or password");
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("Email login failed for: {}", request.getEmail(), e);
            throw SystemException.systemError("Login failed due to system error");
        }
    }

    @Override
    public JSendResponse<AuthTokenResponse> loginWithGoogle(GoogleLoginRequest request) {
        try {
            log.info("Google login attempt");

            // Validate ID token
            if (request.getIdToken() == null || request.getIdToken().isEmpty()) {
                log.warn("Google ID token is null or empty");
                throw ServiceException.businessRuleViolation("Google ID token is required");
            }

            // Verify Google ID token
            Payload payload = googleOAuthService.verifyGoogleIdToken(request.getIdToken());
            if (payload == null) {
                log.warn("Invalid Google ID token");
                throw ServiceException.businessRuleViolation("Invalid Google ID token");
            }

            String email = (String) payload.get("email");
            String name = (String) payload.get("name");
            String picture = (String) payload.get("picture");

            // Validate required fields from Google
            if (email == null || email.trim().isEmpty()) {
                log.warn("Email not found in Google ID token");
                throw ServiceException.businessRuleViolation("Email not found in Google ID token");
            }

            log.info("Google login for email: {}", email);

            // Find or create user
            User user = findOrCreateGoogleUser(email, name, picture);

            CustomUserDetails userDetails = CustomUserDetails.fromUser(user);

            // Generate tokens
            String accessToken = jwtProvider.generateAccessToken(userDetails, user.getId());
            String refreshToken = jwtProvider.generateRefreshToken(userDetails, user.getId());

            // Save or update refresh token
            saveRefreshToken(user.getId(), refreshToken);

            AuthTokenResponse response = AuthTokenResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .expiresIn(jwtProperties.getAccessTokenExpiration() / 1000)
                    .email(user.getEmail())
                    .displayName(user.getDisplayName())
                    .avatarUrl(user.getAvatarUrl())
                    .role("USER")
                    .build();

            log.info("Google login successful for: {}", email);
            return JSendResponse.success(response, "Google login successful");

        } catch (IOException | GeneralSecurityException e) {
            log.error("Google token verification failed", e);
            throw SystemException.externalServiceError("Google authentication service error");
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("Google login failed", e);
            throw SystemException.systemError("Google login failed due to system error");
        }
    }

    private User findOrCreateGoogleUser(String email, String name, String picture) {
        Optional<User> existingUser = userRepository.findByEmail(email);

        if (existingUser.isPresent()) {
            User user = existingUser.get();
            log.info("Existing user found for Google login: {}", email);
            return user;
        } else {
            // Create new user
            User newUser = User.builder()
                    .email(email)
                    .displayName(name != null ? name : email.split("@")[0])
                    .avatarUrl(picture)
                    .provider(OAuthProvider.GOOGLE)
                    .build();

            User savedUser = userRepository.save(newUser);
            log.info("Created new user from Google: {}", email);
            return savedUser;
        }
    }

    private void saveRefreshToken(UUID userId, String tokenValue) {
        try {
            // Find existing refresh token for user
            Optional<RefreshToken> existingToken = refreshTokenRepository.findByUserId(userId);

            if (existingToken.isPresent()) {
                // Update existing token
                RefreshToken token = existingToken.get();
                token.setTokenHash(tokenValue); // In production, should hash this
                token.setExpiresAt(
                        java.time.ZonedDateTime.now().plusSeconds(jwtProperties.getRefreshTokenExpiration() / 1000));
                refreshTokenRepository.save(token);
            } else {
                // Create new refresh token
                RefreshToken refreshToken = RefreshToken.builder()
                        .user(userRepository.findById(userId).orElseThrow())
                        .tokenHash(tokenValue) // In production, should hash this
                        .expiresAt(java.time.ZonedDateTime.now()
                                .plusSeconds(jwtProperties.getRefreshTokenExpiration() / 1000))
                        .build();
                refreshTokenRepository.save(refreshToken);
            }
        } catch (Exception e) {
            log.error("Failed to save refresh token for user: {}", userId, e);
            throw SystemException.systemError("Failed to save refresh token");
        }
    }

    @Override
    public JSendResponse<Void> registerWithEmail(EmailRegisterRequest request) {
        try {
            log.info("Email registration attempt for: {}", request.getEmail());

            // Kiểm tra email đã tồn tại chưa
            if (userRepository.findByEmail(request.getEmail()).isPresent()) {
                log.warn("Registration failed - email already exists: {}", request.getEmail());
                throw ServiceException.businessRuleViolation("Email already registered");
            }

            // Mã hóa password
            String encodedPassword = passwordEncoder.encode(request.getPassword());

            // Tạo verification token
            String verificationToken = UUID.randomUUID().toString();

            // Lưu thông tin đăng ký vào Redis
            EmailVerification emailVerification = EmailVerification.builder()
                    .token(verificationToken)
                    .email(request.getEmail())
                    .password(encodedPassword)
                    .fullName(request.getFullName())
                    .gender(request.getGender())
                    .build();

            emailVerificationRepository.save(emailVerification);

            // Gửi email xác thực
            boolean emailSent = emailService.sendVerificationEmail(
                    request.getEmail(),
                    request.getFullName(),
                    verificationToken);

            if (!emailSent) {
                log.error("Failed to send verification email to: {}", request.getEmail());
                // Xóa dữ liệu đã lưu nếu gửi email thất bại
                emailVerificationRepository.deleteById(verificationToken);
                throw SystemException.externalServiceError("Failed to send verification email");
            }

            log.info("Registration email sent successfully to: {}", request.getEmail());
            return JSendResponse.success(null,
                    "Registration email sent. Please check your email to verify your account.");

        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("Registration failed for: {}", request.getEmail(), e);
            throw SystemException.systemError("Registration failed due to system error");
        }
    }

    @Override
    public JSendResponse<Void> resendVerificationEmail(ResendEmailRequest request) {
        try {
            log.info("Resend verification email request for: {}", request.getEmail());

            // Kiểm tra dữ liệu đăng ký có tồn tại không
            Optional<EmailVerification> existingVerification = emailVerificationRepository
                    .findByEmail(request.getEmail());
            if (existingVerification.isEmpty()) {
                log.warn("No registration data found for: {}", request.getEmail());
                throw ServiceException.businessRuleViolation("No pending registration found for this email");
            }

            EmailVerification emailVerification = existingVerification.get();

            // Kiểm tra cooldown (1 phút) - sử dụng lastResendTime
            long currentTime = System.currentTimeMillis();
            long lastResendTime = emailVerification.getLastResendTime();
            long cooldownPeriod = 60 * 1000; // 1 minute in milliseconds

            if (currentTime - lastResendTime < cooldownPeriod) {
                log.warn("Resend email blocked due to cooldown for: {}", request.getEmail());
                throw ServiceException
                        .businessRuleViolation("Please wait before requesting another verification email");
            }

            // Tạo token mới và cập nhật
            String newVerificationToken = UUID.randomUUID().toString();

            // Xóa verification cũ và tạo mới với lastResendTime mới
            emailVerificationRepository.deleteById(emailVerification.getToken());

            EmailVerification newEmailVerification = EmailVerification.builder()
                    .token(newVerificationToken)
                    .email(emailVerification.getEmail())
                    .password(emailVerification.getPassword())
                    .fullName(emailVerification.getFullName())
                    .gender(emailVerification.getGender())
                    .lastResendTime(currentTime)
                    .build();

            emailVerificationRepository.save(newEmailVerification);

            // Gửi email xác thực
            boolean emailSent = emailService.sendVerificationEmail(
                    request.getEmail(),
                    emailVerification.getFullName(),
                    newVerificationToken);

            if (!emailSent) {
                log.error("Failed to resend verification email to: {}", request.getEmail());
                throw SystemException.externalServiceError("Failed to send verification email");
            }

            log.info("Verification email resent successfully to: {}", request.getEmail());
            return JSendResponse.success(null, "Verification email sent. Please check your email.");

        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("Resend verification email failed for: {}", request.getEmail(), e);
            throw SystemException.systemError("Resend email failed due to system error");
        }
    }

    @Override
    public String verifyEmailRegistration(String token) {
        try {
            log.info("Email verification attempt with token: {}", token);

            // Lấy thông tin xác thực từ token
            Optional<EmailVerification> emailVerificationOpt = emailVerificationRepository.findById(token);
            if (emailVerificationOpt.isEmpty()) {
                log.warn("Invalid or expired verification token: {}", token);
                return "EXPIRED"; // Token không tồn tại hoặc đã hết hạn
            }

            EmailVerification emailVerification = emailVerificationOpt.get();
            String email = emailVerification.getEmail();

            // Kiểm tra email đã tồn tại chưa (double check)
            if (userRepository.findByEmail(email).isPresent()) {
                log.warn("Email already registered during verification: {}", email);
                // Cleanup Redis data
                emailVerificationRepository.deleteById(token);
                return "ALREADY_REGISTERED";
            }

            // Tạo user mới
            User newUser = User.builder()
                    .email(emailVerification.getEmail())
                    .password(emailVerification.getPassword()) // Đã được mã hóa
                    .displayName(emailVerification.getFullName())
                    .gender(null) // Không sử dụng gender nữa
                    .provider(null) // Local registration
                    .build();

            userRepository.save(newUser);

            // Cleanup Redis data
            emailVerificationRepository.deleteById(token);

            log.info("User registration completed successfully for: {}", email);
            return "SUCCESS";

        } catch (Exception e) {
            log.error("Email verification failed for token: {}", token, e);
            return "ERROR";
        }
    }
}
