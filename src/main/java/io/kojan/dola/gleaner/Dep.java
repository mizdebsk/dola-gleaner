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

import java.util.Set;
import java.util.TreeSet;
import org.apache.maven.api.ArtifactCoordinates;
import org.apache.maven.api.model.InputLocation;
import org.apache.maven.api.model.InputLocationTracker;
import org.apache.maven.api.model.InputSource;

class Dep {
    final String id;
    final String rpmDepString;
    ArtifactCoordinates coords;
    final Set<String> foundLocations = new TreeSet<>();
    Boolean resolved;

    static String rpmDepString(
            String groupId,
            String artifactId,
            String extension,
            String classifier,
            String version,
            String packageVersion,
            String namespace) {
        boolean customExtension = !extension.equals("jar");
        boolean customClassifier = !classifier.equals("");
        boolean customVersion = !version.equals("SYSTEM");
        StringBuilder sb = new StringBuilder();
        if (namespace != null && !namespace.isBlank()) {
            sb.append(namespace);
            sb.append("-");
        }
        sb.append("mvn(");
        sb.append(groupId);
        sb.append(":");
        sb.append(artifactId);
        if (customClassifier || customExtension) {
            sb.append(":");
        }
        if (customExtension) {
            sb.append(extension);
        }
        if (customClassifier) {
            sb.append(":");
            sb.append(classifier);
        }
        if (customClassifier || customExtension || customVersion) {
            sb.append(":");
        }
        if (customVersion) {
            sb.append(version);
        }
        sb.append(")");
        if (packageVersion != null) {
            sb.append(" = ");
            sb.append(packageVersion);
        }
        return sb.toString();
    }

    private String location(InputLocationTracker obj) {
        InputLocation location = obj.getLocation("");
        if (location == null) {
            return "UNKNOWN-location";
        }
        int line = location.getLineNumber();
        InputSource source = location.getSource();
        String url = source.getLocation();
        if (url == null) {
            url = "UNKNOWN";
        }
        url = url.replaceAll("^file://", "");
        return url + " line " + line;
    }

    void foundAt(InputLocationTracker location) {
        foundLocations.add(location(location));
    }

    public Dep(String groupId, String artifactId, String extension, String version) {
        this.id = groupId + ':' + artifactId + ':' + extension + ':' + version;
        this.rpmDepString = rpmDepString(groupId, artifactId, extension, "", "SYSTEM", null, null);
    }
}
