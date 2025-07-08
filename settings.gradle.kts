plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version("0.4.0")
}

rootProject.name = "kgdq-parent"
include(":discord")
include(":kgdq")
include(":api-server")
include(":api-client-kt")
include(":api-models")
include(":horaro")
include(":serializers")
include(":reddit")
include(":srcom")
include(":db")
