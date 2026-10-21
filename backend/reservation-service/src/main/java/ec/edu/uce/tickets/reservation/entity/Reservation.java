package ec.edu.uce.tickets.reservation.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "reservation")
@Getter
@Setter
@NoArgsConstructor
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId;

    @Column(name = "code", nullable = false, length = 20)
    private String code;

    @Column(name = "client_id", nullable = false)
    private Long clientId;      // referencia lógica a auth-service (no hay FK entre bases)

    @Column(name = "match_id", nullable = false)
    private Long matchId;       // referencia lógica a match-service

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 12)
    private ReservationStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @OneToMany(mappedBy = "reservation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SeatLock> seatLocks = new ArrayList<>();

    @OneToMany(mappedBy = "reservation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Ticket> tickets = new ArrayList<>();

    public void addSeatLock(SeatLock seatLock) {
        seatLocks.add(seatLock);
        seatLock.setReservation(this);
    }

    public void addTicket(Ticket ticket) {
        tickets.add(ticket);
        ticket.setReservation(this);
    }

    @PrePersist
    void onCreate() {
        if (publicId == null) {
            publicId = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now(ZoneId.of("America/Guayaquil"));
        }
    }
}