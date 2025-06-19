package capston.capston_spring.dto;

import capston.capston_spring.entity.AccuracySession;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@AllArgsConstructor
public class UserResponseDto {
    private Long userId;
    private String name;
    private String email;
    private String profileImageUrl;
    private List<MyVideoResponse> practiceVideos;
    private List<MyVideoResponse> challengeVideos;
    private List<AccuracySessionResponse> accuracyHistory; // 변경: AccuracySession → AccuracySessionResponse로 변경

    public static UserResponseDto fromEntity(capston.capston_spring.entity.AppUser user,
                                             List<MyVideoResponse> practiceVideos,
                                             List<MyVideoResponse> challengeVideos,
                                             List<AccuracySession> accuracySessions) {
        return new UserResponseDto(
                user.getId(), // username 사용
                user.getUsername(),
                user.getEmail(),
                user.getProfileImageUrl(),
                practiceVideos,
                challengeVideos,
                accuracySessions.stream().map(AccuracySessionResponse::fromEntity).collect(Collectors.toList()) // 변환
        );
    }
}