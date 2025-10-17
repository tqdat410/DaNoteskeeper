package app.notekeeper.model.dto.request;

import app.notekeeper.model.enums.Gender;
import lombok.Data;

import java.util.Date;

@Data
public class UserProfileUpdateRequest {
    private String displayName;
    private Gender gender;
    private Date dob;
    private String avatarUrl;
}
