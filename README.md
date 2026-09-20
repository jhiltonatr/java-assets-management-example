# Maven Standards (maven-assets)

Company-wide Maven standards as BOMs ("Bill of Materials"). Every target JDK level is its own
Maven project, so each artifact ships with a stable, concrete `artifactId` (no profiles, no
dynamic coordinates):

- `java-company-standards` — shared plain-Java settings template (defaults to JDK 17)
- `java-company-standards-jdk17` / `java-company-standards-jdk21` / `java-company-standards-jdk25` —
  plain-Java BOMs, one per JDK
- `java-springboot-standards` — shared Spring Boot settings template
- `java-springboot-standards-jdk17` / `java-springboot-standards-jdk21` / `java-springboot-standards-jdk25` —
  Spring Boot BOMs built on the matching company variant
- `sample-springboot-app-jdk17` / `sample-springboot-app-jdk-21` / `sample-springboot-app-jdk-25` —
  standalone demo apps under `test-applications/`, one per Spring Boot flavor, each consuming its
  matching `java-springboot-standards-jdkXX` parent from the local repository. They are
  intentionally NOT part of the aggregator reactor.

## Layout

```
maven-assets/
├── pom.xml                                        maven-standards (aggregator)
├── java-company-standards/
│   ├── pom.xml                                    java-company-standards (template)
│   ├── java-company-standards-jdk17/pom.xml
│   ├── java-company-standards-jdk21/pom.xml
│   └── java-company-standards-jdk25/pom.xml
├── java-springboot-standards/
│   ├── pom.xml                                    java-springboot-standards (template)
│   ├── java-springboot-standards-jdk17/pom.xml
│   ├── java-springboot-standards-jdk21/pom.xml
│   └── java-springboot-standards-jdk25/pom.xml
└── test-applications/
    ├── sample-springboot-app-jdk17/   <- parent java-springboot-standards-jdk17
    ├── sample-springboot-app-jdk-21/  <- parent java-springboot-standards-jdk21
    └── sample-springboot-app-jdk-25/  <- parent java-springboot-standards-jdk25
```

## How it works

The templates hold all shared configuration once: managed dependency versions, build plugin
versions and configuration (compiler `--release`, enforcer, jacoco, spotless, ...), corporate
repositories, and distribution targets.

Each `-jdkXX` variant is a thin POM that extends the template and only pins the settings that
differ per JDK flavor (`java.version`, `maven.compiler.release`, `spring-boot.version`, ...).

The parent chain for a Spring Boot consumer is:

```
app
└── java-springboot-standards-jdkXX
    └── java-springboot-standards (template)
        └── java-company-standards (template)
            └── maven-standards (aggregator)
```

## Build and install

A single build publishes every variant to the local Maven repository:

```
mvn install        # in the project root
```

Consumers that use `<relativePath/>` (like the sample app) resolve the standards parent from the
local cache, so they must be (re)installed whenever the standards change:

```
mvn install -U     # refresh after editing the standards
mvn -f test-applications/sample-springboot-app-jdk-21/pom.xml clean spring-boot:run
```

## BOM reference documentation

Each `java-springboot-standards` module ships an auto-generated reference doc that lists the
**effective** dependency and plugin versions of that flavor:

- `docs/java-springboot-standards/1.0.0-SNAPSHOT/BOM-reference.md` (shared template defaults)
- `docs/java-springboot-standards-jdk17/1.0.0-SNAPSHOT/BOM-reference.md`
- `docs/java-springboot-standards-jdk21/1.0.0-SNAPSHOT/BOM-reference.md`
- `docs/java-springboot-standards-jdk25/1.0.0-SNAPSHOT/BOM-reference.md`

The docs are rendered by a generic Groovy generator (`bom-docs.groovy`, run via `gmavenplus-plugin`'s
`execute` goal bound to the `generate-resources` phase in the `java-springboot-standards` template
POM). The script reads each module's **effective Maven model** directly - `dependencyManagement`
(including imported platform BOMs, which Maven expands into the effective model),
`pluginManagement`, applied plugins and version properties - so the tables are derived from the
POMs themselves. Adding or bumping a library, importing a BOM or changing a plugin version anywhere
in a flavour's parent chain is reflected automatically on the next build. No templates to hand-edit,
no drift from the actual list. The output path includes the module version
(`docs/<artifactId>/<version>/BOM-reference.md`).

Regenerate from the repository root:

```
mvn generate-resources
```

### CVE scanning

Every doc ends with a **Vulnerability Scanning (CVEs)** section. The generator documents OWASP
dependency-check (`org.owasp:dependency-check-maven`, version pinned in the root aggregator),
which runs **keyless** - NVD API unauthenticated (throttled) plus the Google OSV feed, with an
optional NVD API key via `-DnvdApiKey`, `NVD_API_KEY`, or a `settings.xml` server. Scanning is opt-in and not bound to the lifecycle. Because a `pom`-packaging BOM resolves no
libraries to scan, the scan runs against the **consumer applications** under `test-applications/`
(one per JDK flavor); the generator then embeds the findings in every BOM reference doc:

```
mvn -f test-applications/sample-springboot-app-jdk17/pom.xml org.owasp:dependency-check-maven:check
mvn -f test-applications/sample-springboot-app-jdk-21/pom.xml org.owasp:dependency-check-maven:check
mvn -f test-applications/sample-springboot-app-jdk-25/pom.xml org.owasp:dependency-check-maven:check
mvn -f pom.xml org.owasp:dependency-check-maven:aggregate   # whole repo
```

When any app's `target/dependency-check-report.json` exists, the generator:

- merges **all** app reports into one per-GAV CVE index,
- adds a **`known CVEs`** column to the **Managed Dependencies** table and annotates every
  library whose exact GAV was found in any scan (severity-sorted CVE list per version), and
- renders a short **Last local scan** summary (metrics per application + a pointer to the HTML
  reports).

Regenerate (`mvn generate-resources`) after a re-scan to refresh the docs. Note: the scan needs
network access and downloads the NVD feed on the first run (a cached NVD DB makes later scans
work offline). A flavor whose managed versions are not part of any scanned application's
resolved set will simply show an empty `known CVEs` column.

## Using the standards in your application

As a parent (recommended): all standards (JDK level, plugin config, repositories) are inherited
in one step.

```xml
<parent>
    <groupId>com.example.company</groupId>
    <artifactId>java-springboot-standards-jdk21</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <relativePath/>    <!-- resolve from the local repository, not from disk -->
</parent>
```

Switching JDK flavor is just changing the parent `artifactId` to
`java-springboot-standards-jdk17` (or `java-company-standards-jdkXX` for plain-Java projects).

Alternatively the BOM can be used as `dependencyManagement` with scope `import`. Note that an
import only conveys version management — `java.version`, plugin configuration, repositories and
`distributionManagement` are NOT imported; consumers must declare those themselves.

## Managing versions per JDK flavor

Each version knob lives in a specific POM. **The rule: a version override only takes effect in a
POM that sits in the consumer's parent chain.**

| Setting | Where to change it |
|---|---|
| Target JDK (`java.version`, `maven.compiler.release`) | The `-jdkXX` leaf you are publishing |
| Spring Boot version (`spring-boot.version`) | `java-springboot-standards-jdkXX/pom.xml` |
| Springdoc version (`springdoc.version`) | `java-springboot-standards-jdkXX/pom.xml` |
| Library versions that must differ per flavor (e.g. `logback.version`) | `java-springboot-standards-jdkXX/pom.xml` |
| Library versions shared by all flavors (JUnit, Jackson, ...) | `java-company-standards/pom.xml` (template) |

Example — JDK 21 currently pairs Spring Boot 3.5.3 with Logback 1.5.38 and JUnit 5.14.4
(the `java-company-standards` template tracks JUnit 6.x for the Java 25 line, which Spring
Boot 3.x does not use):

```xml
<!-- java-springboot-standards-jdk21/pom.xml -->
<properties>
    <java.version>21</java.version>
    <maven.compiler.release>21</maven.compiler.release>
    <spring-boot.version>3.5.3</spring-boot.version>
    <logback.version>1.5.38</logback.version>
    <junit.version>5.14.4</junit.version>
</properties>
```

### Why the override must live in the Spring Boot variant

`java-company-standards-jdk21` is a *sibling* BOM of the Spring Boot chain, not an ancestor.
Overrides placed there are invisible to Spring Boot consumers (the app inherits the *template*
`java-company-standards`, which defaults `logback.version` to 1.4.14). Put flavor-specific
overrides in the POM the consumer actually inherits — the `-jdkXX` Spring Boot leaf.

### Version skew and dependencyManagement precedence

A direct `<dependencyManagement>` entry in the standards always wins over versions imported via
the Spring Boot `spring-boot-dependencies` BOM. Because of that:

- `logback-classic` and `logback-core` are both pinned at the same `${logback.version}` so the
  pair cannot drift. Mismatched versions break at runtime:
  - classic > core: `NoClassDefFoundError: ch/qos/logback/core/util/StatusPrinter2`
  - classic < core: `PatternLayout ... getDefaultConverterSupplierMap()`
- Spring Boot 3.2.x uses the Logback 1.4.x line, Spring Boot 3.5.x and 4.x the 1.5.x line.
  Keep the flavor's `logback.version` on the same line as its `spring-boot.version`.

## Adding a new JDK flavor

1. Copy `java-company-standards-jdk21` to `java-company-standards-jdk25` (project and folder),
   update `artifactId` and the two JDK properties.
2. Copy `java-springboot-standards-jdk21` to `java-springboot-standards-jdk25`, update
   `artifactId`, JDK properties, and any version that needs a newer line.
3. Register both new modules in the aggregator `pom.xml` (`<modules>`).

Build once with `mvn install`; both old and new flavor artifacts coexist in the local repository
and can be selected per application by parent `artifactId`.