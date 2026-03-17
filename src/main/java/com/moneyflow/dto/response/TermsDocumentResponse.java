package com.moneyflow.dto.response;

import com.moneyflow.domain.terms.DocumentType;
import com.moneyflow.domain.terms.TermsDocument;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 약관 문서 응답 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TermsDocumentResponse {

    private DocumentType type;
    private String version;
    private String title;
    private String content;
    private LocalDateTime effectiveAt;
    private Boolean isRequired;
    private Boolean requiresReconsent;
    private String changeSummary;

    public static TermsDocumentResponse from(TermsDocument document) {
        return TermsDocumentResponse.builder()
            .type(document.getType())
            .version(document.getVersion())
            .title(document.getTitle())
            .content(document.getContent())
            .effectiveAt(document.getEffectiveAt())
            .isRequired(document.getIsRequired())
            .requiresReconsent(document.getRequiresReconsent())
            .changeSummary(document.getChangeSummary())
            .build();
    }
}
