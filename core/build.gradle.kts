import net.labymod.labygradle.common.extension.LabyModAnnotationProcessorExtension.ReferenceType

dependencies {
    labyProcessor()
    api(project(":api"))
    addonMavenDependency("net.baublase.publicapi:baublase-public-api-java-client:1.1.0")
}

labyModAnnotationProcessor {
    referenceType = ReferenceType.DEFAULT
}