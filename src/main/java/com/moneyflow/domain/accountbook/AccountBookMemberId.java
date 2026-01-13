package com.moneyflow.domain.accountbook;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;
import java.util.UUID;

/**
 * AccountBookMember 복합 기본키
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class AccountBookMemberId implements Serializable {

    @Column(name = "account_book_id")
    private UUID accountBookId;

    @Column(name = "user_id")
    private UUID userId;
}
