package ec.edu.uce.tickets.auth.audit;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_log")
@Getter
@Setter
@NoArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private LocalDateTime occurredAt;

    @Column(name = "username", nullable = false, length = 50, updatable = false)
    private String username;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 40, updatable = false)
    private AuditAction action;

    @Column(name = "entity_type", length = 30, updatable = false)
    private String entityType;

    @Column(name = "entity_id", length = 64, updatable = false)
    private String entityId;

    @Enumerated(EnumType.STRING)
    @Column(name = "result", nullable = false, length = 10, updatable = false)
    private AuditResult result;

    @Column(name = "detail", length = 250, updatable = false)
    private String detail;

    @Column(name = "ip_address", length = 45, updatable = false)
    private String ipAddress;
}