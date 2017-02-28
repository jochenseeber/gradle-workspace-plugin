/**
 * BSD 2-Clause License
 *
 * Copyright (c) 2016-2017, Jochen Seeber
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package me.seeber.gradle.workspace;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import javax.inject.Inject;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.ProjectEvaluationListener;
import org.gradle.api.ProjectState;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.DependencyArtifact;
import org.gradle.api.artifacts.ExternalModuleDependency;
import org.gradle.api.artifacts.PublishArtifact;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

/**
 * Plugin to manage multi-project workspaces
 */
public class WorkspacePlugin implements Plugin<Project> {

    /**
     * Joiner used to concatenate artifact parts
     */
    protected static final Joiner ARTIFACT_JOINER = Objects.requireNonNull(Joiner.on(":").skipNulls());

    public static final Comparator<ExportingConfiguration> CONFIGURATION_INFO_COMPARATOR = Comparator
            .<ExportingConfiguration, String> comparing(i -> i.getProject().getPath())
            .thenComparing(i -> i.getConfiguration().getName());

    /**
     * Configuration that exports an artifact
     */
    protected static class ExportingConfiguration {

        /**
         * Project the configuration belongs to
         */
        private final Project project;

        /**
         * Configuration that exports an artifact
         */
        private final Configuration configuration;

        /**
         * Create a new exporting configuration
         *
         * @param project Project the configuration belongs to
         * @param configuration Configuration that exports an artifact
         */
        public ExportingConfiguration(Project project, Configuration configuration) {
            this.project = project;
            this.configuration = configuration;
        }

        /**
         * Get the project the configuration belongs to
         *
         * @return Project
         */
        public Project getProject() {
            return this.project;
        }

        /**
         * Get the configuration that exports an artifact
         *
         * @return Configuration
         */
        public Configuration getConfiguration() {
            return this.configuration;
        }

        /**
         * Get the properties as a map
         *
         * @return Map containing the properties
         */
        public Map<String, ?> getProperties() {
            Map<String, ?> properties = ImmutableMap.of("path", this.project.getPath(), "configuration",
                    this.configuration.getName());
            return properties;
        }

        /**
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals(@Nullable Object object) {
            boolean result = false;

            if (this == object) {
                result = true;
            }
            else if (object != null && getClass() == object.getClass()) {
                ExportingConfiguration other = (ExportingConfiguration) object;

                result = Objects.equals(this.getProject().getPath(), other.getProject().getPath())
                        && Objects.equals(this.getConfiguration().getName(), other.getConfiguration().getName());
            }

            return result;
        }

        /**
         * @see java.lang.Object#hashCode()
         */
        @Override
        public int hashCode() {
            int hash = 0;

            hash = (31 * hash) + getProject().getPath().hashCode();
            hash = (31 * hash) + getConfiguration().getName().hashCode();

            return hash;
        }

        /**
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return getConfiguration().toString();
        }
    }

    /**
     * Project the plugin was applied to
     */
    private @Nullable Project project;

    /**
     * Logger if we feel talkative...
     */
    private final Logger logger;

    /**
     * Create a new workspace plugin
     */
    @Inject
    public WorkspacePlugin() {
        this.logger = Logging.getLogger(getClass());
    }

    /**
     * @see org.gradle.api.Plugin#apply(java.lang.Object)
     */
    @Override
    public void apply(Project project) {
        this.project = project;

        WorkspaceConfig workspaceConfig = new WorkspaceConfig();
        project.getExtensions().add("workspaceConfig", workspaceConfig);

        getLogger().info("Applying workspace plugin to {}", project);

        project.getGradle().addProjectEvaluationListener(new ProjectEvaluationListener() {
            @Override
            public void beforeEvaluate(@Nullable Project project) {
            }

            @Override
            public void afterEvaluate(@Nullable Project project, @Nullable ProjectState state) {
                if (project != null) {
                    replaceDependencies(project);
                }
            }
        });

        getLogger().debug("Applied workspace plugin to {}", project);
    }

    /**
     * Replace dependencies of a project with local projects
     *
     * @param project Project to replace dependencies
     */
    protected void replaceDependencies(Project project) {
        project.getConfigurations().all(c -> {
            Multimap<String, ExportingConfiguration> exports = getExportingConfigurations(
                    project.getRootProject().getAllprojects());

            replaceDependencies(project, c, exports);
        });
    }

    /**
     * Replace the dependencies of a configuration
     *
     * @param project Project the configuration belongs to
     * @param configuration Configuration whose dependencies to replace
     * @param exports Exporting configurations
     */
    protected void replaceDependencies(Project project, Configuration configuration,
            Multimap<@NonNull String, @NonNull ExportingConfiguration> exports) {
        List<@NonNull Dependency> removeDependencies = new ArrayList<>();
        List<@NonNull Dependency> addDependencies = new ArrayList<>();

        getLogger().debug("Replacing dependencies for {}", configuration);

        for (Dependency dependency : configuration.getDependencies()) {
            if (dependency instanceof ExternalModuleDependency) {
                ExternalModuleDependency moduleDependency = (ExternalModuleDependency) dependency;
                Collection<ExportingConfiguration> infos = null;

                if (moduleDependency.getArtifacts().isEmpty()) {
                    infos = exports.get(moduleKey(moduleDependency));
                }
                else {
                    infos = new TreeSet<>(CONFIGURATION_INFO_COMPARATOR);
                    boolean first = false;

                    for (DependencyArtifact artifact : moduleDependency.getArtifacts()) {
                        Collection<@NonNull ExportingConfiguration> additionalInfos = exports
                                .get(moduleKey(dependency, artifact));

                        if (additionalInfos != null) {
                            if (first) {
                                infos.addAll(additionalInfos);
                            }
                            else {
                                infos.retainAll(additionalInfos);
                            }
                        }
                    }
                }

                if (infos != null && !infos.isEmpty()) {
                    ExportingConfiguration info = infos.iterator().next();
                    Dependency projectDependency = project.getDependencies().project(info.getProperties());

                    getLogger().debug("Replacing dependency {} with {}", dependency, projectDependency);

                    removeDependencies.add(moduleDependency);
                    addDependencies.add(projectDependency);
                }
            }
        }

        configuration.getDependencies().removeAll(removeDependencies);
        configuration.getDependencies().addAll(addDependencies);
    }

    /**
     * Get the configurations that export an artifact
     *
     * @param projects Projects to search
     * @return Exporting configurations
     */
    protected Multimap<@NonNull String, @NonNull ExportingConfiguration> getExportingConfigurations(
            Collection<@NonNull Project> projects) {
        Multimap<@NonNull String, @NonNull ExportingConfiguration> exports = Multimaps.newSetMultimap(new HashMap<>(),
                () -> new TreeSet<>(CONFIGURATION_INFO_COMPARATOR));

        for (Project project : projects) {
            Set<String> configurationNames = ImmutableSet.of("default");
            WorkspaceConfig workspaceConfig = project.getExtensions().findByType(WorkspaceConfig.class);

            if (workspaceConfig != null) {
                configurationNames = workspaceConfig.getExportedConfigurations();
            }

            for (String configurationName : configurationNames) {
                Configuration configuration = project.getConfigurations().findByName(configurationName);

                if (configuration != null) {
                    getExportingConfigurations(project, configuration, exports);
                }
            }
        }

        return exports;
    }

    /**
     * Get the configurations that export an artifact
     *
     * @param project Project to search
     * @param configuration Configuration to search
     * @param exports Known modules
     */
    protected void getExportingConfigurations(Project project, Configuration configuration,
            Multimap<@NonNull String, @NonNull ExportingConfiguration> exports) {
        for (PublishArtifact artifact : configuration.getArtifacts()) {
            String key = moduleKey(project, artifact);
            exports.put(key, new ExportingConfiguration(project, configuration));
        }
    }

    /**
     * Provide a string key for an artifact
     *
     * @param project Project the artifact belongs to
     * @param artifact Artifact Artifact to get info for
     * @return Module key
     */
    protected String moduleKey(Project project, PublishArtifact artifact) {
        String key = Objects.requireNonNull(ARTIFACT_JOINER.join(project.getGroup(), artifact.getName(),
                artifact.getType(), artifact.getExtension(), Strings.emptyToNull(artifact.getClassifier())));
        return key;
    }

    /**
     * Provide a string key for a dependency
     *
     * @param dependency Selector to provide key for
     * @return Module key
     */
    protected String moduleKey(ExternalModuleDependency dependency) {
        String key = Objects
                .requireNonNull(ARTIFACT_JOINER.join(dependency.getGroup(), dependency.getName(), "jar", "jar"));
        return key;
    }

    /**
     * Provide a string key for a dependency artifact
     *
     * @param dependency Dependency the artifact belongs to
     * @param artifact Artifact to provide key for
     * @return Module key
     */
    protected String moduleKey(Dependency dependency, DependencyArtifact artifact) {
        String key = Objects.requireNonNull(ARTIFACT_JOINER.join(dependency.getGroup(), artifact.getName(),
                artifact.getType(), artifact.getExtension(), Strings.emptyToNull(artifact.getClassifier())));
        return key;
    }

    /**
     * Get the project the plugin was applied to
     *
     * @return Project
     */
    protected Project getProject() {
        return Objects.requireNonNull(this.project);
    }

    /**
     * Get the logger if we feel talkative
     *
     * @return Logger
     */
    protected Logger getLogger() {
        return this.logger;
    }
}
