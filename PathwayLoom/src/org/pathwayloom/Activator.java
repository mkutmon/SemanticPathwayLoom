package org.pathwayloom;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.pathvisio.desktop.plugin.Plugin;

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
