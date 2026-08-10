package com.owasp.aiassistant.mlflow;

import com.owasp.aiassistant.corporate.auth.DemoUserRoles;
import com.owasp.aiassistant.corporate.config.CorporateApiProperties;
import com.owasp.aiassistant.corporate.enums.DemoUser;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public final class MlflowDemoUserTags {

    public static final String TAG_USER_NAME = "user name";
    public static final String TAG_USER_ROLES = "user roles";

    private MlflowDemoUserTags() {
    }

    public static DemoUser resolveDemoUser(DemoUser demoUser, CorporateApiProperties corporateApiProperties) {
        if (demoUser != null) {
            return demoUser;
        }
        if (corporateApiProperties != null && corporateApiProperties.defaultDemoUser() != null) {
            return corporateApiProperties.defaultDemoUser();
        }
        return DemoUser.BART_PEREZ;
    }

    public static Map<String, String> forUser(DemoUser demoUser) {
        Map<String, String> tags = new LinkedHashMap<>();
        tags.put(TAG_USER_NAME, demoUser.displayName());
        tags.put(
                TAG_USER_ROLES,
                DemoUserRoles.rolesFor(demoUser).stream().collect(Collectors.joining(",")));
        return tags;
    }
}
