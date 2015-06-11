package com.centurylink.icl.admin.activemq;

import org.apache.camel.CamelContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import com.centurylink.icl.component.RoutingGroupServiceBase;

public class ActiveMQAdminService extends RoutingGroupServiceBase {

	private static final Log LOG = LogFactory.getLog(ActiveMQAdminService.class);

	public ActiveMQAdminService(CamelContext context, BundleContext bundleContext, String xmlConfigurationLocation, String serviceName) throws Exception
	{
		super(context, bundleContext, serviceName, xmlConfigurationLocation, LOG);
	}
}