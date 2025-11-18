package com.moneyflow.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OcrResponse {

    private BigDecimal amount;
    private LocalDate date;
    private String merchant;
    private String rawText;
    private Double confidence;
    private String suggestedCategory;
}
