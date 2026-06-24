// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.google.gms.google-services") version "4.4.4" apply false
    alias(libs.plugins.android.application) apply false
}

tasks.register<Exec>("generateDocumentation") {
    group = "documentation"
    description = "Generates the project documentation using Python scripts."
    commandLine("python", "generate_part2.py")
}
