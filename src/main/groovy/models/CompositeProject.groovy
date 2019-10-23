package models

class CompositeProject {
    def isCompositeEnabled
    def settingsGradleFile
    def includedProjects = new HashMap<String, IncludedProject>()

    CompositeProject(File settingsFile) {
        this.settingsGradleFile = settingsFile
        this.isCompositeEnabled = new File(settingsFile.path.replaceAll(/settings.gradle|.kts/, ".composite-enable")).exists()
    }
}