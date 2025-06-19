package capston.capston_spring.service;

import capston.capston_spring.dto.ChatCompletionResponse;
import capston.capston_spring.exception.OpenAiApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
public class OpenAiService {

    private static final Logger log = LoggerFactory.getLogger(OpenAiService.class);
    private final WebClient openAiWebClient;

    private final S3Client s3Client;
    private static final String BUCKET_NAME = "danzle-s3-bucket";

    public OpenAiService(WebClient openAiWebClient, S3Client s3Client) {
        this.openAiWebClient = openAiWebClient;
        this.s3Client = s3Client;
    }

    //이미지 기반 GPT 피드백 메서드 (GPT-4o Vision) 0513
    public Mono<String> getDanceImageFeedback(String userImagePath, String expertImagePath) {
        try {
            String userImageBase64 = encodeImageFromS3(userImagePath);
            String expertImageBase64 = encodeImageFromS3(expertImagePath);

            List<Map<String, Object>> messages = List.of(
                    Map.of(
                            "role", "system",
                            "content", "당신은 춤 연습을 돕는 긍정적이고 안전한 피드백 전문가입니다. 자세, 위치, 방향에 대해서만 피드백하며 외모나 민감한 내용은 절대 언급하지 마세요."
                    ),
                    Map.of(
                            "role", "user",
                            "content", List.of(
                                    Map.of(
                                            "type", "text",
                                            "text", String.join("\n",
                                                    "첫 번째 이미지는 사용자의 동작이고, 두 번째 이미지는 전문가의 시범입니다.",
                                                    "두 이미지를 비교해 팔, 다리, 상체 방향의 차이를 3줄 이내로 간단히 설명해주세요.",
                                                    "다음 사항을 꼭 지켜주세요:",
                                                    "- 인삿말이나 마무리 멘트를 쓰지 마세요.",
                                                    "- 이모지, 마크다운(**, ## 등) 형식을 쓰지 마세요.",
                                                    "- 자연스럽고 간결한 문장으로 피드백만 작성해주세요."
                                            )
                                    ),
                                    Map.of(
                                            "type", "image_url",
                                            "image_url", Map.of("url", "data:image/png;base64," + userImageBase64)
                                    ),
                                    Map.of(
                                            "type", "image_url",
                                            "image_url", Map.of("url", "data:image/png;base64," + expertImageBase64)
                                    )
                            )
                    )
            );

            Map<String, Object> requestBody = Map.of(
                    "model", "gpt-4o",
                    "messages", messages,
                    "temperature", 0.0
            );

            return openAiWebClient.post()
                    .uri("/chat/completions")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(ChatCompletionResponse.class)
                    .map(response -> {
                        if (response.getChoices() == null || response.getChoices().isEmpty()) {
                            throw new OpenAiApiException("OpenAI 응답이 비어 있습니다.", 502);
                        }
                        return response.getChoices().get(0).getMessage().getContent();
                    })
                    .onErrorResume(WebClientResponseException.class, ex -> {
                        String errorBody = ex.getResponseBodyAsString();
                        log.error("OpenAI API 오류: {} - {}", ex.getStatusCode(), errorBody);

                        String errorMessage = String.format(
                                "OpenAI API 호출 실패: [%d] %s",
                                ex.getRawStatusCode(),
                                errorBody != null ? errorBody : "응답 본문 없음"
                        );
                        return Mono.error(new OpenAiApiException(errorMessage, ex.getRawStatusCode()));
                    })
                    .onErrorResume(Exception.class, ex -> {
                        log.error("OpenAI API 호출 중 일반 오류", ex);
                        return Mono.error(new OpenAiApiException("OpenAI API 호출 중 알 수 없는 오류가 발생했습니다.", 500));
                    });

        } catch (IOException e) {
            return Mono.error(new RuntimeException("이미지 인코딩 실패: " + e.getMessage()));
        }
    }

    // Base64 인코딩 유틸 (local version)
    private String encodeImage(String imagePath) throws IOException {
        Path path = Paths.get(imagePath);
        byte[] imageBytes = Files.readAllBytes(path);
        return Base64.getEncoder().encodeToString(imageBytes);
    }

    // Base64 인코딩 유틸 (s3 version)
    private String encodeImageFromS3(String s3Url) throws IOException {
        String key = s3Url.replace("https://" + BUCKET_NAME + ".s3.ap-northeast-2.amazonaws.com/", "");

        try (InputStream inputStream = s3Client.getObject(GetObjectRequest.builder()
                .bucket(BUCKET_NAME)
                .key(key)
                .build())) {
            byte[] bytes = inputStream.readAllBytes();
            return Base64.getEncoder().encodeToString(bytes);
        }
    }
}
