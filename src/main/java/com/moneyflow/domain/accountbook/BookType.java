package com.moneyflow.domain.accountbook;

/**
 * 장부 유형
 */
public enum BookType {
    // 기본 가계부(DEFAULT):
    // - 시스템이 사용자별로 자동 생성/유지하는 기본 장부
    // - accountBookId 미지정 요청의 기본 fallback 대상으로 사용
    // - 사용자가 생성 화면에서 직접 새로 만드는 타입이 아님
    DEFAULT,
    COUPLE_LIVING, // 커플 생활비
    TRIP, // 여행
    // 일반 가계부(PROJECT):
    // - 사용자가 직접 생성해서 자유롭게 사용하는 일반 목적 장부
    // - 기본 가계부와 달리 자동 생성/강제 유지 대상이 아님
    PROJECT
}
