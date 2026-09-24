---
name: enroll-library
description: Use when enrolling a NEW third-party library into the Maven standards BOMs, given a Maven or Gradle dependency format. Triggers: "add library", "enroll", "add dependency", "new lib", "implementation(...)", "com.example:x:1.0". Adds the version property at the top of the hierarchy (java-company-standards), verifies JDK 17/21/25 compatibility, and adds per-JDK overrides only where a variant needs its own version.
---

# Enroll a library

Add a company-approved third-party library to the BOM hierarchy so every consumer inherits a
pinned, CVE-controlled version. Input may be Maven (`group:artifact:version`) or Gradle
(`implementation("group:artifact:version")`) format.

## Steps

1. **Parse the coordinates.** Extract `groupId`, `artifactId`, and version from the input. If no
   version is given, look up the latest stable release (websearch or Maven Central
   `maven-metadata.xml`).
2. **Pick a property name.** Use the `artifactId` as-is, lowercased, e.g. `guava.version`,
   `commons-lang3.version`, `jackson-databind.version`. For a platform/BOM import use the BOM
   artifactId, e.g. `jackson-bom.version`, `junit.version`.
3. **Add property + managed dependency at the top of the hierarchy** in
   `java-company-standards/pom.xml`:
   - add `<${name}.version>X</${name}.version>` to the library versions `<properties>` block
     (the shared template = the top of the parent chain, so all flavors inherit it);
   - add a direct `<dependency>` under `<dependencyManagement>` using `${${name}.version}`.
   Direct entries win over imported BOMs (`spring-boot-dependencies`, `jackson-bom`), so the
   company baseline always wins. This applies even if Spring Boot also manages the library.
   Sort properties/dependencies consistently with the existing blocks.
4. **Verify JDK compatibility (17 / 21 / 25).** The latest version only stays in the template if
   it runs on ALL three JDKs.
   - Check the library's documented Java baseline (official docs / Maven Central).
   - For an objective check, download the jar and read the class-file major version of a class:
     bytes 6-7 big-endian. Mapping: 61=JDK17, 65=JDK21, 69=JDK25. Bytecode > a flavor's JDK
     breaks that flavor; bytecode below the JDK is fine (newer JDKs run it).
   - Note per-era/platform constraints (e.g. a version's Spring Boot line must match the flavor's
     `spring-boot.version`).
5. **Override only where needed.** If the latest version does NOT work on a given JDK, pin the
   newest working version by overriding the SAME property in the matching leaf:
   - library consumed by Spring Boot apps → override in `java-springboot-standards-jdkXX/pom.xml`
     (the `java-company-standards-jdkXX` *sibling* BOM is invisible to Spring Boot consumers);
   - plain-Java-only library → override in `java-company-standards-jdkXX/pom.xml`.
   If a flavor has no limitation, leave it untouched — it inherits the template property.
   Add overrides per flavor ONLY where versions actually differ.
6. **Regenerate docs and verify builds.** Run `mvn generate-resources` (updates the
   BOM-reference docs from the effective model) and `mvn install` to publish all variants.
   Imports of a platform (junit-bom, jackson-bom) sit closer to the consumer and beat the
   Spring Boot import — keep that in mind when the library resolves from a platform.

## Rules

- Never hand-edit `docs/`. Docs come from `mvn generate-resources`.
- Do not use profiles or dynamic coordinates; every flavor is a concrete `artifactId`.
- If the property name already exists in a leaf, an override already exists — treat as a
  `manage-library` task instead.
- `logback-classic` and `logback-core` must stay on the same version (keep them paired).