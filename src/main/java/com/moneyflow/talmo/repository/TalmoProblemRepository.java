package com.moneyflow.talmo.repository;

import com.moneyflow.talmo.domain.TalmoProblem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TalmoProblemRepository extends JpaRepository<TalmoProblem, Long> {

    @Query("SELECT p FROM TalmoProblem p JOIN FETCH p.user ORDER BY p.createdAt DESC")
    List<TalmoProblem> findAllWithUser();

    @Query("SELECT p FROM TalmoProblem p JOIN FETCH p.user WHERE p.createdAt >= :since ORDER BY p.createdAt DESC")
    List<TalmoProblem> findSince(@Param("since") LocalDateTime since);

    List<TalmoProblem> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("SELECT p FROM TalmoProblem p WHERE p.createdAt >= :startOfDay")
    List<TalmoProblem> findTodayProblems(@Param("startOfDay") LocalDateTime startOfDay);

    @Query("SELECT p FROM TalmoProblem p JOIN FETCH p.user WHERE p.id = :id")
    Optional<TalmoProblem> findByIdWithUser(@Param("id") Long id);

    boolean existsByUserIdAndCreatedAtBetween(Long userId, LocalDateTime start, LocalDateTime end);
}
