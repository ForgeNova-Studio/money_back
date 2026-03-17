package com.moneyflow.domain.terms;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 약관 문서 Repository
 */
@Repository
public interface TermsDocumentRepository extends JpaRepository<TermsDocument, UUID> {

    /**
     * 현재 유효한 모든 약관 조회 (is_active = true)
     */
    List<TermsDocument> findByIsActiveTrue();

    /**
     * 특정 타입의 최신 약관 조회
     */
    Optional<TermsDocument> findByTypeAndIsActiveTrue(DocumentType type);

    /**
     * 특정 타입과 버전의 약관 조회
     */
    Optional<TermsDocument> findByTypeAndVersion(DocumentType type, String version);
}
