package com.pulsepass.domain;

import com.pulsepass.domain.enums.TicketStatus;
import com.pulsepass.domain.enums.TicketType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Ticket de PulsePass (PRD Persistencia §6.1, FR-TKT-001..005, BR-005, BR-006).
 * Es una entidad propia y no un @ManyToMany entre User y Event porque tiene datos
 * propios: código, tipo, precio, estado y fecha de compra (BR-006).
 * Se mapea a la tabla {@code tickets} creada en V1.
 */
@Entity
@Table(name = "tickets")
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "ticket_code", nullable = false, length = 30)
    private String ticketCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private TicketType type;

    /** BigDecimal con precisión exacta, nunca float/double (BR-007, NFR-008, V1-5). */
    @Column(name = "price", nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TicketStatus status;

    /** Admite NULL: un ticket RESERVED todavía no se ha comprado (P1-D1). */
    @Column(name = "purchase_date")
    private LocalDateTime purchaseDate;

    /** Todo ticket pertenece a exactamente un usuario (BR-005, FR-TKT-001). Sin cascade. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Todo ticket pertenece a exactamente un evento (BR-005, FR-TKT-001). Sin cascade. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    /** Requerido por JPA para crear la entidad al leerla de la base de datos. */
    protected Ticket() {
    }

    /**
     * Crea un ticket con sus campos obligatorios. purchaseDate admite NULL y se asigna
     * con su setter (P1-08-d).
     */
    public Ticket(String ticketCode, TicketType type, BigDecimal price, TicketStatus status,
                  User user, Event event) {
        this.ticketCode = ticketCode;
        this.type = type;
        this.price = price;
        this.status = status;
        this.user = user;
        this.event = event;
    }

    public Long getId() {
        return id;
    }

    public String getTicketCode() {
        return ticketCode;
    }

    public void setTicketCode(String ticketCode) {
        this.ticketCode = ticketCode;
    }

    public TicketType getType() {
        return type;
    }

    public void setType(TicketType type) {
        this.type = type;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public TicketStatus getStatus() {
        return status;
    }

    public void setStatus(TicketStatus status) {
        this.status = status;
    }

    public LocalDateTime getPurchaseDate() {
        return purchaseDate;
    }

    public void setPurchaseDate(LocalDateTime purchaseDate) {
        this.purchaseDate = purchaseDate;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Event getEvent() {
        return event;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

    /** Dos tickets son iguales solo si ya están persistidos y tienen el mismo id (P1-D4). */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Ticket other)) {
            return false;
        }
        return id != null && id.equals(other.getId());
    }

    /** Valor fijo por tipo: no cambia cuando la base de datos asigna el id (P1-D4). */
    @Override
    public int hashCode() {
        return Ticket.class.hashCode();
    }
}
