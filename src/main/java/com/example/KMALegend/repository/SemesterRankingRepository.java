package com.example.KMALegend.repository;

import com.example.KMALegend.entity.SemesterRanking;
import com.example.KMALegend.entity.Student;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface SemesterRankingRepository extends JpaRepository<SemesterRanking, Long> {
    @Query("SELECT s FROM SemesterRanking s WHERE s.student.studentId = :studentId")
    SemesterRanking findByStudentId(@Param("studentId") Long studentId);
    SemesterRanking findByRanking(Long ranking);
    @Query("SELECT s FROM SemesterRanking s WHERE s.student.studentCode = :studentCode")
    SemesterRanking findByStudentCode(@Param("studentCode") String studentCode);
    @Query("SELECT s FROM SemesterRanking s WHERE s.student.studentCode LIKE :mainCode%")
    List<SemesterRanking> findListFilter(@Param("mainCode") String mainCode);
    @Query("SELECT s FROM SemesterRanking s WHERE s.ranking BETWEEN 1 AND 100 ORDER BY s.ranking ASC")
    List<SemesterRanking> findTop100();

    //        @Query("SELECT sr FROM SemesterRanking sr JOIN sr.student s WHERE sr.ranking BETWEEN 1 AND 40 AND s.studentCode LIKE CONCAT(:filterCode, '%') ORDER BY sr.ranking ASC")
//        List<SemesterRanking> findTopWithMatchingFilterCode(@Param("filterCode") String filterCode, Pageable pageable);
    @Modifying
    @Transactional
    @Query("DELETE FROM SemesterRanking")
    void deleteAllRecords();
    @Modifying
    @Transactional
    @Query(value = "ALTER TABLE semester_ranking AUTO_INCREMENT = 1", nativeQuery = true)
    void resetAutoIncrement();
    @Query("SELECT sr FROM SemesterRanking sr JOIN sr.student st WHERE st.studentCode LIKE :studentCodePrefix OR " +
            "st.studentCode LIKE :studentCodeCyber OR " +
            "st.studentCode LIKE :studentCodeElectric " +
            "ORDER BY sr.gpa DESC")
    List<SemesterRanking> findBlockRanking(@Param("studentCodePrefix") String studentCodePrefix,
                                           @Param("studentCodeCyber") String studentCodeCyber,
                                           @Param("studentCodeElectric") String studentCodeElectric);
    @Query("SELECT sr FROM SemesterRanking sr JOIN sr.student st " +
            "WHERE st.studentCode LIKE CONCAT(:filterCode, '%') " +
            "ORDER BY sr.gpa DESC")
    List<SemesterRanking> findTopWithMatchingFilterCode(@Param("filterCode") String filterCode, Pageable pageable);
    @Query(value = "SELECT ranked.ranking, ranked.student_code, ranked.student_name, ranked.student_class, ranked.gpa, ranked.asia_gpa " +
            "FROM ( " +
            "    SELECT ROW_NUMBER() OVER (ORDER BY sr.gpa DESC) AS ranking, st.student_code, st.student_name, st.student_class, sr.gpa, sr.asia_gpa " +
            "    FROM semester_ranking sr " +
            "    JOIN students st ON sr.student_id = st.student_id " +
            "    WHERE st.student_code LIKE :filterCode " +
            ") AS ranked " +
            "WHERE ranked.student_code = :studentCode OR ranked.ranking <= 3 " +
            "ORDER BY ranked.ranking " +
            "LIMIT 4", nativeQuery = true)
    List<Object[]> findTopStudents(@Param("filterCode") String filterCode, @Param("studentCode") String studentCode);
    @Query(value = "SELECT ranked.ranking, ranked.student_code, ranked.student_name, ranked.student_class, ranked.gpa, ranked.asia_gpa " +
            "FROM ( " +
            "    SELECT ROW_NUMBER() OVER (ORDER BY sr.gpa DESC) AS ranking, st.student_code, st.student_name, st.student_class, sr.gpa, sr.asia_gpa " +
            "    FROM semester_ranking sr " +
            "    JOIN students st ON sr.student_id = st.student_id " +
            "    WHERE st.student_code LIKE :filterCode " +
            ") AS ranked " +
            "WHERE ranked.student_code = :studentCode OR ranked.ranking <= :count " +
            "ORDER BY ranked.ranking " +
            "LIMIT :count", nativeQuery = true)
    List<Object[]> findTopStudents(@Param("filterCode") String filterCode, @Param("studentCode") String studentCode, @Param("count") Long count);
    @Query(value = "SELECT ranked.ranking, ranked.student_code, ranked.student_name, ranked.student_class, ranked.gpa, ranked.asia_gpa " +
            "FROM ( " +
            "    SELECT ROW_NUMBER() OVER (ORDER BY sr.gpa DESC) AS ranking, " +
            "           st.student_code, st.student_name, st.student_class, sr.gpa, sr.asia_gpa " +
            "    FROM semester_ranking sr " +
            "    JOIN students st ON sr.student_id = st.student_id " +
            "    WHERE st.student_code LIKE :filterCode " +
            ") AS ranked " +
            "WHERE ranked.student_code = :studentCode " +
            "ORDER BY ranked.ranking", nativeQuery = true)
    Object[] findStudentRanking(@Param("filterCode") String filterCode, @Param("studentCode") String studentCode);
} 