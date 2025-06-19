package capston.capston_spring.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AccuracyFrameEvaluation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "session_id", nullable = false)
    private AccuracySession session;

    private Integer frameIndex;

    private double score;

    @Column(length = 1000)  // 피드백 내용? : 피드백 길이 제한 (예: perfect, good 등)
    private String resultTag;

    @Column(columnDefinition = "TEXT") // GPT Vision 응답 결과
    private String gptFeedback;

}
