package capston.capston_spring.controller;

import capston.capston_spring.service.AccuracySessionService;
import capston.capston_spring.service.OpenAiService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class AiFeedbackController {

    private static final Logger log = LoggerFactory.getLogger(AiFeedbackController.class);

    // 세션 기반 GPT 피드백용 서비스 주입
    private final OpenAiService openAiService;
    private final AccuracySessionService accuracySessionService;


    /**
     * 이미지 기반 GPT 피드백 요청 엔드포인트(테스트용)
     *
     * @param userImagePath   사용자 이미지 경로 (Base64로 변환할 파일 경로)
     * @param expertImagePath 전문가 이미지 경로 (Base64로 변환할 파일 경로)
     */
    @GetMapping("/api/image-feedback")
    public Mono<ResponseEntity<String>> imageFeedback(
            @RequestParam String userImagePath,
            @RequestParam String expertImagePath
    ) {
        return openAiService.getDanceImageFeedback(userImagePath, expertImagePath)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> {
                    log.error("GPT 이미지 피드백 처리 중 오류 발생: {}", e.getMessage(), e);
                    return Mono.just(ResponseEntity.internalServerError()
                            .body("An error occurred while processing GPT feedback: " + e.getMessage()));
                });
    }


    /**
     * 세션 기반 GPT 피드백 요청 엔드포인트
     *
     * @param sessionId 세션 ID
     * @return 프레임별 피드백 리스트
     */
    @GetMapping("/api/low-score-feedback")
    public Mono<ResponseEntity<?>> getLowScoreFeedback(@RequestParam Long sessionId) {
        return accuracySessionService.generateLowScoreFeedback(sessionId)
                .map(feedbacks -> {
                    if (feedbacks == null || feedbacks.isEmpty()) {
                        return ResponseEntity.status(404)
                                .body(Map.of("error", "No low-score frames found for session ID: " + sessionId));
                    }
                    return ResponseEntity.ok(feedbacks);
                })
                .onErrorResume(e -> {
                    log.error("세션 기반 GPT 피드백 처리 중 오류 (sessionId={}): {}", sessionId, e.getMessage(), e);
                    return Mono.just(ResponseEntity.internalServerError()
                            .body(Map.of("error", "An error occurred while processing GPT feedback: " + e.getMessage())));
                });
    }
}
