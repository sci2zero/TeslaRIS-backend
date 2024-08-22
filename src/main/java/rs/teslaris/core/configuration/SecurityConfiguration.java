package rs.teslaris.core.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter;
import rs.teslaris.core.util.exceptionhandling.RestAuthenticationEntryPoint;
import rs.teslaris.core.util.jwt.JwtFilter;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true, securedEnabled = true, jsr250Enabled = true)
public class SecurityConfiguration {

    private final JwtFilter jwtTokenFilter;
    private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;

    public SecurityConfiguration(JwtFilter jwtTokenFilter,
                                 RestAuthenticationEntryPoint restAuthenticationEntryPoint) {
        this.jwtTokenFilter = jwtTokenFilter;
        this.restAuthenticationEntryPoint = restAuthenticationEntryPoint;
    }

    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http,
                                                       BCryptPasswordEncoder bCryptPasswordEncoder,
                                                       UserDetailsService userDetailsService)
        throws Exception {
        AuthenticationManagerBuilder authenticationManagerBuilder =
            http.getSharedObject(AuthenticationManagerBuilder.class);
        authenticationManagerBuilder
            .userDetailsService(userDetailsService)
            .passwordEncoder(bCryptPasswordEncoder);
        return authenticationManagerBuilder.build();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(
                session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(
                exception -> exception.authenticationEntryPoint(restAuthenticationEntryPoint))
            .authorizeHttpRequests(authorize -> authorize

                // PERMIT FETCHING OF STATIC FILES
                .requestMatchers("/css/**", "/js/**", "/img/**", "/lib/**", "/favicon.ico")
                .permitAll()

                // BASIC ENDPOINT CONFIGURATION

                // USER
                .requestMatchers(HttpMethod.GET, "/api/user/person/{personId}").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/user/authenticate").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/user/refresh-token").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/user/forgot-password").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/user/register-researcher").permitAll()
                .requestMatchers(HttpMethod.PATCH, "/api/user/reset-password").permitAll()
                .requestMatchers(HttpMethod.PATCH, "/api/user/activate-account").permitAll()

                // PERSON
                .requestMatchers(HttpMethod.GET, "/api/person/simple-search").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/person/count").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/person/{personId}").permitAll()

                // COUNTRY
                .requestMatchers(HttpMethod.GET, "/api/country/{countryId}").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/person/{personId}/person-user").permitAll()

                // ORGANISATION UNIT
                .requestMatchers(HttpMethod.GET, "/api/organisation-unit/count").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/organisation-unit/{organisationUnitId}")
                .permitAll()
                .requestMatchers(HttpMethod.GET, "/api/organisation-unit/simple-search").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/organisation-unit-relation/{leafId}")
                .permitAll()

                // LANGUAGE
                .requestMatchers(HttpMethod.GET, "/api/language").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/language/tags").permitAll()

                // DOCUMENT
                .requestMatchers(HttpMethod.GET, "/api/document/count").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/document/simple-search").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/document/for-researcher/{personId}")
                .permitAll()
                .requestMatchers(HttpMethod.GET, "/api/document/for-publisher/{publisherId}")
                .permitAll()
                .requestMatchers(HttpMethod.GET,
                    "/api/document/for-organisation-unit/{organisationUnitId}").permitAll()

                // PROCEEDINGS
                .requestMatchers(HttpMethod.GET, "/api/proceedings/for-event/{eventId}").permitAll()

                // PUBLISHERS
                .requestMatchers(HttpMethod.GET, "/api/publisher/{publisherId}").permitAll()

                // PUBLICATION
                .requestMatchers(HttpMethod.GET, "/api/journal-publication/{documentId}")
                .permitAll()
                .requestMatchers(HttpMethod.GET, "/api/proceedings-publication/{documentId}")
                .permitAll()
                .requestMatchers(HttpMethod.GET, "/api/monograph-publication/{documentId}")
                .permitAll()
                .requestMatchers(HttpMethod.GET,
                    "/api/journal-publication/journal/{journalId}").permitAll()
                .requestMatchers(HttpMethod.GET,
                    "/api/proceedings-publication/event/{eventId}").permitAll()
                .requestMatchers(HttpMethod.GET,
                    "/api/monograph-publication/monograph/{monographId}").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/software/{documentId}").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/dataset/{documentId}").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/patent/{documentId}").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/monograph/{documentId}").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/journal/{documentId}").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/proceedings/{documentId}").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/thesis/{documentId}").permitAll()

                // INVOLVEMENT
                .requestMatchers(HttpMethod.GET, "/api/involvement/employment/{employmentId}")
                .permitAll()
                .requestMatchers(HttpMethod.GET, "/api/involvement/education/{educationId}")
                .permitAll()
                .requestMatchers(HttpMethod.GET, "/api/involvement/membership/{membershipId}")
                .permitAll()
                .requestMatchers(HttpMethod.GET, "/api/person/employed-at/{organisationUnitId}")
                .permitAll()

                // EVENTS RELATION
                .requestMatchers(HttpMethod.GET, "/api/events-relation/{eventId}").permitAll()
                .requestMatchers(HttpMethod.GET,
                    "/api/events-relation/serial-event/{serialEventId}").permitAll()

                // ORGANISATION UNIT RELATION
                .requestMatchers(HttpMethod.GET,
                    "/api/organisation-unit-relation/get-all/{sourceId}").permitAll()

                // RESEARCH AREA
                .requestMatchers(HttpMethod.GET, "/api/research-area").permitAll()

                // FILE
                .requestMatchers(HttpMethod.GET, "/api/file/{serverFilename}").permitAll()

                // ERROR
                .requestMatchers("/error").permitAll()

                // EXPORT
                .requestMatchers(HttpMethod.GET, "/OAIHandlerOpenAIRECRIS").permitAll()

                // EVERYTHING ELSE
                .anyRequest().authenticated()
            );

        http.headers(headers ->
            headers.xssProtection(
                xss -> xss.headerValue(XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK)
            ).contentSecurityPolicy(
                cps -> cps.policyDirectives("script-src 'self'")
            ));

        http.addFilterBefore(jwtTokenFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
