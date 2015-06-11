package com.centurylink.icl.admin.routeregistry;

import org.apache.camel.builder.RouteBuilder;

public class RouteRegistryServiceRoute extends RouteBuilder
{
	@Override
	public void configure() throws Exception {
				
		from("direct:GetRegisterdRoutes")
			.routeId("GetRegisterdRoutes")
			.beanRef("routeRegistryList", "getRegisterdRoutesList")
			;

		from("direct:GetCacheStats")
			.routeId("GetCacheStats")
			.beanRef("cacheStats", "getCacheStatistics")
			;

	}
}
