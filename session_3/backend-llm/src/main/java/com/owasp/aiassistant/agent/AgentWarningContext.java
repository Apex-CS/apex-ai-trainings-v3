package com.owasp.aiassistant.agent;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class AgentWarningContext {

    private final ThreadLocal<List<String>> warnings = ThreadLocal.withInitial(ArrayList::new);

    public void add(String warning) {
        if (warning != null && !warning.isBlank()) {
            warnings.get().add(warning);
        }
    }

    public List<String> drain() {
        List<String> collected = List.copyOf(warnings.get());
        warnings.remove();
        return collected;
    }

    public void clear() {
        warnings.remove();
    }

    public List<String> peek() {
        return Collections.unmodifiableList(warnings.get());
    }
}
