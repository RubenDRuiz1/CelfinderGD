package com.celfinder.Security;

import com.celfinder.Procesos.UsuarioService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/api/**", "/ventas/carrito/**", "/asistente", "/mcp/**") // Permitimos POST en carrito, asistente y MCP sin CSRF
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED) // Permite @SessionScope
            )
            .authorizeHttpRequests(auth -> auth
                // Rutas Públicas
                .requestMatchers("/", "/usuarios/registro", "/usuarios/registrar", "/usuarios/login", "/usuarios/perfil/**", "/resources/**", "/static/**", "/css/**", "/js/**", "/images/**", "/vendor/**", "/favicon.ico").permitAll()
                .requestMatchers("/ventas/listar", "/ventas/buscar", "/ventas/detalle/**", "/ventas/comparar/**", "/ventas/comparar-media/**").permitAll()
                
                // APIs y Recursos específicos para Vendedores/Usuarios
                .requestMatchers("/admin/categorias/api/**").hasAnyRole("VENDEDOR", "ADMIN")

                // Rutas de Usuario Autenticado
                .requestMatchers("/menu", "/usuarios/perfil", "/usuarios/editar-perfil", "/asistente").hasRole("USER")
                .requestMatchers("/ventas/carrito/**", "/admin/solicitar-vendedor").hasRole("USER")
                .requestMatchers("/ventas/historial-compras", "/ventas/historial-solicitudes-vendedor", "/ventas/notificaciones").hasRole("USER")
                .requestMatchers("/ventas/solicitar/**", "/ventas/procesar-compra", "/ventas/reseña/**", "/ventas/seleccionar-comparacion/**").hasRole("USER")
                
                // Rutas de Vendedor / Admin
                .requestMatchers("/ventas/publicar", "/ventas/editar/**", "/ventas/eliminar/**").hasAnyRole("VENDEDOR", "ADMIN")
                .requestMatchers("/ventas/gestionar-solicitudes", "/ventas/gestionar-solicitud/**", "/ventas/historial-ventas").hasAnyRole("VENDEDOR", "ADMIN")
                
                // Rutas del Módulo MCP (Model Context Protocol) - Solo Admin
                .requestMatchers("/mcp/**").hasRole("ADMIN")
                
                // Rutas de Admin
                .requestMatchers("/admin/**").hasRole("ADMIN")
                
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/usuarios/login")
                .loginProcessingUrl("/usuarios/login")
                .defaultSuccessUrl("/menu", false)
                .failureUrl("/usuarios/login?error=true")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/usuarios/login?logout")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            );

        return http.build();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder);
        return authProvider;
    }

    @Bean
    public org.springframework.security.web.context.SecurityContextRepository securityContextRepository() {
        return new org.springframework.security.web.context.HttpSessionSecurityContextRepository();
    }
}