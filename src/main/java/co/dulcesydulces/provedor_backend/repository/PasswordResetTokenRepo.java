package co.dulcesydulces.provedor_backend.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import co.dulcesydulces.provedor_backend.domain.entidades.PasswordResetToken;

public interface PasswordResetTokenRepo extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findFirstByTokenHashAndUsadoEnIsNullAndExpiraEnAfter(
        String tokenHash, LocalDateTime now
    );
}
