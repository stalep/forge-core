/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.aesh;

import org.jboss.aesh.cl.CommandLine;
import org.jboss.aesh.cl.CommandLineParser;
import org.jboss.forge.ui.UIBuilder;
import org.jboss.forge.ui.UIContext;
import org.jboss.forge.ui.UIInput;
import org.jboss.forge.ui.UIValidationContext;
import org.jboss.forge.ui.wizard.UIWizardContext;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class ShellContext implements UIValidationContext, UIContext, UIBuilder {

    private CommandLineParser parser;
    private CommandLine commandLine;
    private List<UIInput<?>> inputs = new ArrayList<UIInput<?>>();

    public ShellContext() {
    }

    public CommandLine getCommandLine() {
        return commandLine;
    }

    public void setCommandLine(CommandLine cl) {
        commandLine = cl;
    }

    @Override
    public UIBuilder add(UIInput<?> input) {
        inputs.add(input);
        return this;
    }

    @Override
    public void addValidationError(UIInput<?> input, String errorMessage) {
        //TODO: ignoring errorMessage for now
        inputs.add(input);
    }

    @Override
    public UIBuilder getUIBuilder() {
        return this;
    }
}
