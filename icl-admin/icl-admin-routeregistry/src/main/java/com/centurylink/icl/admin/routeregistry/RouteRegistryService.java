package com.centurylink.icl.admin.routeregistry;

import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import com.centurylink.icl.component.RoutingGroupServiceBase;
import com.centurylink.icl.exceptions.ICLException;
import com.centurylink.icl.service.ICLRequestFact;
import com.centurylink.icl.service.ICLRouteIdentity;
import com.centurylink.icl.service.ICLRouteRegistry;

public class RouteRegistryService extends RoutingGroupServiceBase {

	private static final Log LOG = LogFactory.getLog(RouteRegistryService.class);

	public RouteRegistryService(CamelContext context, BundleContext bundleContext, String xmlConfigurationLocation, String serviceName) throws Exception
	{
		super(context, bundleContext, serviceName, xmlConfigurationLocation, LOG);
	}
	
}