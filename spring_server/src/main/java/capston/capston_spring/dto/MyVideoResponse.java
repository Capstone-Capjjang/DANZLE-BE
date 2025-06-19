package capston.capston_spring.dto;

import capston.capston_spring.entity.VideoMode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class MyVideoResponse {
    private Long sessionId;
    private String songTitle;
    private String artist;
    private VideoMode mode;
    private String songImgPath;
    private String videoPath;
    private String thumbnailUrl;

    public MyVideoResponse(Long sessionId, String songTitle, String artist, VideoMode mode, String songImgPath, String videoPath, String thumbnailUrl) {
        this.sessionId = sessionId;
        this.songTitle = songTitle;
        this.artist = artist;
        this.mode = mode;
        this.songImgPath = songImgPath;
        this.videoPath = videoPath;
        this.thumbnailUrl = thumbnailUrl;
    }
}