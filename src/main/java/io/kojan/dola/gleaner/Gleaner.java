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

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import javax.inject.Inject;
import org.apache.maven.api.ArtifactCoordinates;
import org.apache.maven.api.DownloadedArtifact;
import org.apache.maven.api.RemoteRepository;
import org.apache.maven.api.Session;
import org.apache.maven.api.model.Dependency;
import org.apache.maven.api.model.Model;
import org.apache.maven.api.model.Parent;
import org.apache.maven.api.model.Plugin;
import org.apache.maven.api.services.ArtifactResolverException;
import org.apache.maven.api.services.VersionRangeResolverException;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.LifecycleExecutor;
import org.apache.maven.lifecycle.MavenExecutionPlan;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.project.MavenProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Mikolaj Izdebski
 */
public class Gleaner {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Inject private Collector collector;

    @Inject private LifecycleExecutor lifecycleExecutor;

    private Map<String, Map<String, Map<String, MavenProject>>> reactorMap;

    private Set<String> brs = new TreeSet<>();

    private MavenProject lookupReactor(ArtifactCoordinates coords) {
        String groupId = coords.getGroupId();
        String artifactId = coords.getArtifactId();
        String version = coords.getVersionConstraint().getRecommendedVersion().asString();
        return reactorMap
                .getOrDefault(groupId, Map.of())
                .getOrDefault(artifactId, Map.of())
                .getOrDefault(version, null);
    }

    private Session session;

    private DependencyFilter filter;
    private CompatVersionResolver compatVersionResolver;

    static void checkVersionRange(ArtifactCoordinates coords) {
        if (coords.getVersionConstraint().getVersionRange() != null) {
            throw new RuntimeException("Version ranges are not supported: " + coords);
        }
    }

    private Path tryResolve(Session session, ArtifactCoordinates coords) {
        try {
            List<RemoteRepository> repositories = session.getRemoteRepositories();
            checkVersionRange(coords);
            DownloadedArtifact resolved = session.resolveArtifact(coords, repositories);
            logger.debug("Dependency found at {}", resolved.getPath());
            return resolved.getPath();
        } catch (VersionRangeResolverException | ArtifactResolverException e) {
            return null;
        }
    }

    private void processParent(Parent parent) {
        ArtifactCoordinates coords =
                session.createArtifactCoordinates(
                        parent.getGroupId(), parent.getArtifactId(), parent.getVersion(), "pom");
        MavenProject reactorProject = lookupReactor(coords);
        if (reactorProject != null) {
            logger.debug("    --> reactor: {}", reactorProject.getArtifactId());
            return;
        }
        Dep dep = collector.lookup(coords);
        dep.foundAt(parent);
    }

    private void processDependency(Dependency dependency) {
        ArtifactCoordinates coords =
                session.createArtifactCoordinates(
                        dependency.getGroupId(),
                        dependency.getArtifactId(),
                        dependency.getVersion(),
                        dependency.getClassifier(),
                        null,
                        dependency.getType());
        MavenProject reactorProject = lookupReactor(coords);
        if (reactorProject != null) {
            logger.debug("    --> reactor: {}", reactorProject.getArtifactId());
            return;
        }
        Dep dep = collector.lookup(coords);
        dep.foundAt(dependency);
    }

    private void processPlugin(Plugin plugin) {
        ArtifactCoordinates coords =
                session.createArtifactCoordinates(
                        plugin.getGroupId(),
                        plugin.getArtifactId(),
                        plugin.getVersion(),
                        null,
                        null,
                        "maven-plugin");
        MavenProject reactorProject = lookupReactor(coords);
        if (reactorProject != null) {
            logger.debug("    --> reactor: {}", reactorProject.getArtifactId());
            return;
        }
        Dep dep = collector.lookup(coords);
        dep.foundAt(plugin);
    }

    private void addDep(Dep dep) {
        if (filter.isDependencyFiltered(dep)) {
            logger.warn("Dependency {} is filtered", dep.id);
            return;
        }
        String version = compatVersionResolver.resolveVersionFor(dep);
        if (!version.equals("SYSTEM")) {
            logger.info("Using compat version {} for {}", version, dep.id);
        }
        dep.resolvedVersion = version;
        brs.add(dep.rpmDepString());
    }

    private boolean resolveDeps() {
        brs.clear();
        List<Dep> unresolved = new ArrayList<>();
        for (Dep dep : collector.deps.values()) {
            if (dep.resolved == null) {
                ArtifactCoordinates coords = dep.coords;
                logger.debug("Resolving dep {}", coords);
                Path path = tryResolve(session, coords);
                if (path != null) {
                    dep.resolved = true;
                    logger.debug("Dependency {} found at {}", coords, path);
                } else {
                    dep.resolved = false;
                    unresolved.add(dep);
                    logger.debug("Dependency {} ABSENT", coords);
                }
            } else if (!dep.resolved) {
                unresolved.add(dep);
            }
        }
        List<Dep> strong =
                collector.deps.values().stream()
                        .filter(dep -> !dep.foundLocations.isEmpty())
                        .toList();
        boolean unresolvedStrong = false;
        for (Dep dep : strong) {
            if (dep.resolved) {
                logger.info("Strong dependency: {}", dep.id);
            } else {
                unresolvedStrong = true;
                logger.error("Unresolved strong dependency: {}", dep.id);
            }
            for (String location : dep.foundLocations) {
                logger.info("  declared at {}", location);
            }
            addDep(dep);
        }
        if (unresolved.isEmpty()) {
            return true;
        }
        if (unresolvedStrong) {
            return false;
        }
        for (Dep dep : unresolved) {
            if (!dep.resolved) {
                logger.error("Unresolved weak dependency: {}", dep.id);
                addDep(dep);
            }
        }
        return false;
    }

    private Set<String> mapPluginDepScope(String scope) {
        if (scope == null) {
            return Set.of();
        }
        return switch (scope) {
            case Artifact.SCOPE_COMPILE ->
                    Set.of(Artifact.SCOPE_SYSTEM, Artifact.SCOPE_PROVIDED, Artifact.SCOPE_COMPILE);
            case Artifact.SCOPE_RUNTIME -> Set.of(Artifact.SCOPE_COMPILE, Artifact.SCOPE_RUNTIME);
            case Artifact.SCOPE_COMPILE_PLUS_RUNTIME ->
                    Set.of(
                            Artifact.SCOPE_SYSTEM,
                            Artifact.SCOPE_PROVIDED,
                            Artifact.SCOPE_COMPILE,
                            Artifact.SCOPE_RUNTIME);
            case Artifact.SCOPE_RUNTIME_PLUS_SYSTEM ->
                    Set.of(Artifact.SCOPE_SYSTEM, Artifact.SCOPE_COMPILE, Artifact.SCOPE_RUNTIME);
            case Artifact.SCOPE_TEST ->
                    Set.of(
                            Artifact.SCOPE_SYSTEM,
                            Artifact.SCOPE_PROVIDED,
                            Artifact.SCOPE_COMPILE,
                            Artifact.SCOPE_RUNTIME,
                            Artifact.SCOPE_TEST);
            default -> Set.of();
        };
    }

    private void output() {
        logger.info(
                "BEGIN MAVEN BUILD DEPENDENCIES"
                        + brs.stream()
                                .map(br -> "\nBuildRequires:  " + br)
                                .collect(Collectors.joining()));
        logger.info("END MAVEN BUILD DEPENDENCIES");
        String outFileProp = System.getProperty("dola.gleaner.outputFile");
        if (outFileProp != null) {
            Path path = Path.of(outFileProp);
            try (Writer w = Files.newBufferedWriter(path)) {
                for (String br : brs) {
                    w.write(br);
                    w.write("\n");
                }
            } catch (IOException e) {
                logger.error("I/O exception when writing output file " + path, e);
            }
        }
    }

    public void execute(MavenSession mavenSession) {

        filter = DependencyFilter.parseFromProperties(System.getProperties());
        compatVersionResolver = CompatVersionResolver.parseFromProperties(System.getProperties());

        session = mavenSession.getSession();

        List<MavenProject> allProjects = mavenSession.getAllProjects();
        reactorMap = new LinkedHashMap<>();
        allProjects.forEach(
                project ->
                        reactorMap
                                .computeIfAbsent(project.getGroupId(), k -> new LinkedHashMap<>())
                                .computeIfAbsent(
                                        project.getArtifactId(), k -> new LinkedHashMap<>())
                                .put(project.getVersion(), project));

        for (MavenProject project : mavenSession.getAllProjects()) {
            Parent parent = project.getModel().getDelegate().getParent();
            if (parent != null) {
                processParent(parent);
            }
        }

        if (!resolveDeps()) {
            logger.error("Missing model dependencies");
            output();
            return;
        }

        try {
            for (MavenProject project : mavenSession.getAllProjects()) {
                mavenSession.setCurrentProject(project);
                lifecycleExecutor.calculateExecutionPlan(
                        mavenSession, false, mavenSession.getGoals().toArray(new String[0]));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        if (!resolveDeps()) {
            logger.error("Missing plan dependencies");
            output();
            return;
        }

        try {
            for (MavenProject project : mavenSession.getAllProjects()) {
                Model model = project.getModel().getDelegate();
                Parent parent = model.getParent();
                if (parent != null) {
                    processParent(parent);
                }
                mavenSession.setCurrentProject(project);
                MavenExecutionPlan plan =
                        lifecycleExecutor.calculateExecutionPlan(
                                mavenSession, true, mavenSession.getGoals().toArray(new String[0]));
                logger.info("Build plan for project {}", model.getArtifactId());
                String phase = "";
                Set<String> scopes = new LinkedHashSet<>();
                for (MojoExecution execution : plan.getMojoExecutions()) {
                    if (!phase.equals(execution.getLifecyclePhase())) {
                        phase = execution.getLifecyclePhase();
                        logger.info("  Phase {}", phase);
                    }
                    Plugin plugin = execution.getPlugin().getDelegate();
                    processPlugin(plugin);
                    MojoDescriptor mojo = execution.getMojoDescriptor();
                    String reqScope = mojo.getDependencyResolutionRequired();
                    Set<String> thisScopes = mapPluginDepScope(reqScope);
                    scopes.addAll(thisScopes);
                    logger.info(
                            "    Execution: plugin {} goal {} id {} scope {}{}",
                            execution.getArtifactId(),
                            execution.getGoal(),
                            execution.getExecutionId(),
                            reqScope,
                            thisScopes);
                    for (Dependency dependency : plugin.getDependencies()) {
                        String scope = dependency.getScope();
                        if (thisScopes.contains(scope)) {
                            processDependency(dependency);
                        } else {
                            logger.debug("Plugin dependency scope {} excluded", scope);
                        }
                    }
                }
                logger.info("  Required dependency scopes: {}", scopes);
                for (Dependency dependency : model.getDependencies()) {
                    String scope = dependency.getScope();
                    if (scopes.contains(scope)) {
                        processDependency(dependency);
                    } else {
                        logger.debug("Dependency scope {} excluded", scope);
                    }
                }
            }
            collector.summarize();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        if (!resolveDeps()) {
            logger.error("Missing exec dependencies");
            output();
            return;
        }
        logger.info("BUILD DEPS READY");
        output();
    }
}
