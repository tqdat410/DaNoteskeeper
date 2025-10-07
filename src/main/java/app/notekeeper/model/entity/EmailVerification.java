package app.notekeeper.model.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@RedisHash(value = "email_verification", timeToLive = 300) // 300 seconds = 5 minutes
public class EmailVerification {

    @Id
    private String token;

    private String email;
    private String password; // Encoded password
    private String fullName;
    private String gender;

    @Builder.Default
    private Long lastResendTime = 0L; // Timestamp của lần resend cuối

}