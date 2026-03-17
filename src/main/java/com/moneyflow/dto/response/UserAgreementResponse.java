package com.moneyflow.dto.response;

import com.moneyflow.domain.terms.DocumentType;
import com.moneyflow.domain.terms.UserAgreement;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 사용자 약관 동의 이력 응답 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAgreementResponse {

    private DocumentType documentType;
    private String documentVersion;
    private Boolean agreed;
    private LocalDateTime agreedAt;

    public static UserAgreementResponse from(UserAgreement agreement) {
        return UserAgreementResponse.builder()
            .documentType(agreement.getDocumentType())
            .documentVersion(agreement.getDocumentVersion())
            .agreed(agreement.getAgreed())
            .agreedAt(agreement.getAgreedAt())
            .build();
    }
}
