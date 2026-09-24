---
name: upgrade-libraries
description: Use when checking every library managed by the standards BOMs for newer versions and upgrading them, applying manage-library's overwrite rules. Triggers: "check for updates", "upgrade libraries", "update all dependencies", "bump everything to latest", "refresh versions", "latest versions". Reuses the manage-library workflow for each version-touch it makes.
---

# Upgrade managed libraries to latest

Audit all library versions managed by the BOM hierarchy against the latest stable releases and
upgrade them, applying the same rules as `manage-library` (per-JDK compatibility + overwrite only
where needed). Read `.opencode/skills/manage-library/SKILL.md` and follow its Steps 2-6 for every
property this skill changes.

## Steps

1. **Enumerate what is managed.** Scan all standards POMs
   (`java-company-standards/pom.xml`, `java-company-standards-jdk*/pom.xml`,
   `java-springboot-standards/pom.xml`, `java-springboot-standards-jdk*/pom.xml`). Collect each
   `<dependencyManagement>` entry whose version is a property (`${x.version}`), including platform
   BOM imports (`spring-boot-dependencies`, `junit-bom`, `jackson-bom`). Resolve each property to
   its current value and note the POM it is defined in. Skip build-plugin versions
   (`maven-*-plugin.version`, `jacoco-*`, `spotless-*`, ...) — this task is about libraries only.
2. **Check the latest for each.** Use Maven Central `maven-metadata.xml` / websearch to find the
   latest STABLE release of every unique property value (one lookup per version property). Skip
   pre-release/snapshot/RC markers unless the project itself only ships prereleases.
3. **Build a diff, then apply.** For each property with a newer version:
   - Update the template definition (`java-company-standards/pom.xml`, or
     `java-springboot-standards/pom.xml` for spring-boot/springdoc/jackson-bom) to the latest.
   - Apply the `manage-library` JDK-compatibility check (docs + class-file major version:
     61=JDK17, 65=JDK21, 69=JDK25). If the latest works on all three JDKs → delete any now-
     redundant per-JDK overrides and stop. If a flavor needs an older version → override that
     variant's property in the matching leaf (Spring Boot consumers:
     `java-springboot-standards-jdkXX/pom.xml`; plain Java: `java-company-standards-jdkXX/pom.xml`).
   - Never upgrade a library whose new version is older-line or CVE-affected when a newer-good
     exists; when lowering a version below the Spring Boot platform line, keep the reason in your
     summary.
4. **Special cases.**
   - `spring-boot.version`: a major line change (3.x→4.x) is breaking and turns every other
     managed version moot — treat as a deliberate, separate decision and flag it in the summary.
     After a Boot bump, re-verify flavor `logback.version` matches the Boot line
     (3.2.x→1.4.x, 3.5.x/4.x→1.5.x+).
   - `logback-classic`/`logback-core`: must stay paired on the same version — bump together via
     one property.
   - Platform BOMs (`junit-bom`, `jackson-bom`): bump the import property, then keep any
     platform-specific direct overrides that exist in leaves.
5. **Regenerate docs and verify.** `mvn generate-resources` + `mvn install`. Docs are generated
   from the effective model — never hand-edit.

## Rules

- Produce a per-library verdict before editing ("upgraded / skipped-reason / overridden-on-JDKx")
  so the change is reviewable.
- If no newer version exists for a library, leave it untouched.
- Overrides live ONLY in a POM inside the consumer's parent chain; sibling
  `java-company-standards-jdkXX` overrides are invisible to Spring Boot consumers.