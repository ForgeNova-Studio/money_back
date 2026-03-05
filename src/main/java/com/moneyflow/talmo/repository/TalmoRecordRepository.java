package com.moneyflow.talmo.repository;

import com.moneyflow.talmo.domain.TalmoRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface TalmoRecordRepository extends JpaRepository<TalmoRecord, Long> {

    List<TalmoRecord> findAllByOrderByCompletedAtDesc();

    List<TalmoRecord> findByUserIdOrderByCompletedAtDesc(Long userId);

    @Query("SELECT r FROM TalmoRecord r JOIN FETCH r.user WHERE r.completedAt >= :startOfDay ORDER BY r.completedAt DESC")
    List<TalmoRecord> findTodayRecords(@Param("startOfDay") LocalDateTime startOfDay);

    @Query("SELECT r FROM TalmoRecord r JOIN FETCH r.user ORDER BY r.completedAt DESC")
    List<TalmoRecord> findAllWithUser();

    boolean existsByUserIdAndCompletedAtBetween(Long userId, LocalDateTime start, LocalDateTime end);
}
