package co.dulcesydulces.provedor_backend.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.dulcesydulces.provedor_backend.domain.entidades.PasswordResetToken;
import co.dulcesydulces.provedor_backend.domain.entidades.Usuarios;
import co.dulcesydulces.provedor_backend.repository.PasswordResetTokenRepo;
import co.dulcesydulces.provedor_backend.repository.UsuarioRepository;


@Service
public class PasswordRecoveryService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordResetTokenRepo tokenRepo;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    public PasswordRecoveryService(UsuarioRepository usuarioRepository,
                                   PasswordResetTokenRepo tokenRepo,
                                   PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.tokenRepo = tokenRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void iniciarRecuperacionPorEmail(String email) {
        // No revelar si existe o no: siempre responder “ok”
        usuarioRepository.findByEmail(email).ifPresent(usuario -> {
            String token = generarToken();
            String tokenHash = sha256(token);

            PasswordResetToken prt = new PasswordResetToken();
            prt.setUsuarioCodigo(usuario.getCodigo());
            prt.setTokenHash(tokenHash);
            prt.setExpiraEn(LocalDateTime.now().plusMinutes(20));
            tokenRepo.save(prt);

            
            String link = "http://localhost:8080/reset-password?token=" + token;
            System.out.println("LINK RECUPERACION: " + link);
        });
    }

    @Transactional
    public void restablecerConToken(String tokenEnClaro, String nuevaClave) {
        String tokenHash = sha256(tokenEnClaro);

        PasswordResetToken prt = tokenRepo
            .findFirstByTokenHashAndUsadoEnIsNullAndExpiraEnAfter(tokenHash, LocalDateTime.now())
            .orElseThrow(() -> new RuntimeException("Token inválido o expirado."));

        Usuarios usuario = usuarioRepository.findById(prt.getUsuarioCodigo())
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado."));

        // OJO: tu entidad actualmente usa get/setPassword_hash
        usuario.setPassword_hash(passwordEncoder.encode(nuevaClave));
        usuarioRepository.save(usuario);

        prt.setUsadoEn(LocalDateTime.now());
        tokenRepo.save(prt);
    }

    private String generarToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    
private String sha256(String input) {
    try {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(digest);
    } catch (NoSuchAlgorithmException e) {
        throw new RuntimeException("Error generando hash", e);
    }
}

}