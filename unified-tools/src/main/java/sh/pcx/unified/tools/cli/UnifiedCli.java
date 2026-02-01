/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.tools.cli;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.concurrent.Callable;

/**
 * Main entry point for the Unified CLI tool.
 *
 * <p>Usage examples:
 * <pre>
 * unified new my-plugin                           # Create new plugin
 * unified new my-plugin --template=economy        # Create from template
 * unified generate module MyModule                # Generate a module
 * unified generate command MyCommand              # Generate a command
 * unified generate gui MyGui                      # Generate a GUI
 * unified generate                                # Interactive mode
 * </pre>
 *
 * @since 1.0.0
 */
@Command(
        name = "unified",
        mixinStandardHelpOptions = true,
        version = "UnifiedPlugin CLI 1.0.0",
        description = "CLI tool for creating and managing UnifiedPlugin projects",
        subcommands = {
                NewCommand.class,
                GenerateCommand.class,
                CommandLine.HelpCommand.class
        }
)
public final class UnifiedCli implements Callable<Integer> {

    @Option(names = {"-v", "--verbose"}, description = "Verbose output")
    private boolean verbose;

    /**
     * Main entry point.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        int exitCode = new CommandLine(new UnifiedCli())
                .setExecutionExceptionHandler((ex, commandLine, parseResult) -> {
                    System.err.println("Error: " + ex.getMessage());
                    if (commandLine.isUsageHelpRequested()) {
                        commandLine.usage(System.err);
                    }
                    return 1;
                })
                .execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() {
        // Show help when no subcommand is provided
        CommandLine.usage(this, System.out);
        return 0;
    }

    /**
     * Returns whether verbose mode is enabled.
     *
     * @return true if verbose
     */
    public boolean isVerbose() {
        return verbose;
    }
}
