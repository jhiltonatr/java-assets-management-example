# AGENTS.md

Maven BOM hierarchy (`com.example.company:maven-standards`). One Maven project per JDK level so each
artifact ships a stable `artifactId` — **no profiles, no dynamic coordinates**.

## Structure

```
maven-standards (aggregator, pom.xml <modules>)
├── java-company-standards           <-- shared plain-Java template (all shared lib/plugin versions)
├── java-company-standards-jdk{17,21,25}   <-- thin leaf: pins java.version / maven.compiler.release
├── java-springboot-standards        <-- Spring Boot template (spring-boot-dependencies import, docs gen)
├── java-springboot-standards-jdk{17,21,25} <-- leaf: per-flavor Spring Boot/logback/etc. overrides
└── test-applications/sample-springboot-app-jdk{17,-21,-25}  <-- standalone, parent = leaf, <relativePath/>
                                                                 NOT in the reactor
```

Parent chain for a Spring Boot consumer:
`app -> java-springboot-standards-jdkXX -> java-springboot-standards -> java-company-standards -> maven-standards`.

## Core rules

- **Version overrides only take effect in a POM in the consumer's parent chain.** Shared/across-flavor
  versions (JUnit, Jackson, logback base, plugin versions) go in `java-company-standards/pom.xml`.
  Per-JDK versions (`java.version`, `maven.compiler.release`, `spring-boot.version`, `springdoc.version`,
  flavor-specific logback) go in the `java-springboot-standards-jdkXX/pom.xml` leaf. Overrides in the
  `java-company-standards-jdkXX` *sibling* BOM are invisible to Spring Boot consumers.
- **Direct `<dependencyManagement>` entries beat imported BOMs** (`spring-boot-dependencies`, jackson-bom).
  Use direct entries to pin CVE-fixed versions. Keep `logback-classic` and `logback-core` at the same
  `${logback.version}` (they cannot drift). Match the Logback line to the Spring Boot line (3.2.x→1.4.x, 3.5.x/4.x→1.5.x+).
- Keep dependency-check plugin pinned to 12.2.2 (13.0.x fails NVD feed updates without an API key).
- `checkstyle.location` / `checkstyle-suppression.location` are per-JDK files under the leaf.

## Commands

```
mvn install                 # root: build + install all variants to local repo (-U to force refresh)
mvn generate-resources      # root: regenerate docs/BOM-reference.md (effective POM, incl. CVEs)
```
CVE scan (opt-in, no API key needed; scans test apps, then regenerate docs):
```
mvn -f test-applications/sample-springboot-app-jdk17/pom.xml org.owasp:dependency-check-maven:check
mvn -f test-applications/sample-springboot-app-jdk-21/pom.xml org.owasp:dependency-check-maven:check
mvn -f test-applications/sample-springboot-app-jdk-25/pom.xml org.owasp:dependency-check-maven:check
mvn -f pom.xml org.owasp:dependency-check-maven:aggregate
```
Run a sample app: `mvn -f test-applications/sample-springboot-app-jdk-21/pom.xml clean spring-boot:run`
(standards must be installed first). Verify with a sample-app build after editing standards.

## Generated docs

`docs/<artifactId>/<version>/BOM-reference.md` is rendered from the **effective Maven model** by
`java-springboot-standards/src/main/resources/bom-docs.groovy` (gmavenplus `execute` bound to
`generate-resources`). Any version/BOM/plugin change is reflected after a rebuild — never hand-edit docs.
If an app's `target/dependency-check-report.json` exists, the generator merges findings into a
`known CVEs` column and a scan summary.

## Adding a JDK flavor

1. Copy `java-company-standards-jdk21` → new folder/artifactId, update the two JDK properties.
2. Copy `java-springboot-standards-jdk21` → new folder/artifactId, update JDK properties + any per-flavor versions.
3. Register both in aggregator `pom.xml` `<modules>`.
4. Optionally add a matching `test-applications/sample-springboot-app-*` (parent = new leaf, `<relativePath/>`).