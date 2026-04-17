package co.dulcesydulces.provedor_backend.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.expression.WebExpressionAuthorizationManager;

import co.dulcesydulces.provedor_backend.domain.entidades.Usuarios;
import co.dulcesydulces.provedor_backend.repository.UsuarioRepository;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http, UsuarioRepository usuarioRepository) throws Exception {
    http
      .csrf(csrf -> csrf.disable())
      .authorizeHttpRequests(auth -> auth
    .requestMatchers("/css/**", "/js/**", "/JS/**", "/img/**", "/icons/**", "/favicon.ico").permitAll()
    .requestMatchers("/login", "/error").permitAll()
    .requestMatchers("/forgot-password", "/reset-password").permitAll()
    .requestMatchers("/home", "/uploads/**")
      .access(new WebExpressionAuthorizationManager("hasAuthority('permImportarArchivosView')"))
    .requestMatchers("/egresos/**")
      .access(new WebExpressionAuthorizationManager("hasAuthority('permComprobanteEgresosView')"))
    .requestMatchers("/configuracion")
      .access(new WebExpressionAuthorizationManager("hasAuthority('permUsuariosView') or hasAuthority('permPerfilView')"))
    .requestMatchers("/configuracion/perfil", "/api/usuarios/*/foto")
      .access(new WebExpressionAuthorizationManager("hasAuthority('permPerfilView')"))
    .requestMatchers("/usuarios/**", "/api/usuarios/**")
      .access(new WebExpressionAuthorizationManager("hasAuthority('permUsuariosView')"))
    .requestMatchers("/historial/**", "/api/historial/**")
      .access(new WebExpressionAuthorizationManager("hasAuthority('permHistorialView')"))
    .anyRequest().authenticated()
)

      .formLogin(form -> form
        .loginPage("/login")
        .loginProcessingUrl("/login")  
        .usernameParameter("codigo")
        .passwordParameter("clave")
        .failureHandler((request, response, exception) -> {
          if (exception instanceof DisabledException) {
            response.sendRedirect("/login?inactivo");
          } else {
            response.sendRedirect("/login?error");
          }
        })
        .successHandler((request, response, authentication) -> {
          String codigo = authentication.getName();
          Usuarios user = usuarioRepository.findByCodigo(codigo).orElse(null);

          List<String> permisos = (user != null && user.getPermisos() != null) ? user.getPermisos() : List.of();

          boolean canImportFiles = permisos.stream()
            .anyMatch(p -> "permImportarArchivosView".equalsIgnoreCase(p != null ? p.trim() : ""));
          boolean canComprobanteEgresos = permisos.stream()
            .anyMatch(p -> "permComprobanteEgresosView".equalsIgnoreCase(p != null ? p.trim() : ""));
          boolean canHistorial = permisos.stream()
            .anyMatch(p -> "permHistorialView".equalsIgnoreCase(p != null ? p.trim() : ""));
          boolean canPerfil = permisos.stream()
            .anyMatch(p -> "permPerfilView".equalsIgnoreCase(p != null ? p.trim() : ""));

          if (canImportFiles) {
            response.sendRedirect("/home");
          } else if (canComprobanteEgresos) {
            response.sendRedirect("/egresos");
          } else if (canHistorial) {
            response.sendRedirect("/historial");
          } else if (canPerfil) {
            response.sendRedirect("/configuracion/perfil");
          } else {
            response.sendRedirect("/login?sinpermisos");
          }
        })
        .permitAll()
      )
      .logout(logout -> logout
        .logoutUrl("/logout")
        .logoutSuccessUrl("/login?logout")
        .permitAll()
      );

    return http.build();
  }
}
