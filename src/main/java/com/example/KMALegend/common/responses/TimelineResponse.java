package com.example.KMALegend.common.responses;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimelineResponse {
    @JsonProperty("study_days")
    private String studyDays;
    @JsonProperty("teacher")
    private String teacher;
    @JsonProperty("course_code")
    private String courseCode;
    @JsonProperty("course_name")
    private String courseName;
    @JsonProperty("study_location")
    private String studyLocation;
    @JsonProperty("lessons")
    private String lessons;
}
