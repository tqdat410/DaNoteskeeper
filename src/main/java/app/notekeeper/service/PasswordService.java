package app.notekeeper.service;

import app.notekeeper.model.dto.request.ChangePasswordRequest;
import app.notekeeper.model.dto.request.ForgotPasswordRequest;
import app.notekeeper.model.dto.request.ResetPasswordRequest;
import app.notekeeper.model.dto.response.JSendResponse;

public interface PasswordService {

    JSendResponse<Void> changePassword(ChangePasswordRequest request);

    JSendResponse<Void> forgotPassword(ForgotPasswordRequest request);

    String resetPassword(String token, ResetPasswordRequest request);
}
