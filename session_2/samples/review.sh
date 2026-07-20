#!/usr/bin/env bash
# Demo helper: send the ugly sample code to /review
curl -s -X POST localhost:9090/review \
  -H "Content-Type: text/plain" \
  --data-binary @"$(dirname "$0")/bad-code.java" | python3 -m json.tool
