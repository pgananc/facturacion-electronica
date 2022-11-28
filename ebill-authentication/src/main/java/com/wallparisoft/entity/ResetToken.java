package com.wallparisoft.entity;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "reset_token")
public class ResetToken {


    @Id
    @SequenceGenerator(sequenceName = "reset_token_seq", name = "reset_token_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "reset_token_seq")
    private Long idRestToken;

    @Column(nullable = false, unique = true)
    private String token;

    @ManyToOne
    @JoinColumn(name = "id_user",referencedColumnName = "idUser")
    private User user;

    @Column(name="expiration_date", nullable = false)
    private LocalDateTime expirationDate;

    @Column(name = "status", nullable = false)
    private Boolean status;

    @Column(name = "creation_date")
    private LocalDateTime creationDate;

    @PrePersist
    public void preInsert() {
        creationDate = LocalDateTime.now();
    }

    public void setExpirationDate(int minutes) {
        LocalDateTime hoy = LocalDateTime.now();
        LocalDateTime exp = hoy.plusMinutes(minutes);
        this.expirationDate = exp;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isBefore(this.expirationDate);
    }
}
