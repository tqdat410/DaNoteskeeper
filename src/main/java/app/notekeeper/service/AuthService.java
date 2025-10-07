package app.notekeeper.service;

import app.notekeeper.model.dto.request.EmailLoginRequest;
import app.notekeeper.model.dto.request.EmailRegisterRequest;
import app.notekeeper.model.dto.request.GoogleLoginRequest;
import app.notekeeper.model.dto.request.ResendEmailRequest;
import app.notekeeper.model.dto.response.AuthTokenResponse;
import app.notekeeper.model.dto.response.JSendResponse;

public interface AuthService {

    JSendResponse<AuthTokenResponse> loginWithEmail(EmailLoginRequest request);

    JSendResponse<AuthTokenResponse> loginWithGoogle(GoogleLoginRequest request);

    JSendResponse<Void> registerWithEmail(EmailRegisterRequest request);

    JSendResponse<Void> resendVerificationEmail(ResendEmailRequest request);

    String verifyEmailRegistration(String token);

}
