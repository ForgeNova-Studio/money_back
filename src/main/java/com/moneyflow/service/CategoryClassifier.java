package com.moneyflow.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 가맹점명 기반 카테고리 자동 분류 서비스
 * Phase 1: 키워드 기반 규칙
 * Phase 2: ML 모델로 교체 예정
 */
@Component
@Slf4j
public class CategoryClassifier {

    private static final Map<String, String> KEYWORD_CATEGORY_MAP = new HashMap<>();
    private static final String DEFAULT_CATEGORY = "ETC";

    static {
        // 식비 (FOOD)
        KEYWORD_CATEGORY_MAP.put("식당", "FOOD");
        KEYWORD_CATEGORY_MAP.put("레스토랑", "FOOD");
        KEYWORD_CATEGORY_MAP.put("음식점", "FOOD");
        KEYWORD_CATEGORY_MAP.put("카페", "FOOD");
        KEYWORD_CATEGORY_MAP.put("커피", "FOOD");
        KEYWORD_CATEGORY_MAP.put("베이커리", "FOOD");
        KEYWORD_CATEGORY_MAP.put("빵집", "FOOD");
        KEYWORD_CATEGORY_MAP.put("치킨", "FOOD");
        KEYWORD_CATEGORY_MAP.put("피자", "FOOD");
        KEYWORD_CATEGORY_MAP.put("햄버거", "FOOD");
        KEYWORD_CATEGORY_MAP.put("분식", "FOOD");
        KEYWORD_CATEGORY_MAP.put("한식", "FOOD");
        KEYWORD_CATEGORY_MAP.put("일식", "FOOD");
        KEYWORD_CATEGORY_MAP.put("중식", "FOOD");
        KEYWORD_CATEGORY_MAP.put("양식", "FOOD");
        KEYWORD_CATEGORY_MAP.put("스타벅스", "FOOD");
        KEYWORD_CATEGORY_MAP.put("맥도날드", "FOOD");
        KEYWORD_CATEGORY_MAP.put("롯데리아", "FOOD");
        KEYWORD_CATEGORY_MAP.put("버거킹", "FOOD");
        KEYWORD_CATEGORY_MAP.put("편의점", "FOOD");
        KEYWORD_CATEGORY_MAP.put("GS25", "FOOD");
        KEYWORD_CATEGORY_MAP.put("CU", "FOOD");
        KEYWORD_CATEGORY_MAP.put("세븐일레븐", "FOOD");

        // 교통 (TRANSPORT)
        KEYWORD_CATEGORY_MAP.put("택시", "TRANSPORT");
        KEYWORD_CATEGORY_MAP.put("버스", "TRANSPORT");
        KEYWORD_CATEGORY_MAP.put("지하철", "TRANSPORT");
        KEYWORD_CATEGORY_MAP.put("기차", "TRANSPORT");
        KEYWORD_CATEGORY_MAP.put("KTX", "TRANSPORT");
        KEYWORD_CATEGORY_MAP.put("SRT", "TRANSPORT");
        KEYWORD_CATEGORY_MAP.put("주유", "TRANSPORT");
        KEYWORD_CATEGORY_MAP.put("주차", "TRANSPORT");
        KEYWORD_CATEGORY_MAP.put("카카오T", "TRANSPORT");
        KEYWORD_CATEGORY_MAP.put("우버", "TRANSPORT");
        KEYWORD_CATEGORY_MAP.put("쏘카", "TRANSPORT");
        KEYWORD_CATEGORY_MAP.put("그린카", "TRANSPORT");
        KEYWORD_CATEGORY_MAP.put("티머니", "TRANSPORT");

        // 쇼핑 (SHOPPING)
        KEYWORD_CATEGORY_MAP.put("백화점", "SHOPPING");
        KEYWORD_CATEGORY_MAP.put("마트", "SHOPPING");
        KEYWORD_CATEGORY_MAP.put("이마트", "SHOPPING");
        KEYWORD_CATEGORY_MAP.put("롯데마트", "SHOPPING");
        KEYWORD_CATEGORY_MAP.put("홈플러스", "SHOPPING");
        KEYWORD_CATEGORY_MAP.put("쿠팡", "SHOPPING");
        KEYWORD_CATEGORY_MAP.put("11번가", "SHOPPING");
        KEYWORD_CATEGORY_MAP.put("G마켓", "SHOPPING");
        KEYWORD_CATEGORY_MAP.put("옥션", "SHOPPING");
        KEYWORD_CATEGORY_MAP.put("네이버쇼핑", "SHOPPING");
        KEYWORD_CATEGORY_MAP.put("다이소", "SHOPPING");
        KEYWORD_CATEGORY_MAP.put("올리브영", "SHOPPING");
        KEYWORD_CATEGORY_MAP.put("의류", "SHOPPING");
        KEYWORD_CATEGORY_MAP.put("신발", "SHOPPING");
        KEYWORD_CATEGORY_MAP.put("가방", "SHOPPING");
        KEYWORD_CATEGORY_MAP.put("화장품", "SHOPPING");

        // 문화생활 (CULTURE)
        KEYWORD_CATEGORY_MAP.put("영화", "CULTURE");
        KEYWORD_CATEGORY_MAP.put("CGV", "CULTURE");
        KEYWORD_CATEGORY_MAP.put("롯데시네마", "CULTURE");
        KEYWORD_CATEGORY_MAP.put("메가박스", "CULTURE");
        KEYWORD_CATEGORY_MAP.put("공연", "CULTURE");
        KEYWORD_CATEGORY_MAP.put("콘서트", "CULTURE");
        KEYWORD_CATEGORY_MAP.put("전시", "CULTURE");
        KEYWORD_CATEGORY_MAP.put("박물관", "CULTURE");
        KEYWORD_CATEGORY_MAP.put("미술관", "CULTURE");
        KEYWORD_CATEGORY_MAP.put("서점", "CULTURE");
        KEYWORD_CATEGORY_MAP.put("교보문고", "CULTURE");
        KEYWORD_CATEGORY_MAP.put("예스24", "CULTURE");
        KEYWORD_CATEGORY_MAP.put("알라딘", "CULTURE");
        KEYWORD_CATEGORY_MAP.put("노래방", "CULTURE");
        KEYWORD_CATEGORY_MAP.put("PC방", "CULTURE");
        KEYWORD_CATEGORY_MAP.put("오락실", "CULTURE");

        // 주거/통신 (HOUSING)
        KEYWORD_CATEGORY_MAP.put("월세", "HOUSING");
        KEYWORD_CATEGORY_MAP.put("관리비", "HOUSING");
        KEYWORD_CATEGORY_MAP.put("전기세", "HOUSING");
        KEYWORD_CATEGORY_MAP.put("수도세", "HOUSING");
        KEYWORD_CATEGORY_MAP.put("가스비", "HOUSING");
        KEYWORD_CATEGORY_MAP.put("통신비", "HOUSING");
        KEYWORD_CATEGORY_MAP.put("휴대폰", "HOUSING");
        KEYWORD_CATEGORY_MAP.put("인터넷", "HOUSING");
        KEYWORD_CATEGORY_MAP.put("SK텔레콤", "HOUSING");
        KEYWORD_CATEGORY_MAP.put("KT", "HOUSING");
        KEYWORD_CATEGORY_MAP.put("LG유플러스", "HOUSING");

        // 의료/건강 (MEDICAL)
        KEYWORD_CATEGORY_MAP.put("병원", "MEDICAL");
        KEYWORD_CATEGORY_MAP.put("약국", "MEDICAL");
        KEYWORD_CATEGORY_MAP.put("한의원", "MEDICAL");
        KEYWORD_CATEGORY_MAP.put("치과", "MEDICAL");
        KEYWORD_CATEGORY_MAP.put("안과", "MEDICAL");
        KEYWORD_CATEGORY_MAP.put("헬스", "MEDICAL");
        KEYWORD_CATEGORY_MAP.put("피트니스", "MEDICAL");
        KEYWORD_CATEGORY_MAP.put("요가", "MEDICAL");
        KEYWORD_CATEGORY_MAP.put("필라테스", "MEDICAL");

        // 교육 (EDUCATION)
        KEYWORD_CATEGORY_MAP.put("학원", "EDUCATION");
        KEYWORD_CATEGORY_MAP.put("과외", "EDUCATION");
        KEYWORD_CATEGORY_MAP.put("교육", "EDUCATION");
        KEYWORD_CATEGORY_MAP.put("강의", "EDUCATION");
        KEYWORD_CATEGORY_MAP.put("학교", "EDUCATION");
        KEYWORD_CATEGORY_MAP.put("등록금", "EDUCATION");

        // 경조사 (EVENT)
        KEYWORD_CATEGORY_MAP.put("축의금", "EVENT");
        KEYWORD_CATEGORY_MAP.put("부의금", "EVENT");
        KEYWORD_CATEGORY_MAP.put("선물", "EVENT");
        KEYWORD_CATEGORY_MAP.put("꽃집", "EVENT");
    }

    /**
     * 가맹점명을 기반으로 카테고리 예측
     * @param merchant 가맹점명
     * @return 예측된 카테고리 코드
     */
    public String classify(String merchant) {
        if (merchant == null || merchant.trim().isEmpty()) {
            return DEFAULT_CATEGORY;
        }

        String normalizedMerchant = merchant.toLowerCase().trim();

        // 키워드 매칭
        for (Map.Entry<String, String> entry : KEYWORD_CATEGORY_MAP.entrySet()) {
            if (normalizedMerchant.contains(entry.getKey().toLowerCase())) {
                log.debug("Classified '{}' as category '{}'", merchant, entry.getValue());
                return entry.getValue();
            }
        }

        log.debug("No category match for '{}', using default category '{}'", merchant, DEFAULT_CATEGORY);
        return DEFAULT_CATEGORY;
    }
}
