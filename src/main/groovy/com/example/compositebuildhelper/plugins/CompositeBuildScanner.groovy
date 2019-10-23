package com.example.compositebuildhelper.plugins

import models.CompositeProject
import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings
import utils.Benchmarking
import utils.ForkJoinFileProcessor

import java.util.concurrent.ForkJoinPool

class CompositeBuildScanner implements Plugin<Settings> {

    Settings settings

    @Override
    void apply(Settings settings) {
        this.settings = settings
        settings.gradle.ext.compositeProjects = new HashMap<String, CompositeProject>()
        if (new File("${settings.rootDir}/.compositeScan-enable").exists()) {
            def compositePaths = getCompositePaths()
            scanCompositePaths(compositePaths)
            processComposite()
            substituteBinaryDependencies()
        }
    }

    def getCompositePaths() {
        def compositeProjectPathsFile = new File("${settings.rootDir}/.compositeProjectPaths.txt")
        if (!settings.gradle.hasProperty("compositeProjectPath") && !compositeProjectPathsFile.exists()) {
            throw new Exception("Cannot scan for composite projects\n" +
                    "Possible Solution: " +
                    "Please define a gradle object extra properties named compositeProjectPath \n" +
                    "in settings.gradle file that apply CompositeBuildHelper plugin. Or create a file named .compositeProjectPaths.txt\n" +
                    "in your root project directory.\n" +
                    "The extra property is an array containing paths where your composite project saved.\n" +
                    "The file contains paths where your composite project saved (1 path per line and all path are relative to your root project directory).\n" +
                    "Example when using extra property: gradle.ext.compositeProjectPath = [\"\$rootDir/../subProjects\"]\n" +
                    "Example when using file : ../subProjects\n")
        }

        def compositePaths = new HashSet<String>()

        if (settings.gradle.hasProperty("compositeProjectPath")) {
            compositePaths.addAll(settings.gradle.ext.compositeProjectPath)
        }

        if (compositeProjectPathsFile.exists() && compositeProjectPathsFile.canRead()) {
            compositePaths.addAll(compositeProjectPathsFile.readLines())
        }

        println "Found ${compositePaths.size()} paths to scan"

        return compositePaths
    }

    def scanCompositePaths(compositePaths) {
        def benchmark = new Benchmarking("Composite Scan")

        benchmark.start()

        compositePaths.each { compositePath ->
            println "Scanning projects at directory ${compositePath}"
            def settingsFiles = new ArrayList<File>()
            traverse(new File(compositePath.replace("${settings.rootDir.path}", "")), settingsFiles)
            scanComposite(settingsFiles)
        }

        benchmark.stop()
        benchmark.print()
    }

    def scanComposite(settingsFiles) {
        settingsFiles.each {
            def path = it.path.replaceAll(/\/settings.gradle|.kts/, "")
            def tempPath = path.split("/")
            println "Reading project ${tempPath[tempPath.size() - 1]} settings.gradle file"
            if (path != settings.rootDir.path) {
                settings.gradle.ext.compositeProjects.putIfAbsent(path, new CompositeProject(it))
            }
        }
    }

    def processComposite() {
        def benchmark = new Benchmarking("Processing projects")
        benchmark.start()

        ForkJoinPool forkJoinPool = new ForkJoinPool()
        forkJoinPool.invoke(new ForkJoinFileProcessor(settings.gradle.ext.compositeProjects, settings.rootDir.path))

        benchmark.stop()
        benchmark.print()
    }

    def substituteBinaryDependencies() {
        def compositeIterator = settings.gradle.ext.compositeProjects.entrySet().iterator()
        def benchmark = new Benchmarking("Substituting dependencies")
        benchmark.start()

        while (compositeIterator.hasNext()) {
            def compositePair = compositeIterator.next()
            if (compositePair.value.isCompositeEnabled) {
                settings.includeBuild(compositePair.key) { includedBuild ->
                    includedBuild.dependencySubstitution { depSubstitution ->
                        def includedIterator = compositePair.value.includedProjects.entrySet().iterator()
                        while (includedIterator.hasNext()) {
                            def includedPair = includedIterator.next()
                            if (includedPair.value.groupId != null && includedPair.value.artifactId != null) {
                                depSubstitution.substitute depSubstitution.module("${includedPair.value.groupId}:${includedPair.value.artifactId}") with depSubstitution.project("${includedPair.value.moduleName}")
                            }
                        }
                    }
                }
            }
        }

        benchmark.stop()
        benchmark.print()
    }

    def traverse(File path, ArrayList<File> settingsFiles) {
        if (path.isDirectory()) {
            if (isPathExcluded(path)) {
                path.eachFile {
                    if (it.isFile()) {
                        if (it.name.matches("settings.gradle|settings.gradle.kts")) {
                            settingsFiles.add(it)
                        }
                    } else if (isPathExcluded(it)) {
                        traverse(it, settingsFiles)
                    }
                }
            }
        } else {
            if (path.name.matches("settings.gradle|settings.gradle.kts")) {
                settingsFiles.add(path)
            }
        }
    }

    def isPathExcluded(path) {
        return !path.name.matches("src|.gradle|.idea|build")
    }
}
