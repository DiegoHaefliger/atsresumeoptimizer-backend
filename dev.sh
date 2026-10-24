#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"

TRIGGER=target/classes/.reloadtrigger
STAMP=target/.dev-stamp
POLL_SECONDS=2

trap 'kill 0' EXIT INT TERM

./mvnw -q compile
touch "$TRIGGER" "$STAMP"

./mvnw spring-boot:run \
	-Dspring-boot.run.jvmArguments="-Dspring.devtools.restart.trigger-file=.reloadtrigger" &
APP_PID=$!

while kill -0 "$APP_PID" 2>/dev/null; do
	sleep "$POLL_SECONDS"
	if [ -n "$(find src -type f -newer "$STAMP" -print -quit)" ]; then
		touch "$STAMP"
		if ./mvnw -q compile; then
			touch "$TRIGGER"
			echo "[dev] código recompilado, reiniciando o backend..."
		else
			echo "[dev] erro de compilação: o backend continua com a versão anterior."
		fi
	fi
done
