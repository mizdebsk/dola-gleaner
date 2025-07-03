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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

public class CompatVersionResolver {

    private final Map<Pattern, String> map;

    private CompatVersionResolver(Map<Pattern, String> map) {
        this.map = Map.copyOf(map);
    }

    public static CompatVersionResolver parseFromProperties(Properties properties) {
        List<String> keys =
                properties.keySet().stream()
                        .map(Object::toString)
                        .filter(key -> key.toString().startsWith("dola.gleaner.version."))
                        .sorted()
                        .toList();
        Map<Pattern, String> map = new LinkedHashMap<>();
        for (String key : keys) {
            String val = properties.getProperty(key);
            String[] valSplit = val.split("=", 3);
            if (valSplit.length != 2) {
                throw new RuntimeException("Invalid property value of " + key + ": " + val);
            }
            String matcher = valSplit[0];
            String version = valSplit[1];
            map.put(Pattern.compile(matcher), version);
        }
        return new CompatVersionResolver(map);
    }

    public String resolveVersionFor(Dep dep) {
        String depId = dep.groupId + ":" + dep.artifactId;
        for (var entry : map.entrySet()) {
            Pattern pattern = entry.getKey();
            String version = entry.getValue();
            if (pattern.matcher(depId).matches()) {
                return version;
            }
        }
        return "SYSTEM";
    }
}
