import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.intellij.platform.gradle.tasks.PrepareSandboxTask

/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

plugins {
  id("java")
  kotlin("jvm")
  id("org.jetbrains.intellij.platform.module")
}

val kotlinVersion: String by project
val ideaType: String by project
val ideaVersion: String by project
val javaVersion: String by project

repositories {
  mavenCentral()

  intellijPlatform {
    defaultRepositories()
  }
}

dependencies {
  testImplementation("org.junit.jupiter:junit-jupiter")
  compileOnly("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
  testImplementation("org.jetbrains.kotlin:kotlin-test:$kotlinVersion")
  testImplementation(testFixtures(project(":"))) // The root project

  intellijPlatform {
    create(ideaType, ideaVersion)
    testFramework(TestFrameworkType.Platform)
    testFramework(TestFrameworkType.JUnit5)
    bundledPlugins("com.intellij.java", "org.jetbrains.plugins.yaml")
    instrumentationTools()
  }
}

intellijPlatform {
  buildSearchableOptions = false
}

tasks {
  withType<Jar>().configureEach { outputs.cacheIf("BL: Hack: For every task of type jar, like org.jetbrains.intellij.platform.gradle.tasks.InstrumentedJarTask, cache it unconditionally.", {true}) }
//  withType<PrepareSandboxTask>().configureEach { outputs.cacheIf("BL: Prepare sandbox task creates the plugin config dir as a \"side effect\", so we always want it to run because we don't want to fix the task for now.",  {false}) }
  test {
    useJUnitPlatform()
  }
}

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(javaVersion))
  }
}

kotlin {
  jvmToolchain {
    languageVersion.set(JavaLanguageVersion.of(javaVersion))
  }
}
