
package com.jobready.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Policy for the session cookies set at the browser edge. Only the attributes that
 * legitimately vary by environment live here; {@code httpOnly} is intentionally NOT configurable —
 * it is always {@code true}, since JS-readable session cookies would defeat the whole split-token
 * strategy.
 */
@Data
@ConfigurationProperties(prefix = "auth.cookie")
public class CookieProperties {
    private boolean secure = true;
    private String sameSite = "Strict";
    private String accessName = "jr_access";
    private String refreshName = "jr_refresh";
    private String accessPath = "/";
    private String refreshPath = "/api/v1/auth";
}
