/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.aesh;

import org.jboss.aesh.cl.Parameter;
import org.jboss.forge.container.services.Remote;
import org.jboss.forge.ui.Result;
import org.jboss.forge.ui.UICommand;
import org.jboss.forge.ui.UIContext;
import org.jboss.forge.ui.UIValidationContext;


/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
@Parameter(name="foo")
@Remote
public class FooCommand implements UICommand {

    @Override
    public void initializeUI(UIContext context) throws Exception {
    }

    @Override
    public void validate(UIValidationContext context) {
    }

    @Override
    public Result execute(UIContext context) throws Exception {
        return Result.success("boo");
    }
}
