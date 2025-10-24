package app.notekeeper.model.dto.request;

import app.notekeeper.model.enums.Gender;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UserProfileUpdateRequest {
    private String displayName;
    private Gender gender;
    private LocalDate dob;
    private String avatarUrl;
}
