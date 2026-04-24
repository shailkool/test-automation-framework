# ── Stage 1: dependency cache ───────────────────────────────────────────────
# Pull deps into a layer that only rebuilds when pom.xml files change.
FROM maven:3.9.6-eclipse-temurin-17 AS deps

WORKDIR /build

# Copy only POMs first — maximises layer cache hits
COPY pom.xml .
COPY core-framework/pom.xml          core-framework/pom.xml
COPY application-automation/pom.xml  application-automation/pom.xml
COPY application-tests/pom.xml       application-tests/pom.xml

RUN mvn dependency:go-offline -B --no-transfer-progress

# ── Stage 2: compile & package ──────────────────────────────────────────────
FROM deps AS build

# Copy source (changes more often than POMs, so comes after dep cache)
COPY core-framework/src          core-framework/src
COPY application-automation/src  application-automation/src
COPY application-tests/src       application-tests/src

RUN mvn clean install -DskipTests -B --no-transfer-progress

# ── Stage 3: runtime image ──────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-jammy AS runner

# System packages Playwright needs for headless Chromium on Ubuntu 22.04
RUN apt-get update && apt-get install -y --no-install-recommends \
        libnss3 libatk1.0-0 libatk-bridge2.0-0 libcups2 \
        libdrm2 libxkbcommon0 libxcomposite1 libxdamage1 \
        libxfixes3 libxrandr2 libgbm1 libasound2 \
        libpango-1.0-0 libcairo2 libxshmfence1 \
        maven \
    && rm -rf /var/lib/apt/lists/*

# Create a non-root user — Playwright refuses to run Chromium as root
RUN groupadd -r automation && useradd -r -g automation -m automation

WORKDIR /app
USER automation

# Copy the compiled Maven project from the build stage
COPY --from=build --chown=automation:automation /build .

# Install Playwright browser binaries for this user
# PLAYWRIGHT_BROWSERS_PATH pins the install location so it survives the COPY
ENV PLAYWRIGHT_BROWSERS_PATH=/home/automation/.cache/ms-playwright
RUN mvn exec:java \
      -pl application-tests \
      -e -Dexec.mainClass=com.microsoft.playwright.CLI \
      -Dexec.args="install chromium" \
      --no-transfer-progress

# Workspace is where ci.json already expects output
# ( "${WORKSPACE:-/tmp/ci}/reports" etc. )
ENV WORKSPACE=/workspace
VOLUME ["/workspace"]

# Default: run the CI profile smoke suite
# Override CMD at runtime for regression or nightly
CMD ["mvn", "test", \
     "-pl", "application-tests", \
     "-Dprofile=ci", \
     "-Denv=qa", \
     "-Dsurefire.suiteXmlFiles=src/test/resources/smoke.xml", \
     "--no-transfer-progress"]
