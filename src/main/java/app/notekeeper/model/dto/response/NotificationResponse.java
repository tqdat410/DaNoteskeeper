package app.notekeeper.model.dto.response;

import java.time.ZonedDateTime;
import java.util.UUID;

import app.notekeeper.model.enums.NotiResourceType;
import app.notekeeper.model.enums.NotiType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {

    private UUID id;
    private NotiType type;
    private String message;
    private NotiResourceType resourceType;
    private UUID resourceId;
    private Boolean isRead;
    private ZonedDateTime createdAt;
}
