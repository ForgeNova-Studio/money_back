package com.moneyflow.domain.category;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "categories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category {

    @Id
    @Column(name = "category_code", length = 50)
    private String categoryCode;

    @Column(name = "category_name", nullable = false, length = 50)
    private String categoryName;

    @Column(name = "icon_name", length = 50)
    private String iconName;

    @Column(name = "color_hex", length = 7)
    private String colorHex;

    @Column(name = "sort_order")
    private Integer sortOrder;
}
