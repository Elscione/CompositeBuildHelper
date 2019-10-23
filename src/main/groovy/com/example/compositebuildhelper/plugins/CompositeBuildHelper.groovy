package com.example.compositebuildhelper.plugins

import org.gradle.api.Plugin
import org.gradle.api.Project
import utils.Benchmarking

class CompositeBuildHelper implements Plugin<Project> {

    private Project project

    @Override
    void apply(Project target) {
        this.project = target
        registerCompositeScanEnablerTask()
        registerCompositeScanDisablerTask()

        if (isCompositeEnabled() && project.gradle.hasProperty("compositeProjects")) {
            def paths = new ArrayList<String>()
            def iterator = project.gradle.ext.compositeProjects.entrySet().iterator()
            while (iterator.hasNext()) {
                def pair = iterator.next()
                paths.add(pair.key)
            }

            registerEnablerDisablerCompositeTasks(paths)
        }
    }

    def isCompositeEnabled() {
        return new File("${project.rootDir}/.compositeScan-enable").exists()
    }

    def registerCompositeScanEnablerTask() {
        project.tasks.register("enableComposite") { task ->
            task.group = 'CompositeEnabler'
            task.description = "Enable scaning process for composite build"
            task.doLast {
                new File("${project.rootDir}/.compositeScan-enable").createNewFile()
            }
        }
    }

    def registerCompositeScanDisablerTask() {
        project.tasks.register("disableComposite") { task ->
            task.group = 'CompositeDisable'
            task.description = "Disable scaning process for composite build"
            task.doLast {
                def compositeScanEnableFile = new File("${project.rootDir}/.compositeScan-enable")
                if (compositeScanEnableFile.exists()) {
                    compositeScanEnableFile.delete()
                }
            }
        }
    }

    def registerEnablerDisablerCompositeTasks(projectsPath) {
        def benchmark = new Benchmarking("Creating enabler and disabler tasks")
        benchmark.start()

        projectsPath.each { path ->
            if (project.rootDir.path != path) {
                def lastPaths = path.replace("${project.rootDir}/", "").replace("\"", "").trim().split("/")
                def projectName = lastPaths[lastPaths.size() - 1]
                registerEnablerCompositeTask(path, projectName)
                registerDisablerCompositeTask(path, projectName)
            }
        }

        benchmark.stop()
        benchmark.print()
    }

    def registerEnablerCompositeTask(path, projectName) {
        project.tasks.register("enable${projectName}") { task ->
            task.group = 'CompositeEnabler'
            task.description = "Enable ${projectName} composite build"
            task.doLast {
                new File("${path}/.composite-enable").createNewFile()
            }
        }
    }

    def registerDisablerCompositeTask(path, projectName) {
        project.tasks.register("disable${projectName}") { task ->
            task.group = 'CompositeDisabler'
            task.description = "Disable ${projectName} composite build"
            task.doLast {
                File fileComposite = new File("${path}/.composite-enable")
                if (fileComposite.exists()) {
                    fileComposite.delete()
                }
            }
        }
    }
}
