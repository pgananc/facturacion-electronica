package com.wallparisoft.ebill.auth.util;


import com.wallparisoft.ebill.auth.util.service.IAuthService;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;

@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true, securedEnabled = true, jsr250Enabled = true)
public class SecurityConfig {

    final IAuthService authService;

    public SecurityConfig(IAuthService authService) {
        this.authService = authService;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf()
                .disable()
                .addFilterAt(this::authenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .authorizeRequests()
                .antMatchers("/api/login/**")
                .permitAll()
                .antMatchers("/api/mail/**")
                .permitAll()
                .antMatchers("/api/token/**")
                .permitAll()
                .antMatchers("/api/user/restore/**")
                .permitAll()
                .antMatchers("/api/menu/**")
                .authenticated()
                .antMatchers("/api/role/**")
                .authenticated()
                .antMatchers("/api/user/**")
                .authenticated()
                .antMatchers("/api/client/**")
                .authenticated()
                .antMatchers("/api/product/**")
                .authenticated()
                .and()
                .httpBasic()
                .and()
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .exceptionHandling(conf -> {
                    conf.authenticationEntryPoint(this::authenticationFailedHandler);
                });

        return http.build();
    }



    private void authenticationFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        Optional<Authentication> authentication = this.authenticate((HttpServletRequest) request);
        authentication.ifPresent(SecurityContextHolder.getContext()::setAuthentication);
        chain.doFilter(request, response);
    }

    private Optional<Authentication> authenticate(HttpServletRequest request) {
        Optional<Authentication> authentication = authService.authenticate(request);
        if (authentication.isPresent()) {
            return authentication;
        }
        return Optional.empty();
    }

    private void authenticationFailedHandler(HttpServletRequest request, HttpServletResponse response,
                                             AuthenticationException authException) {
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Basic");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }

}
