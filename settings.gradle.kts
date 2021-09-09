val properties = File(rootDir, "gradle.properties").inputStream().use {
    java.util.Properties().apply { load(it) }
}

rootProject.name = properties.getValue("mod.name") as String

