package com.moneyflow.domain.terms;

import com.moneyflow.domain.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 사용자 약관 동의 이력 엔티티
 *
 * 법적 증거를 위해 사용자별 약관 동의 이력을 버전별로 저장합니다.
 */
@Entity
@Table(
    name = "user_agreements",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "document_type", "document_version"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAgreement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "agreement_id")
    private UUID agreementId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 50)
    private DocumentType documentType;

    @Column(name = "document_version", nullable = false, length = 20)
    private String documentVersion;

    @Column(name = "agreed", nullable = false)
    private Boolean agreed;

    @CreationTimestamp
    @Column(name = "agreed_at")
    private LocalDateTime agreedAt;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;
}
