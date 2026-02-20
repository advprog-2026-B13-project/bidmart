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

