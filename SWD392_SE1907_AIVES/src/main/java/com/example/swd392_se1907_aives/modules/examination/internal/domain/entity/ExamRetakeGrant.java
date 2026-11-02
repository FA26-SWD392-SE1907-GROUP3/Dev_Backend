package com.example.swd392_se1907_aives.modules.examination.internal.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.OffsetDateTime;
import java.util.UUID;

/** An explicit, single-use lecturer authorization. Earlier attempts remain immutable. */
@Entity @Table(name = "exam_retake_grants") @Getter @Setter
public class ExamRetakeGrant {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name="candidate_id", nullable=false) private UUID candidateId;
    @Column(name="previous_attempt_id", nullable=false) private UUID previousAttemptId;
    @Column(name="next_attempt_no", nullable=false) private int nextAttemptNo;
    @Column(name="granted_by", nullable=false) private UUID grantedBy;
    @Column(name="reason", nullable=false, length=1000) private String reason;
    @Column(name="scheduled_start", nullable=false) private OffsetDateTime scheduledStart;
    @Column(name="scheduled_end", nullable=false) private OffsetDateTime scheduledEnd;
    @Column(name="granted_at", nullable=false) private OffsetDateTime grantedAt;
    @Column(name="consumed_at") private OffsetDateTime consumedAt;
    @Column(name="revoked_at") private OffsetDateTime revokedAt;
}
