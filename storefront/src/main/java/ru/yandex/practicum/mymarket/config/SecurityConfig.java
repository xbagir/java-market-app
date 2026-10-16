package ru.yandex.practicum.mymarket.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UserDetailsRepositoryReactiveAuthenticationManager;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.header.ClearSiteDataServerHttpHeadersWriter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationEntryPoint;
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationFailureHandler;
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationSuccessHandler;
import org.springframework.security.web.server.authentication.logout.DelegatingServerLogoutHandler;
import org.springframework.security.web.server.authentication.logout.HeaderWriterServerLogoutHandler;
import org.springframework.security.web.server.authentication.logout.RedirectServerLogoutSuccessHandler;
import org.springframework.security.web.server.authentication.logout.SecurityContextServerLogoutHandler;
import org.springframework.security.web.server.context.ServerSecurityContextRepository;
import org.springframework.security.web.server.context.WebSessionServerSecurityContextRepository;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    private static final String[] PUBLIC_PATHS = {
            "/login", "/register", "/health", "/css/**", "/js/**", "/images/**",
            "/favicon.ico", "/error/**"
    };

    private static final String[] PROTECTED_PATHS = {
            "/cart", "/cart/**", "/orders", "/orders/**", "/buy", "/logout"
    };

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public ReactiveAuthenticationManager authenticationManager(
            ReactiveUserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        UserDetailsRepositoryReactiveAuthenticationManager manager =
                new UserDetailsRepositoryReactiveAuthenticationManager(userDetailsService);
        manager.setPasswordEncoder(passwordEncoder);
        return manager;
    }

    @Bean
    public ServerSecurityContextRepository securityContextRepository() {
        return new WebSessionServerSecurityContextRepository();
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(
            ServerHttpSecurity http,
            ReactiveAuthenticationManager authenticationManager,
            ServerSecurityContextRepository securityContextRepository) {
        http
                .securityContextRepository(securityContextRepository)
                .authorizeExchange(exchange -> exchange
                        .pathMatchers(PUBLIC_PATHS).permitAll()
                        .pathMatchers(HttpMethod.GET, "/", "/items", "/items/**").permitAll()
                        .pathMatchers(PROTECTED_PATHS).authenticated()
                        .pathMatchers(HttpMethod.POST, "/items", "/items/**").authenticated()
                        .anyExchange().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .authenticationManager(authenticationManager)
                        .authenticationSuccessHandler(
                                new RedirectServerAuthenticationSuccessHandler("/"))
                        .authenticationFailureHandler(
                                new RedirectServerAuthenticationFailureHandler("/login?error")))
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessHandler(logoutSuccessHandler())
                        .logoutHandler(new DelegatingServerLogoutHandler(
                                new SecurityContextServerLogoutHandler(),
                                new HeaderWriterServerLogoutHandler(
                                        new ClearSiteDataServerHttpHeadersWriter(
                                                ClearSiteDataServerHttpHeadersWriter.Directive.COOKIES,
                                                ClearSiteDataServerHttpHeadersWriter.Directive.STORAGE)))))
                .exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(
                        new RedirectServerAuthenticationEntryPoint("/login")));
        return http.build();
    }

    private RedirectServerLogoutSuccessHandler logoutSuccessHandler() {
        RedirectServerLogoutSuccessHandler handler = new RedirectServerLogoutSuccessHandler();
        handler.setLogoutSuccessUrl(java.net.URI.create("/login?logout"));
        return handler;
    }
}
