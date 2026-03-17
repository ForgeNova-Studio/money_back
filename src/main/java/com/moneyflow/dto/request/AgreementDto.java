package com.moneyflow.dto.request;

import com.moneyflow.domain.terms.DocumentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 약관 동의 DTO
 *
 * 회원가입 및 재동의 시 사용
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgreementDto {

    @NotNull(message = "약관 타입은 필수입니다")
    private DocumentType type;

    @NotBlank(message = "약관 버전은 필수입니다")
    private String version;

    @NotNull(message = "동의 여부는 필수입니다")
    private Boolean agreed;
}
