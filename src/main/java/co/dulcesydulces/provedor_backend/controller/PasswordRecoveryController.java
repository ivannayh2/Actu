package co.dulcesydulces.provedor_backend.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import co.dulcesydulces.provedor_backend.service.PasswordRecoveryService;

@Controller
public class PasswordRecoveryController {

    private final PasswordRecoveryService recoveryService;

    public PasswordRecoveryController(PasswordRecoveryService recoveryService) {
        this.recoveryService = recoveryService;
    }

    @GetMapping("/forgot-password")
    public String forgotPassword() {
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    public String doForgotPassword(@RequestParam("email") String email, Model model) {
        recoveryService.iniciarRecuperacionPorEmail(email);
        model.addAttribute("msg", "Si el correo existe, enviaremos instrucciones para restablecer la contraseña.");
        return "forgot-password";
    }

    @GetMapping("/reset-password")
    public String resetPassword(@RequestParam("token") String token, Model model) {
        model.addAttribute("token", token);
        return "reset-password";
    }

    @PostMapping("/reset-password")
    public String doResetPassword(@RequestParam("token") String token,
                                  @RequestParam("clave") String clave,
                                  @RequestParam("clave2") String clave2,
                                  Model model) {
        if (!clave.equals(clave2)) {
            model.addAttribute("error", "Las contraseñas no coinciden.");
            model.addAttribute("token", token);
            return "reset-password";
        }

        try {
            recoveryService.restablecerConToken(token, clave);
            return "redirect:/login?resetok";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("token", token);
            return "reset-password";
        }
    }
}
