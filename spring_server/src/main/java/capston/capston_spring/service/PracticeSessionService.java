package capston.capston_spring.service;

import capston.capston_spring.dto.PracticeSessionDto;
import capston.capston_spring.entity.AppUser;
import capston.capston_spring.entity.PracticeSession;
import capston.capston_spring.entity.Song;
import capston.capston_spring.exception.SessionNotFoundException;
import capston.capston_spring.repository.PracticeSessionRepository;
import capston.capston_spring.repository.SongRepository;
import capston.capston_spring.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PracticeSessionService {

    private final PracticeSessionRepository practiceSessionRepository;
    private final UserRepository userRepository;
    private final SongRepository songRepository;

    /** username 기반으로 1절/하이라이트 연습 세션 시작 (공통 처리) **/
    public PracticeSession startPracticeSessionByUsername(String username, Long songId, String section) {
        AppUser user = getUserByUsername(username); // 예외 통일
        Song song = getSongById(songId);            // 예외 통일

        int startSec;
        int endSec;

        switch (section.toLowerCase()) {
            case "full" -> {
                startSec = song.getFullStartTime();
                endSec = song.getFullEndTime();
            }
            case "highlight" -> {
                startSec = song.getHighlightStartTime();
                endSec = song.getHighlightEndTime();
            }
            default -> throw new IllegalArgumentException("Invalid section: " + section);
        }

        LocalDateTime startTime = LocalDateTime.of(1970, 1, 1, 0, 0).plusSeconds(startSec);
        LocalDateTime endTime = LocalDateTime.of(1970, 1, 1, 0, 0).plusSeconds(endSec);

        PracticeSession session = new PracticeSession();
        session.setUser(user);
        session.setSong(song);
        session.setStartTime(startTime);  // 0515 수정
        session.setEndTime(endTime);
        session.setMode(section);

        return practiceSessionRepository.save(session);
    }

    /** username 기반 1절 연습 세션 시작 */
    public PracticeSession startFullPracticeSessionByUsername(String username, Long songId) {
        return startPracticeSessionByUsername(username, songId, "full");
    }

    /** username 기반 하이라이트 연습 세션 시작 **/
    public PracticeSession startHighlightPracticeSessionByUsername(String username, Long songId) {
        return startPracticeSessionByUsername(username, songId, "highlight");
    }

    /** DTO 기반 연습 세션 저장 (username은 토큰 기반으로 컨트롤러에서 전달) **/
    public PracticeSession savePracticeSession(PracticeSessionDto dto, String username) {
        PracticeSession session = convertToEntity(dto, username);  // username을 외부에서 전달
        return practiceSessionRepository.save(session);
    }

    /** PracticeSessionDto → Entity 변환 (username 기반) **/
    private PracticeSession convertToEntity(PracticeSessionDto dto, String username) {
        AppUser user = getUserByUsername(username);
        Song song = getSongById(dto.getSongId());

        PracticeSession session = new PracticeSession();
        session.setUser(user);
        session.setSong(song);
        session.setStartTime(dto.getStartTime());
        session.setEndTime(dto.getEndTime());
        session.setMode(dto.getMode());

        return session;
    }

    /** username으로 사용자 조회 **/
    private AppUser getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
    }

    /** songId로 곡 조회 **/
    private Song getSongById(Long songId) {
        return songRepository.findById(songId)
                .orElseThrow(() -> new IllegalArgumentException("Song not found: " + songId));
    }

    /** username 기반 사용자 연습 세션 조회 */
    public List<PracticeSession> getByUsername(String username) {
        AppUser user = getUserByUsername(username);
        return practiceSessionRepository.findByUserId(user.getId());
    }

    /** username 기반 곡 + 사용자 연습 세션 조회 */
    public List<PracticeSession> getBySongAndUsername(Long songId, String username) {
        AppUser user = getUserByUsername(username);
        return practiceSessionRepository.findBySongIdAndUserId(songId, user.getId());
    }


    /** Song 엔티티에서 1절 끝나는 시간 조회 (초 단위) **/
    public int getFullEndTime(Long songId) {
        Song song = getSongById(songId);
        return song.getFullEndTime();
    }

    /** Song 엔티티에서 하이라이트 구간 조회 **/
    public int[] getHighlightRange(Long songId) {
        Song song = getSongById(songId);
        return new int[]{song.getHighlightStartTime(), song.getHighlightEndTime()};
    }

    /** 곡 제목으로 연습용 비디오 경로 조회 (실루엣 + 가이드) **/
    public Map<String, String> getPracticeModeVideoPathsBySongTitle(String songTitle) {
        Song song = songRepository.findByTitleIgnoreCase(songTitle)
                .orElseThrow(() -> new IllegalArgumentException("Song not found: " + songTitle));

        Map<String, String> paths = new HashMap<>();
        paths.put("danceGuideUrl", song.getDanceGuidePath());
        paths.put("silhouetteVideoUrl", song.getSilhouetteVideoPath());
        return paths;
    }

    public Object saveSession(Long sessionId) {
        PracticeSession session = practiceSessionRepository.findById(sessionId)
                .orElseThrow(() -> new SessionNotFoundException("해당 세션이 존재하지 않습니다."));

        return ResponseEntity.ok().build();
    }
}
