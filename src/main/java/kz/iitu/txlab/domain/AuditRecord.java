package kz.iitu.txlab.domain;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="audit_records")
public class AuditRecord {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="run_id",nullable=false,length=80) private String runId;
    @Column(nullable=false,length=40) private String action;
    @Column(name="transaction_id",nullable=false) private long transactionId;
    @Column(nullable=false,length=200) private String message;
    @Column(name="created_at",nullable=false) private Instant createdAt;
    protected AuditRecord() { }
    public AuditRecord(String runId,String action,long transactionId){
        this.runId=runId;this.action=action;this.transactionId=transactionId;
        message="Outer operation will roll back after this audit call";createdAt=Instant.now();
    }
    public Long getId(){return id;} public String getRunId(){return runId;}
    public long getTransactionId(){return transactionId;}
}
