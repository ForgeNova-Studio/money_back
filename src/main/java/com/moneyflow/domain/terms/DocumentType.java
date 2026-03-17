package com.moneyflow.domain.terms;

/**
 * 약관 문서 타입
 */
public enum DocumentType {
    /**
     * 서비스 이용약관 (필수)
     */
    SERVICE_TERMS,

    /**
     * 개인정보 수집·이용 동의 (필수)
     */
    PRIVACY_COLLECTION,

    /**
     * 마케팅 정보 수신 동의 (선택)
     */
    MARKETING
}
