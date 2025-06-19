package capston.capston_spring.service;

import capston.capston_spring.dto.AccuracySessionSummaryResponse;
import capston.capston_spring.dto.LowScoreFeedbackResponse;
import capston.capston_spring.entity.AccuracyFrameEvaluation;
import capston.capston_spring.entity.AccuracySession;
import capston.capston_spring.entity.AppUser;
import capston.capston_spring.entity.Song;
import capston.capston_spring.exception.SessionNotFoundException;
import capston.capston_spring.exception.SongNotFoundException;
import capston.capston_spring.repository.AccuracyFrameEvaluationRepository;
import capston.capston_spring.repository.AccuracySessionRepository;
import capston.capston_spring.repository.SongRepository;
import capston.capston_spring.repository.UserRepository;
import capston.capston_spring.utils.MultipartInputStreamFileResource;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.s3.S3Client;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class AccuracySessionService {
    private final Logger log = LoggerFactory.getLogger(AccuracySessionService.class);

    private final AccuracySessionRepository accuracySessionRepository;
    private final AccuracyFrameEvaluationRepository frameEvaluationRepository;
    private final SongRepository songRepository;
    private final UserRepository userRepository;

    private final S3Client s3Client;
    private static final String BUCKET_NAME = "danzle-s3-bucket";
    private static final String EXPERT_FRAMES_DIR = "expert_frames/";
    private static final String VIDEO_STORAGE_DIR = "user_videos/";


    // GPT 호출용 OpenAiService 주입
    private final OpenAiService openAiService;
    private final VideoService videoService;

    @Value("${flask.api.analyze}")
    private String flaskAnalyzeUrl;

    @Value("${flask.api.clean}")
    private String flaskCleanUrl;

    // 저장된 프레임 이미지 경로 주입
    @Value("${storage.frame-base-path}")
    private String frameBasePath;

    /**
     * ID 기반 곡 조회
     **/
    private Song getSongById(Long songId) {
        return songRepository.findById(songId)
                .orElseThrow(() -> new SongNotFoundException("Song not found: " + songId));
    }

    /**
     * username 기반 사용자 조회 (기존 + 유지)
     **/
    private AppUser getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
    }

    /**
     * 특정 사용자(username)의 정확도 세션 조회
     **/
    public List<AccuracySession> getByUsername(String username) {
        AppUser user = getUserByUsername(username);
        return accuracySessionRepository.findByUserId(user.getId());
    }

    /**
     * 특정 사용자(username) + 곡의 정확도 세션 조회
     **/
    public List<AccuracySession> getBySongAndUsername(Long songId, String username) {
        AppUser user = getUserByUsername(username);
        Song song = getSongById(songId);
        return accuracySessionRepository.findByUserIdAndSongId(user.getId(), song.getId());
    }

    /**
     * 특정 세션 ID로 정확도 세션 조회
     **/
    public Optional<AccuracySession> getSessionById(Long sessionId) {
        return accuracySessionRepository.findById(sessionId);
    }

    /**
     * 사용자가 플레이한 게임에 대한 결과(session info) 저장
     **/
    public Object saveSession(Long sessionId) {
        AccuracySession session = accuracySessionRepository.findById(sessionId)
                .orElseThrow(() -> new SessionNotFoundException("해당 세션이 존재하지 않습니다."));


        // 1. 해당 세션의 모든 프레임 평가 가져오기
        List<AccuracyFrameEvaluation> evaluations = frameEvaluationRepository.findBySession(session);

        // 2. 평균 점수 계산
        double avg = evaluations.stream()
                .mapToDouble(AccuracyFrameEvaluation::getScore)
                .average()
                .orElse(0.0);  // 점수가 없을 경우 0.0

        session.setAvg_score(avg);

        accuracySessionRepository.save(session);

        RestTemplate rt = new RestTemplate();
        try {
            log.info("Flask /save 호출 시작 (sessionId={})", sessionId);
            rt.postForEntity(flaskCleanUrl, null, String.class);
            log.info("Flask /save 호출 성공");
        } catch (Exception e) {
            log.error("Flask /save 호출 실패: {}", e.getMessage());
        }

        return ResponseEntity.ok().build();
    }

    /**
     * 정확도 분석 후 결과 저장 (Flask 연동 유지)
     **/
    public AccuracyFrameEvaluation analyzeAndStoreFrameStep(String username, Long songId, Long sessionId, Integer frameIndex, MultipartFile image) throws IOException {
        AppUser user = getUserByUsername(username);
        Song song = getSongById(songId);

        AccuracySession session = accuracySessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("frame", new MultipartInputStreamFileResource(image.getInputStream(), image.getOriginalFilename()));// 0415 "image" → "frame"
        body.add("song_title", song.getTitle());
        body.add("session_id", sessionId);
        body.add("frame_index", frameIndex);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);
        RestTemplate rt = new RestTemplate();
        ResponseEntity<Map> response = rt.postForEntity(flaskAnalyzeUrl, request, Map.class);

        // Flask 응답 전체 로그 출력
        log.info(" Flask 응답 상태: {}", response.getStatusCode());
        log.info(" Flask 응답 헤더: {}", response.getHeaders());
        log.info(" Flask 응답 본문: {}", response.getBody());

        Map responseBody = response.getBody();
        if (!response.getStatusCode().is2xxSuccessful() || responseBody == null ||
                !responseBody.containsKey("score") || !responseBody.containsKey("feedback")) {
            throw new RuntimeException("Flask 응답 오류: 필수 필드 누락 ('score' 또는 'feedback') - 응답 내용: " + responseBody);
        }

        double accuracyScore = ((Number) responseBody.get("score")).doubleValue();
        String resultTag = (String) responseBody.get("feedback");

        AccuracyFrameEvaluation frame = new AccuracyFrameEvaluation();
        frame.setSession(session);
        frame.setFrameIndex(frameIndex);
        frame.setScore(accuracyScore);
        frame.setResultTag(resultTag);

        return frameEvaluationRepository.save(frame);
    }


    /**
     * 곡 제목으로 실루엣 + 가이드 영상 경로 반환
     **/
    public Map<String, String> getVideoPathsBySongTitle(String songTitle) {
        Song song = songRepository.findByTitleIgnoreCase(songTitle)
                .orElseThrow(() -> new SongNotFoundException("Song not found with title: " + songTitle));

        Map<String, String> paths = new HashMap<>();
        paths.put("silhouetteVideoUrl", song.getSilhouetteVideoPath());
        return paths;
    }

    /**
     * 정확도 세션 시작 - mode (full) 에 따라 자동 시간 설정 후 저장
     **/
    public AccuracySession createAccuracySession(String username, Long songId, String mode) {
        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        Song song = songRepository.findById(songId)
                .orElseThrow(() -> new IllegalArgumentException("Song not found: " + songId));

        if (!"full".equalsIgnoreCase(mode) && !"highlight".equalsIgnoreCase(mode)) {
            throw new IllegalArgumentException("Invalid accuracy mode: " + mode);
        }

        LocalDateTime now = LocalDateTime.now();

        AccuracySession session = new AccuracySession();
        session.setUser(user);
        session.setSong(song);
        session.setMode(mode.toLowerCase());
        session.setAvg_score(0.0);
        session.setStartTime(now);
        session.setEndTime(now);

        accuracySessionRepository.save(session);

        return session;
    }


    /** gpt요청 병렬 호출 **/
    public Mono<List<LowScoreFeedbackResponse>> generateLowScoreFeedback(Long sessionId) {
        AccuracySession session = accuracySessionRepository.findById(sessionId)
                .orElseThrow(() -> new SessionNotFoundException("Session not found: " + sessionId));

        String songTitle = session.getSong().getTitle();
        Long userId = session.getUser().getId();
        String mode = session.getMode();

        String userVideoKey = VIDEO_STORAGE_DIR + userId + "_ACCURACY_" + mode + "_" + sessionId + ".mp4";
        int totalFrames = videoService.getTotalFrameCount(userVideoKey);

        List<AccuracyFrameEvaluation> lowScoreFrames =
                frameEvaluationRepository.findTop3BySessionOrderByScoreAsc(session);

        List<AccuracyFrameEvaluation> validFrames = lowScoreFrames.stream()
                .filter(f -> f.getFrameIndex() < totalFrames)
                .collect(Collectors.toList());

        List<Integer> frameIndices = validFrames.stream()
                .map(AccuracyFrameEvaluation::getFrameIndex)
                .collect(Collectors.toList());

        if (frameIndices.isEmpty()) {
            log.warn("세션 {} → 추출할 프레임이 존재하지 않음 (하위 점수 프레임 없음)", sessionId);
            return Mono.just(List.of());  // 빈 피드백 리스트 반환
        }


        Map<Integer, String> userImageMap = videoService.extractAndUploadMultipleFrames(
                userVideoKey, frameIndices, sessionId, mode, userId);

        List<Mono<LowScoreFeedbackResponse>> monoList = lowScoreFrames.stream()
                .map(frame -> {
                    int frameIndex = frame.getFrameIndex();
                    String paddedFrame = String.format("%04d", frameIndex);
                    String expertImagePath = "https://" + BUCKET_NAME + ".s3.ap-northeast-2.amazonaws.com/"
                            + EXPERT_FRAMES_DIR + songTitle + "/" + paddedFrame + ".jpg";
                    String userImagePath = userImageMap.get(frameIndex);

                    if (userImagePath == null) {
                        return Mono.just(new LowScoreFeedbackResponse(
                                frameIndex,
                                List.of("사용자 이미지 업로드 실패로 피드백 생략됨"),
                                null,
                                expertImagePath
                        ));
                    }

                    return openAiService.getDanceImageFeedback(userImagePath, expertImagePath)
                            .map(feedback -> {
                                frame.setGptFeedback(feedback);
                                frameEvaluationRepository.save(frame);

                                AtomicInteger idx = new AtomicInteger(1);
                                List<String> top3 = Arrays.stream(feedback.split("\n"))
                                        .map(String::trim)
                                        .filter(s -> !s.isBlank())
                                        .limit(3)
                                        .map(s -> idx.getAndIncrement() + ". " + s)
                                        .collect(Collectors.toList());

                                return new LowScoreFeedbackResponse(frameIndex, top3, userImagePath, expertImagePath);
                            })
                            .onErrorResume(e -> {
                                log.error("GPT 피드백 생성 실패 (Frame {}): {}", frameIndex, e.getMessage());
                                return Mono.just(new LowScoreFeedbackResponse(
                                        frameIndex,
                                        List.of("GPT feedback failed - " + e.getMessage()),
                                        userImagePath,
                                        expertImagePath
                                ));
                            });
                })
                .collect(Collectors.toList());

        return Mono.zip(monoList, results -> {
            session.setFeedbackCompleted(true);
            accuracySessionRepository.save(session);
            return Arrays.stream(results)
                    .map(r -> (LowScoreFeedbackResponse) r)
                    .collect(Collectors.toList());
        });
    }



    // 0515 정확도 요약 응답 생성 메서드
    public AccuracySessionSummaryResponse getAccuracySummary(Long sessionId) {
        try {
            AccuracySession session = accuracySessionRepository.findById(sessionId)
                    .orElseThrow(() -> new IllegalArgumentException("Accuracy session not found for sessionId: " + sessionId));

            List<AccuracyFrameEvaluation> frames = frameEvaluationRepository.findBySession(session);

            if (frames == null || frames.isEmpty()) {
                throw new IllegalStateException("No frame evaluations found for sessionId: " + sessionId);
            }

            int perfect = 0, good = 0, normal = 0, bad = 0, miss = 0;
            for (AccuracyFrameEvaluation frame : frames) {
                String feedback = frame.getResultTag();
                if ("perfect".equalsIgnoreCase(feedback)) perfect++;
                else if ("good".equalsIgnoreCase(feedback)) good++;
                else if ("normal".equalsIgnoreCase(feedback)) normal++;
                else if ("bad".equalsIgnoreCase(feedback)) bad++;
                else miss++;
            }

            // 수정된 점수 → 등급 변환 로직
            int totalFrames = perfect + good + normal + bad + miss;
            int totalScore = (4 * perfect) + (3 * good) + (2 * normal) + (bad);

            double accuracyPercentage = totalFrames == 0 ? 0.0 : (double) totalScore / (totalFrames * 3) * 100;

            String resultLevel;
            if (accuracyPercentage >= 80) resultLevel = "Perfect";
            else if (accuracyPercentage >= 60) resultLevel = "Good";
            else if (accuracyPercentage >= 50) resultLevel = "Normal";
            else if (accuracyPercentage >= 40) resultLevel = "Bad";
            else resultLevel = "Miss";

            String timestamp = session.getCreatedAt() != null
                    ? session.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                    : "Unknown";

            AccuracySessionSummaryResponse.Song songInfo =
                    new AccuracySessionSummaryResponse.Song(session.getSong().getId(), session.getSong().getTitle(),
                            session.getSong().getArtist(), session.getSong().getCoverImagePath(), session.getSong().getCreatedBy());

            session.setResultLevel(resultLevel);
            accuracySessionRepository.save(session);

            // resultLevel만 포함되도록 응답 생성
            return new AccuracySessionSummaryResponse(
                    session.getId(),
                    songInfo,
                    session.getAvg_score(),
                    timestamp,
                    perfect,
                    good,
                    normal,
                    bad,
                    miss,
                    resultLevel
            );

        } catch (IllegalArgumentException e) {
            log.warn("[Summary Error] Invalid sessionId provided: {}", sessionId, e);
            throw new IllegalArgumentException("Invalid sessionId provided: " + sessionId + ". " + e.getMessage());
        } catch (IllegalStateException e) {
            log.warn("[Summary Error] Evaluation data missing for sessionId={}. Suggest user check their recording steps.", sessionId);
            throw new IllegalStateException("No evaluation data found. The session may not have processed any frames. Please ensure that analysis has been performed.");
        } catch (Exception e) {
            log.error("[Summary Error] Unexpected error occurred during summary calculation for sessionId={}: {}", sessionId, e.getMessage(), e);
            throw new RuntimeException("An unexpected error occurred during summary calculation. Contact admin if issue persists.");
        }
    }
}
