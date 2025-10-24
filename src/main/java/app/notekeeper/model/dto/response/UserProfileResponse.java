package app.notekeeper.model.dto.response;

import app.notekeeper.model.enums.Gender;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Date;
import java.util.UUID;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {
    private UUID id;
    private String displayName;
    private String email;
    private Gender gender;
    private LocalDate dob;
    private String avatarUrl;
}
