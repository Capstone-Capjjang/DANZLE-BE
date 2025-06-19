package capston.capston_spring.service;

import capston.capston_spring.dto.MyVideoResponse;
import capston.capston_spring.dto.RecordedVideoDto;
import capston.capston_spring.entity.*;
import capston.capston_spring.exception.SessionNotFoundException;
import capston.capston_spring.exception.UserNotFoundException;
import capston.capston_spring.repository.*;
import lombok.RequiredArgsConstructor;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VideoService {
    private final Logger log = LoggerFactory.getLogger(AccuracySessionService.class);

    private final RecordedVideoRepository recordedVideoRepository;
    private final UserRepository userRepository;
    private final PracticeSessionRepository practiceSessionRepository;
    private final ChallengeSessionRepository challengeSessionRepository;
    private final AccuracySessionRepository accuracySessionRepository; // 추가
    private final S3Client s3Client;

    private static final String BUCKET_NAME = "danzle-s3-bucket";
    private static final String VIDEO_STORAGE_DIR = "user_videos/";
    private static final String USER_FRAMES_DIR = "user_frames/";

    private final Java2DFrameConverter converter = new Java2DFrameConverter();


    /** 사용자명 기반 모든 영상 조회 **/
    public List<MyVideoResponse> getAllUserVideosByUsername(String username) {
        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found with username: " + username)); // 수정됨

        return recordedVideoRepository.findByUserId(user.getId())
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /** 사용자명 + 모드 기반 영상 조회 **/
    public List<MyVideoResponse> getVideosByModeByUsername(String username, VideoMode mode) {
        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found with username: " + username)); // 수정됨

        return recordedVideoRepository.findByUserIdAndMode(user.getId(), mode)
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    // 기존 메소드에서 sessionId를 쿼리 파라미터로 받도록 수정
    /** 특정 세션의 영상 조회 **/
    public List<MyVideoResponse> getVideosBySession(@RequestParam Long sessionId, @RequestParam VideoMode mode) { // 수정된 부분
        List<RecordedVideo> videos = switch (mode) {
            case PRACTICE -> recordedVideoRepository.findByPracticeSessionId(sessionId);
            case CHALLENGE -> recordedVideoRepository.findByChallengeSessionId(sessionId);
            case ACCURACY -> recordedVideoRepository.findByAccuracySessionId(sessionId);
        };
        return videos.stream().map(this::convertToResponse).collect(Collectors.toList());
    }

    /** 특정 영상 조회 **/
    public Optional<MyVideoResponse> getVideoById(Long videoId) {
        return recordedVideoRepository.findById(videoId).map(this::convertToResponse);
    }

    /** 특정 모드의 영상 조회 (username 기반) **/
    public List<MyVideoResponse> getVideosByMode(String username, VideoMode mode) {
        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found with username: " + username));

        return recordedVideoRepository.findByUserIdAndMode(user.getId(), mode)
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /** 특정 연습 모드 녹화 영상 조회 **/
    public List<RecordedVideo> getRecordedVideosByPracticeSession(@RequestParam Long sessionId) { // 수정된 부분
        PracticeSession practiceSession = practiceSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Practice session not found with ID: " + sessionId));
        return recordedVideoRepository.findByPracticeSession(practiceSession);
    }

    /** 특정 챌린지 모드 녹화 영상 조회 **/
    public List<RecordedVideo> getRecordedVideosByChallengeSession(@RequestParam Long sessionId) { // 수정된 부분
        ChallengeSession challengeSession = challengeSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Challenge session not found with ID: " + sessionId));
        return recordedVideoRepository.findByChallengeSession(challengeSession);
    }

    /** 특정 정확도 모드 녹화 영상 조회 **/
    public List<RecordedVideo> getRecordedVideosByAccuracySession(@RequestParam Long sessionId) { // 수정된 부분
        AccuracySession accuracySession = accuracySessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Accuracy session not found with ID: " + sessionId));
        return recordedVideoRepository.findByAccuracySession(accuracySession);
    }


    /** 녹화된 영상 저장하고, RecordedVideo 엔티티로 변환 후 저장 (S3 업로드) **/
    public RecordedVideo saveRecordedVideo(RecordedVideoDto dto, MultipartFile file, String username) {
        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found with username: " + username));

        String fileName = VIDEO_STORAGE_DIR + user.getId() + "_" + dto.getVideoMode() + "_" + dto.getSessionId() + ".mp4";
        if(dto.getVideoMode().toString().equals("ACCURACY")) {
            AccuracySession accuracySession = accuracySessionRepository.findById(dto.getSessionId()).orElseThrow(() -> new SessionNotFoundException("Session not exists"));
            fileName = VIDEO_STORAGE_DIR + user.getId() + "_ACCURACY_" + accuracySession.getMode() + "_" + dto.getSessionId() + ".mp4";
        }
        String videoUrl = uploadToS3(file, fileName);

        RecordedVideo video = convertToEntity(dto, username);
        video.setUser(user);
        video.setVideoPath(videoUrl);

        // 썸네일 저장
        try (InputStream videoStream = file.getInputStream()) {
            List<Integer> frameIdxList = List.of(10);
            Map<Integer, BufferedImage> extraced = extractMultipleFramesFromVideo(videoStream, frameIdxList);

            BufferedImage thumbnail = extraced.get(10);
            if(thumbnail != null) {
                String thumbnailUrl = uploadImageToS3(thumbnail, user.getId(), dto.getSessionId(), 10);
                video.setThumbnailUrl(thumbnailUrl);
            }
        } catch (Exception e) {
            log.warn("썸네일 생성 실패: {}", e.getMessage());
        }


        return recordedVideoRepository.save(video);
    }

    /** 기존 영상 수정 및 파일 재업로드 기능 (S3 파일 덮어쓰기) **/
    public ResponseEntity<String> editVideo(Long videoId, MultipartFile file) {
        return recordedVideoRepository.findById(videoId).map(video -> {
            String fileName = video.getVideoPath().replace("https://" + BUCKET_NAME + ".s3.ap-northeast-2.amazonaws.com/", "");
            String newVideoUrl = uploadToS3(file, fileName);
            video.setVideoPath(newVideoUrl);
            recordedVideoRepository.save(video);
            return ResponseEntity.ok("Video updated successfully");
        }).orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body("Video not found"));
    }

    /** S3 파일 업로드 (handleFileUpload를 대신하는 메소드 -> 비디오 파일 저장 및 덮어쓰기 기능) **/
    private String uploadToS3(MultipartFile file, String fileName) {
        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(BUCKET_NAME)
                    .key(fileName)
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            return "https://" + BUCKET_NAME + ".s3.ap-northeast-2.amazonaws.com/" + fileName;
        } catch (IOException e) {
            throw new RuntimeException("Error occurred during video upload", e); // 메시지 영어로 변경
        }
    }

    /** RecordedVideo 엔티티를 DTO로 변환하는 메서드 **/
    private MyVideoResponse convertToResponse(RecordedVideo video) {
        String title, artist, coverImagePath, thumbnailPath;
        Long sessionId;

        if (video.getMode() == VideoMode.PRACTICE && video.getPracticeSession() != null) {
            title = video.getPracticeSession().getSong().getTitle();
            artist = video.getPracticeSession().getSong().getArtist();
            coverImagePath = video.getPracticeSession().getSong().getCoverImagePath();
            sessionId = video.getPracticeSession().getId();
        } else if (video.getMode() == VideoMode.CHALLENGE && video.getChallengeSession() != null) {
            title = video.getChallengeSession().getSong().getTitle();
            artist = video.getChallengeSession().getSong().getArtist();
            coverImagePath = video.getChallengeSession().getSong().getCoverImagePath();
            sessionId = video.getChallengeSession().getId();
        } else if (video.getMode() == VideoMode.ACCURACY && video.getAccuracySession() != null) {
            title = video.getAccuracySession().getSong().getTitle();
            artist = video.getAccuracySession().getSong().getArtist();
            coverImagePath = video.getAccuracySession().getSong().getCoverImagePath();
            sessionId = video.getAccuracySession().getId();
        } else {
            throw new IllegalStateException("RecordedVideo has an invalid session state.");
        }

        thumbnailPath = video.getThumbnailUrl();

        return new MyVideoResponse(sessionId, title, artist, video.getMode(), coverImagePath, video.getVideoPath(), thumbnailPath);
    }

    /** DTO 데이터를 Entity로 변환하는 메서드 **/
    private RecordedVideo convertToEntity(RecordedVideoDto dto, String username) {
        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found with username: " + username));

        RecordedVideo video = new RecordedVideo();
        video.setUser(user);
        video.setRecordedAt(dto.getRecordedAt());
        video.setDuration(dto.getDuration());
        video.setMode(dto.getVideoMode());

        switch(dto.getVideoMode()){
            case PRACTICE -> {
                PracticeSession practiceSession = practiceSessionRepository.findById(dto.getSessionId())
                        .orElseThrow(() -> new SessionNotFoundException("PracticeSession not found with id: " + dto.getSessionId()));
                video.setPracticeSession(practiceSession);
            }
            case CHALLENGE -> {
                ChallengeSession challengeSession = challengeSessionRepository.findById(dto.getSessionId())
                        .orElseThrow(() -> new RuntimeException("ChallengeSession not found with id: " + dto.getSessionId()));
                video.setChallengeSession(challengeSession);
            }
            case ACCURACY -> {
                AccuracySession accuracySession = accuracySessionRepository.findById(dto.getSessionId())
                        .orElseThrow(() -> new RuntimeException("AccuracySession not found with id: " + dto.getSessionId()));
                video.setAccuracySession(accuracySession);
            }
            default -> throw new IllegalArgumentException("Unsupported VideoMode: " + dto.getVideoMode());
        }

        return video;
    }

    /** 녹화된 사용자 영상에서 프레임 5개 추출 */
    public Map<Integer, BufferedImage> extractMultipleFramesFromVideo(InputStream videoStream, List<Integer> targetIndices) throws Exception {
        Map<Integer, BufferedImage> resultMap = new HashMap<>();
        Set<Integer> targetSet = new HashSet<>(targetIndices);

        if (targetSet.isEmpty()) {
            throw new IllegalArgumentException("추출할 프레임 인덱스가 없습니다.");
        }

        FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(videoStream);
        try {
            grabber.start();

            // 회전 정보 읽기 (없으면 0)
            String rotationStr = grabber.getVideoMetadata("rotate");
            int rotation = rotationStr != null ? Integer.parseInt(rotationStr) : 0;

            int totalFrames = grabber.getLengthInFrames();
            for (Integer idx : targetSet) {
                if (idx >= totalFrames) {
                    throw new IllegalArgumentException("Frame index " + idx + " exceeds total frames: " + totalFrames);
                }
            }

            int currentFrame = 0;
            Frame frame;

            while (currentFrame <= Collections.max(targetSet) && (frame = grabber.grabImage()) != null) {
                if (targetSet.contains(currentFrame)) {
                    BufferedImage img = converter.convert(frame);
                    if (rotation != 0) {
                        img = rotateImage(img, rotation);
                    }
                    resultMap.put(currentFrame, img);
                }
                currentFrame++;
            }

            log.info("총 추출된 프레임 수: {}", resultMap.size());
            return resultMap;

        } finally {
            grabber.stop();
        }
    }

    /** BufferedImage → S3 업로드 후 URL 반환  **/
    public String uploadImageToS3(BufferedImage image, Long userId, Long sessionId, int frameIndex) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", os);
        byte[] imageBytes = os.toByteArray();

        String uploadKey = USER_FRAMES_DIR  + userId + "/" + sessionId + "_" + frameIndex + ".jpg";

        // 3. PutObjectRequest 생성
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(BUCKET_NAME)
                .key(uploadKey)
                .contentType("image/jpg")
                .build();

        // 4. S3 업로드 실행
        s3Client.putObject(putObjectRequest, RequestBody.fromBytes(imageBytes));

        return "https://" + BUCKET_NAME + ".s3.ap-northeast-2.amazonaws.com/" + uploadKey;
    }

    /** 영상에서 여러 프레임을 S3에서 추출 후 이미지 업로드까지 처리  **/
    public Map<Integer, String> extractAndUploadMultipleFrames(String s3VideoPath, List<Integer> frameIndices, Long sessionId, String mode, Long userId) {
        try {
            log.info("S3 영상에서 프레임 추출 시작: video={}, session={}, frames={}", s3VideoPath, sessionId, frameIndices);

            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(BUCKET_NAME)
                    .key(s3VideoPath)
                    .build();

            try (InputStream videoStream = s3Client.getObject(getObjectRequest)) {
                Map<Integer, BufferedImage> extractedFrames = extractMultipleFramesFromVideo(videoStream, frameIndices);
                Map<Integer, String> uploadedUrls = new HashMap<>();

                for (Map.Entry<Integer, BufferedImage> entry : extractedFrames.entrySet()) {
                    String imageUrl = uploadImageToS3(entry.getValue(), userId, sessionId, entry.getKey());
                    uploadedUrls.put(entry.getKey(), imageUrl);
                    log.info("프레임 {} 업로드 완료 → {}", entry.getKey(), imageUrl);
                }
                return uploadedUrls;
            }

        } catch (Exception e) {
            log.error("여러 프레임 추출 실패 (sessionId={}, frameIndexList={}): {}", sessionId, frameIndices, e.getMessage());
            throw new RuntimeException("여러 프레임 추출 및 업로드 실패", e);
        }
    }

    private BufferedImage rotateImage(BufferedImage img, int angle) {
        int w = img.getWidth();
        int h = img.getHeight();

        BufferedImage rotatedImage;
        Graphics2D g;

        switch (angle) {
            case 90:
                rotatedImage = new BufferedImage(h, w, img.getType());
                g = rotatedImage.createGraphics();
                g.translate((h - w) / 2, (h - w) / 2);
                g.rotate(Math.toRadians(angle), h / 2.0, w / 2.0);
                g.drawRenderedImage(img, null);
                g.dispose();
                return rotatedImage;
            case 180:
                rotatedImage = new BufferedImage(w, h, img.getType());
                g = rotatedImage.createGraphics();
                g.rotate(Math.toRadians(angle), w / 2.0, h / 2.0);
                g.drawRenderedImage(img, null);
                g.dispose();
                return rotatedImage;
            case 270:
                rotatedImage = new BufferedImage(h, w, img.getType());
                g = rotatedImage.createGraphics();
                g.translate((h - w) / 2, (h - w) / 2);
                g.rotate(Math.toRadians(angle), h / 2.0, w / 2.0);
                g.drawRenderedImage(img, null);
                g.dispose();
                return rotatedImage;
            default:
                return img; // No rotation
        }
    }

    public int getTotalFrameCount(String s3VideoKey) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(BUCKET_NAME)
                .key(s3VideoKey)
                .build();

        try (InputStream videoStream = s3Client.getObject(getObjectRequest)) {
            FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(videoStream);
            grabber.start();
            int totalFrames = grabber.getLengthInFrames();
            grabber.stop();
            return totalFrames;
        } catch (Exception e) {
            throw new RuntimeException("총 프레임 수 추출 실패", e);
        }
    }




}
