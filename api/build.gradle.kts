import net.labymod.labygradle.common.extension.LabyModAnnotationProcessorExtension.ReferenceType

dependencies {
    labyProcessor()
    labyApi("api")
    addonMavenDependency("net.baublase.publicapi:baublase-public-api-java-client:1.1.0")
}

labyModAnnotationProcessor {
    referenceType = ReferenceType.INTERFACE
}