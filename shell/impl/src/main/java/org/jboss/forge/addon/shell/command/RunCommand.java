/**
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.jboss.forge.addon.shell.command;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.inject.Inject;

import org.jboss.aesh.console.Prompt;
import org.jboss.aesh.console.command.invocation.CommandInvocation;
import org.jboss.aesh.console.settings.Settings;
import org.jboss.aesh.console.settings.SettingsBuilder;
import org.jboss.forge.addon.resource.DirectoryResource;
import org.jboss.forge.addon.resource.FileResource;
import org.jboss.forge.addon.resource.Resource;
import org.jboss.forge.addon.resource.ResourceFactory;
import org.jboss.forge.addon.resource.util.ResourcePathResolver;
import org.jboss.forge.addon.shell.Shell;
import org.jboss.forge.addon.shell.ShellFactory;
import org.jboss.forge.addon.shell.ui.AbstractShellCommand;
import org.jboss.forge.addon.shell.ui.ShellContext;
import org.jboss.forge.addon.ui.command.AbstractCommandExecutionListener;
import org.jboss.forge.addon.ui.command.CommandExecutionListener;
import org.jboss.forge.addon.ui.command.UICommand;
import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.context.UIValidationContext;
import org.jboss.forge.addon.ui.hints.InputType;
import org.jboss.forge.addon.ui.input.UIInput;
import org.jboss.forge.addon.ui.input.UIInputMany;
import org.jboss.forge.addon.ui.metadata.UICommandMetadata;
import org.jboss.forge.addon.ui.metadata.WithAttributes;
import org.jboss.forge.addon.ui.output.UIOutput;
import org.jboss.forge.addon.ui.result.Failed;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;
import org.jboss.forge.addon.ui.util.Metadata;
import org.jboss.forge.furnace.exception.ContainerException;
import org.jboss.forge.furnace.spi.ListenerRegistration;
import org.jboss.forge.furnace.util.Assert;
import org.jboss.forge.furnace.util.OperatingSystemUtils;
import org.jboss.forge.furnace.util.Streams;

/**
 * Implementation of the "run script" command
 * 
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
public class RunCommand extends AbstractShellCommand
{
   @Inject
   ResourceFactory resourceFactory;

   @Inject
   @WithAttributes(label = "Timeout (seconds)", defaultValue = "500", required = false,
            description = "Set the timeout after which this script should abort if execution has not completed.")
   private UIInput<Integer> timeout;

   @Inject
   @WithAttributes(label = "Arguments", type = InputType.FILE_PICKER, required = false)
   private UIInputMany<String> arguments;

   @Inject
   @WithAttributes(label = "Command", shortName = 'c', required = false)
   private UIInput<String> command;

   @Inject
   private ShellFactory shellFactory;

   @Override
   public UICommandMetadata getMetadata(UIContext context)
   {
      return Metadata.from(super.getMetadata(context), getClass()).name("run")
               .description("Execute/run a forge script file.");
   }

   @Override
   public void initializeUI(UIBuilder builder) throws Exception
   {
      builder.add(timeout).add(arguments).add(command);
   }

   @Override
   public void validate(UIValidationContext validator)
   {
      if (!command.hasValue() && !arguments.hasValue())
      {
         validator.addValidationError(null, "Command or script file must be informed");
      }
   }

   @Override
   public Result execute(UIExecutionContext context) throws Exception
   {
      Result result = null; //Results.fail("Error executing script.");
      Resource<?> currentResource = (Resource<?>) context.getUIContext().getInitialSelection().get();
      ShellContext shellContext = (ShellContext) context.getUIContext();
      final UIOutput output = context.getUIContext().getProvider().getOutput();
      if (command.hasValue())
      {
         String[] commands = command.getValue().split(" ");
         ProcessBuilder processBuilder = new ProcessBuilder(commands);
         Object currentDir = currentResource.getUnderlyingResourceObject();
         if (currentDir instanceof File)
         {
            processBuilder.directory((File) currentDir);
         }
         final Process process = processBuilder.start();
         ExecutorService executor = Executors.newFixedThreadPool(2);
         // Read std out
         executor.submit(new Runnable()
         {
            @Override
            public void run()
            {
               Streams.write(process.getInputStream(), output.out());
            }
         });
         // Read std err
         executor.submit(new Runnable()
         {
            @Override
            public void run()
            {
               Streams.write(process.getErrorStream(), output.err());
            }
         });
         executor.shutdown();
         int returnCode = process.waitFor();
         if (returnCode == 0)
         {
            result = Results.success();
         }
         else
         {
            result = Results.fail("Error while executing native command. See output for more details");
         }
      }
      else
      {
          //we need to put this command in the background so it can execute new commands
          CommandInvocation commandInvocation = (CommandInvocation) shellContext.getAttributeMap().get(CommandInvocation.class);
          if(commandInvocation != null)
          {
              commandInvocation.putProcessInBackground();
          }
         ALL: for (String path : arguments.getValue())
         {
            List<Resource<?>> resources = new ResourcePathResolver(resourceFactory, currentResource, path).resolve();
            for (Resource<?> resource : resources)
            {
               if (resource.exists())
               {

                  try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                           resource.getResourceInputStream()))) {
                      long startTime = System.currentTimeMillis();
                      Shell shell = (Shell) shellContext.getAttributeMap().get(Shell.class);
                      //lets create a listener:
                      //gah, shell is null. george you fix? :)
                      if (shell != null) {
                          ScriptCommandListener listener = new ScriptCommandListener();
                          ListenerRegistration<CommandExecutionListener> listenerRegistration = shell
                                  .addCommandExecutionListener(listener);

                          while (reader.ready()) {
                              try {
                                  String line = reader.readLine();
                                  if (isComment(line)) {
                                      // Skip Comments
                                      continue;
                                  }
                                  if (!line.trim().isEmpty()) {
                                      if (!line.trim().endsWith(OperatingSystemUtils.getLineSeparator()))
                                          line = line + OperatingSystemUtils.getLineSeparator();

                                      commandInvocation.getShell().out().println("EXECUTING:");

                                      commandInvocation.executeCommand(line);
                                  }
                                  while (!listener.isExecuted()) {
                                      if (System.currentTimeMillis() >
                                              (startTime + TimeUnit.MILLISECONDS.convert(timeout.getValue(), TimeUnit.SECONDS))) {
                                          throw new TimeoutException("Timeout expired waiting for command [" + line + "] to execute.");
                                      }

                                      try {
                                          Thread.sleep(10);
                                      }
                                      catch (InterruptedException e) {
                                          throw new ContainerException("Command [" + line + "] did not respond.", e);
                                      }
                                  }
                                  result = listener.getResult();

                                  if (result != null) {
                                      if (result instanceof Failed)
                                          break ALL;
                                  }
                              }
                              catch (TimeoutException e) {
                                  result = Results.fail(path + ": timed out.");
                                  break ALL;
                              }
                          }
                      }
                      else
                          commandInvocation.getShell().out().println("Shell is NULL :/");
                  }
               }
               else
               {
                  result = Results.fail(path + ": not found.");
                  break ALL;
               }
            }
         }
      }
      return result;
   }

   public Result execute(Shell shell, BufferedWriter stdin, String line, int quantity, TimeUnit unit, long startTime)
            throws TimeoutException
   {
      Assert.notNull(line, "Line to execute cannot be null.");
      if (isComment(line))
      {
         return Results.success();
      }
      Result result = null;

      if (!line.trim().isEmpty())
      {
         if (!line.trim().endsWith(OperatingSystemUtils.getLineSeparator()))
            line = line + OperatingSystemUtils.getLineSeparator();

         ScriptCommandListener listener = new ScriptCommandListener();
         ListenerRegistration<CommandExecutionListener> listenerRegistration = shell
                  .addCommandExecutionListener(listener);
         try
         {
            stdin.write(line);
            stdin.flush();
            while (!listener.isExecuted())
            {
               if (System.currentTimeMillis() > (startTime + TimeUnit.MILLISECONDS.convert(quantity, unit)))
               {
                  throw new TimeoutException("Timeout expired waiting for command [" + line + "] to execute.");
               }

               try
               {
                  Thread.sleep(10);
               }
               catch (InterruptedException e)
               {
                  throw new ContainerException("Command [" + line + "] did not respond.", e);
               }
            }
            result = listener.getResult();
         }
         catch (IOException e)
         {
            throw new RuntimeException("Failed to execute command.", e);
         }
         finally
         {
            listenerRegistration.removeListener();
         }
      }
      return result;
   }

   public class ScriptCommandListener extends AbstractCommandExecutionListener
   {
      Result result;

      @Override
      public void preCommandExecuted(UICommand command, UIExecutionContext context)
      {
      }

      @Override
      public void postCommandExecuted(UICommand command, UIExecutionContext context, Result result)
      {
         synchronized (this)
         {
            this.result = result;
         }
      }

      @Override
      public void postCommandFailure(UICommand command, UIExecutionContext context, Throwable failure)
      {
         synchronized (this)
         {
            this.result = Results.fail("Error encountered during command execution.", failure);
         }
      }

      public boolean isExecuted()
      {
         synchronized (this)
         {
            return result != null;
         }
      }

      public Result getResult()
      {
         synchronized (this)
         {
            return result;
         }
      }

      public void reset()
      {
         synchronized (this)
         {
            result = null;
         }
      }
   }

   @Override
   public boolean isEnabled(ShellContext context)
   {
      return super.isEnabled(context) && context.getInitialSelection().get() instanceof DirectoryResource;
   }

   private boolean isComment(String line)
   {
      return line.startsWith("#");
   }

   private static class UncloseablePrintStream extends PrintStream
   {
      public UncloseablePrintStream(PrintStream stream)
      {
         super(stream, true);
      }

      @Override
      public void close()
      {
         // Uncloseable
      }
   }
}
