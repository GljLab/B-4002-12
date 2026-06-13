#!/usr/bin/env bash
set -euo pipefail

current_java_major="$(java -version 2>&1 | sed -n '1s/.*version "\(.*\)".*/\1/p' | awk -F. '{print $1}')"

if [[ "${current_java_major}" -lt 21 ]] && command -v /usr/libexec/java_home >/dev/null 2>&1; then
  candidate_java_home="$(
    /usr/libexec/java_home -v 25 2>/dev/null \
    || /usr/libexec/java_home -v 24 2>/dev/null \
    || /usr/libexec/java_home -v 23 2>/dev/null \
    || /usr/libexec/java_home -v 22 2>/dev/null \
    || /usr/libexec/java_home -v 21 2>/dev/null \
    || true
  )"
  if [[ -n "${candidate_java_home}" ]]; then
    candidate_java_major="$("${candidate_java_home}/bin/java" -version 2>&1 | sed -n '1s/.*version "\(.*\)".*/\1/p' | awk -F. '{print $1}')"
    if [[ "${candidate_java_major}" -lt 21 ]]; then
      candidate_java_home=""
    fi
  fi
  if [[ -z "${candidate_java_home}" ]]; then
    candidate_java_home="$(
      ls -d /opt/homebrew/Cellar/openjdk/*/libexec/openjdk.jdk/Contents/Home 2>/dev/null | sort -Vr | head -n 1
    )"
  fi
  if [[ -n "${candidate_java_home}" ]]; then
    export JAVA_HOME="${candidate_java_home}"
    export PATH="${JAVA_HOME}/bin:${PATH}"
  fi
fi

if ! java -version 2>&1 | grep -Eq 'version "(21|22|23|24|25|26)'; then
  echo "E2E 启动失败：需要 Java 21+（当前 JAVA_HOME=${JAVA_HOME:-unset}）。" >&2
  exit 1
fi

exec ./mvnw -q spring-boot:run -Dspring-boot.run.profiles=e2e
