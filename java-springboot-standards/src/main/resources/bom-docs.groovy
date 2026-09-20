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

def versionProps = props.keySet().toList()
        .collect { it.toString() }
        .findAll { it.toLowerCase().contains('version') }
        .sort()

def managed = project?.dependencyManagement?.dependencies ?: []

def mgmtPlugins = project?.build?.pluginManagement?.plugins ?: []
def appliedPlugins = project?.build?.plugins ?: []

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
    md('| groupId | artifactId | type | classifier | version |')
    md('| :--- | :--- | :--- | :--- | :--- |')
    managed.sort { "$it.groupId:$it.artifactId" }.each { d ->
        def type = d.type ?: 'jar'
        def classifier = d.classifier ?: '-'
        md("| `${d.groupId}` | `${d.artifactId}` | `${type}` | `${classifier}` | `${d.version ?: 'n/a'}` |")
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