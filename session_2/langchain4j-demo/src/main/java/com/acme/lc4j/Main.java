package com.acme.lc4j;

import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.service.AiServices;

public class Main {

    private static final String DEFAULT_OLLAMA_BASE_URL = "http://127.0.0.1:11434";

    public static void main(String[] args) {

        // 1. Low-level portable abstraction (same idea as Spring AI's ChatModel).
        //    Swap OllamaChatModel -> AzureOpenAiChatModel: the variable type doesn't change.
        ChatModel model = OllamaChatModel.builder()
            .baseUrl(System.getenv().getOrDefault("OLLAMA_BASE_URL", DEFAULT_OLLAMA_BASE_URL))
                .modelName("llama3.2")
                .temperature(0.2)
                .build();

        // 2. High-level declarative style: AI Services
        ReviewAssistant assistant = AiServices.builder(ReviewAssistant.class)
                .chatModel(model)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                .build();

        String uglyCode = """
                public class UserDao {
                    static Connection conn;
                    public List<String> getUsers(String name) throws Exception {
                        conn = DriverManager.getConnection("jdbc:mysql://prod-db/users", "root", "root123");
                        ResultSet rs = conn.createStatement()
                            .executeQuery("SELECT * FROM users WHERE name = '" + name + "'");
                        List<String> l = new ArrayList();
                        while (rs.next()) l.add(rs.getString(1));
                        return l;
                    }
                }
                """;

        CodeReview review = assistant.review(uglyCode);   // typed result, zero JSON parsing
        System.out.println("== Structured review ==");
        System.out.println(review);

        System.out.println("\n== Commit message ==");
        System.out.println(assistant.commitMessage("- fixed SQL injection in UserDao\n- use try-with-resources"));
    }
}
