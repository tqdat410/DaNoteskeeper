package app.notekeeper.model.entity;

import java.util.Date;
import java.util.UUID;

import app.notekeeper.model.enums.Gender;
import app.notekeeper.model.enums.OAuthProvider;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User extends BaseEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "email", nullable = false, length = 100, unique = true)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = true, length = 50)
    private OAuthProvider provider;

    @Column(name = "password_hash", nullable = true)
    private String password;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", nullable = true, length = 20)
    private Gender gender;

    @Temporal(TemporalType.DATE)
    @Column(name = "dob", nullable = true)
    private Date dob;

    @Column(name = "avatar_url", nullable = true)
    private String avatarUrl;

}
