package com.moneyflow.domain.asset;

/**
 * 자산 카테고리 enum (2단계 그룹핑)
 *
 * 그룹:
 * - CASH_LIKE: 현금성 자산
 * - INVESTMENT: 투자 자산
 * - PHYSICAL: 실물 자산
 * - OTHER: 기타
 */
public enum AssetCategory {
    // 현금성 (CASH_LIKE)
    CASH("현금", AssetGroup.CASH_LIKE),
    SAVINGS("예적금", AssetGroup.CASH_LIKE),

    // 투자 (INVESTMENT)
    STOCK("주식", AssetGroup.INVESTMENT),
    CRYPTO("암호화폐", AssetGroup.INVESTMENT),
    BOND("채권", AssetGroup.INVESTMENT),
    PENSION("연금", AssetGroup.INVESTMENT),

    // 실물 (PHYSICAL)
    REAL_ESTATE("부동산", AssetGroup.PHYSICAL),
    CAR("자동차", AssetGroup.PHYSICAL),

    // 기타 (OTHER)
    LENT_MONEY("빌려준 돈", AssetGroup.OTHER),
    OTHER("기타", AssetGroup.OTHER);

    private final String label;
    private final AssetGroup group;

    AssetCategory(String label, AssetGroup group) {
        this.label = label;
        this.group = group;
    }

    public String getLabel() {
        return label;
    }

    public AssetGroup getGroup() {
        return group;
    }

    /**
     * 자산 그룹 (Level 1)
     */
    public enum AssetGroup {
        CASH_LIKE("현금성"),
        INVESTMENT("투자"),
        PHYSICAL("실물"),
        OTHER("기타");

        private final String label;

        AssetGroup(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }
}
