/**
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.jboss.forge.addon.javaee.ejb;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;

import javax.ejb.MessageDriven;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.jms.Message;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.forge.addon.facets.FacetFactory;
import org.jboss.forge.addon.javaee.ProjectHelper;
import org.jboss.forge.addon.javaee.ejb.ui.NewEJBCommand;
import org.jboss.forge.addon.parser.java.facets.JavaSourceFacet;
import org.jboss.forge.addon.parser.java.resources.JavaResource;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.ui.controller.WizardCommandController;
import org.jboss.forge.addon.ui.result.Failed;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.arquillian.AddonDependency;
import org.jboss.forge.arquillian.Dependencies;
import org.jboss.forge.arquillian.archive.ForgeArchive;
import org.jboss.forge.furnace.repositories.AddonDependencyEntry;
import org.jboss.forge.parser.java.JavaClass;
import org.jboss.forge.ui.test.UITestHarness;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
@RunWith(Arquillian.class)
public class NewEJBCommandTest
{
   @Deployment
   @Dependencies({
            @AddonDependency(name = "org.jboss.forge.addon:ui"),
            @AddonDependency(name = "org.jboss.forge.addon:ui-test-harness"),
            @AddonDependency(name = "org.jboss.forge.addon:javaee"),
            @AddonDependency(name = "org.jboss.forge.addon:maven")
   })
   public static ForgeArchive getDeployment()
   {
      return ShrinkWrap
               .create(ForgeArchive.class)
               .addClass(ProjectHelper.class)
               .addBeansXML()
               .addAsAddonDependencies(
                        AddonDependencyEntry.create("org.jboss.forge.furnace.container:cdi"),
                        AddonDependencyEntry.create("org.jboss.forge.addon:projects"),
                        AddonDependencyEntry.create("org.jboss.forge.addon:javaee"),
                        AddonDependencyEntry.create("org.jboss.forge.addon:maven"),
                        AddonDependencyEntry.create("org.jboss.forge.addon:ui"),
                        AddonDependencyEntry.create("org.jboss.forge.addon:ui-test-harness")
               );
   }

   @Inject
   private UITestHarness testHarness;

   @Inject
   private ProjectHelper projectHelper;

   @Inject
   private FacetFactory facetFactory;

   @Test
   public void testCreateEJB() throws Exception
   {
      Project project = projectHelper.createJavaLibraryProject();
      facetFactory.install(project, JavaSourceFacet.class);
      WizardCommandController controller = testHarness.createWizardController(NewEJBCommand.class,
               project.getProjectRoot());
      controller.initialize();
      controller.setValueFor("named", "TestEJB");
      controller.setValueFor("targetPackage", "org.jboss.forge.test");
      controller.setValueFor("serializable", "true");
      Assert.assertFalse(controller.canMoveToNextStep());
      Assert.assertTrue(controller.isValid());
      Assert.assertTrue(controller.canExecute());
      Result result = controller.execute();
      Assert.assertThat(result, is(not(instanceOf(Failed.class))));

      JavaSourceFacet facet = project.getFacet(JavaSourceFacet.class);
      JavaResource javaResource = facet.getJavaResource("org.jboss.forge.test.TestEJB");
      Assert.assertNotNull(javaResource);
      Assert.assertThat(javaResource.getJavaSource(), is(instanceOf(JavaClass.class)));
      Assert.assertTrue(javaResource.getJavaSource().hasAnnotation(Stateless.class));
      Assert.assertTrue(((JavaClass) javaResource.getJavaSource()).hasField("serialVersionUID"));
   }

   @Test
   public void testCreateMDB() throws Exception
   {
      Project project = projectHelper.createJavaLibraryProject();
      facetFactory.install(project, JavaSourceFacet.class);
      WizardCommandController controller = testHarness.createWizardController(NewEJBCommand.class,
               project.getProjectRoot());
      controller.initialize();
      controller.setValueFor("named", "TestEJB");
      controller.setValueFor("type", EJBType.MESSAGEDRIVEN);
      controller.setValueFor("targetPackage", "org.jboss.forge.test");
      controller.setValueFor("serializable", "false");
      Assert.assertTrue(controller.isValid());
      Assert.assertFalse(controller.canExecute());

      Assert.assertTrue(controller.canMoveToNextStep());
      controller.next();
      Assert.assertFalse(controller.canExecute());

      controller.setValueFor("destType", JMSDestinationType.TOPIC);
      controller.setValueFor("destName", "destination");

      Assert.assertTrue(controller.canExecute());

      Result result = controller.execute();
      Assert.assertThat(result, is(not(instanceOf(Failed.class))));

      JavaSourceFacet facet = project.getFacet(JavaSourceFacet.class);
      JavaResource javaResource = facet.getJavaResource("org.jboss.forge.test.TestEJB");
      Assert.assertNotNull(javaResource);
      Assert.assertThat(javaResource.getJavaSource(), is(instanceOf(JavaClass.class)));
      Assert.assertTrue(javaResource.getJavaSource().hasAnnotation(MessageDriven.class));
      Assert.assertFalse(((JavaClass) javaResource.getJavaSource()).hasField("serialVersionUID"));
      Assert.assertNotNull(((JavaClass) javaResource.getJavaSource()).getMethod("onMessage", Message.class));
   }
}
