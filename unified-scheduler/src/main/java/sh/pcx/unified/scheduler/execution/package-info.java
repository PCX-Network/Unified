/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */

/**
 * Task execution infrastructure and context.
 *
 * <p>This package provides the low-level infrastructure for executing
 * scheduled tasks, including:
 * <ul>
 *   <li>{@link sh.pcx.unified.scheduler.execution.ExecutionContext} -
 *       Runtime context for context-aware tasks</li>
 *   <li>{@link sh.pcx.unified.scheduler.execution.TaskExecutor} -
 *       Executes tasks with proper error handling</li>
 *   <li>{@link sh.pcx.unified.scheduler.execution.TaskQueue} -
 *       Priority queue for pending tasks</li>
 * </ul>
 *
 * <h2>Execution Flow</h2>
 * <ol>
 *   <li>Task is added to TaskQueue with scheduled execution time</li>
 *   <li>TaskExecutor polls queue for ready tasks</li>
 *   <li>ExecutionContext is created for the task</li>
 *   <li>Task is executed on the appropriate thread</li>
 *   <li>Metrics are recorded and callbacks invoked</li>
 * </ol>
 *
 * <h2>Context-Aware Execution</h2>
 * <pre>{@code
 * scheduler.builder()
 *     .repeat(20)
 *     .executeWithContext(ctx -> {
 *         log.info("Execution #{}", ctx.getExecutionCount());
 *
 *         // Store data between executions
 *         int counter = ctx.getData("counter", Integer.class).orElse(0);
 *         ctx.setData("counter", counter + 1);
 *
 *         // Self-cancellation
 *         if (ctx.getExecutionCount() >= 10) {
 *             ctx.cancel();
 *         }
 *     })
 *     .build();
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 */
package sh.pcx.unified.scheduler.execution;
