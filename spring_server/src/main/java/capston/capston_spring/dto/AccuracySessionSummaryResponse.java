package capston.capston_spring.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AccuracySessionSummaryResponse {

    private Long sessionId;
    private Song song;  // 0515
    private double score;
    // private String mode;
    private String timestamp;

    private int perfect;
    private int good;
    private int normal;
    private int bad;
    private int miss;

    private String resultLevel;  // 최종 등급 산출: resultLevel 필드 추가

    @Getter
    @AllArgsConstructor
    public static class Song {
        private Long id;
        private String title;
        private String artist;
        private String coverImagePath;
        private String createdBy;
    }
}
