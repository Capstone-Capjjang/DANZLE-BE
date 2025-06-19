package capston.capston_spring.dto;

import capston.capston_spring.entity.AppUser;
import capston.capston_spring.entity.Song;
import capston.capston_spring.entity.AccuracySession;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.time.Duration;


@Getter
@AllArgsConstructor
public class AccuracySessionResponse {
    private Long sessionId;
    private UserInfo user;
    private SongInfo song;
    private Double score;
    private String mode; // 연습모드에서만
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private LocalDateTime startTime;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private LocalDateTime endTime;

    private String duration;
    private LocalDateTime timestamp;

    @Getter
    @AllArgsConstructor
    public static class UserInfo {
        private String username;

        public static UserInfo from(AppUser user) {
            return new UserInfo(user.getUsername());
        }
    }

    @Getter
    @AllArgsConstructor
    public static class SongInfo {
        private Long id;
        private String title;

        public static SongInfo from(Song song) {
            return new SongInfo(song.getId(), song.getTitle());
        }
    }

    public static AccuracySessionResponse fromEntity(AccuracySession session) {
        Song song = session.getSong();
        String mode = session.getMode();

        // duration 계산 (startTime, endTime이 null이 아닌 경우만)
        String formattedDuration = null;
        if (session.getStartTime() != null && session.getEndTime() != null) {
            Duration duration = Duration.between(session.getStartTime(), session.getEndTime());
            formattedDuration = String.format("00:00:%02d", duration.toSeconds());
        }

        return new AccuracySessionResponse(
                session.getId(),
                UserInfo.from(session.getUser()),
                SongInfo.from(song),
                session.getAvg_score(),
                session.getMode(),
                // 정확도 모드일 경우 null로 전달 0515
                mode.equalsIgnoreCase("full") ? null : session.getStartTime(),
                mode.equalsIgnoreCase("full") ? null : session.getEndTime(),
                formattedDuration,
                session.getCreatedAt()
        );
    }
}
