package app.notekeeper.controller;

import app.notekeeper.model.dto.request.UserProfileUpdateRequest;
import app.notekeeper.model.dto.response.JSendResponse;
import app.notekeeper.model.dto.response.UserProfileResponse;
import app.notekeeper.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "User Profile", description = "APIs for viewing and updating user profiles")
public class UserController {

    private final UserService userService;

    /**
     * 🟡 View user profile
     */
    @GetMapping("/profile")
    @Operation(summary = "Get user profile", description = "Retrieve the profile of the logged-in user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Profile retrieved successfully",
                    content = @Content(schema = @Schema(implementation = JSendResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden",
                    content = @Content(schema = @Schema(implementation = JSendResponse.class))),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content(schema = @Schema(implementation = JSendResponse.class)))
    })
    public ResponseEntity<JSendResponse<UserProfileResponse>> getUserProfile() {
        JSendResponse<UserProfileResponse> response = userService.getUserProfile();
        return ResponseEntity.ok(response);
    }

    /**
     * 🟠 Update user profile
     */
    @PutMapping("/profile")
    @Operation(summary = "Update user profile", description = "Update profile of the logged-in user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Profile updated successfully",
                    content = @Content(schema = @Schema(implementation = JSendResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = JSendResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden",
                    content = @Content(schema = @Schema(implementation = JSendResponse.class))),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content(schema = @Schema(implementation = JSendResponse.class)))
    })
    public ResponseEntity<JSendResponse<UserProfileResponse>> updateUserProfile(
            @RequestBody UserProfileUpdateRequest request) {


        JSendResponse<UserProfileResponse> response = userService.updateUserProfile(request);

        return ResponseEntity.ok(response);
    }
}
