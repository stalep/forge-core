/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.aesh;

import org.jboss.aesh.cl.CommandLine;
import org.jboss.aesh.cl.CommandLineParser;
import org.jboss.forge.ui.UIContext;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public abstract class ShellContext implements UIContext {

    private CommandLine commandLine;
    private CommandLineParser parser;

    public CommandLine getCommandLine() {
        return commandLine;
    }

    public void setCommandLine(CommandLine cl) {
       commandLine = cl;
    }

}
