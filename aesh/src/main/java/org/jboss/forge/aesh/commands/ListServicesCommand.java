/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.aesh.commands;

import org.jboss.aesh.cl.Parameter;
import org.jboss.forge.container.Addon;
import org.jboss.forge.container.AddonRegistry;
import org.jboss.forge.container.services.Remote;
import org.jboss.forge.ui.Result;
import org.jboss.forge.ui.UICommand;
import org.jboss.forge.ui.UIContext;
import org.jboss.forge.ui.UIValidationContext;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
@Parameter(name="list-services")
@Remote
public class ListServicesCommand implements UICommand {

    private String name = "list-services";
    private AddonRegistry registry;

    public ListServicesCommand(AddonRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void initializeUI(UIContext context) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void validate(UIValidationContext context) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Result execute(UIContext context) throws Exception {
        return Result.success(listServices());
    }

    private String listServices() throws IOException
    {
        StringBuilder builder = new StringBuilder();
        Set<Addon> addons = registry.getRegisteredAddons();
        System.out.println("listing addons: "+addons);
        for (Addon addon : addons)
        {
            Set<Class<?>> serviceClasses = addon.getServiceRegistry().getServices();
            for (Class<?> type : serviceClasses)
            {
                System.out.println("NAME: "+type.getName());
                //getConsole().pushToStdOut(type.getName());
                builder.append(type.getName()).append("\n");
                for (Method method : type.getMethods())
                {
                    builder.append("\n\type - " + getName(method));
                    //getConsole().pushToStdOut("\n\t - " + getName(method));
                }
                //getConsole().pushToStdOut("\n");
                builder.append("\n");
            }
        }

        return builder.toString();
    }

   public String getName(Method method)
   {
      String params = "(";
      List<Class<?>> parameters = Arrays.asList(method.getParameterTypes());

      Iterator<Class<?>> iterator = parameters.iterator();
      while (iterator.hasNext())
      {
         Class<?> p = iterator.next();
         params += p.getName();

         if (iterator.hasNext())
         {
            params += ",";
         }
      }

      params += ")";

      String returnType = method.getReturnType().getName() == null ? "void" : method.getReturnType().getName();
      return method.getName() + params + "::" + returnType;
   }

}
