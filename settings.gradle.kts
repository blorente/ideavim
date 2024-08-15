// Set repository for snapshot versions of gradle plugin
pluginManagement {
  repositories {
    maven {
      url = uri("https://oss.sonatype.org/content/repositories/snapshots/")
    }
    gradlePluginPortal()
  }
}

plugins {
  id("com.gradle.develocity") version("3.17.6")
  id("com.gradle.common-custom-user-data-gradle-plugin") version "2.0.2"
}

rootProject.name = "IdeaVIM"

develocity {
  server.set("http://my-dv.54.234.192.216.nip.io/")
  allowUntrustedServer.set(true)
  buildScan {
      uploadInBackground.set(true) // TODO BL: Adjust for CI if we had a CI flag
  }
}

include("vim-engine")
include("scripts")
include("annotation-processors")
include("tests:java-tests")
include("tests:property-tests")
include("tests:long-running-tests")
include("tests:ui-ij-tests")
include("tests:ui-py-tests")
include("tests:ui-fixtures")
