package org.pathwayloom;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.pathvisio.desktop.plugin.Plugin;

/**
 * Activator registering the PathwayLoom plugin
 * with the OSGi registry
 * 
 * @author msk
 *
 */
public class Activator implements BundleActivator  {
	private PppPlugin plugin;
	
	@Override
	public void start(BundleContext context) throws Exception {
		plugin = new PppPlugin();
		context.registerService(Plugin.class.getName(), plugin, null);
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		plugin.done();
	}
}
