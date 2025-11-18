package com.example.KMALegend.repository;

import com.example.KMALegend.entity.Ranking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface RankingRepository extends JpaRepository<Ranking, Long> {
    @Query("SELECT r FROM Ranking r WHERE r.student.studentId = :studentId")
    Ranking findByStudentId(@Param("studentId") Long studentId);
    Ranking findByRanking(Long ranking);
    @Query("SELECT r FROM Ranking r WHERE r.student.studentCode = :studentCode")
    Ranking findByStudentCode(@Param("studentCode") String studentCode);
    @Query("SELECT r FROM Ranking r WHERE r.student.studentCode LIKE CONCAT(:mainCode, '%')")
    List<Ranking> findListFilter(@Param("mainCode") String mainCode);
    @Query("SELECT r FROM Ranking r WHERE r.student.studentCode LIKE CONCAT(:mainCode, '%') OR r.student.studentCode LIKE CONCAT(:cyberCode, '%') OR r.student.studentCode LIKE CONCAT(:electronicCode, '%')")
    List<Ranking> findListFilterBlock(@Param("mainCode") String mainCode,
                                      @Param("cyberCode") String cyberCode,
                                      @Param("electronicCode") String electronicCode);
    @Query("SELECT r FROM Ranking r WHERE r.ranking IN (1, 2, 3)")
    List<Ranking> findTopRankingsByStudentCodes(
            @Param("mainCode") String mainCode);
    @Query("SELECT r FROM Ranking r WHERE r.ranking IN (1, 2, 3)")
    List<Ranking> findListTopRanking();
    @Modifying
    @Query("DELETE FROM Ranking")
    @Transactional
    void deleteAllRecords();
    @Modifying
    @Transactional
    @Query(value = "ALTER TABLE ranking AUTO_INCREMENT = 1", nativeQuery = true)
    void resetAutoIncrement();
    @Query("SELECT rk FROM Ranking rk JOIN rk.student st WHERE st.studentCode LIKE :studentCodePrefix OR " +
            "st.studentCode LIKE :studentCodeCyber OR " +
            "st.studentCode LIKE :studentCodeElectric " +
            "ORDER BY rk.gpa DESC")
    List<Ranking> findBlockRanking(@Param("studentCodePrefix") String studentCodePrefix,
                                   @Param("studentCodeCyber") String studentCodeCyber,
                                   @Param("studentCodeElectric") String studentCodeElectric);
    @Query("SELECT rk FROM Ranking rk JOIN rk.student st WHERE st.studentCode LIKE :studentCodePrefix ORDER BY rk.gpa DESC")
    List<Ranking> findOneParam(@Param("studentCodePrefix") String studentCodePrefix);
} 