/*
 * Copyright (C) 2019 The Turms Project
 * https://github.com/turms-im/turms
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.turms.server.common.infra.task;

import im.turms.server.common.infra.logging.core.logger.Logger;
import im.turms.server.common.infra.logging.core.logger.LoggerFactory;
import im.turms.server.common.infra.thread.NamedThreadFactory;
import im.turms.server.common.infra.thread.ThreadNameConst;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

/**
 * Handle tasks in a thread
 *
 * @author James Chen
 */
@Component
public class TaskManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskManager.class);
    private static final int SLOW_SLOG_THRESHOLD_MILLIS = 1000;

    private final Map<String, ScheduledFuture<?>> scheduledTaskMap;

    private final TaskScheduler taskScheduler;

    public TaskManager() {
        scheduledTaskMap = new ConcurrentHashMap<>(32);
        NamedThreadFactory threadFactory = new NamedThreadFactory(ThreadNameConst.TASK_MANAGER, true);
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(threadFactory);
        taskScheduler = new ConcurrentTaskScheduler(executor);
    }

    public void reschedule(String taskName, String cronExpression, Runnable runnable) {
        CronTrigger trigger = new CronTrigger(cronExpression);
        ScheduledFuture<?> task = taskScheduler.schedule(new Task(runnable), trigger);
        ScheduledFuture<?> previousTask = scheduledTaskMap.put(taskName, task);
        if (previousTask != null) {
            previousTask.cancel(false);
        }
    }

    private static class Task implements Runnable {

        private final Runnable runnable;

        public Task(Runnable runnable) {
            this.runnable = runnable;
        }

        @Override
        public void run() {
            long startTime = System.currentTimeMillis();
            try {
                runnable.run();
            } catch (Exception e) {
                LOGGER.error("Caught an error while running the task: " + runnable.getClass().getName(), e);
            }
            long endTime = System.currentTimeMillis();
            long diff = endTime - startTime;
            if (diff > SLOW_SLOG_THRESHOLD_MILLIS) {
                String name = runnable.getClass().getName();
                LOGGER.warn("A slow task [" + name + "] took " + diff + " milliseconds to execute");
            }
        }
    }

}
