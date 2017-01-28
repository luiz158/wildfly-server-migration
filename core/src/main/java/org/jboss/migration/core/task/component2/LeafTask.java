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

package org.jboss.migration.core.task.component2;

import org.jboss.migration.core.task.ServerMigrationTask;
import org.jboss.migration.core.task.ServerMigrationTaskName;

/**
 * @author emmartins
 */
public class LeafTask extends ComponentTask {

    public LeafTask(ServerMigrationTaskName name, Runnable runnable) {
        super(name, runnable);
    }

    protected static abstract class BaseBuilder<P extends Parameters, T extends BaseBuilder<P, T>> extends ComponentTask.Builder<P, T> {

        private RunnableFactory<P> runnableFactory;

        public BaseBuilder() {
            super();
        }

        public BaseBuilder(BaseBuilder<P, ?> other) {
            super(other);
            this.runnableFactory = other.runnableFactory;
        }

        @Override
        public T run(RunnableFactory<P> runnableFactory) {
            this.runnableFactory = runnableFactory;
            return getThis();
        }

        @Override
        public RunnableFactory<P> getRunnableFactory() {
            return runnableFactory;
        }
    }

    public static class Builder<P extends Parameters> extends BaseBuilder<P, Builder<P>> {

        public Builder() {
            super();
        }

        public Builder(Builder<P> other) {
            super(other);
        }

        @Override
        public Builder<P> clone() {
            return new Builder(this);
        }

        @Override
        protected Builder<P> getThis() {
            return this;
        }

        @Override
        protected ServerMigrationTask buildTask(ServerMigrationTaskName name, Runnable runnable) {
            return new LeafTask(name, runnable);
        }
    }
}