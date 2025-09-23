package rs.teslaris.core.util.session;

import java.util.Objects;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import nl.basjes.parse.useragent.UserAgentAnalyzer;
import rs.teslaris.core.util.functional.Pair;

@Slf4j
public class YauaaBotDetector {

    private static final UserAgentAnalyzer uaa;

    // Limit that covers 99.9% of legitimate user agents
    private static final int MAX_SAFE_USER_AGENT_LENGTH = 2048;

    // Absolute upper limit to prevent abuse and spam
    private static final int MAX_ABSOLUTE_USER_AGENT_LENGTH = 8192;

    private static final Pattern MALICIOUS_PATTERN = Pattern.compile("[\r\n\\x00-\\x1F\\x7F]");


    static {
        uaa = UserAgentAnalyzer.newBuilder()
            .hideMatcherLoadStats()
            .withCache(10000)
            .withField("DeviceClass")
            .withField("AgentName")
            .withField("AgentVersion")
            .withField("OperatingSystemName")
            .withField("DeviceBrand")
            .withField("DeviceName")
            .build();
    }

    public static boolean isBot(String userAgent) {
        if (Objects.isNull(userAgent) || userAgent.trim().isEmpty() || userAgent.equals("N/A")) {
            return false;
        }

        var agent = uaa.parse(userAgent);
        String deviceClass = agent.getValue("DeviceClass");

        if ("Robot".equals(deviceClass) || "Robot Mobile".equals(deviceClass)) {
            return true;
        }

        String agentName = agent.getValue("AgentName");
        return Objects.nonNull(agentName) &&
            (agentName.toLowerCase().contains("bot") ||
                agentName.toLowerCase().contains("crawler") ||
                agentName.toLowerCase().contains("spider"));
    }

    public static Pair<String, String> getDeviceClassAndOS(String userAgent) {
        if (Objects.isNull(userAgent) || userAgent.trim().isEmpty() || userAgent.equals("N/A")) {
            return new Pair<>(null, null);
        }

        var agent = uaa.parse(userAgent);
        return new Pair<>(agent.getValue("DeviceClass"), agent.getValue("OperatingSystemName"));
    }

    public static boolean isValidUserAgent(String userAgent) {
        if (Objects.isNull(userAgent)) {
            return false;
        }

        int length = userAgent.length();

        if (length > MAX_ABSOLUTE_USER_AGENT_LENGTH) {
            log.warn("Malicious User-Agent detected: {} characters. From: {}. Sanitized: {}",
                length, SessionUtil.getCurrentClientIP(), sanitizeForLog(userAgent));
            return false;
        }

        if (length > MAX_SAFE_USER_AGENT_LENGTH) {
            log.warn("Unusually long User-Agent detected: {} characters. From: {}. Sanitized: {}",
                length, SessionUtil.getCurrentClientIP(), sanitizeForLog(userAgent));
            return false;
        }

        if (containsMaliciousContent(userAgent)) {
            log.warn("Malicious content in User-Agent. Length: {} characters. From: {}. Type: {}",
                length, SessionUtil.getCurrentClientIP(), detectMaliciousType(userAgent));
            return false;
        }

        return true;
    }

    private static String sanitizeForLog(String input) {
        if (Objects.isNull(input)) {
            return "N/A";
        }

        String truncated = input.length() > 100 ? input.substring(0, 100) + "..." : input;
        truncated = MALICIOUS_PATTERN.matcher(truncated).replaceAll("?");

        return truncated.replace("%", "%%"); // For log formatting
    }

    private static boolean containsMaliciousContent(String userAgent) {
        return MALICIOUS_PATTERN.matcher(userAgent).find() ||
            userAgent.contains("${") || // Potential log injection
            userAgent.contains("<!--") || // HTML injection
            userAgent.toLowerCase().contains("script") || // XSS
            userAgent.contains("' OR '1'='1"); // SQL injection pattern
    }

    private static String detectMaliciousType(String userAgent) {
        if (MALICIOUS_PATTERN.matcher(userAgent).find()) {
            return "control-characters";
        }

        if (userAgent.contains("${")) {
            return "log-injection";
        }

        if (userAgent.contains("<!--") || userAgent.toLowerCase().contains("script")) {
            return "xss";
        }

        if (userAgent.contains("' OR '1'='1")) {
            return "sql-injection";
        }

        return "suspicious-pattern";
    }
}
