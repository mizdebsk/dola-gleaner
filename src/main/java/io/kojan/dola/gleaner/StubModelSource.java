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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import javax.xml.stream.XMLStreamException;
import org.apache.maven.api.model.Model;
import org.apache.maven.api.services.ModelSource;
import org.apache.maven.api.services.Source;
import org.apache.maven.model.v4.MavenStaxWriter;

class StubModelSource implements ModelSource {

    private final String modelString;

    public StubModelSource(String groupId, String artifactId, String version, String packaging) {
        Model model =
                Model.newBuilder()
                        .modelVersion("4.0.0")
                        .groupId(groupId)
                        .artifactId(artifactId)
                        .version(version)
                        .packaging(packaging)
                        .build();
        MavenStaxWriter pomWriter = new MavenStaxWriter();
        StringWriter sw = new StringWriter();
        try {
            pomWriter.write(sw, model);
        } catch (IOException | XMLStreamException e) {
            throw new RuntimeException(e);
        }
        modelString = sw.toString();
    }

    @Override
    public InputStream openStream() throws IOException {
        return new ByteArrayInputStream(modelString.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public Path getPath() {
        return null;
    }

    @Override
    public String getLocation() {
        return "XMvn stub";
    }

    @Override
    public Source resolve(String relative) {
        throw new IllegalStateException("SMS resolve called");
    }

    @Override
    public ModelSource resolve(ModelLocator modelLocator, String relative) {
        throw new IllegalStateException("SMS resolve called");
    }
}
