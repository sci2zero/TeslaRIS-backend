package rs.teslaris.assessment.util;

import com.maxmind.geoip2.DatabaseReader;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Objects;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class GeoliteIPUtil {

    private DatabaseReader reader;

    @PostConstruct
    public void init() throws IOException {
        var resource = new ClassPathResource("geolite/GeoLite2-Country.mmdb");
        try (var inputStream = resource.getInputStream()) {
            reader = new DatabaseReader.Builder(inputStream).build();
        }
    }

    public String getCountry(String ipAddress) {
        if (ipAddress.equals("N/A")) {
            return "N/A";
        }

        try {
            var ip = InetAddress.getByName(ipAddress);
            var response = reader.country(ip);
            var country = response.getCountry();
            return country.getName();
        } catch (Exception e) {
            return "N/A";
        }
    }

    public String getCountryCode(String ipAddress) {
        if (ipAddress.equals("N/A")) {
            return "N/A";
        }

        try {
            var ip = InetAddress.getByName(ipAddress);
            var response = reader.country(ip);
            var country = response.getCountry();
            return country.getIsoCode();
        } catch (Exception e) {
            return "N/A";
        }
    }

    @PreDestroy
    public void cleanup() {
        if (Objects.nonNull(reader)) {
            try {
                reader.close();
            } catch (IOException e) {
                log.error("Error closing GeoIP database: " + e.getMessage());
            }
        }
    }
}
