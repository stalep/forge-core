/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.aesh;

import org.jboss.aesh.cl.CommandLine;
import org.jboss.aesh.cl.CommandLineParser;
import org.jboss.aesh.cl.ParserGenerator;
import org.jboss.aesh.cl.internal.ParameterInt;
import org.jboss.aesh.complete.CompleteOperation;
import org.jboss.aesh.complete.Completion;
import org.jboss.aesh.console.Console;
import org.jboss.aesh.console.ConsoleOutput;
import org.jboss.forge.ui.Result;
import org.jboss.forge.ui.UIBuilder;
import org.jboss.forge.ui.UICommand;
import org.jboss.forge.ui.UIContext;
import org.jboss.forge.ui.UIValidationContext;

import java.io.IOException;
import java.util.List;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class ShellCommand implements Completion {

    private Console console;
    private CommandLineParser parser;
    private UICommand command;

    public ShellCommand(UICommand command) {
        this.command = command;
        generateParser(command);
    }

    public void setConsole(Console console) {
        this.console = console;
    }

    public Console getConsole() {
        return console;
    }

    public void generateParser(UICommand command) {
        //here we generate the CommandLineParser based on command
        parser = ParserGenerator.generateParser(command.getClass());
    }

    public CommandLine parse(String line) throws IllegalArgumentException {
        return parser.parse(line);
    }

    public void run(ConsoleOutput consoleOutput, CommandLine commandLine) throws Exception {
        ShellContext context = new ShellContext() {
            @Override
            public UIBuilder getUIBuilder() {
                return null;
            }
        };
        context.setCommandLine(commandLine);
        Result result = command.execute(context);
        getConsole().pushToStdOut(result.getMessage());
    }

    @Override
    public void complete(CompleteOperation completeOperation) {
        List<ParameterInt> parameters = parser.getParameters();
        for(ParameterInt param : parameters) {
            if(param.getName().startsWith(completeOperation.getBuffer()))
                completeOperation.addCompletionCandidate(param.getName());
        }
    }
}
