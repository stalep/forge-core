package org.jboss.forge.addon.parser.java;

/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;

import java.io.File;

import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.forge.addon.parser.java.resources.JavaResource;
import org.jboss.forge.addon.resource.Resource;
import org.jboss.forge.addon.resource.ResourceFactory;
import org.jboss.forge.arquillian.AddonDependency;
import org.jboss.forge.arquillian.Dependencies;
import org.jboss.forge.arquillian.archive.ForgeArchive;
import org.jboss.forge.furnace.repositories.AddonDependencyEntry;
import org.jboss.forge.parser.JavaParser;
import org.jboss.forge.parser.java.JavaClass;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class JavaParserResourcesTest
{
   @Deployment
   @Dependencies({
            @AddonDependency(name = "org.jboss.forge.furnace.container:cdi"),
            @AddonDependency(name = "org.jboss.forge.addon:resources"),
            @AddonDependency(name = "org.jboss.forge.addon:parser-java")
   })
   public static ForgeArchive getDeployment()
   {
      ForgeArchive archive = ShrinkWrap
               .create(ForgeArchive.class)
               .addBeansXML()
               .addAsAddonDependencies(
                        AddonDependencyEntry.create("org.jboss.forge.furnace.container:cdi"),
                        AddonDependencyEntry.create("org.jboss.forge.addon:parser-java"),
                        AddonDependencyEntry.create("org.jboss.forge.addon:resources")
               );

      return archive;
   }

   @Inject
   private ResourceFactory factory;

   @Test
   public void testJavaResourceCreation() throws Exception
   {
      JavaClass javaClass = JavaParser.create(JavaClass.class).setPackage("org.jboss.forge.test").setName("Example");
      JavaResource resource = factory.create(JavaResource.class, File.createTempFile("forge", ".java"));
      resource.createNewFile();
      resource.setContents(javaClass);

      Assert.assertEquals("Example", resource.getJavaSource().getName());
      Assert.assertEquals("org.jboss.forge.test", resource.getJavaSource().getPackage());
   }

   @Test
   public void testJavaResourceCreationSpecialized() throws Exception
   {
      JavaClass javaClass = JavaParser.create(JavaClass.class).setPackage("org.jboss.forge.test").setName("Example");
      JavaResource resource = factory.create(JavaResource.class, File.createTempFile("forge", ".java"));
      resource.createNewFile();
      resource.setContents(javaClass);

      Resource<File> newResource = factory.create(resource.getUnderlyingResourceObject());

      Assert.assertThat(newResource, is(instanceOf(JavaResource.class)));
      Assert.assertEquals(resource, newResource);
   }
}
