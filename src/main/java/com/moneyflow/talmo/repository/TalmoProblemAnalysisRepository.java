package com.moneyflow.talmo.repository;

import com.moneyflow.talmo.domain.TalmoProblemAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TalmoProblemAnalysisRepository extends JpaRepository<TalmoProblemAnalysis, Long> {

    Optional<TalmoProblemAnalysis> findTopByProblemIdAndSolutionVersionOrderByCreatedAtDesc(Long problemId, Integer solutionVersion);

    boolean existsByProblemIdAndSolutionVersionAndNotificationStatus(Long problemId, Integer solutionVersion, String notificationStatus);

    List<TalmoProblemAnalysis> findByProblemIdOrderByCreatedAtDesc(Long problemId);
}
