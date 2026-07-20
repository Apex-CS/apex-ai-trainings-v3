# AI Training — Topic 2: Core Java AI Frameworks
### Spring AI · LangChain4j · Micrometer/OpenTelemetry · Azure OpenAI

## Quick start (no API keys needed)
```bash
ollama pull llama3.2                    # local model
curl http://localhost:11434/api/tags    # smoke test: default Ollama endpoint
docker compose up -d                    # Grafana + OTel backend (from step-5 onward)
cd spring-ai-demo
mvn spring-boot:run                     # default profile = ollama
# tip: generate the wrapper once with: mvn wrapper:wrapper
curl "localhost:9090/ask?q=hello"
```

If Ollama is running on a non-default port, set `OLLAMA_BASE_URL` before starting either demo.

## Provider swap
```bash
SPRING_PROFILES_ACTIVE=openai mvn spring-boot:run   # needs OPENAI_API_KEY
SPRING_PROFILES_ACTIVE=azure  mvn spring-boot:run   # needs AZURE_OPENAI_KEY + AZURE_OPENAI_ENDPOINT
```

