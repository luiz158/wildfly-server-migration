/*
 * Copyright 2017 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.migration.wfly10.config.task.subsystem;

import org.jboss.migration.core.ServerMigrationTaskName;
import org.jboss.migration.core.ServerMigrationTaskResult;
import org.jboss.migration.core.TaskContext;
import org.jboss.migration.core.env.TaskEnvironment;
import org.jboss.migration.wfly10.config.management.SubsystemsManagement;

/**
 * @author emmartins
 */
public interface SubsystemConfigurationSubtask<S> {

    /**
     *
     * @param parentTaskContext
     * @return
     */
    ServerMigrationTaskName getName(ParentTaskContext parentTaskContext);

    /**
     *
     * @param parentTaskContext
     * @param taskContext
     * @param taskEnvironment
     * @return
     * @throws Exception
     */
    ServerMigrationTaskResult run(ParentTaskContext parentTaskContext, TaskContext taskContext, TaskEnvironment taskEnvironment) throws Exception;

    /**
     *
     */
    interface ParentTaskContext<S> {
        String getExtension();
        String getSubsystem();
        S getSource();
        SubsystemsManagement getSubsystemsManagement();
        String getConfigName();
    }
}