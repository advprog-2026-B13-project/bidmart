package id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.adapter.input.rest;

import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.SessionClientInfo;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class SessionClientInfoResolver {

    public SessionClientInfo resolve(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        String explicitDevice = request.getHeader("X-Device-Name");
        String device = StringUtils.hasText(explicitDevice) ? explicitDevice : userAgent;

        String ip = extractClientIp(request);
        String location = firstPresent(
                request.getHeader("X-Geo-Country"),
                request.getHeader("CF-IPCountry"),
                request.getHeader("X-Country-Code"),
                request.getHeader("X-Location")
        );

        return SessionClientInfo.of(device, ip, location);
    }

    private String extractClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwardedFor)) {
            String[] values = forwardedFor.split(",");
            if (values.length > 0 && StringUtils.hasText(values[0])) {
                return values[0].trim();
            }
        }

        String realIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(realIp)) {
            return realIp.trim();
        }

        return request.getRemoteAddr();
    }

    private String firstPresent(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }
}

