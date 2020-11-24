plugins {
    kotlin("jvm")
    `maven-publish`
    `java-library`
    id("org.jetbrains.dokka")
    id("org.jlleitschuh.gradle.ktlint")
    id("com.jfrog.bintray")
}

repositories {
    jcenter()
    mavenCentral()
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.gradle.maven-publish")
    apply(plugin = "org.jetbrains.dokka")
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
    apply(plugin = "com.jfrog.bintray")
    apply(plugin = "org.gradle.java-library")

    val projectGroup: String by project
    val projectVersion: String by project
    val projectDescription: String by project
    group = projectGroup
    version = projectVersion
    description = projectDescription

    repositories {
        jcenter()
        mavenCentral()
    }

    val grpcVersion: String by project
    val kotlinxVersion: String by project
    val slf4jVersion: String by project
    val logbackVersion: String by project
    val jUnitVersion: String by project
    val assertjVersion: String by project

    plugins.withType<JavaPlugin> {
        extensions.configure<JavaPluginExtension> {
            sourceCompatibility = JavaVersion.VERSION_1_8
            targetCompatibility = JavaVersion.VERSION_1_8
        }

        dependencies {
            implementation(platform("io.grpc:grpc-bom:$grpcVersion"))
            implementation(platform("org.jetbrains.kotlinx:kotlinx-coroutines-bom:$kotlinxVersion"))
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
            implementation("io.grpc:grpc-core")
            implementation("io.grpc:grpc-services")
            implementation("org.slf4j:slf4j-api:$slf4jVersion")
            runtimeOnly("io.grpc:grpc-netty")
            runtimeOnly("ch.qos.logback:logback-classic:$logbackVersion")
            testImplementation(platform("org.junit:junit-bom:$jUnitVersion"))
            testImplementation("org.junit.jupiter:junit-jupiter-api")
            testImplementation("org.junit.jupiter:junit-jupiter")
            testImplementation("org.assertj:assertj-core:$assertjVersion")
            testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-debug")
        }
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions {
            freeCompilerArgs = listOf("-Xjsr305=strict")
            jvmTarget = "1.8"
        }
    }

    kotlin {
        explicitApi = org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode.Strict
    }

    tasks.test {
        useJUnitPlatform()
        testLogging {
            showStandardStreams = true
        }
        jvmArgs = listOf("-ea")
    }

    if (!project.name.endsWith("-test")) {
        tasks.dokkaHtml.configure {
            outputDirectory.set(buildDir.resolve("javadoc"))
            dokkaSourceSets.configureEach {
                jdkVersion.set(8)
                skipEmptyPackages.set(true)
                platform.set(org.jetbrains.dokka.Platform.jvm)
            }
        }

        val sourcesJar by tasks.creating(Jar::class) {
            group = JavaBasePlugin.DOCUMENTATION_GROUP
            description = "Creates a sources JAR"
            archiveClassifier.set("sources")
            from(sourceSets.main.get().allSource)
        }

        val kdocJar by tasks.creating(Jar::class) {
            group = JavaBasePlugin.DOCUMENTATION_GROUP
            description = "Creates KDoc"
            archiveClassifier.set("javadoc")
            from(tasks.dokkaHtml)
        }

        tasks.jar.configure {
            finalizedBy(sourcesJar, kdocJar)
        }

        val licenseName: String by project
        val licenseUrl: String by project
        val developerName: String by project
        val developerEmail: String by project
        val gitHubUsername: String by project

        val gitHubUrl: String by lazy { "github.com/$gitHubUsername/${rootProject.name}" }

        publishing {
            publications {
                create<MavenPublication>("maven") {
                    afterEvaluate {
                        val shadowJar = tasks.findByName("shadowJar")
                        if (shadowJar == null) from(components["java"])
                        else artifact(shadowJar)
                    }
                    artifact(kdocJar)
                    artifact(sourcesJar)
                    pom {
                        name.set("${project.group}:${project.name}")
                        description.set(project.description)
                        url.set("https://$gitHubUrl")
                        licenses {
                            license {
                                name.set(licenseName)
                                url.set(licenseUrl)
                            }
                        }
                        developers {
                            developer {
                                name.set(developerName)
                                email.set(developerEmail)
                            }
                        }
                        scm {
                            connection.set("scm:git:git://$gitHubUrl.git")
                            developerConnection.set("scm:git:ssh://github.com:$gitHubUsername/${rootProject.name}.git")
                            url.set("https://$gitHubUrl")
                        }
                    }
                }
            }
        }

        val bintrayRepo: String by project
        val projectLabels: String by project
        bintray {
            user = (findProperty("bintrayUser") ?: System.getenv("BINTRAY_USER"))?.toString()
            key = (findProperty("bintrayKey") ?: System.getenv("BINTRAY_KEY"))?.toString()
            setPublications(*publishing.publications.names.toTypedArray())
            with(pkg) {
                repo = bintrayRepo
                name = "${project.group}:${project.name}"
                desc = project.description
                websiteUrl = "https://$gitHubUrl"
                vcsUrl = "https://$gitHubUrl.git"
                setLabels(*projectLabels.split(",".toRegex()).map { it.trim() }.toTypedArray())
                setLicenses(licenseName)
                with(version) {
                    name = project.version.toString()
                    with(gpg) {
                        sign = true
                    }
                    with(mavenCentralSync) {
                        sync = true
                        user = (findProperty("sonatypeUser") ?: System.getenv("SONATYPE_USER"))?.toString()
                        password = (findProperty("sonatypePwd") ?: System.getenv("SONATYPE_PWD"))?.toString()
                    }
                }
            }
            publish = true
            override = false
            dryRun = false
        }
    }
}

val bintrayUpload: com.jfrog.bintray.gradle.tasks.BintrayUploadTask by tasks

bintrayUpload.enabled = false
