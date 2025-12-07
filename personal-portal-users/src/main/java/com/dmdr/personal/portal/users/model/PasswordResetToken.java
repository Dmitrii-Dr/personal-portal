package com.dmdr.personal.portal.users.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "password_reset_tokens")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String token;

    @OneToOne(targetEntity = User.class, fetch = FetchType.EAGER)
    @JoinColumn(nullable = false, name = "user_id")
    private User user;

    @Column(name = "expiry_date", nullable = false)
    private OffsetDateTime expiryDate;

    /**
     * Sets the expiry date to the current time plus the specified number of minutes.
     *
     * @param minutes the number of minutes from now until expiry
     */
    public void setExpiryDate(int minutes) {
        this.expiryDate = OffsetDateTime.now().plusMinutes(minutes);
    }

    /**
     * Checks if the token has expired.
     *
     * @return true if the current time is after the expiry date, false otherwise
     */
    public boolean isExpired() {
        return OffsetDateTime.now().isAfter(this.expiryDate);
    }
}

