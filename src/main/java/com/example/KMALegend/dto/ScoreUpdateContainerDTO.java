package com.example.KMALegend.dto;

import com.example.KMALegend.entity.Score;
import lombok.*;
import org.springframework.stereotype.Component;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Component
@Data
@Builder
public class ScoreUpdateContainerDTO {
    private Score score;
    private boolean isUpdate;
}
