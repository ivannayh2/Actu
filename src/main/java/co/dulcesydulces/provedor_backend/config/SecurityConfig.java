package co.dulcesydulces.provedor_backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
      .csrf(csrf -> csrf.disable())
      .authorizeHttpRequests(auth -> auth
    .requestMatchers("/css/**", "/js/**", "/JS/**", "/img/**", "/favicon.ico").permitAll()
    .requestMatchers("/login", "/error").permitAll()
    .requestMatchers("/usuarios/**").hasAnyAuthority("ADMINISTRADOR", "PUBLICADOR")
    .anyRequest().authenticated()
)

      .formLogin(form -> form
        .loginPage("/login")
        .loginProcessingUrl("/login")  
        .usernameParameter("codigo")
        .passwordParameter("clave")
        .failureUrl("/login?error")
        .successHandler((request, response, authentication) -> {
          boolean isProveedor = authentication.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("PROVEEDORES"));
          if (isProveedor) {
            response.sendRedirect("/egresos");
          } else {
            response.sendRedirect("/home");
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
