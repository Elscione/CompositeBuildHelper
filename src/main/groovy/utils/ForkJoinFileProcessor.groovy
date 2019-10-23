package utils

import models.CompositeProject
import models.IncludedProject

import java.util.concurrent.RecursiveAction

class ForkJoinFileProcessor extends RecursiveAction {

    static String rootDir
    static final int MAX_FILE_PER_THREAD = 50

    Map<String, CompositeProject> compositeProjects

    ForkJoinFileProcessor(Map<String, CompositeProject> compositeProjects, String rootDir) {
        this.compositeProjects = compositeProjects
        this.rootDir = rootDir
    }

    ForkJoinFileProcessor(Map<String, CompositeProject> compositeProjects) {
        this.compositeProjects = compositeProjects
    }

    @Override
    protected void compute() {
        if (compositeProjects.size() > MAX_FILE_PER_THREAD) {
            createSubTasks()
        } else {
            processFile()
        }
    }

    def createSubTasks() {
        def leftTask
        def rightTask

        def left = new HashMap<String, CompositeProject>()
        def right = new HashMap<String, CompositeProject>()

        def iterator = compositeProjects.entrySet().iterator()

        while (iterator.hasNext()) {
            def pair = iterator.next()

            if (left.size() < iterator.size() / 2) {
                left.put(pair.key, pair.value)
            } else {
                right.put(pair.key, pair.value)
            }

            if (left.size() == iterator.size() / 2 && leftTask == null) {
                leftTask = new ForkJoinFileProcessor(left)
                leftTask.fork()
            }

            if (!iterator.hasNext()) {
                rightTask = new ForkJoinFileProcessor(right)
                rightTask.fork()
            }
        }

        if (leftTask != null) leftTask.join()
        if (rightTask != null) rightTask.join()
    }

    def processFile() {
        def compositeIterator = compositeProjects.entrySet().iterator()
        while (compositeIterator.hasNext()) {
            def compositeProject = compositeIterator.next()

            if (compositeProject.value.isCompositeEnabled) {
                processSettingsGradle compositeProject.key, compositeProject.value

                def includedIterator = compositeProject.value.includedProjects.entrySet().iterator()
                while (includedIterator.hasNext()) {
                    processBuildGradle includedIterator.next().value
                }
            }
        }
    }

    def processBuildGradle(IncludedProject project) {
        project.getGradleFile().eachLine { line ->
            if (line.trim().startsWith("groupId")) {
                project.groupId = line.split(/['"]/)[1]
            } else if (line.trim().startsWith("artifactId")) {
                project.artifactId = line.split(/['"]/)[1]
            }
        }
    }

    def processSettingsGradle(path, compositeProject) {
        compositeProject.settingsGradleFile.eachLine { line ->
            if (line.startsWith("include") && !line.startsWith("includeBuild")) {
                String[] tempString = line.split(/['"]/)
                for (int i = 1; i < tempString.size(); i += 2) {
                    def includedPath = "${path}/${tempString[i].replace(":", "")}".replace("//", "/")
                    def includedProject = new IncludedProject(null, null, tempString[i], includedPath)
                    compositeProject.includedProjects.put(tempString[i], includedProject)
                }
            } else if (line.contains("projectDir")) {
                def tempString = line.split(/['"]/)
                compositeProject.includedProjects.get(tempString[1]).path = "${path}/${tempString[3]}".replace("//", "/")
            }
        }
    }
}