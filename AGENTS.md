# Repository Guidelines
## Communication
- Default to replying in Chinese unless the user explicitly asks for another language.

## Project Structure & Module Organization
- Multi-module Maven project rooted at `pom.xml`.
- Modules:
  - `disruptor-spring-boot-autoconfigure/`: core auto-configuration and infrastructure (`src/main/java`), plus Spring metadata in `src/main/resources/META-INF`.
  - `disruptor-spring-boot-starter/`: starter dependency that pulls in the autoconfigure module (`src/main/java`).
- There are currently no `src/test/java` directories; add them under each module if you introduce tests.

## Build, Test, and Development Commands
- `mvn clean install`: build all modules and install artifacts to your local Maven repo.
- `mvn -pl disruptor-spring-boot-autoconfigure -am test`: run tests for a single module (and required dependencies).
- `mvn -DskipTests clean install`: build without running tests.
- `mvn clean verify`: full verification build (includes Surefire + JaCoCo reporting as configured).

## Coding Style & Naming Conventions
- Java 17, Spring Boot 3.5.x; keep code compatible with these versions.
- Use standard Java conventions: 4-space indentation, one public top-level class per file, and package names under `com.childrengreens.disruptor.*`.
- Keep configuration and Spring metadata in `src/main/resources/META-INF`.
- No formatter or linter is configured; keep changes consistent with existing style and Javadoc tone.

## Testing Guidelines
- Tests run via Maven Surefire; JaCoCo is configured to generate coverage reports during `test`.
- If you add tests, prefer JUnit 5 naming conventions like `*Test` or `*Tests` and place them under `src/test/java` in the module they exercise.
- When adding new features, include unit tests in the autoconfigure module where the behavior lives.

## Commit & Pull Request Guidelines
- Commit messages in this repo are short, imperative sentences (e.g., “Add Disruptor actuator auto-configuration”). Follow that style.
- PRs should include: a concise description, affected modules, and any configuration or Actuator changes.
- If behavior changes, note the test coverage or add new tests. Include sample configuration snippets when you introduce new properties.

## Configuration & Runtime Notes
- Configuration lives under `spring.disruptor.*` (see `README.md` for examples).
- Ring buffer sizes must be powers of two; publish before lifecycle start throws an error. Document any new constraints.
