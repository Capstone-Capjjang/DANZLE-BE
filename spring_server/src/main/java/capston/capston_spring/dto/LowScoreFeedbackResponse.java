package capston.capston_spring.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class LowScoreFeedbackResponse {
    private int frameIndex;
    private List<String> feedbacks;
    private String userImageUrl;
    private String expertImageUrl;
}
