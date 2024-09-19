package io.github.zuccherosintattico.gradle

import io.github.zuccherosintattico.gradle.CheckNodeTask.Companion.nodeBundleFile
import io.github.zuccherosintattico.gradle.Constants.MISSING_PACKAGE_JSON_ERROR
import io.github.zuccherosintattico.gradle.Constants.MISSING_TS_CONFIG_ERROR
import io.github.zuccherosintattico.gradle.Constants.PACKAGE_JSON
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.register

/**
 * A plugin to compile TypeScript files.
 */
open class Typescript : Plugin<Project> {
    override fun apply(project: Project) {
        val nodeExtension = project.extensions.create<NodeExtension>("node")
        val typescriptExtension = project.extensions.create<TypescriptExtension>("typescript")
        val checkNodeTask = project.registerTask<CheckNodeTask>("checkNode") {
            shouldInstall.set(nodeExtension.shouldInstall)
            zipUrl.set(nodeExtension.zipUrl)
            version.set(nodeExtension.version)
            nodeBundleFile.set(project.nodeBundleFile())
            projectDir.set(project.projectDir)
            outputs.upToDateWhen { false } // Don't allow gradle to mark this task as UP-TO-DATE
        }
        if (!project.fileExist(PACKAGE_JSON)) {
            throw GradleException(MISSING_PACKAGE_JSON_ERROR)
        }
        val npmDependenciesTask = project.registerTask<NpmDependenciesTask>("npmDependencies") {
            nodeBundleFile.set(checkNodeTask.flatMap { it.nodeBundleFile })
            projectDir.set(project.projectDir)
        }
        if (!project.fileExist(typescriptExtension.tsConfig.get())) {
            throw GradleException(MISSING_TS_CONFIG_ERROR)
        }
        val compileTypescriptTask = project.registerTask<TypescriptTask>("compileTypescript") {
            dependsOn(npmDependenciesTask)
            tsConfig.set(typescriptExtension.tsConfig)
            buildDir.set(typescriptExtension.outputDir)
            buildCommandExecutable.set(typescriptExtension.buildCommandExecutable)
            buildCommand.set(typescriptExtension.buildCommand)
            nodeBundleFile.set(checkNodeTask.flatMap { it.nodeBundleFile })
            projectDir.set(project.projectDir)
        }
        project.registerTask<RunJSTask>("runJS") {
            dependsOn(compileTypescriptTask)
            entrypoint.set(typescriptExtension.entrypoint)
            buildDir.set(typescriptExtension.outputDir)
            nodeBundleFile.set(checkNodeTask.flatMap { it.nodeBundleFile })
            projectDir.set(project.projectDir)
        }
        project.apply<org.gradle.api.plugins.BasePlugin>()
        project.tasks.named("build").configure {
            it.dependsOn(compileTypescriptTask)
        }
    }

    companion object {
        private inline fun <reified T : Task> Project.registerTask(name: String, noinline action: T.() -> Unit = {}) =
            tasks.register<T>(name, action)

        private fun Project.fileExist(file: String): Boolean = file(file).exists()
    }
}
