package org.jboss.forge.aesh;

import javax.inject.Inject;

import junit.framework.TestCase;
import org.jboss.aesh.cl.CommandLine;
import org.jboss.aesh.cl.Parameter;
import org.jboss.aesh.cl.internal.ParameterInt;
import org.jboss.aesh.complete.CompleteOperation;
import org.jboss.aesh.complete.Completion;
import org.jboss.aesh.console.Config;
import org.jboss.aesh.console.Console;
import org.jboss.aesh.console.ConsoleOutput;
import org.jboss.aesh.console.settings.Settings;
import org.jboss.aesh.edit.KeyOperation;
import org.jboss.aesh.edit.actions.Operation;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.forge.aesh.commands.ClearCommand;
import org.jboss.forge.aesh.commands.ListServicesCommand;
import org.jboss.forge.aesh.commands.StopCommand;
import org.jboss.forge.arquillian.archive.ForgeArchive;
import org.jboss.forge.ui.UICommand;
import org.jboss.forge.ui.UIContext;
import org.jboss.forge.ui.UIValidationContext;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

/**
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
@RunWith(Arquillian.class)
//@Ignore("It locks up the UI while running the tests")
public class AeshAddonTest extends TestCase
{

    public AeshAddonTest() {
    }

   @Deployment
   public static ForgeArchive getDeployment()
   {
      ForgeArchive archive = ShrinkWrap
               .create(ForgeArchive.class)
               .addClasses(UICommand.class)
              .addClasses(UIContext.class)
              .addClasses(UIValidationContext.class)
              .addClasses(AeshShell.class)
              .addClass(ShellCommand.class)
              .addClass(ShellContext.class)
              .addClass(Parameter.class)
              .addClass(ParameterInt.class)
              .addClass(Completion.class)
              .addClass(Console.class)
              .addClass(ConsoleOutput.class)
              .addClass(CommandLine.class)
              .addClass(CompleteOperation.class)
              .addClass(StopCommand.class)
              .addClass(ClearCommand.class)
              .addClass(FooCommand.class)
              .addClass(ListServicesCommand.class)
               .addAsLibraries(
                       Maven.resolver().loadPomFromFile("pom.xml").resolve("org.jboss.aesh:aesh:0.29")
                               .withTransitivity().asFile())
               .addAsManifestResource(EmptyAsset.INSTANCE, ArchivePaths.create("beans.xml"))
               .setAsForgeXML(new StringAsset("<addon/>"));

      return archive;
   }

   @Inject
   private AeshShell shell;

   @Test
   public void testContainerInjection() {
       try {
           Assert.assertNotNull(shell);

           PipedOutputStream outputStream = new PipedOutputStream();
           PipedInputStream pipedInputStream = new PipedInputStream(outputStream);
           ByteArrayOutputStream out = new ByteArrayOutputStream();

           setupSettings(pipedInputStream, out);

           shell.initShell();
           shell.addCommand(new ShellCommand(new FooCommand()));

           outputStream.write(("foo\n").getBytes());
           shell.startShell();
           String outString = out.toString();
           assertEquals("boo\n", outString.substring(shell.getPrompt().length()+"foo\n".length()));

           outputStream.write(("exit\n").getBytes());
           shell.startShell();

           shell.stopShell();
       }
       catch (Exception ioe) {
           ioe.printStackTrace();
       }
   }

   @Test
   public void testLifecycle() throws Exception
   {

   }

    private void setupSettings(InputStream input, OutputStream out) {
        Settings.getInstance().setName("test");
        Settings.getInstance().setInputStream(input);
        Settings.getInstance().setStdOut(out);
        //aeshProducer.getSettings().setStdOut(new ByteArrayOutputStream());
        if(!Config.isOSPOSIXCompatible())
            Settings.getInstance().setAnsiConsole(false);

        Settings.getInstance().getOperationManager().addOperation(new KeyOperation(10, Operation.NEW_LINE));
    }

}