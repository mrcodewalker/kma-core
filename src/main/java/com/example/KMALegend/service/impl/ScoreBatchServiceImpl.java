package com.example.KMALegend.service.impl;

import com.example.KMALegend.dto.ScoreBatchRequestDTO;
import com.example.KMALegend.entity.ScoreBatch;
import com.example.KMALegend.entity.ScoreItem;
import com.example.KMALegend.repository.ScoreBatchRepository;
import com.example.KMALegend.repository.ScoreItemRepository;
import com.example.KMALegend.service.ScoreBatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ScoreBatchServiceImpl implements ScoreBatchService {

    private final ScoreBatchRepository scoreBatchRepository;
    private final ScoreItemRepository scoreItemRepository;

    @Override
    @Transactional
    public ScoreBatch createOrUpdateScoreBatch(ScoreBatchRequestDTO requestDTO) {
        String studentCode = requestDTO.getStudentInfo().getStudentCode();
        
        // Tìm kiếm ScoreBatch hiện tại theo studentCode
        Optional<ScoreBatch> existingBatch = scoreBatchRepository.findByStudentCode(studentCode);

        ScoreBatch scoreBatch;
        if (existingBatch.isPresent()) {
            // Cập nhật ScoreBatch hiện tại
            scoreBatch = existingBatch.get();
            scoreBatch.setStudentName(requestDTO.getStudentInfo().getStudentName());
            scoreBatch.setStudentClass(requestDTO.getStudentInfo().getStudentClass());
            scoreBatch.setLastUpdated(requestDTO.getLastUpdated() != null ? 
                    requestDTO.getLastUpdated() : LocalDateTime.now());
            
            // Lưu ScoreBatch
            scoreBatch = scoreBatchRepository.save(scoreBatch);
            
            // Xóa tất cả ScoreItem cũ (nhanh hơn với batch delete)
            scoreItemRepository.deleteByScoreBatch_BatchId(scoreBatch.getBatchId());
        } else {
            // Tạo ScoreBatch mới
            scoreBatch = ScoreBatch.builder()
                    .studentCode(studentCode)
                    .studentName(requestDTO.getStudentInfo().getStudentName())
                    .studentClass(requestDTO.getStudentInfo().getStudentClass())
                    .lastUpdated(requestDTO.getLastUpdated() != null ? 
                            requestDTO.getLastUpdated() : LocalDateTime.now())
                    .build();
            
            // Lưu ScoreBatch
            scoreBatch = scoreBatchRepository.save(scoreBatch);
        }
        
        final ScoreBatch finalScoreBatch = scoreBatch;

        // Tạo ScoreItem mới (không set ID để tránh conflict)
        List<ScoreItem> scoreItems = requestDTO.getScores().stream()
                .map(scoreDTO -> ScoreItem.builder()
                        .scoreBatch(finalScoreBatch)
                        .scoreText(scoreDTO.getScoreText())
                        .scoreFirst(scoreDTO.getScoreFirst())
                        .scoreSecond(scoreDTO.getScoreSecond())
                        .scoreFinal(scoreDTO.getScoreFinal())
                        .scoreOverall(scoreDTO.getScoreOverall())
                        .subjectName(scoreDTO.getSubjectName())
                        .subjectCredit(scoreDTO.getSubjectCredit())
                        .isSelected(scoreDTO.getIsSelected())
                        .build())
                .toList();

        // Lưu tất cả ScoreItem trong một lần (batch insert)
        // Sử dụng batch size để tối ưu performance
        List<ScoreItem> savedItems;
        if (scoreItems.size() > 100) {
            // Chia nhỏ batch nếu quá lớn
            savedItems = new ArrayList<>();
            for (int i = 0; i < scoreItems.size(); i += 50) {
                int end = Math.min(i + 50, scoreItems.size());
                List<ScoreItem> batch = scoreItems.subList(i, end);
                savedItems.addAll(scoreItemRepository.saveAll(batch));
            }
        } else {
            savedItems = scoreItemRepository.saveAll(scoreItems);
        }
        
        // Set scoreItems để tránh lazy loading
        finalScoreBatch.setScoreItems(savedItems);

        return finalScoreBatch;
    }

    @Override
    public ScoreBatch getScoreBatchByStudentCode(String studentCode) {
        return scoreBatchRepository.findByStudentCode(studentCode)
                .orElse(null);
    }
    
    // Method tối ưu để update nhanh hơn
    @Transactional
    public ScoreBatch updateScoreBatchOptimized(ScoreBatchRequestDTO requestDTO) {
        String studentCode = requestDTO.getStudentInfo().getStudentCode();
        
        // Tìm ScoreBatch hiện tại
        Optional<ScoreBatch> existingBatch = scoreBatchRepository.findByStudentCode(studentCode);
        
        if (existingBatch.isPresent()) {
            ScoreBatch scoreBatch = existingBatch.get();
            
            // Update thông tin cơ bản
            scoreBatch.setStudentName(requestDTO.getStudentInfo().getStudentName());
            scoreBatch.setStudentClass(requestDTO.getStudentInfo().getStudentClass());
            scoreBatch.setLastUpdated(requestDTO.getLastUpdated() != null ? 
                    requestDTO.getLastUpdated() : LocalDateTime.now());
            
            // Lưu ScoreBatch
            final ScoreBatch finalScoreBatch = scoreBatchRepository.save(scoreBatch);
            
            // Xóa và tạo mới ScoreItem (cách nhanh nhất)
            scoreItemRepository.deleteByScoreBatch_BatchId(finalScoreBatch.getBatchId());
            
            // Tạo ScoreItem mới
            List<ScoreItem> scoreItems = requestDTO.getScores().stream()
                    .map(scoreDTO -> ScoreItem.builder()
                            .scoreBatch(finalScoreBatch)
                            .scoreText(scoreDTO.getScoreText())
                            .scoreFirst(scoreDTO.getScoreFirst())
                            .scoreSecond(scoreDTO.getScoreSecond())
                            .scoreFinal(scoreDTO.getScoreFinal())
                            .scoreOverall(scoreDTO.getScoreOverall())
                            .subjectName(scoreDTO.getSubjectName())
                            .subjectCredit(scoreDTO.getSubjectCredit())
                            .isSelected(scoreDTO.getIsSelected())
                            .build())
                    .toList();
            
            // Batch insert
            List<ScoreItem> savedItems = scoreItemRepository.saveAll(scoreItems);
            finalScoreBatch.setScoreItems(savedItems);
            
            return finalScoreBatch;
        } else {
            // Tạo mới nếu chưa tồn tại
            return createOrUpdateScoreBatch(requestDTO);
        }
    }
}
