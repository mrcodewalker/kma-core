package com.example.KMALegend.controller;

import com.example.KMALegend.common.responses.FilterRankingResponse;
import com.example.KMALegend.common.responses.RankingResponse;
import com.example.KMALegend.common.responses.StatusResponse;
import com.example.KMALegend.dto.ListScoreDTO;
import com.example.KMALegend.encode.DecryptedRequestWrapper;
import com.example.KMALegend.service.RankingService;
import com.example.KMALegend.service.impl.RankingServiceImpl;
import com.example.KMALegend.service.impl.SemesterRankingServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/ranking")
@RequiredArgsConstructor
@Tag(name = "Ranking", description = "Ranking management APIs")
public class RankingController {
    private final RankingServiceImpl rankingService;
    private final ObjectMapper objectMapper;
    private final SemesterRankingServiceImpl semesterRankingService;

    @Operation(
            summary = "Update GPA for all students",
            description = "Updates the GPA and ranking for all students in the system"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "GPA updated successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/update-gpa")
    public ResponseEntity<String> updateGPA() throws Exception {
        rankingService.updateGPA();
        return ResponseEntity.ok("GPA updated successfully");
    }

    @Operation(
            summary = "Get school ranking",
            description = "Get ranking information for a specific student and top performers"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Found the rankings",
                    content = @Content(schema = @Schema(implementation = RankingResponse.class))
            ),
            @ApiResponse(responseCode = "404", description = "Student not found")
    })
    @GetMapping("/school/{studentCode}")
    public ResponseEntity<List<RankingResponse>> getSchoolRanking(
            @Parameter(description = "Student code to get ranking for")
            @PathVariable String studentCode
    ) {
        return ResponseEntity.ok(rankingService.findSchoolRanking(studentCode));
    }

    @Operation(
            summary = "Get student scores",
            description = "Get detailed score information for a specific student"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Found the student scores",
                    content = @Content(schema = @Schema(implementation = FilterRankingResponse.class))
            ),
            @ApiResponse(responseCode = "404", description = "Student not found")
    })
    @PostMapping("/scores")
    public ResponseEntity<FilterRankingResponse> getStudentScores(
            @Parameter(description = "Student code to get scores for")
            HttpServletRequest request
    ) throws Exception {
        DecryptedRequestWrapper wrapper = new DecryptedRequestWrapper(request, objectMapper);
        Map<String, String> credentials = wrapper.getDecryptedBody(Map.class);
        String studentCode = credentials.get("studentCode");
        FilterRankingResponse response = rankingService.getScoreByStudentCode(studentCode);
        response.setSubjectDTOS(this.semesterRankingService.findSubjects(studentCode));
        return ResponseEntity.ok(response);
    }
    @Operation(summary = "Update scholarship", description = "Updates scholarship information for all students")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Scholarship updated successfully"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/update/scholarship")
    public ResponseEntity<StatusResponse> updateScholarShip() throws Exception {
        this.rankingService.updateScholarShip();
        return ResponseEntity.ok(
                StatusResponse.builder()
                        .status("200")
                        .build());
    }

    @Operation(summary = "Update semester ranking", description = "Updates semester ranking for all students")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Semester ranking updated successfully"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/update/semester_ranking")
    public ResponseEntity<StatusResponse> updateSemesterRanking() throws Exception {
        this.rankingService.updateSemesterRanking();
        return ResponseEntity.ok(
                StatusResponse.builder()
                        .status("200")
                        .build());
    }

    @Operation(summary = "Update semester table", description = "Updates semester table information")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Semester table updated successfully"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/update/semester/table")
    public ResponseEntity<StatusResponse> updateSemesterTable() throws Exception {
        this.rankingService.updateSemesterTable();
        return ResponseEntity.ok(
                StatusResponse.builder()
                        .status("200")
                        .build());
    }

    @Operation(summary = "Get student ranking", description = "Get ranking information for a specific student")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Ranking found",
                content = @Content(schema = @Schema(implementation = RankingResponse.class))),
        @ApiResponse(responseCode = "404", description = "Student not found")
    })
    @GetMapping("/school")
    public ResponseEntity<?> getRanking(
        @Parameter(description = "Student code to get ranking for")
        @RequestParam("student_code") String studentCode
    ) {
        return ResponseEntity.ok(
                this.rankingService.findSchoolRanking(studentCode)
        );
    }

    @Operation(summary = "Get top rankings", description = "Get list of top ranked students")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Top rankings found",
                content = @Content(schema = @Schema(implementation = RankingResponse.class))),
        @ApiResponse(responseCode = "404", description = "No rankings found")
    })
    @GetMapping("/top")
    public ResponseEntity<?> findTopRanking() {
        return ResponseEntity.ok(this.rankingService.findListTopRanking());
    }
}
