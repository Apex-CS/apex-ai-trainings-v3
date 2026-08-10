package com.owasp.aiassistant.corporate.auth;

import com.owasp.aiassistant.corporate.enums.DemoUser;
import org.springframework.stereotype.Component;

@Component
public class CorporateApiAuthContext {

    private final ThreadLocal<DemoUser> demoUser = new ThreadLocal<>();

    public void set(DemoUser user) {
        demoUser.set(user);
    }

    public DemoUser get() {
        return demoUser.get();
    }

    public void clear() {
        demoUser.remove();
    }
}
