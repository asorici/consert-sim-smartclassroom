package org.aimas.ami.cmm.simulation;

import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

//@Component
//@Instantiate
public class CMMBundleInstaller {
	private static final String CONSERT_CMM_AGENT_BUNDLE = "consert-middleware.agent-bundle.jar";
	
	public CMMBundleInstaller(BundleContext context) {
		String bundleLocationBase = "file:./bundle";
		try {
			context.installBundle(bundleLocationBase + "/" + CONSERT_CMM_AGENT_BUNDLE);
		}
        catch (BundleException e) {
	        e.printStackTrace();
        }
	}
}
