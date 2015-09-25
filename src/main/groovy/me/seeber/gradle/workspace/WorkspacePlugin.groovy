/*
 * Copyright (c) 2015, Jochen Seeber
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package me.seeber.gradle.workspace

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.DependencyArtifact
import org.gradle.api.artifacts.DependencyResolutionListener
import org.gradle.api.artifacts.DependencySet
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.artifacts.PublishArtifact
import org.gradle.api.artifacts.ResolvableDependencies
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

public class WorkspacePlugin implements Plugin<Project> {

    protected static class ArtifactInfo {

        String group

        String name

        String type

        String classifier

        Project project

        Configuration configuration

        String getKey() {
            "${group}:${name}:${type}:${classifier}".toString()
        }

        String toString() {
            key
        }
    }

    private Logger logger = Logging.getLogger(WorkspacePlugin)

    void apply(Project targetProject) {
        logger.info "Applying workspace plugin to ${targetProject}"

        targetProject.gradle.projectsEvaluated { resolveLocalProjects(targetProject)  }

        logger.debug "Applied workspace plugin to ${targetProject}"
    }

    protected void resolveLocalProjects(Project rootProject) {
        logger.debug "Resolving local projects for ${rootProject}"

        Map<String, ArtifactInfo> knownArtifacts = [:]

        rootProject.allprojects.each { Project project ->
            project.configurations.each { Configuration configuration ->
                configuration.artifacts.each { PublishArtifact artifact ->
                    if(artifact.classifier || configuration.name == 'runtime') {
                        ArtifactInfo info = new ArtifactInfo(group: project.group, name: artifact.name, type: artifact.type, classifier: artifact.classifier, project: project, configuration: configuration)
                        knownArtifacts.put(info.key, info)
                        logger.debug "Found artifact ${info} in ${project} ${configuration}"
                    }
                }
            }
        }

        logger.debug "Replacing dependencies for ${rootProject}"

        rootProject.allprojects.each { Project project ->
            project.configurations.each { Configuration configuration ->
                replaceDependencies(project, configuration, knownArtifacts)
            }
        }

        logger.debug "Done replacing dependencies for ${rootProject}"
    }

    protected void replaceDependencies(Project project, Configuration configuration, Map<String, ArtifactInfo> knownArtifacts) {
        Set<ExternalModuleDependency> dependencies = new HashSet(configuration.dependencies.withType(ExternalModuleDependency))

        dependencies.each { ExternalModuleDependency dependency ->
            logger.debug "Inspecting dependency ${dependency}"

            ArtifactInfo knownArtifact = null

            if(dependency.artifacts.empty) {
                String key = "${dependency.group}:${dependency.name}:jar:".toString()
                knownArtifact = knownArtifacts[key]
            }
            else {
                dependency.artifacts.find { DependencyArtifact artifact ->
                    String key = "${dependency.group}:${artifact.name}:${artifact.type}:${artifact.classifier}".toString()
                    knownArtifact = knownArtifacts[key]
                    knownArtifact != null
                }
            }

            if(knownArtifact != null) {
                logger.info "Replacing dependency to ${dependency} with ${knownArtifact.project} ${knownArtifact.configuration}"

                Map<String, Object> properties = [
                    path: knownArtifact.project.path,
                    configuration: knownArtifact.configuration.name
                ]

                ProjectDependency projectDependency = project.dependencies.project(properties)
                configuration.dependencies.remove(dependency)
                configuration.dependencies.add(projectDependency)
            }
        }
    }
}
