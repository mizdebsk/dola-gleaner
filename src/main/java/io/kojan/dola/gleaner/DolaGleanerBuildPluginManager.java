/*-
 * Copyright (c) 2025 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.kojan.dola.gleaner;

import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.InvalidPluginDescriptorException;
import org.apache.maven.plugin.MavenPluginManager;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.MojoNotFoundException;
import org.apache.maven.plugin.PluginConfigurationException;
import org.apache.maven.plugin.PluginDescriptorParsingException;
import org.apache.maven.plugin.PluginManagerException;
import org.apache.maven.plugin.PluginNotFoundException;
import org.apache.maven.plugin.PluginResolutionException;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.sisu.Priority;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
@Priority(100)
public class DolaGleanerBuildPluginManager implements BuildPluginManager {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Inject private MavenPluginManager mavenPluginManager;

    @Inject private Collector collector;

    @Override
    public MojoDescriptor getMojoDescriptor(
            Plugin plugin,
            String goal,
            List<RemoteRepository> repositories,
            RepositorySystemSession session)
            throws PluginNotFoundException,
                    PluginResolutionException,
                    PluginDescriptorParsingException,
                    MojoNotFoundException,
                    InvalidPluginDescriptorException {

        Dep dep =
                collector.lookup(
                        plugin.getGroupId(),
                        plugin.getArtifactId(),
                        "jar",
                        "",
                        plugin.getVersion());
        try {
            MojoDescriptor mojo =
                    mavenPluginManager.getMojoDescriptor(plugin, goal, repositories, session);
            dep.resolved = true;
            return mojo;
        } catch (Exception e) {
            dep.resolved = false;

            PluginDescriptor pd = new PluginDescriptor();
            pd.setGroupId(plugin.getGroupId());
            pd.setArtifactId(plugin.getArtifactId());
            pd.setVersion(plugin.getVersion());
            pd.setPlugin(plugin);

            MojoDescriptor md = new MojoDescriptor();
            md.setGoal(goal);
            md.setPluginDescriptor(pd);
            md.setPhase("validate");

            logger.debug("Stubbed plugin {} goal {}", plugin.getArtifactId(), goal);
            return md;
        }
    }

    @Override
    public PluginDescriptor loadPlugin(
            Plugin plugin, List<RemoteRepository> repositories, RepositorySystemSession session)
            throws PluginNotFoundException,
                    PluginResolutionException,
                    PluginDescriptorParsingException,
                    InvalidPluginDescriptorException {
        throw new IllegalStateException("XBPM: loadPlugin called");
    }

    @Override
    public ClassRealm getPluginRealm(MavenSession session, PluginDescriptor pluginDescriptor)
            throws PluginResolutionException, PluginManagerException {
        throw new IllegalStateException("XBPM: getPluginRealm called");
    }

    @Override
    public void executeMojo(MavenSession session, MojoExecution execution)
            throws MojoFailureException,
                    MojoExecutionException,
                    PluginConfigurationException,
                    PluginManagerException {
        throw new IllegalStateException("XBPM: executeMojo called");
    }
}
