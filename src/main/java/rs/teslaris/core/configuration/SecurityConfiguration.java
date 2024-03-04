package rs.teslaris.core.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import rs.teslaris.core.util.exceptionhandling.RestAuthenticationEntryPoint;
import rs.teslaris.core.util.jwt.JwtFilter;

@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true, securedEnabled = true, jsr250Enabled = true)
public class SecurityConfiguration {

    private final JwtFilter jwtTokenFilter;

    private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;

    @Autowired
    public SecurityConfiguration(JwtFilter jwtTokenFilter,
                                 RestAuthenticationEntryPoint restAuthenticationEntryPoint) {
        this.jwtTokenFilter = jwtTokenFilter;
        this.restAuthenticationEntryPoint = restAuthenticationEntryPoint;
    }

    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http,
                                                       BCryptPasswordEncoder bCryptPasswordEncoder,
                                                       UserDetailsService userDetailService)
        throws Exception {
        return http.getSharedObject(AuthenticationManagerBuilder.class)
            .userDetailsService(userDetailService)
            .passwordEncoder(bCryptPasswordEncoder)
            .and()
            .build();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http = http.cors().and().csrf().disable();

        http =
            http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS).and();

        http.exceptionHandling().authenticationEntryPoint(restAuthenticationEntryPoint);

        http.authorizeRequests()

            // PERMIT FETCHING OF STATIC FILES
            .antMatchers("/css/**", "/js/**", "/img/**", "/lib/**", "/favicon.ico").permitAll()

            // BASIC ENDPOINT CONFIGURATION

            // USER
            .antMatchers(HttpMethod.GET, "/api/user/person/{personId}").permitAll()
            .antMatchers(HttpMethod.POST, "/api/user/authenticate").permitAll()
            .antMatchers(HttpMethod.POST, "/api/user/refresh-token").permitAll()
            .antMatchers(HttpMethod.POST, "/api/user/forgot-password").permitAll()
            .antMatchers(HttpMethod.POST, "/api/user/register-researcher").permitAll()
            .antMatchers(HttpMethod.PATCH, "/api/user/reset-password").permitAll()
            .antMatchers(HttpMethod.PATCH, "/api/user/activate-account").permitAll()

            // PERSON
            .antMatchers(HttpMethod.GET, "/api/person/simple-search").permitAll()
            .antMatchers(HttpMethod.GET, "/api/person/count").permitAll()
            .antMatchers(HttpMethod.GET, "/api/person/{personId}").permitAll()

            // COUNTRY
            .antMatchers(HttpMethod.GET, "/api/country/{countryId}").permitAll()
            .antMatchers(HttpMethod.GET, "/api/person/{personId}/person-user").permitAll()

            // ORGANISATION UNIT
            .antMatchers(HttpMethod.GET, "/api/organisation-unit/count").permitAll()
            .antMatchers(HttpMethod.GET, "/api/organisation-unit/simple-search").permitAll()

            // LANGUAGE
            .antMatchers(HttpMethod.GET, "/api/language").permitAll()
            .antMatchers(HttpMethod.GET, "/api/language/tags").permitAll()

            // DOCUMENT
            .antMatchers(HttpMethod.GET, "/api/document/count").permitAll()
            .antMatchers(HttpMethod.GET, "/api/document/simple-search").permitAll()
            .antMatchers(HttpMethod.GET, "/api/document/for-researcher/{personId}").permitAll()

            // PROCEEDINGS
            .antMatchers(HttpMethod.GET, "/api/proceedings/for-event/{eventId}").permitAll()

            // PUBLICATION
            .antMatchers(HttpMethod.GET,
                "/api/journal-publication/journal/{journalId}/my-publications").permitAll()
            .antMatchers(HttpMethod.GET,
                "/api/proceedings-publication/event/{eventId}/my-publications").permitAll()

            // INVOLVEMENT
            .antMatchers(HttpMethod.GET,
                "/api/involvement/employment/{employmentId}").permitAll()
            .antMatchers(HttpMethod.GET,
                "/api/involvement/education/{educationId}").permitAll()
            .antMatchers(HttpMethod.GET,
                "/api/involvement/membership/{membershipId}").permitAll()

            // FILE
            .antMatchers(HttpMethod.GET,
                "/api/file/{serverFilename}").permitAll()

            // IMPORTER
            .antMatchers(HttpMethod.GET, "/api/import/harvest").permitAll()
            .antMatchers(HttpMethod.GET, "/api/import/load").permitAll()
            .antMatchers(HttpMethod.GET, "/api/import/load-wizard/**").permitAll()
            .anyRequest().fullyAuthenticated();

        http.headers().xssProtection().and().contentSecurityPolicy("script-src 'self'");

        http.addFilterBefore(jwtTokenFilter, BasicAuthenticationFilter.class);

        return http.build();
    }
}
