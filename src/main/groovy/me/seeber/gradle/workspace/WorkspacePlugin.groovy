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
import org.gradle.api.artifacts.DependencyArtifact
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.artifacts.PublishArtifact
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

public class WorkspacePlugin implements Plugin<Project> {

    protected static class ArtifactInfo {

        String name

        String type

        String classifier

        Project project

        Configuration configuration

        public String getKey() {
            "${name}:${type}:${classifier}"
        }
    }

    private Logger logger = Logging.getLogger(WorkspacePlugin)

    public void apply(Project targetProject) {
        targetProject.configure(targetProject) {
            gradle.projectsEvaluated { resolveLocalProjects(targetProject.rootProject) }
        }
    }

    protected void resolveLocalProjects(Project rootProject) {
        Map<String, ArtifactInfo> knownArtifacts = [:]

        rootProject.allprojects.each { Project project ->
            project.configurations.each { Configuration configuration ->
                configuration.artifacts.each { PublishArtifact artifact ->
                    ArtifactInfo info = new ArtifactInfo(name: artifact.name, type: artifact.type, classifier: artifact.classifier, project: project, configuration: configuration)
                    knownArtifacts.put(info.key, info)
                }
            }
        }

        rootProject.allprojects.each { Project project ->
            project.configurations.each { Configuration configuration ->
                configuration.dependencies.withType(ExternalModuleDependency).collect().each { ExternalModuleDependency dependency ->
                    String type = "jar"
                    String classifier = ""
                    DependencyArtifact artifact = dependency.artifacts[0]

                    if(artifact != null) {
                        type = artifact.type
                        classifier = artifact.classifier
                    }

                    String key = "${dependency.name}:${type}:${classifier}"
                    ArtifactInfo knownArtifact = knownArtifacts[key]

                    if(knownArtifact != null) {
                        logger.info "Replacing dependency to ${dependency} with ${knownArtifact.project}:${knownArtifact.configuration}"

                        Map<String, Object> properties = [
                            path: knownArtifact.project.path,
                            configuration: knownArtifact.configuration.name
                        ]

                        ProjectDependency projectDependency = project.dependencies.project(properties)
                        dependency.artifacts = projectDependency.artifacts.clone

                        configuration.dependencies.remove(dependency)
                        configuration.dependencies.add(projectDependency)
                    }
                }
            }
        }
    }
}