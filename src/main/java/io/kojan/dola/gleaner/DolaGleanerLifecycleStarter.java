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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.internal.LifecycleStarter;
import org.eclipse.sisu.Priority;

@Named
@Singleton
@Priority(100)
public class DolaGleanerLifecycleStarter implements LifecycleStarter {

    @Inject private Gleaner gleaner;

    @Override
    public void execute(MavenSession session) {
        gleaner.execute(session);
    }
}
