# Bidmart Core Service

Core backend service built with Java and Spring Boot.
 
## Overview

Main backend service for the Bidmart platform.
 
**Technology Stack:**
- Language: Java 21
- Framework: Spring Boot 4.0.3
- Build Tool: Gradle
- Testing: JUnit 5

## Prerequisites

- Java Development Kit (JDK) 21+
- Gradle (included via gradlew)

## Installation

1. Install JDK 21:
```bash
java -version  # Should show java 21
```

2. Navigate to the service directory:
```bash
cd apps/bidmart-core
```

## Running

### Development server
```bash
./gradlew bootRun
```

Use `dev` profile (recommended while frontend runs on localhost):
```bash
SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun
```

The server runs on `http://localhost:8080` (default Spring Boot port)

### Build
```bash
./gradlew build
```

Builds JAR file at `build/libs/`

### Run JAR
```bash
java -jar build/libs/bidmart-core-0.0.1-SNAPSHOT.jar
```

## Testing

Run all tests:
```bash
./gradlew test
```

## Project Structure

```
src/
├── main/
│   ├── java/           # Java source code
│   └── resources/      # Properties, configs
└── test/
    ├── java/           # Test code
    └── resources/      # Test configs
```

## CORS Configuration

CORS is configured globally via `app.cors.*` properties and can be set per environment.

Default properties are in `src/main/resources/application.properties`, with profile overrides in:
- `src/main/resources/application-dev.properties`
- `src/main/resources/application-prod.properties`

### Dev example (frontend on localhost:3000)
```bash
SPRING_PROFILES_ACTIVE=dev APP_CORS_ALLOWED_ORIGINS=http://localhost:3000 ./gradlew bootRun
```

### Prod example
```bash
SPRING_PROFILES_ACTIVE=prod APP_CORS_ALLOWED_ORIGINS=https://bidmart.store ./gradlew bootRun
``` 

### Prod with staging subdomains example
```bash
SPRING_PROFILES_ACTIVE=prod APP_CORS_ALLOWED_ORIGINS=https://bidmart.store APP_CORS_ALLOWED_ORIGIN_PATTERNS=https://*.bidmart.store ./gradlew bootRun
```

### Staging with temporary GCP frontend URL example
```bash
SPRING_PROFILES_ACTIVE=prod APP_CORS_ALLOWED_ORIGINS=https://staging.bidmart.store APP_CORS_ALLOWED_ORIGIN_PATTERNS=https://*.run.app ./gradlew bootRun
```

You can also override additional settings:
- `APP_CORS_ALLOWED_METHODS` (comma separated)
- `APP_CORS_ALLOWED_HEADERS` (comma separated)
- `APP_CORS_EXPOSED_HEADERS` (comma separated)
- `APP_CORS_ALLOWED_ORIGIN_PATTERNS` (comma separated, supports wildcard subdomains)
- `APP_CORS_ALLOW_CREDENTIALS` (`true` or `false`)
- `APP_CORS_MAX_AGE` (seconds)
