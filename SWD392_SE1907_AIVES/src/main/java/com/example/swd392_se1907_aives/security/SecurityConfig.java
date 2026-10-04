package com.example.swd392_se1907_aives.security;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
@Configuration
@EnableMethodSecurity
@io.swagger.v3.oas.annotations.OpenAPIDefinition(security={@io.swagger.v3.oas.annotations.security.SecurityRequirement(name="bearerAuth")})
@io.swagger.v3.oas.annotations.security.SecurityScheme(name="bearerAuth", type=io.swagger.v3.oas.annotations.enums.SecuritySchemeType.HTTP, scheme="bearer")
@io.swagger.v3.oas.annotations.security.SecurityRequirement(name="bearerAuth")
public class SecurityConfig {
 @Bean org.springframework.security.core.userdetails.UserDetailsService bearerOnlyUsers() {
  return username -> { throw new org.springframework.security.core.userdetails.UsernameNotFoundException("Use /api/auth/login and bearer authentication"); };
 }
 @Bean org.springframework.boot.web.servlet.FilterRegistrationBean<BearerFilter> bearerRegistration(BearerFilter filter) {
  var registration = new org.springframework.boot.web.servlet.FilterRegistrationBean<>(filter);
  registration.setEnabled(false); return registration;
 }
 @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
 @Bean SecurityFilterChain security(HttpSecurity http, BearerFilter bearer) throws Exception {
  return http.csrf(c -> c.disable()).sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
   .authorizeHttpRequests(a -> a.requestMatchers("/api/auth/login","/api/auth/register","/api/auth/google","/api/auth/google/config","/api/auth/google/challenge","/swagger-ui/**","/swagger-ui.html","/v3/api-docs/**","/error").permitAll().anyRequest().authenticated())
   .exceptionHandling(e -> e.authenticationEntryPoint((req,res,ex) -> res.sendError(401))
     .accessDeniedHandler((req,res,ex) -> res.sendError(403)))
   .addFilterBefore(bearer, UsernamePasswordAuthenticationFilter.class).build();
 }
}
