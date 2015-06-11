package com.centurylink.icl.admin.bundles;

import org.apache.camel.CamelContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import com.centurylink.icl.component.RoutingGroupServiceBase;

public class BundleService extends RoutingGroupServiceBase {

	private static final Log LOG = LogFactory.getLog(BundleService.class);

	public BundleService(CamelContext context, BundleContext bundleContext, String xmlConfigurationLocation, String serviceName) throws Exception
	{
		super(context, bundleContext, serviceName, xmlConfigurationLocation, LOG);
	}
}