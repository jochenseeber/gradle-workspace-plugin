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

import java.util.Set;

import com.google.common.collect.ImmutableSet;

/**
 * Workspace configuration
 */
public class WorkspaceConfig {

    /**
     * Configurations that are exported to workspace builds
     *
     * The workspace plugin only replaces dependencies to projects from the build if the required artifacts are created
     * by an exported configurationof the target project.
     *
     * Defaults to "runtime" and "testRuntime".
     */
    private Set<String> exportedConfigurations = ImmutableSet.of("runtime", "testRuntime");

    /**
     * Get the exported configurations
     *
     * @return Exported configurations
     */
    public Set<String> getExportedConfigurations() {
        return this.exportedConfigurations;
    }

    /**
     * Set the exported configurations
     *
     * @param configurations Exported configurations
     */
    public void setExportedConfigurations(Set<String> configurations) {
        this.exportedConfigurations = ImmutableSet.copyOf(configurations);
    }

}
