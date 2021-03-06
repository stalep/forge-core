package org.jboss.forge.addon.templates;

import org.jboss.forge.addon.resource.Resource;

/**
 * An abstract representation of a template. Concrete instances of this class are used to wrap {@link Resource}
 * instances representing template resources.
 *
 * @author Vineet Reynolds
 */
public interface Template
{
   /**
    * Fetches the underlying resource associated with this Template instance.
    *
    * @return the resource associated with this instance
    */
   Resource<?> getResource();

   /**
    * Indicates whether the template exists or not, usually through it's underlying resource.
    *
    * @return whether the template exists or not
    */
   boolean exists();
}
