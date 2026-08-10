package com.owasp.aiassistant.mlflow;

import com.owasp.aiassistant.corporate.config.CorporateApiProperties;
import com.owasp.aiassistant.corporate.enums.DemoUser;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MlflowDemoUserTagsTest {

    @Test
    void buildsUserNameAndCommaSeparatedRoles() {
        Map<String, String> tags = MlflowDemoUserTags.forUser(DemoUser.FULANO_SMITH);

        assertEquals("Fulano Smith", tags.get(MlflowDemoUserTags.TAG_USER_NAME));
        assertEquals(
                "financial-admin,it-user,marketing-user,sales-user",
                tags.get(MlflowDemoUserTags.TAG_USER_ROLES));
    }

    @Test
    void resolvesDefaultDemoUserWhenRequestOmitsOne() {
        CorporateApiProperties properties = new CorporateApiProperties(
                "http://localhost:8091",
                "http://localhost:8092",
                "http://localhost:8093",
                DemoUser.SUTANO_DOE,
                Map.of());

        DemoUser resolved = MlflowDemoUserTags.resolveDemoUser(null, properties);

        assertEquals(DemoUser.SUTANO_DOE, resolved);
    }
}
