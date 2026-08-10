#!/usr/bin/env bash
set -euo pipefail

ollama serve &
SERVER_PID=$!


cat "/usr/local/share/ca-certificates/Apex Zscaler Root CA.crt"

update-ca-certificates

CHAT_MODEL="${OLLAMA_CHAT_MODEL:-llama3.2}"
EMBEDDING_MODEL="${OLLAMA_EMBEDDING_MODEL:-nomic-embed-text}"
MAX_ATTEMPTS=60


echo "Waiting for Ollama to start..."
attempt=0

until ollama list >/dev/null 2>&1; do
  attempt=$((attempt+1))

  if ["${attempt}" -ge "${MAX_ATTEMPTS}"]; then
    echo "Error. Maximum attempts reached " >&2
    exit 1
  fi

  echo "Attempt ${attempt} of ${MAX_ATTEMPTS}: Ollama is not ready, retying in 2 seconds..."
  sleep 2
done


echo "Ollama Api is up"

echo "Pulling chat model yeah!: ${CHAT_MODEL}"

if ! ollama pull "${CHAT_MODEL}"; then
  echo "Error: failed to pull chat model ${CHAT_MODEL}" >&2
  exit 1
fi

if ! ollama pull "${EMBEDDING_MODEL}"; then
  echo "Error: failed to pull embedding model ${EMBEDDING_MODEL}" >&2
  exit 1
fi


echo "Ollama is ready, installed models yeah!!!"
wait "$SERVER_PID"