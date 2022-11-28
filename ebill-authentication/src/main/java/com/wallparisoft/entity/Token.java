package com.wallparisoft.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "token")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Token {


    @Id
    @SequenceGenerator(sequenceName = "token_seq", name = "token_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "token_seq")
    private Long idToken;

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
