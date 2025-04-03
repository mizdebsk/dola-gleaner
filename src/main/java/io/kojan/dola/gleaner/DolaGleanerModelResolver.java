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

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.apache.maven.api.ArtifactCoordinates;
import org.apache.maven.api.DownloadedArtifact;
import org.apache.maven.api.RemoteRepository;
import org.apache.maven.api.Session;
import org.apache.maven.api.model.Dependency;
import org.apache.maven.api.model.Parent;
import org.apache.maven.api.services.ArtifactResolverException;
import org.apache.maven.api.services.ModelSource;
import org.apache.maven.api.services.VersionRangeResolverException;
import org.apache.maven.api.services.model.ModelResolver;
import org.apache.maven.api.services.model.ModelResolverException;
import org.eclipse.sisu.Priority;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
@Priority(100)
public class DolaGleanerModelResolver implements ModelResolver {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Inject private Collector collector;

    @Override
    public ModelSource resolveModel(
            Session session,
            List<RemoteRepository> repositories,
            Parent parent,
            AtomicReference<Parent> modified)
            throws ModelResolverException {
        ModelSource ms = tryResolveParent(session, repositories, parent);
        if (ms != null) {
            return ms;
        }
        logger.debug("Stubbed parent POM {}", parent.getArtifactId());
        return new StubModelSource(
                parent.getGroupId(), parent.getArtifactId(), parent.getVersion(), "pom");
    }

    private ModelSource tryResolveParent(
            Session session, List<RemoteRepository> repositories, Parent parent)
            throws ModelResolverException {
        String groupId = parent.getGroupId();
        String artifactId = parent.getArtifactId();
        String version = parent.getVersion();
        ArtifactCoordinates coords =
                session.createArtifactCoordinates(groupId, artifactId, version, "pom");
        collector.lookup(coords);
        try {
            Gleaner.checkVersionRange(coords);
            DownloadedArtifact resolved = session.resolveArtifact(coords, repositories);
            Path path = resolved.getPath();
            logger.debug("Parent POM found at {}", path);
            return new FileModelSource(path);
        } catch (VersionRangeResolverException | ArtifactResolverException e) {
            return null;
        }
    }

    @Override
    public ModelSource resolveModel(
            Session session,
            List<RemoteRepository> repositories,
            Dependency dependency,
            AtomicReference<Dependency> modified)
            throws ModelResolverException {
        ModelSource ms = tryResolveDependency(session, repositories, dependency);
        if (ms != null) {
            return ms;
        }
        logger.warn("Stubbed dependency POM {}", dependency.getArtifactId());
        return new StubModelSource(
                dependency.getGroupId(),
                dependency.getArtifactId(),
                dependency.getVersion(),
                dependency.getType());
    }

    private ModelSource tryResolveDependency(
            Session session, List<RemoteRepository> repositories, Dependency dependency)
            throws ModelResolverException {
        String groupId = dependency.getGroupId();
        String artifactId = dependency.getArtifactId();
        String classifier = dependency.getClassifier();
        String version = dependency.getVersion();
        String type = dependency.getType();
        ArtifactCoordinates coords =
                session.createArtifactCoordinates(
                        groupId, artifactId, version, classifier, null, type);
        Dep dep = collector.lookup(coords);
        try {
            Gleaner.checkVersionRange(coords);
            DownloadedArtifact resolved = session.resolveArtifact(coords, repositories);
            Path path = resolved.getPath();
            logger.debug("Dependency POM found at {}", path);
            dep.resolved = true;
            return new FileModelSource(path);
        } catch (VersionRangeResolverException | ArtifactResolverException e) {
            dep.resolved = false;
            return null;
        }
    }

    @Override
    public ModelResolverResult resolveModel(ModelResolverRequest request)
            throws ModelResolverException {
        throw new IllegalStateException("XMR resolveModel(generic) called");
    }
}
