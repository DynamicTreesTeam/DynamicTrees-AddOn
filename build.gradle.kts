import com.google.gson.Gson
import com.google.gson.JsonObject
import com.matthewprenger.cursegradle.CurseArtifact
import com.matthewprenger.cursegradle.CurseProject
import com.matthewprenger.cursegradle.CurseRelation
import org.apache.tools.ant.filters.ReplaceTokens
import java.io.InputStreamReader
import java.time.Instant
import java.time.format.DateTimeFormatter

fun property(key: String) = project.findProperty(key).toString()
fun modProperty(key: String) = property("mod.$key")
fun authorProperty(key: String) = property("author.$key")
fun mappingsProperty(key: String) = property("mappings.$key")
fun curseProperty(key: String) = property("curse.$key")

fun dependencyInfo(name: String, key: String) = property("dependency.${name.toLowerCase()}.$key")
fun dependencyVersion(name: String) = dependencyInfo(name, "version")

fun hasProperty(key: String) = project.hasProperty(key) && property(key) != null

plugins {
    id("java")
    id("net.minecraftforge.gradle")
    id("org.parchmentmc.librarian.forgegradle")
    id("idea")
    id("maven-publish")
    id("com.matthewprenger.cursegradle") version "1.4.0"
}

repositories {
    maven("https://ldtteam.jfrog.io/ldtteam/modding/")
    maven("https://maven.tehnut.info")
    maven("https://www.cursemaven.com") {
        content {
            includeGroup("curse.maven")
        }
    }
    maven("https://harleyoconnor.com/maven")
    maven("https://squiddev.cc/maven/")
}

/* Get Project Properties from `gradle.properties` */

val modName = modProperty("name")
val modId = modProperty("id")
val modVersion = modProperty("version")

val mcVersion = dependencyVersion("minecraft")
val forgeVersion = dependencyVersion("forge")

val authorName = authorProperty("name")
val authorEmail = authorProperty("email")
val authorGitHub = authorProperty("github")

version = "$mcVersion-$modVersion"
group = modProperty("group")

val modNameWithoutSpaces = modName.replace(" ", "")
val projectId = "$modNameWithoutSpaces-$mcVersion"

minecraft {
    mappings(mappingsProperty("channel"), "${mappingsProperty("version")}-$mcVersion")

    runs {
        create("client") {
            workingDirectory = file("run").absolutePath

            property("forge.logging.markers", "SCAN,REGISTRIES,REGISTRYDUMP")
            property("forge.logging.console.level", "debug")

            property("mixin.env.remapRefMap", "true")
            property("mixin.env.refMapRemappingFile", "${buildDir}/createSrgToMcp/output.srg")

            if (project.hasProperty("mcUuid")) {
                args("--uuid", property("mcUuid"))
            }
            if (project.hasProperty("mcUsername")) {
                args("--username", property("mcUsername"))
            }
            if (project.hasProperty("mcAccessToken")) {
                args("--accessToken", property("mcAccessToken"))
            }

            mods {
                create(modId) {
                    source(sourceSets.main.get())
                }
            }
        }

        create("server") {
            workingDirectory = file("run").absolutePath

            property("forge.logging.markers", "SCAN,REGISTRIES,REGISTRYDUMP")
            property("forge.logging.console.level", "debug")

            property("mixin.env.remapRefMap", "true")
            property("mixin.env.refMapRemappingFile", "${buildDir}/createSrgToMcp/output.srg")

            mods {
                create(modId) {
                    source(sourceSets.main.get())
                }
            }
        }

        create("data") {
            workingDirectory = file("run").absolutePath

            property("forge.logging.markers", "SCAN,REGISTRIES,REGISTRYDUMP")
            property("forge.logging.console.level", "debug")

            property("mixin.env.remapRefMap", "true")
            property("mixin.env.refMapRemappingFile", "${buildDir}/createSrgToMcp/output.srg")

            args("--mod", modId, "--all", "--output", file("src/generated/resources/"))

            mods {
                create(modId) {
                    source(sourceSets.main.get())
                }
            }
        }
    }
}

sourceSets.main.get().resources {
    srcDir("src/generated/resources")
}

val resourceTargets = arrayOf("META-INF/mods.toml", "pack.mcmeta")
val intoTargets = arrayOf("$rootDir/out/production/resources/", "$rootDir/out/production/${project.name}.main/", "$rootDir/bin/main/")
val replaceProperties = mapOf(
    "name" to modName,
    "id" to modId,
    "version" to modVersion,
    "license" to modProperty("license"),
    "url" to modProperty("url"),
    "issuesUrl" to modProperty("issues.url"),
    "updateUrl" to modProperty("update.url"),
    "description" to modProperty("description"),
    "credits" to modProperty("credits"),
    "authors" to modProperty("authors"),
    "packFormat" to modProperty("pack.format"),
    "mcRange" to dependencyInfo("minecraft", "range"),
    "forgeRange" to dependencyInfo("forge", "range"),
    "dynamicTreesRange" to dependencyInfo("dynamictrees", "range")
)

tasks.processResources {
    inputs.properties(replaceProperties)

    filesMatching(mutableListOf(*resourceTargets)) {
        expand(replaceProperties)
    }

    intoTargets.forEach { target ->
        if (file(target).exists()) {
            copy {
                from(sourceSets.main.get().resources.srcDirs) {
                    include(*resourceTargets)
                    expand(replaceProperties)
                }
                into(target)
            }
        }
    }
}

// Assign MOD_ID constant in main class.
val prepareSources = tasks.register("prepareSources", Copy::class) {
    from("src/main/java")
    into("build/src/main/java")
    filter<ReplaceTokens>("tokens" to mapOf("MOD_ID" to modId))
}

tasks.compileJava {
    source = prepareSources.get().outputs.files.asFileTree
}

dependencies {
    /* Required Dependencies */
    this.minecraft("net.minecraftforge:forge:$mcVersion-$forgeVersion")
    this.modImplementation("com.ferreusveritas", "DynamicTrees")
    // Compile other required mods here, if applicable.

    /* Runtime Dependencies [OPTIONAL] */
    this.modRuntime("com.ferreusveritas", "DynamicTreesPlus")
    this.modRuntime("mcp.mobius.waila", "Hwyla", "", "", "")
    this.modRuntime("mezz", "jei")
    this.modRuntime("com.harleyoconnor", "SuggestionProviderFix", "$mcVersion-", "")

    this.runWithComputerCraft()
}

/**
 * At runtime, use Computer Craft mod for creating growth chambers.
 *
 * @see <a href=" https://github.com/ferreusveritas/DynamicTrees/wiki/Growth-Chambers">Growth Chambers Wiki Page</a>
 */
fun DependencyHandler.runWithComputerCraft() {
    this.modRuntime("org.squiddev", "cc-tweaked", groupSuffix = "")
}

fun DependencyHandler.modImplementation(baseGroup: String, id: String) {
    this.implementation(deobfMod(baseGroup, id))
}

fun DependencyHandler.modRuntime(
    baseGroup: String,
    id: String,
    versionPrefix: String = "",
    idSuffix: String = "-$mcVersion",
    groupSuffix: String = "." + id.toLowerCase()
) {
    this.runtimeOnly(deobfMod(baseGroup, id, versionPrefix, idSuffix, groupSuffix))
}

fun deobfMod(
    baseGroup: String,
    id: String,
    versionPrefix: String = "",
    idSuffix: String = "-$mcVersion",
    groupSuffix: String = "." + id.toLowerCase()
): Dependency {
    return fg.deobf("$baseGroup$groupSuffix:$id$idSuffix:$versionPrefix${dependencyVersion(id)}")
}


tasks.jar {
    manifest.attributes(
        "Specification-Title" to modName,
        "Specification-Vendor" to authorName,
        "Specification-Version" to "1",
        "Implementation-Title" to modName,
        "Implementation-Version" to project.version,
        "Implementation-Vendor" to authorName,
        "Implementation-Timestamp" to DateTimeFormatter.ISO_INSTANT.format(Instant.now())
    )

    archiveBaseName.set(projectId)
    finalizedBy("reobfJar")
}

java {
    withSourcesJar()

    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
}


/* CurseForge uploading via CurseGradle */

/**
 * Reads changelog from [version_info.json](./version_info.json).
 */
fun readChangelog(): String? {
    val versionInfoFile = file("version_info.json")
    val jsonObject = Gson().fromJson(InputStreamReader(versionInfoFile.inputStream()), JsonObject::class.java)
    return jsonObject
        .get(mcVersion)?.asJsonObject
        ?.get(project.version.toString())?.asString
}

curseforge {
    if (!hasProperty("curseApiKey") || !hasProperty("curse.file.type") || !hasProperty("curse.id")) {
        project.logger.log(LogLevel.WARN, "API Key and file type for CurseForge not detected; uploading will be disabled.")
        return@curseforge
    }
    
    apiKey = property("curseApiKey")

    project {
        id = curseProperty("id")

        addGameVersion(mcVersion)

        changelog = readChangelog() ?: "No changelog provided."
        changelogType = "markdown"
        releaseType = curseProperty("file.type")

        addArtifact(tasks.findByName("sourcesJar"))

        mainArtifact(tasks.findByName("jar")) {
            relations {
                requiredDependency("dynamictrees")
                // Add the mod ID of the other add-on dependency as a required dependency here, if applicable.
                optionalDependency("dynamictreesplus") // If you don't use DT+ you should remove this line.
            }
        }
    }
}


/* Maven Publishing */

tasks.withType<GenerateModuleMetadata> {
    enabled = false
}

fun hasMavenAccess() = hasProperty("harleyOConnorMavenUsername") && hasProperty("harleyOConnorMavenPassword")

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = projectId
            version = modVersion

            from(components["java"])

            pom {
                name.set(modName)
                url.set("https://github.com/$authorGitHub/$modNameWithoutSpaces")
                licenses {
                    license {
                        name.set("MIT")
                        url.set("https://mit-license.org")
                    }
                }
                developers {
                    developer {
                        id.set(authorGitHub)
                        name.set(authorName)
                        email.set(authorEmail)
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/$authorGitHub/$modNameWithoutSpaces.git")
                    developerConnection.set("scm:git:ssh://github.com/$authorGitHub/$modNameWithoutSpaces.git")
                    url.set("https://github.com/$authorGitHub/$modNameWithoutSpaces")
                }
            }

            pom.withXml {
                val element = asElement()

                // Clear dependencies.
                for (i in 0 until element.childNodes.length) {
                    val node = element.childNodes.item(i)
                    if (node?.nodeName == "dependencies") {
                        element.removeChild(node)
                    }
                }
            }
        }
    }
    repositories {
        maven("file:///${project.projectDir}/mcmodsrepo")

        if (!hasMavenAccess()) {
            logger.log(LogLevel.WARN, "Credentials for maven not detected; it will be disabled.")
            return@repositories
        }

        maven("https://harleyoconnor.com/maven") {
            name = "HarleyOConnor"
            credentials { // If you'd like to upload this to my maven feel free to ask for credentials.
                username = property("harleyOConnorMavenUsername")
                password = property("harleyOConnorMavenPassword")
            }
        }
    }
}

/**
 * Composite task that runs the CurseForge and maven upload tasks.
 */
tasks.register("publishToAll") {
    this.group = "publishing"
    this.dependsOn("curseforge")
    if (hasMavenAccess()) {
        this.dependsOn("publishMavenJavaPublicationToHarleyOConnorRepository")
    }
}


/* Extensions to make CurseGradle extension slightly neater. */

fun com.matthewprenger.cursegradle.CurseExtension.project(action: CurseProject.() -> Unit) {
    this.project(closureOf(action))
}

fun CurseProject.mainArtifact(artifact: Task?, action: CurseArtifact.() -> Unit) {
    this.mainArtifact(artifact, closureOf(action))
}

fun CurseArtifact.relations(action: CurseRelation.() -> Unit) {
    this.relations(closureOf(action))
}
