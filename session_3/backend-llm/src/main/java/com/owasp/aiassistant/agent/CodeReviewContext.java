package com.owasp.aiassistant.agent;

import com.owasp.aiassistant.dto.CodeAttachment;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class CodeReviewContext {

    private final ThreadLocal<CodeAttachment> attachment = new ThreadLocal<>();

    public void set(CodeAttachment codeAttachment) {
        attachment.set(codeAttachment);
    }

    public Optional<CodeAttachment> get() {
        return Optional.ofNullable(attachment.get());
    }

    public void clear() {
        attachment.remove();
    }
}
