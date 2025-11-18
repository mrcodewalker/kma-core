package com.example.KMALegend.common.responses;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VirtualCalendarResponse {
    @JsonProperty("course")
    private String course;
    @JsonProperty("details")
    private TimelineResponse timelineResponse;
    @JsonProperty("base_time")
    private String baseTime;
    @JsonProperty("course_name")
    private String courseName;
}
