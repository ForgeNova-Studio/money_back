package com.moneyflow.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 가맹점명 기반 카테고리 자동 분류 서비스
 * 
 * 12개 기본 카테고리 + 미분류:
 * - FOOD: 식비 (음식점, 배달)
 * - CAFE_SNACK: 카페/간식 (카페, 베이커리, 디저트)
 * - TRANSPORT: 교통 (택시, 주유, 대중교통)
 * - HOUSING: 주거 (월세, 관리비, 공과금)
 * - COMMUNICATION: 통신/인터넷 (휴대폰, 인터넷)
 * - SUBSCRIPTION: 구독 (넷플릭스, 멜론, 정기결제)
 * - LIVING: 생활 (마트, 편의점, 생활용품)
 * - SHOPPING: 쇼핑 (백화점, 의류, 온라인쇼핑)
 * - HEALTH: 건강 (병원, 약국, 헬스)
 * - EDUCATION: 교육 (학원, 강의, 서점)
 * - CULTURE: 문화 (영화, 공연, 전시)
 * - INSURANCE: 보험 (생명보험, 손해보험)
 * - UNCATEGORIZED: 미분류 (기본값)
 * 
 * Phase 1: 키워드 기반 규칙
 * Phase 2: ML 모델로 교체 예정
 */
@Component
@Slf4j
public class CategoryClassifier {

    private static final String DEFAULT_CATEGORY = "UNCATEGORIZED";

    // 카테고리별 키워드 맵 (우선순위: 더 구체적인 키워드가 먼저 매칭되도록)
    private static final Map<String, List<String>> CATEGORY_KEYWORDS = new LinkedHashMap<>();

    static {
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        // 식비 (FOOD) - 음식점, 배달앱
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        CATEGORY_KEYWORDS.put("FOOD", Arrays.asList(
                // 배달앱
                "배민", "배달의민족", "쿠팡이츠", "요기요", "배달",
                // 음식점 유형
                "식당", "레스토랑", "음식점",
                "한식", "중식", "일식", "양식", "분식",
                // 음식 종류
                "치킨", "피자", "햄버거", "버거", "족발", "보쌈", "곱창", "삼겹살",
                // 프랜차이즈
                "맥도날드", "롯데리아", "버거킹", "맘스터치", "KFC",
                "굽네", "BBQ", "BHC", "교촌", "네네치킨",
                "도미노", "피자헛", "미스터피자"));

        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        // 카페/간식 (CAFE_SNACK) - 카페, 베이커리, 디저트
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        CATEGORY_KEYWORDS.put("CAFE_SNACK", Arrays.asList(
                // 키워드
                "카페", "커피", "베이커리", "빵집", "제과", "디저트", "아이스크림",
                // 카페 프랜차이즈
                "스타벅스", "이디야", "투썸", "메가커피", "컴포즈", "빽다방", "할리스",
                "폴바셋", "블루보틀", "탐앤탐스", "커피빈", "파스쿠찌", "엔제리너스",
                // 베이커리 프랜차이즈
                "파리바게뜨", "파바", "뚜레쥬르", "성심당", "던킨", "크리스피크림",
                // 아이스크림
                "배스킨라빈스", "배라", "나뚜루", "설빙", "빙수"));

        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        // 교통 (TRANSPORT) - 택시, 주유, 대중교통
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        CATEGORY_KEYWORDS.put("TRANSPORT", Arrays.asList(
                // 키워드
                "주유", "주유소", "주차", "택시", "톨게이트", "하이패스",
                // 대중교통
                "버스", "지하철", "기차", "KTX", "SRT", "코레일",
                // 모빌리티 서비스
                "카카오T", "타다", "우버", "쏘카", "그린카", "렌트카",
                // 결제
                "티머니", "캐시비"));

        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        // 주거 (HOUSING) - 월세, 관리비, 공과금
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        CATEGORY_KEYWORDS.put("HOUSING", Arrays.asList(
                "월세", "전세", "관리비",
                "전기세", "수도세", "가스비", "난방비",
                "한전", "한국전력", "도시가스", "수도사업소"));

        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        // 통신/인터넷 (COMMUNICATION)
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        CATEGORY_KEYWORDS.put("COMMUNICATION", Arrays.asList(
                // 키워드
                "통신비", "휴대폰", "핸드폰", "인터넷", "IPTV", "알림서비스",
                // 통신사
                "SKT", "SK텔레콤", "KT", "LG유플러스", "유플러스", "LGU+"));

        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        // 구독 (SUBSCRIPTION) - 정기결제 서비스
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        CATEGORY_KEYWORDS.put("SUBSCRIPTION", Arrays.asList(
                // 키워드
                "구독", "월정액", "정기결제", "이용료",
                // 스트리밍
                "넷플릭스", "유튜브프리미엄", "웨이브", "왓챠", "티빙", "쿠팡플레이", "디즈니+", "애플TV",
                // 음악
                "멜론", "지니", "벅스", "플로", "애플뮤직", "스포티파이",
                // 클라우드/앱
                "iCloud", "아이클라우드", "원드라이브", "드롭박스", "모빌리언스"));

        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        // 생활 (LIVING) - 마트, 편의점, 생활용품
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        CATEGORY_KEYWORDS.put("LIVING", Arrays.asList(
                // 키워드
                "마트", "편의점", "생활용품", "세탁", "클리닝", "철물점",
                // 마트
                "이마트", "홈플러스", "롯데마트", "하나로마트", "코스트코", "트레이더스",
                // 편의점
                "GS25", "CU", "세븐일레븐", "이마트24", "미니스톱",
                // 생활
                "다이소", "아성다이소", "무인양품"));

        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        // 쇼핑 (SHOPPING) - 백화점, 의류, 온라인쇼핑
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        CATEGORY_KEYWORDS.put("SHOPPING", Arrays.asList(
                // 키워드
                "백화점", "아울렛", "의류", "옷가게", "신발", "가방", "화장품",
                // 백화점
                "롯데백화점", "신세계", "현대백화점", "갤러리아",
                // 온라인
                "쿠팡", "11번가", "G마켓", "옥션", "네이버쇼핑", "SSG",
                // 패션
                "무신사", "29CM", "W컨셉", "지그재그", "에이블리",
                // 뷰티
                "올리브영", "시코르", "세포라"));

        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        // 건강 (HEALTH) - 병원, 약국, 헬스
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        CATEGORY_KEYWORDS.put("HEALTH", Arrays.asList(
                // 의료
                "병원", "의원", "약국", "한의원", "치과", "안과", "피부과", "정형외과", "내과", "외과",
                // 운동
                "헬스", "피트니스", "요가", "필라테스", "짐", "스포츠센터", "수영장"));

        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        // 교육 (EDUCATION) - 학원, 강의, 서점
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        CATEGORY_KEYWORDS.put("EDUCATION", Arrays.asList(
                // 키워드
                "학원", "과외", "교육", "강의", "학교", "등록금", "교재",
                // 서점
                "서점", "교보문고", "영풍문고", "알라딘", "예스24", "반디앤루니스",
                // 온라인 교육
                "인프런", "클래스101", "탈잉", "숨고"));

        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        // 문화 (CULTURE) - 영화, 공연, 전시
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        CATEGORY_KEYWORDS.put("CULTURE", Arrays.asList(
                // 영화
                "영화", "CGV", "롯데시네마", "메가박스",
                // 공연/전시
                "공연", "콘서트", "전시", "박물관", "미술관", "뮤지컬",
                // 레저
                "노래방", "PC방", "당구장", "볼링", "스크린골프", "게임"));

        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        // 보험 (INSURANCE)
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        CATEGORY_KEYWORDS.put("INSURANCE", Arrays.asList(
                "보험", "보험료",
                "삼성생명", "한화생명", "교보생명", "동양생명", "미래에셋생명",
                "삼성화재", "DB손해보험", "현대해상", "KB손해보험", "메리츠화재"));
    }

    /**
     * 가맹점명을 기반으로 카테고리 예측
     * 
     * @param merchant 가맹점명
     * @return 예측된 카테고리 코드 (12개 카테고리 중 하나 또는 UNCATEGORIZED)
     */
    public String classify(String merchant) {
        if (merchant == null || merchant.trim().isEmpty()) {
            log.debug("Empty merchant, returning default category: {}", DEFAULT_CATEGORY);
            return DEFAULT_CATEGORY;
        }

        String normalizedMerchant = merchant.toLowerCase().trim();

        // 카테고리별 키워드 매칭 (LinkedHashMap이라 순서 보장)
        for (Map.Entry<String, List<String>> entry : CATEGORY_KEYWORDS.entrySet()) {
            String category = entry.getKey();
            List<String> keywords = entry.getValue();

            for (String keyword : keywords) {
                if (normalizedMerchant.contains(keyword.toLowerCase())) {
                    log.debug("Classified '{}' as '{}' (matched keyword: '{}')",
                            merchant, category, keyword);
                    return category;
                }
            }
        }

        log.debug("No category match for '{}', using default: {}", merchant, DEFAULT_CATEGORY);
        return DEFAULT_CATEGORY;
    }

    /**
     * 지원되는 모든 카테고리 코드 목록 반환
     * 
     * @return 카테고리 코드 Set
     */
    public Set<String> getSupportedCategories() {
        Set<String> categories = new LinkedHashSet<>(CATEGORY_KEYWORDS.keySet());
        categories.add(DEFAULT_CATEGORY);
        return categories;
    }

    /**
     * 특정 카테고리의 키워드 목록 반환 (디버깅/관리 용도)
     * 
     * @param category 카테고리 코드
     * @return 키워드 목록 (없으면 빈 리스트)
     */
    public List<String> getKeywordsForCategory(String category) {
        return CATEGORY_KEYWORDS.getOrDefault(category, Collections.emptyList());
    }
}
