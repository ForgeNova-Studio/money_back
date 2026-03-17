package com.moneyflow.domain.terms;

import com.moneyflow.domain.user.User;
import com.moneyflow.dto.request.AgreementDto;
import com.moneyflow.dto.response.TermsDocumentResponse;
import com.moneyflow.dto.response.UserAgreementResponse;
import com.moneyflow.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 약관 관리 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class TermsService {

    private final TermsDocumentRepository termsDocumentRepository;
    private final UserAgreementRepository userAgreementRepository;

    /**
     * 현재 유효한 모든 약관 조회
     */
    public List<TermsDocumentResponse> getActiveTerms() {
        return termsDocumentRepository.findByIsActiveTrue()
            .stream()
            .map(TermsDocumentResponse::from)
            .collect(Collectors.toList());
    }

    /**
     * 사용자의 약관 동의 이력 조회
     */
    public List<UserAgreementResponse> getUserAgreements(User user) {
        return userAgreementRepository.findByUserOrderByAgreedAtDesc(user)
            .stream()
            .map(UserAgreementResponse::from)
            .collect(Collectors.toList());
    }

    /**
     * 약관 동의 저장
     *
     * @param user 사용자
     * @param agreements 동의 목록
     * @param ipAddress IP 주소 (선택)
     * @param userAgent User-Agent (선택)
     */
    @Transactional
    public void saveAgreements(
        User user,
        List<AgreementDto> agreements,
        String ipAddress,
        String userAgent
    ) {
        // 필수 약관 검증
        validateRequiredAgreements(agreements);

        // 약관 버전 검증 및 저장
        for (AgreementDto agreementDto : agreements) {
            TermsDocument termsDocument = termsDocumentRepository
                .findByTypeAndVersion(agreementDto.getType(), agreementDto.getVersion())
                .orElseThrow(() -> new BusinessException(
                    String.format("존재하지 않는 약관입니다. type=%s, version=%s",
                        agreementDto.getType(), agreementDto.getVersion())));

            // 필수 약관인데 동의하지 않은 경우
            if (termsDocument.getIsRequired() && !agreementDto.getAgreed()) {
                throw new BusinessException(
                    String.format("%s는 필수 약관입니다.", termsDocument.getTitle()));
            }

            // 기존 동의 이력 확인 (중복 방지)
            userAgreementRepository.findByUserAndDocumentTypeAndDocumentVersion(
                user,
                agreementDto.getType(),
                agreementDto.getVersion()
            ).ifPresent(existing -> {
                throw new BusinessException("이미 동의한 약관입니다.");
            });

            // 동의 이력 저장
            UserAgreement userAgreement = UserAgreement.builder()
                .user(user)
                .documentType(agreementDto.getType())
                .documentVersion(agreementDto.getVersion())
                .agreed(agreementDto.getAgreed())
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build();

            userAgreementRepository.save(userAgreement);
        }

        log.info("약관 동의 저장 완료: userId={}, agreementCount={}", user.getUserId(), agreements.size());
    }

    /**
     * 마케팅 수신 동의 변경
     */
    @Transactional
    public void updateMarketingConsent(User user, Boolean agreed, String ipAddress, String userAgent) {
        // 현재 유효한 마케팅 약관 조회
        TermsDocument marketingTerms = termsDocumentRepository
            .findByTypeAndIsActiveTrue(DocumentType.MARKETING)
            .orElseThrow(() -> new BusinessException("마케팅 약관이 존재하지 않습니다."));

        // 이미 동일 버전에 동의한 이력이 있는지 확인
        userAgreementRepository.findByUserAndDocumentTypeAndDocumentVersion(
            user,
            DocumentType.MARKETING,
            marketingTerms.getVersion()
        ).ifPresent(existing -> {
            throw new BusinessException("이미 현재 버전의 마케팅 약관에 대한 동의 이력이 있습니다.");
        });

        // 새로운 동의 이력 저장
        UserAgreement userAgreement = UserAgreement.builder()
            .user(user)
            .documentType(DocumentType.MARKETING)
            .documentVersion(marketingTerms.getVersion())
            .agreed(agreed)
            .ipAddress(ipAddress)
            .userAgent(userAgent)
            .build();

        userAgreementRepository.save(userAgreement);

        log.info("마케팅 수신 동의 변경: userId={}, agreed={}", user.getUserId(), agreed);
    }

    /**
     * 필수 약관 동의 여부 검증
     */
    private void validateRequiredAgreements(List<AgreementDto> agreements) {
        // 필수 약관 타입 목록
        List<DocumentType> requiredTypes = List.of(
            DocumentType.SERVICE_TERMS,
            DocumentType.PRIVACY_COLLECTION
        );

        // 제출된 약관 타입 목록
        List<DocumentType> submittedTypes = agreements.stream()
            .filter(AgreementDto::getAgreed)
            .map(AgreementDto::getType)
            .collect(Collectors.toList());

        // 필수 약관 누락 확인
        for (DocumentType requiredType : requiredTypes) {
            if (!submittedTypes.contains(requiredType)) {
                throw new BusinessException(
                    String.format("%s 동의는 필수입니다.", requiredType));
            }
        }
    }
}
