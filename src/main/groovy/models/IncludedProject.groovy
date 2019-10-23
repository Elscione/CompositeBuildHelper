package models

class IncludedProject {
    def groupId, artifactId, moduleName, path

    IncludedProject(groupId, artifactId, moduleName, path) {
        this.groupId = groupId
        this.artifactId = artifactId
        this.moduleName = moduleName
        this.path = path
    }

    def getGradleFile() {
        def gradleFile = new File("$path/build.gradle")
        if (!gradleFile.exists()) {
            gradleFile = new File("$path/build.gradle.kts")
        }

        return gradleFile
    }
}
