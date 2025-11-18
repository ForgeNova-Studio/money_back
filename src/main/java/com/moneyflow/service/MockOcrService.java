package com.moneyflow.service;

import com.moneyflow.dto.response.OcrResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Random;

/**
 * Mock OCR 서비스 (개발용)
 * 실제 OCR 처리 없이 고정/랜덤 데이터 반환
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MockOcrService implements OcrService {

    private final CategoryClassifier categoryClassifier;
    private final Random random = new Random();

    private static final String[] MERCHANTS = {
            "스타벅스 강남점",
            "이마트 역삼점",
            "CU편의점",
            "맥도날드",
            "GS칼텍스 주유소",
            "CGV 영화관",
            "올리브영",
            "교보문고",
            "카카오T 택시"
    };

    @Override
    public OcrResponse processImage(MultipartFile image) {
        log.info("Processing OCR with Mock Service: {}", image.getOriginalFilename());

        // 랜덤 가맹점 선택
        String merchant = MERCHANTS[random.nextInt(MERCHANTS.length)];

        // 랜덤 금액 생성 (1,000 ~ 100,000원)
        BigDecimal amount = BigDecimal.valueOf((random.nextInt(99) + 1) * 1000);

        // 랜덤 날짜 (최근 7일 이내)
        LocalDate date = LocalDate.now().minusDays(random.nextInt(7));

        // 카테고리 자동 분류
        String suggestedCategory = categoryClassifier.classify(merchant);

        String rawText = String.format("승인 %s원\n%s\n%s",
                String.format("%,d", amount.intValue()), merchant, date);

        log.info("Mock OCR result: merchant={}, amount={}, category={}",
                merchant, amount, suggestedCategory);

        return OcrResponse.builder()
                .amount(amount)
                .date(date)
                .merchant(merchant)
                .rawText(rawText)
                .confidence(0.95)
                .suggestedCategory(suggestedCategory)
                .build();
    }
}
