package capston.capston_spring.controller;

import capston.capston_spring.dto.SongDto;
import capston.capston_spring.entity.Song;
import capston.capston_spring.service.SongService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/song")
@RequiredArgsConstructor
public class SongController {

    private final SongService songService;

    /** 모든 노래 리스트 조회 **/
    @GetMapping("/all")
    public ResponseEntity<Object> getAllSongs(@RequestParam(required = false) String keyword) {
        try {
            List<Song> songs;
            if (keyword != null && !keyword.isBlank()) {
                songs = songService.searchSongs(keyword, keyword);
            } else {
                songs = songService.getAllSongs();
            }

            List<Map<String, Object>> songDtos = songs.stream()
                    .map(song -> {
                        Map<String, Object> songMap = new HashMap<>();
                        songMap.put("id", song.getId());  // 0414 songId 추가
                        songMap.put("title", song.getTitle());
                        songMap.put("artist", song.getArtist());
                        songMap.put("coverImagePath", song.getCoverImagePath());
                        return songMap;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(songDtos); // 성공적으로 노래 리스트 반환

        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Internal Server Error");
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /** 검색 (제목 또는 가수 또는 둘 다 검색 가능) **/
    @GetMapping("/search")
    public ResponseEntity<Object> searchSongs(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String artist) {

        // 제목과 가수 중 하나라도 포함된 노래 찾기
        List<Map<String, Object>> songs = songService.searchSongs(title, artist).stream()
                .map(song -> {
                    Map<String, Object> songMap = new HashMap<>();
                    songMap.put("id", song.getId()); // 0414 추가
                    songMap.put("title", song.getTitle());
                    songMap.put("artist", song.getArtist());
                    songMap.put("coverImagePath", song.getCoverImagePath()); // 커버 이미지 경로
                    return songMap;
                })
                .collect(Collectors.toList());

        if (songs.isEmpty()) {
            // 404 상태 코드와 함께 JSON 오류 메시지 반환
            Map<String, String> response = new HashMap<>();
            response.put("error", "No search results found");
            return ResponseEntity.status(404).body(response); // Map으로 반환
        }

        return ResponseEntity.ok(songs); // 검색 결과 반환
    }

    /** 곡의 댄스 가이드 영상 및 구간 정보 조회 **/
    @GetMapping("/{songId}/song-info")
    public ResponseEntity<Map<String, Object>> getPracticeInfo(
            @PathVariable Long songId,
            @RequestParam(name = "mode", required = false) String mode,
            @RequestParam(name = "for", required = false) String requestFor // 🎯 정확도 vs 연습 모드 구분
    ) {
        String silhouetteVideoUrl = songService.getSilhouetteVideoPath(songId);
        Optional<Object[]> practiceSections = songService.getPracticeSections(songId);

        Map<String, Object> response = new HashMap<>();
        response.put("silhouetteVideoUrl", silhouetteVideoUrl);

        // 정확도 모드일 경우 시간 정보 포함하지 않음
        if ("accuracy".equalsIgnoreCase(requestFor)) {
            return ResponseEntity.ok(response);
        }
        // 연습 모드일 경우 mode에 따라 startTime, endTime 추가
        if (practiceSections.isPresent() && mode != null) {
            Object[] sections = practiceSections.get();
            switch (mode) {
                case "full" -> {
                    response.put("startTime", sections[0]);
                    response.put("endTime", sections[1]);
                }
                case "highlight" -> {
                    response.put("startTime", sections[2]);
                    response.put("endTime", sections[3]);
                }
                default -> response.put("message", "Invalid mode. Use 'full' or 'highlight'.");
            }
        }
        return ResponseEntity.ok(response);
    }

    /** 노래 저장 (DTO 기반)
     * TODO: ADMIN 권한으로 변경하기
     * **/
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/add-song")
    public ResponseEntity<Song> saveSong(@RequestBody SongDto songDto) {
        return ResponseEntity.ok(songService.saveSong(songDto));
    }
}
