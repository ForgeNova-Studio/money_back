package com.moneyflow.domain.terms;

import com.moneyflow.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 사용자 약관 동의 이력 Repository
 */
@Repository
public interface UserAgreementRepository extends JpaRepository<UserAgreement, UUID> {

    /**
     * 특정 사용자의 모든 동의 이력 조회
     */
    List<UserAgreement> findByUserOrderByAgreedAtDesc(User user);

    /**
     * 특정 사용자의 특정 타입 약관 동의 이력 조회 (최신순)
     */
    List<UserAgreement> findByUserAndDocumentTypeOrderByAgreedAtDesc(User user, DocumentType documentType);

    /**
     * 특정 사용자가 특정 타입의 약관에 동의했는지 확인
     */
    Optional<UserAgreement> findTopByUserAndDocumentTypeAndAgreedTrueOrderByAgreedAtDesc(
        User user,
        DocumentType documentType
    );

    /**
     * 특정 사용자, 타입, 버전의 동의 이력 조회
     */
    Optional<UserAgreement> findByUserAndDocumentTypeAndDocumentVersion(
        User user,
        DocumentType documentType,
        String documentVersion
    );
}
