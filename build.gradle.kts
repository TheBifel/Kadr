plugins {
    id("com.android.application") version "9.1.0" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.10" apply false
    id("org.jetbrains.kotlin.jvm") version "2.2.10" apply false
}

val ktfmt by
    configurations.creating {
        attributes.attribute(
            org.gradle.api.attributes.Bundling.BUNDLING_ATTRIBUTE,
            objects.named(org.gradle.api.attributes.Bundling.SHADOWED),
        )
    }

dependencies { ktfmt("com.facebook:ktfmt:0.58") }

val kotlinSources =
    fileTree(rootDir) {
        include("**/*.kt", "**/*.kts")
        exclude("**/build/**", ".gradle/**", ".kotlin/**", "artifacts/**")
    }

tasks.register<JavaExec>("formatKotlin") {
    group = "formatting"
    description = "Format Kotlin sources and Gradle scripts."
    classpath = ktfmt
    mainClass.set("com.facebook.ktfmt.cli.Main")
    args("--kotlinlang-style")
    args(kotlinSources.files.sorted().map { it.path })
}

tasks.register<JavaExec>("checkKotlinFormatting") {
    group = "verification"
    description = "Check Kotlin formatting without changing files."
    classpath = ktfmt
    mainClass.set("com.facebook.ktfmt.cli.Main")
    args("--kotlinlang-style", "--dry-run", "--set-exit-if-changed")
    args(kotlinSources.files.sorted().map { it.path })
}
