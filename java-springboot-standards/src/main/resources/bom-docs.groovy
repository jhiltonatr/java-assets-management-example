/**
 * Generic BOM reference generator.
 *
 * Renders an auto-generated Markdown document from this Maven module's EFFECTIVE model, so the
 * doc always matches the actual dependencyManagement / pluginManagement / properties and never
 * needs manual edits when a library is added or bumped anywhere in the parent chain.
 *
 * Invoked per module by gmavenplus-plugin (execute goal, generate-resources phase) from the
 * java-springboot-standards template POM. Writes to:
 *
 *   <repo-root>/docs/<artifactId>/<version>/BOM-reference.md
 *
 * Inputs (gmavenplus bindings): project (MavenProject)
 */
def props = project.properties

if (project.packaging != 'pom') {
    println "[bom-docs] skipped $project.artifactId - BOM reference docs only for 'pom' packaging modules"
    return
}

def versionProps = props.keySet().toList()
        .collect { it.toString() }
        .findAll { it.toLowerCase().contains('version') }
        .sort()

def managed = project?.dependencyManagement?.dependencies ?: []

def mgmtPlugins = project?.build?.pluginManagement?.plugins ?: []
def appliedPlugins = project?.build?.plugins ?: []

def cveSevRank = { String s ->
    ['critical': 0, 'high': 1, 'medium': 2, 'moderate': 2, 'low': 3, 'info': 4, 'n/a': 5]
        .getOrDefault((s ?: 'n/a').toLowerCase(), 5)
}

def cveSevRankIcon = { String s ->
    ['critical': ':triangular_flag_on_post:', 'high': ':red_circle:', 'medium': ':warning:', 'moderate': ':warning:', 'low': ':small_blue_diamond:', 'info': ':small_blue_diamond:', 'n/a': ':small_blue_diamond:']
            .getOrDefault((s ?: 'n/a').toLowerCase(), '')
}
/*
 * CVE data comes from the OWASP dependency-check reports of the scanned consumer applications
 * (one per JDK flavor under <repo-root>/test-applications/), because a 'pom'-packaging BOM module
 * resolves no libraries to scan itself. All reports found are merged into one GAV -> CVEs index;
 * the -Ddependency.checkReport=... user property can point at a single explicit file instead.
 */
def cveReportFiles = []
def appDir = project.basedir
while (appDir != null && !new File(appDir, 'test-applications').isDirectory()) { appDir = appDir.parentFile }
if (props['dependency.checkReport']) {
    cveReportFiles = [new File(props['dependency.checkReport'].toString())]
} else if (appDir != null) {
    new File(appDir, 'test-applications').listFiles().each { candidate ->
        if (candidate.isDirectory() && candidate.name.startsWith('sample-springboot-app-jdk' + project?.properties['maven.compiler.release'])) {
            def rep = new File(candidate, 'target/dependency-check-report.json')
            if (rep.isFile()) cveReportFiles << rep
        }
    }
}
cveReportFiles = cveReportFiles.findAll { it.isFile() }

def cveByGav = [:]
def cveReportSummaries = [:]
cveReportFiles.each { repFile ->
    try {
        def rep = new groovy.json.JsonSlurper().parse(repFile)
        (rep.dependencies ?: []).each { d ->
            (d.packages ?: []).each { p ->
                def purl = p.id?.toString()
                if (purl?.startsWith('pkg:maven/')) {
                    def rest = purl.substring('pkg:maven/'.length())
                    def at = rest.lastIndexOf('@')
                    if (at > 0) {
                        def coords = rest.substring(0, at)
                        def slash = coords.indexOf('/')
                        if (slash > 0) {
                            def g = coords.substring(0, slash).toLowerCase()
                            def a = coords.substring(slash + 1).toLowerCase()
                            def version = rest.substring(at + 1)
                            cveByGav["$g:$a:$version"] = (d.vulnerabilities ?: []).collect { v ->
                                [id: v.name ?: 'n/a', severity: v.severity ?: 'n/a',
                                 score: v.cvssScore ?: null, source: v.source ?: '',
                                 icon: cveSevRankIcon(v.severity)]
                            }
                        }
                    }
                }
            }
        }
        def vulnerable = (rep.dependencies ?: []).findAll { d -> !(d.vulnerabilities ?: []).isEmpty() }
        cveReportSummaries[repFile.absolutePath] = [
                project: rep.projectName ?: repFile.parentFile.parentFile.name,
                depsScanned: (rep.dependencies ?: []).size(),
                vulnDeps: vulnerable.size(),
                vulnsFound: vulnerable.sum { d -> (d.vulnerabilities ?: []).size() } ?: 0]
    } catch (Exception e) {
        // skip a broken report silently
    }
}

def out = new StringBuilder()
def md = { String s = '' -> out.append(s).append('\n') }

md("# `$project.artifactId` - BOM Reference")
md()
md("> **Auto-generated.** This document is rendered from this module's **effective Maven model** by the")
md("> Groovy generator in `java-springboot-standards` (via `gmavenplus-plugin`, bound to the")
md("> `generate-resources` phase). The tables are derived from the POMs themselves, so adding or")
md("> changing a managed dependency, importing a BOM or bumping a plugin version anywhere in this")
md("> module's parent chain is reflected here automatically on the next `mvn generate-resources` -")
md("> no manual edits, no drift from the actual list.")
md()
md('---')
md()
md('## Project & Environment Baseline')
md()
md('| Key | Value |')
md('| :--- | :--- |')
md("| groupId | `$project.groupId` |")
md("| artifactId | `$project.artifactId` |")
md("| version | `$project.version` |")
md("| packaging | `$project.packaging` |")
md("| Target JDK (`maven.compiler.release`) | `${props['maven.compiler.release'] ?: 'n/a'}` |")
md("| Checkstyle rules | `${props['checkstyle.location'] ?: 'n/a'}` |")
md("| Team in charge | `${props['company.team'] ?: 'n/a'}` |")
md()
md('---')
md()
md('## Imported Platform BOMs')
md()
md('Platform BOMs are pulled in via `<dependencyManagement>` entries of `type=pom` and `scope=import`')
md('(e.g. `spring-boot-dependencies`, `junit-bom`, `jackson-bom`). Maven expands these imports into the')
md('effective model, so **every managed version they contribute appears directly in the Managed')
md('Dependencies table below** and the platform versions themselves are pinned in the Effective Version')
md('Properties table. No separate enumeration is needed and nothing can drift from the actual list.')
md()
md('---')
md()
md('## Managed Dependencies')
md()
md('The fully resolved managed set from the effective model. This includes everything declared directly')
md('plus the expanded contents of imported platform BOMs.')
md()
if (managed.empty) {
    md('*None.*')
} else {
    md('| groupId | artifactId | type | classifier | version | known CVEs |')
    md('| :--- | :--- | :--- | :--- | :--- | :--- |')
    managed.sort { "$it.groupId:$it.artifactId" }.each { d ->
        def type = d.type ?: 'jar'
        def classifier = d.classifier ?: '-'
        def cveCell = '-'
        if (d.version) {
            def cves = cveByGav["${d.groupId.toLowerCase()}:${d.artifactId.toLowerCase()}:${d.version}"]
            if (cves) {
                cves.sort { a, b -> cveSevRank(a.severity) <=> cveSevRank(b.severity) }
                cveCell = cves.collect { c ->
                    c.score != null ? "`${c.id}` (${c.icon} ${c.severity.toLowerCase()}, ${c.score})" : "`${c.id}` (${c.icon} ${c.severity.toLowerCase()})"
                }.join('<br>')
            }
        }
        md("| `${d.groupId}` | `${d.artifactId}` | `${type}` | `${classifier}` | `${d.version ?: 'n/a'}` | $cveCell |")
    }
}
md()
md('---')
md()
md('## Maven Plugin Suite')
md()
md('Plugins versioned via `<pluginManagement>` - the approved set consumers inherit.')
md()
if (mgmtPlugins.empty) {
    md('*None.*')
} else {
    md('| groupId | artifactId | version |')
    md('| :--- | :--- | :--- |')
    mgmtPlugins.sort { "${it.groupId ?: 'org.apache.maven.plugins'}:$it.artifactId" }.each { p ->
        md("| `${p.groupId ?: 'org.apache.maven.plugins'}` | `${p.artifactId}` | `${p.version ?: 'n/a'}` |")
    }
}
md()
if (!appliedPlugins.empty) {
    md("Plugins actually bound in this module's build:")
    md()
    md('| groupId | artifactId | version | executions |')
    md('| :--- | :--- | :--- | :--- |')
    appliedPlugins.sort { "${it.groupId ?: 'org.apache.maven.plugins'}:$it.artifactId" }.each { p ->
        def execs = p.executions.empty
                ? '-'
                : p.executions.collect { e ->
                    def goalText = e.goals ? "-> ${e.goals.join(', ')}" : ''
                    "${e.id ?: e.phase ?: 'execution'}$goalText"
                }.join('; ')
        md("| `${p.groupId ?: 'org.apache.maven.plugins'}` | `${p.artifactId}` | `${p.version ?: 'n/a'}` | `$execs` |")
    }
    md()
}
md('---')
md()
md('## Effective Version Properties')
md()
md('Catch-all snapshot: every `<property>` whose name contains **version**, as resolved in this')
md('module. A new library or plugin introduced with a version property shows up here automatically.')
md()
if (versionProps.empty) {
    md('*None.*')
} else {
    md('| property | value |')
    md('| :--- | :--- |')
    versionProps.each { k ->
        md("| `$k` | `${props[k]}` |")
    }
}
md()
md('---')
md()
md('## How to Consume This BOM')
md()
md('Use as parent (recommended - carries JDK level, plugin configuration and repositories):')
md()
md('```xml')
md('<parent>')
md('    <groupId>com.example.company</groupId>')
md("    <artifactId>$project.artifactId</artifactId>")
md('    <version>1.0.0-SNAPSHOT</version>')
md('    <relativePath/>')
md('</parent>')
md('```')
md()
md('Alternatively import for `dependencyManagement` only (JDK level, plugins and repositories are')
md('**not** inherited):')
md()
md('```xml')
md('<dependencyManagement>')
md('    <dependencies>')
md('        <dependency>')
md('            <groupId>com.example.company</groupId>')
md("            <artifactId>$project.artifactId</artifactId>")
md('            <version>1.0.0-SNAPSHOT</version>')
md('            <type>pom</type>')
md('            <scope>import</scope>')
md('        </dependency>')
md('    </dependencies>')
md('</dependencyManagement>')
md('```')
md()
md('When declaring dependencies, **omit the `<version>` element** to inherit the approved version.')
md()
md('---')
md()
md('## Vulnerability Scanning (CVEs)')
md()
def dcVersion = props['dependency-check.version'] ?: 'n/a'
md('Dependencies are checked with **OWASP dependency-check**')
md("(`org.owasp:dependency-check-maven`, version `$dcVersion`), managed for every module that inherits")
md('these standards. It works **keyless**: the NVD API is accessed unauthenticated (throttled to one')
md('request per 8 seconds) and the Google OSV feed provides additional coverage without any key. To')
md('lift the NVD throttle, supply an API key via `-DnvdApiKey=...`, the `NVD_API_KEY` environment')
md('variable, or a Maven `settings.xml` server entry referenced by `nvdApiServerId`.')
md()
md('Scanning is **opt-in** - it is not bound to the build lifecycle because it needs network access')
md('and performs a one-time NVD feed download on the first run. Scan each consumer application')
md('(one per JDK flavor under `test-applications/`; the first run downloads the NVD feed and may')
md('take a while):')
md()
md('```')
md('mvn -f test-applications/sample-springboot-app-jdk17/pom.xml org.owasp:dependency-check-maven:check')
md('mvn -f test-applications/sample-springboot-app-jdk-21/pom.xml org.owasp:dependency-check-maven:check')
md('mvn -f test-applications/sample-springboot-app-jdk-25/pom.xml org.owasp:dependency-check-maven:check')
md('```')
md()
md('Scan the whole repository (aggregated report in the reactor root `target/`):')
md()
md('```')
md('mvn -f pom.xml org.owasp:dependency-check-maven:aggregate')
md('```')
md()
md('Reports are written as `target/dependency-check-report.html` and')
md('`target/dependency-check-report.json`. A `pom`-packaging BOM module resolves no libraries to')
md('scan, so the scan runs against the **consumer applications** under `test-applications/`. Every')
md('report found there is merged into a single per-GAV CVE index at generation time and annotated')
md('per library version in the **Managed Dependencies** table above (`known CVEs` column, matched on')
md('the exact GAV). Regenerate (`mvn generate-resources`) after a re-scan to refresh everything.')
md()
if (!cveReportSummaries.isEmpty()) {
    def totalFindings = cveByGav.values().findAll { !it.isEmpty() }.flatten().size()
    md('### Last local scan')
    md()
    md('| metric | value |')
    md('| :--- | :--- |')
    md("| scanned applications | `${cveReportSummaries.size()}` |")
    md("| unique packages scanned | `${cveByGav.size()}` |")
    md("| unique vulnerable packages | `${cveByGav.count { k, v -> !v.isEmpty() }}` |")
    md("| vulnerabilities found | `$totalFindings` |")
    md()
    md('Per application:')
    md()
    cveReportSummaries.each { path, s ->
        md("- `${s.project}`: `${s.depsScanned}` packages scanned, `${s.vulnDeps}` vulnerable, `${s.vulnsFound}` findings")
    }
    md()
    def annotated = managed.count { d ->
        d.version && cveByGav["${d.groupId.toLowerCase()}:${d.artifactId.toLowerCase()}:${d.version}"]
    }
    if (annotated > 0) {
        md('Findings are annotated per library version in the **Known CVEs** column of the **Managed')
        md("Dependencies** table above. Full details: each app's `target/dependency-check-report.html`.")
    } else {
        md('None of the library versions managed in this BOM were resolved by any scanned application,')
        md('so the scan results do not annotate the Managed Dependencies table (the `known CVEs` column')
        md("stays empty). Full details: each app's `target/dependency-check-report.html`.")
    }
} else {
    md('**No local scan results yet.** Run the `check` goal against each consumer application under')
    md('`test-applications/` (first run downloads the NVD feed and may take a while), then regenerate')
    md('to embed the findings:')
    md()
    md('```')
    md('mvn -f test-applications/sample-springboot-app-jdk17/pom.xml org.owasp:dependency-check-maven:check')
    md('mvn -f test-applications/sample-springboot-app-jdk-21/pom.xml org.owasp:dependency-check-maven:check')
    md('mvn -f test-applications/sample-springboot-app-jdk-25/pom.xml org.owasp:dependency-check-maven:check')
    md('mvn generate-resources')
    md('```')
}
md()
md('---')
md()
md('## Regeneration')
md()
md('Docs are regenerated during the normal build. From the repository root:')
md()
md('```')
md('mvn generate-resources')
md('```')

def root = System.getProperty('maven.multiModuleProjectDirectory')
if (!root) { root = project.basedir.parentFile.parentFile.absolutePath }
def outFile = new File("$root/docs/$project.artifactId/$project.version/BOM-reference.md")
outFile.parentFile.mkdirs()
outFile.text = out.toString()
println("[bom-docs] wrote ${outFile.absolutePath.replace('\\', '/')}")