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

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

public class DependencyFilter {

    private final List<Pattern> patterns;

    private DependencyFilter(List<Pattern> patterns) {
        this.patterns = List.copyOf(patterns);
    }

    public static DependencyFilter parseFromProperties(Properties properties) {
        List<String> keys =
                properties.keySet().stream()
                        .map(Object::toString)
                        .filter(key -> key.toString().startsWith("dola.gleaner.filter."))
                        .sorted()
                        .toList();
        List<Pattern> patterns = new ArrayList<>();
        for (String key : keys) {
            String val = properties.getProperty(key);
            patterns.add(Pattern.compile(val));
        }
        return new DependencyFilter(patterns);
    }

    public boolean isDependencyFiltered(Dep dep) {
        String depId = dep.groupId + ":" + dep.artifactId;
        for (Pattern pattern : patterns) {
            if (pattern.matcher(depId).matches()) {
                return true;
            }
        }
        return false;
    }
}
