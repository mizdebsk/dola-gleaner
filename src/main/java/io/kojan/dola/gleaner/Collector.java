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

import java.util.Map;
import java.util.TreeMap;
import javax.inject.Named;
import javax.inject.Singleton;
import org.apache.maven.api.ArtifactCoordinates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class Collector {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    final Map<String, Dep> deps = new TreeMap<>();

    synchronized Dep lookup(ArtifactCoordinates coords) {
        Dep dep =
                lookup(
                        coords.getGroupId(),
                        coords.getArtifactId(),
                        coords.getExtension(),
                        coords.getClassifier(),
                        coords.getVersionConstraint().toString());
        dep.coords = coords;
        return dep;
    }

    synchronized Dep lookup(
            String groupId,
            String artifactId,
            String extension,
            String classifier,
            String version) {
        Dep dep = new Dep(groupId, artifactId, extension, classifier, version);
        return deps.computeIfAbsent(dep.id, x -> dep);
    }

    public void summarize() {
        for (Dep dep : deps.values()) {
            logger.debug("Found dependency: {}", dep.id);
            for (String location : dep.foundLocations) {
                logger.debug("  at {}", location);
            }
        }
    }
}
