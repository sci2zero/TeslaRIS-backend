package rs.teslaris.core.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
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
@EnableMethodSecurity(securedEnabled = true, jsr250Enabled = true)
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
                .requestMatchers(HttpMethod.PATCH, "/api/user/confirm-email-change").permitAll()

                // PERSON
                .requestMatchers(HttpMethod.GET, "/api/person/simple-search").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/person/count").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/person/{personId}").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/person/old-id/{personOldId}").permitAll()

                // COUNTRY
                .requestMatchers(HttpMethod.GET, "/api/country/{countryId}").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/person/{personId}/person-user").permitAll()

                // ORGANISATION UNIT
                .requestMatchers(HttpMethod.GET, "/api/organisation-unit/count").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/organisation-unit/{organisationUnitId}")
                .permitAll()
                .requestMatchers(HttpMethod.GET,
                    "/api/organisation-unit/sub-units/{organisationUnitId}")
                .permitAll()
                .requestMatchers(HttpMethod.GET,
                    "/api/organisation-unit/old-id/{organisationUnitOldId}")
                .permitAll()
                .requestMatchers(HttpMethod.GET, "/api/organisation-unit/simple-search").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/organisation-unit-relation/{leafId}")
                .permitAll()

                // LANGUAGE
                .requestMatchers(HttpMethod.GET, "/api/language").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/language/ui-languages").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/language/tags").permitAll()

                // DOCUMENT
                .requestMatchers(HttpMethod.GET, "/api/document/{documentId}").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/document/count").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/document/{documentId}/cite").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/document/simple-search").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/document/for-researcher/{personId}")
                .permitAll()
                .requestMatchers(HttpMethod.GET, "/api/document/for-publisher/{publisherId}")
                .permitAll()
                .requestMatchers(HttpMethod.GET, "/api/document/wordcloud/{documentId}").permitAll()
                .requestMatchers(HttpMethod.GET,
                    "/api/document/for-organisation-unit/{organisationUnitId}").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/book-series/publications/{bookSeriesId}")
                .permitAll()

                // PROCEEDINGS
                .requestMatchers(HttpMethod.GET, "/api/proceedings/for-event/{eventId}").permitAll()

                // PUBLISHERS
                .requestMatchers(HttpMethod.GET, "/api/publisher/{publisherId}").permitAll()

                // PUBLICATION
                .requestMatchers(HttpMethod.GET, "/api/journal-publication/{documentId}")
                .permitAll()
                .requestMatchers(HttpMethod.GET, "/api/proceedings-publication/{documentId}")
                .permitAll()
                .requestMatchers(HttpMethod.GET, "/api/monograph/{documentId}")
                .permitAll()
                .requestMatchers(HttpMethod.GET, "/api/monograph-publication/{documentId}")
                .permitAll()
                .requestMatchers(HttpMethod.GET,
                    "/api/journal-publication/journal/{journalId}").permitAll()
                .requestMatchers(HttpMethod.GET,
                    "/api/proceedings-publication/event/{eventId}").permitAll()
                .requestMatchers(HttpMethod.GET,
                    "/api/monograph-publication/monograph/{monographId}").permitAll()
                .requestMatchers(HttpMethod.GET,
                    "/api/proceedings-publication/event/{eventId}/my-publications").permitAll()
                .requestMatchers(HttpMethod.GET,
                    "/api/proceedings-publication/proceedings/{proceedingsId}").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/software/{documentId}").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/dataset/{documentId}").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/patent/{documentId}").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/monograph/{documentId}").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/journal/{documentId}").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/book-series/{documentId}").permitAll()
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

                // EVENTS
                .requestMatchers(HttpMethod.GET, "/api/event/{eventId}").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/conference/{conference}").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/conference/simple-search").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/conference/old-id/{oldConferenceId}")
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
                .requestMatchers(HttpMethod.GET, "/api/research-area/children/{parentId}")
                .permitAll()

                // FILE
                .requestMatchers(HttpMethod.GET, "/api/file/{serverFilename}").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/file/logo/{organisationUnitId}").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/file/image/{personId}").permitAll()

                // ERROR
                .requestMatchers("/error").permitAll()

                // EXPORT
                .requestMatchers(HttpMethod.GET, "/api/export/{handlerName}").permitAll()

                // SEARCH TABLE EXPORT
                .requestMatchers(HttpMethod.GET, "/api/csv-export/records-per-page").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/csv-export/documents").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/csv-export/persons").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/csv-export/organisation-units").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/thesis-library/csv-export").permitAll()

                // ASSESSMENT
                .requestMatchers(HttpMethod.GET, "/api/assessment/document-indicator/{documentId}")
                .permitAll()
                .requestMatchers(HttpMethod.GET, "/api/assessment/person-indicator/{personId}")
                .permitAll()
                .requestMatchers(HttpMethod.GET, "/api/assessment/event-indicator/{eventId}")
                .permitAll()
                .requestMatchers(HttpMethod.GET,
                    "/api/assessment/organisation-unit-indicator/{organisationUnitId}")
                .permitAll()
                .requestMatchers(HttpMethod.GET,
                    "/api/assessment/publication-series-indicator/{publicationSeriesId}")
                .permitAll()
                .requestMatchers(HttpMethod.GET,
                    "/api/assessment/person-assessment-classification/assess/{personId}")
                .permitAll()
                .requestMatchers(HttpMethod.GET, "/api/assessment/research-area").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/assessment/commission").permitAll()
                .requestMatchers(HttpMethod.GET,
                    "/api/external-indicator-configuration/document/{documentId}").permitAll()

                // STATISTICS
                .requestMatchers(HttpMethod.GET, "/api/statistics/{statisticsType}").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/statistics/person/{personId}").permitAll()
                .requestMatchers(HttpMethod.POST,
                    "/api/statistics/organisation-unit/{organisationUnitId}").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/statistics/document/{documentId}")
                .permitAll()
                .requestMatchers(HttpMethod.POST,
                    "/api/statistics/publication-series/{publicationSeriesId}")
                .permitAll()
                .requestMatchers(HttpMethod.POST, "/api/statistics/event/{eventId}")
                .permitAll()

                // BRANDING INFORMATION
                .requestMatchers(HttpMethod.GET, "/api/branding").permitAll()

                // PUBLIC ASSESSMENT SERVICE
                // through WEB UI
                .requestMatchers(HttpMethod.POST,
                    "/api/assessment/document-assessment-classification/imaginary-journal-publication")
                .permitAll()
                .requestMatchers(HttpMethod.POST,
                    "/api/assessment/document-assessment-classification/imaginary-proceedings-publication")
                .permitAll()
                // through open API
                .requestMatchers(HttpMethod.POST,
                    "/api/assessment/document-assessment-classification/journal-m-service")
                .permitAll()
                .requestMatchers(HttpMethod.POST,
                    "/api/assessment/document-assessment-classification/conference-m-service")
                .permitAll()

                // CSV EXPORT
                .requestMatchers(HttpMethod.GET, "/api/document/fields").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/person/fields").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/organisation-unit/fields").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/csv-export/documents").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/csv-export/persons").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/csv-export/organisation-units").permitAll()

                // THESIS LIBRARY
                .requestMatchers(HttpMethod.GET, "/api/thesis-library/search/fields").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/thesis-library/search/simple").permitAll()
                .requestMatchers(HttpMethod.POST,
                    "/api/thesis-library/search/wordcloud/{queryType}").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/thesis-library/search/advanced").permitAll()
                .requestMatchers(HttpMethod.PATCH,
                    "/api/registry-book/cancel-attendance/{attendanceId}").permitAll()
                .requestMatchers(HttpMethod.GET,
                    "/api/registry-book/is-attendance-cancellable/{registryBookEntryId}")
                .permitAll()

                // COOKIES
                .requestMatchers(HttpMethod.PATCH, "/api/cookie").permitAll()

                // LEGACY NAVIGATION
                .requestMatchers(HttpMethod.GET,
                    "/api/legacy-navigation/entity-landing-page/{oldId}").permitAll()
                .requestMatchers(HttpMethod.GET,
                    "/api/legacy-navigation/document-file/{oldServerFilename}").permitAll()

                // HEALTH CHECK
                .requestMatchers(HttpMethod.GET, "/api/health-check/version").permitAll()

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
