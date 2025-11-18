package com.moneyflow.service;

import com.moneyflow.dto.response.OcrResponse;
import org.springframework.web.multipart.MultipartFile;

/**
 * OCR 서비스 인터페이스
 * 구현체: MockOcrService (개발), GoogleVisionOcrService (프로덕션)
 */
public interface OcrService {

    /**
     * 이미지에서 결제 정보 추출
     * @param image 이미지 파일
     * @return OCR 처리 결과
     */
    OcrResponse processImage(MultipartFile image);
}
