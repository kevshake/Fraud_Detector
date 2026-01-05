package com.posgateway.aml.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

import com.posgateway.aml.service.auth.CustomUserDetailsService;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "spring.security.enabled", havingValue = "true", matchIfMissing = true)
public class SecurityConfig {

        private final CustomUserDetailsService userDetailsService;
        private final CustomAuthenticationFailureHandler failureHandler;

        public SecurityConfig(CustomUserDetailsService userDetailsService,
                        CustomAuthenticationFailureHandler failureHandler) {
                this.userDetailsService = userDetailsService;
                this.failureHandler = failureHandler;
        }

        // ...

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

        @Bean
        public AuthenticationProvider authenticationProvider() {
                DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
                authProvider.setUserDetailsService(userDetailsService);
                authProvider.setPasswordEncoder(passwordEncoder());
                return authProvider;
        }

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                http
                                .csrf(csrf -> csrf
                                                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                                                .ignoringRequestMatchers("/actuator/**", "/api/v1/merchants/health",
                                                                "/api/v1/*/health", "/api/v1/pricing/**",
                                                                "/perform_login", "/api/v1/perform_login"))
                                .authenticationProvider(authenticationProvider())
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers("/api/v1/merchants/onboard").permitAll()
                                                .requestMatchers("/api/v1/merchants/health").permitAll()
                                                .requestMatchers("/api/v1/pricing/**").permitAll()
                                                .requestMatchers("/actuator/**").permitAll()
                                                // Role-based access control examples
                                                // Role-based access control
                                                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                                                .requestMatchers("/api/v1/users/**").hasAnyRole("ADMIN", "MANAGE_USERS")
                                                .requestMatchers("/api/v1/roles/**").hasAnyRole("ADMIN", "MANAGE_ROLES")
                                                .requestMatchers("/api/v1/auth/login", "/api/v1/psps/auth/login")
                                                .permitAll()
                                                .requestMatchers("/api/v1/auth/csrf").permitAll()
                                                .requestMatchers("/api/v1/auth/session/check",
                                                                "/api/v1/auth/session/refresh")
                                                .authenticated()
                                                .requestMatchers("/api/v1/auth/**").permitAll()
                                                .requestMatchers("/api/v1/psps/register").permitAll() // Allow initial
                                                                                                      // registration if
                                                                                                      // public
                                                .requestMatchers("/api/v1/cases/**")
                                                .hasAnyRole("COMPLIANCE_OFFICER", "ADMIN", "PSP_ADMIN", "PSP_USER")
                                                .requestMatchers("/api/v1/psps/**")
                                                .hasAnyRole("ADMIN", "PSP_ADMIN", "PSP_USER", "APP_CONTROLLER")
                                                .requestMatchers("/api/v1/merchants/**").authenticated() // Detailed
                                                                                                         // control via
                                                                                                         // @PreAuthorize

                                                // Static resources
                                                .requestMatchers("/css/**", "/js/**", "/images/**", "/error")
                                                .permitAll()
                                                .requestMatchers("/logout-success.html").permitAll()
                                                .requestMatchers("/", "/index.html").permitAll() // Login page should be
                                                                                                 // public usually, but
                                                                                                 // here we use index as
                                                                                                 // app.
                                                // Assuming index.html is the protected app, checking authentication
                                                // would be
                                                // handled by filter or it redirects to login.
                                                // For basic auth, we can just require auth for root.
                                                .anyRequest().authenticated()) // Secure by default
                                .formLogin(login -> login
                                                .loginPage("/login.html")
                                                .loginProcessingUrl("/perform_login")
                                                .defaultSuccessUrl("/index.html", true)
                                                .failureHandler(failureHandler)
                                                .permitAll())
                                .logout(logout -> logout
                                                .logoutUrl("/logout")
                                                .logoutSuccessUrl("/logout-success.html")
                                                .deleteCookies("JSESSIONID")
                                                .invalidateHttpSession(true)
                                                .clearAuthentication(true)
                                                .permitAll())
                                .sessionManagement(session -> {
                                        session.maximumSessions(1)
                                                        .maxSessionsPreventsLogin(false)
                                                        .expiredUrl("/login.html?expired=true");
                                        session.sessionCreationPolicy(
                                                        org.springframework.security.config.http.SessionCreationPolicy.IF_REQUIRED);
                                        session.invalidSessionUrl("/login.html?invalid=true");
                                        session.sessionFixation().migrateSession();
                                });
                // HTTP Basic Auth disabled - using form-based login only
                // .httpBasic(basic -> {
                // });

                return http.build();
        }
}
