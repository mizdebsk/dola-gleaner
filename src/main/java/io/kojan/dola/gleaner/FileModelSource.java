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
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.maven.api.services.ModelSource;
import org.apache.maven.api.services.Source;

class FileModelSource implements ModelSource {

    private final Path modelPath;

    public FileModelSource(Path modelPath) {
        this.modelPath = modelPath;
    }

    @Override
    public Path getPath() {
        return modelPath;
    }

    @Override
    public InputStream openStream() throws IOException {
        return Files.newInputStream(modelPath);
    }

    @Override
    public String getLocation() {
        return modelPath.toUri().toString();
    }

    @Override
    public Source resolve(String relative) {
        throw new IllegalStateException("FMS resolve called");
    }

    @Override
    public ModelSource resolve(ModelLocator modelLocator, String relative) {
        throw new IllegalStateException("FMS resolve called");
    }
}
