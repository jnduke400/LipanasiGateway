package com.hybrid9.pg.Lipanasi.entities.payments.tqs;

import com.hybrid9.pg.Lipanasi.models.Auditable;
import com.hybrid9.pg.Lipanasi.entities.payments.pushussd.PushUssd;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "tigo_transaction_responses")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TigoTransactionResponse extends Auditable<String> {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "push_ussd_id", nullable = false)
    private PushUssd pushUssd;

    private String type;

    @Column(name = "reference_id", nullable = false, length = 20)
    private String referenceId;

    @Column(name = "external_ref_id", nullable = false, length = 20)
    private String externalRefId;

    @Column(name = "transaction_id", length = 20)
    private String transactionId;

    @Column(name = "result_code", length = 14)
    private String resultCode;

    @Column(name = "result_desc", length = 160)
    private String resultDesc;

    @Column(name = "transaction_status", length = 25)
    private String transactionStatus;

    @Column(name = "refund_request_id", length = 20)
    private String refundRequestId;

    @Column(name = "refund_request_status", length = 1)
    private String refundRequestStatus;

    @Column(name = "refund_transaction_id", length = 20)
    private String refundTransactionId;

    @CreationTimestamp
    @Column(name = "response_date", nullable = false)
    private LocalDateTime responseDate;

    @Column(name = "raw_response", columnDefinition = "TEXT")
    private String rawResponse;

    // Custom enum for transaction status mapping
    public enum TransactionStatus {
        POSTED("Posted", "COLLECTED"),
        DECLINED("Declined", "FAILED"),
        REFUND("Refund", "REFUND"),
        REFUND_DECLINED("Refund Declined", "REFUND_FAILED");

        private final String tigoStatus;
        private final String systemStatus;

        TransactionStatus(String tigoStatus, String systemStatus) {
            this.tigoStatus = tigoStatus;
            this.systemStatus = systemStatus;
        }

        public static String mapToSystemStatus(String tigoStatus) {
            for (TransactionStatus status : values()) {
                if (status.tigoStatus.equals(tigoStatus)) {
                    return status.systemStatus;
                }
            }
            return null;
        }
    }

    // Helper method to get system status
    public String getSystemStatus() {
        return TransactionStatus.mapToSystemStatus(this.transactionStatus);
    }
}
