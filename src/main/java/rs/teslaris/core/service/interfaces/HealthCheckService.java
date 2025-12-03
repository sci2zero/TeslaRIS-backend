package rs.teslaris.core.service.interfaces;

import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public interface HealthCheckService {

    Map<String, String> checkPostgres();

    Map<String, String> checkElasticsearch();

    Map<String, String> checkMongo();

    Map<String, String> checkMinio();

    Map<String, String> checkMail();
}
