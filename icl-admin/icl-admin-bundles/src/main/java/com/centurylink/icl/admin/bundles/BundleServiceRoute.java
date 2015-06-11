package com.centurylink.icl.admin.bundles;

import org.apache.camel.builder.RouteBuilder;

public class BundleServiceRoute extends RouteBuilder
{
	@Override
	public void configure() throws Exception {
				
		from("direct:GetInstalledBundles")
			.routeId("GetInstalledBundles")
			.beanRef("bundleList", "getBundleList")
			;

		from("direct:GetHeaders")
			.routeId("GetHeaders")
			.beanRef("headersDetail", "getHeadersDetail")
			;

		from("direct:GetExport")
			.routeId("GetExport")
			.beanRef("exportList", "getExportList")
			;

		from("direct:BundleStateChange")
			.routeId("BundleStateChange")
			.beanRef("bundleStateChange", "changeBundleState")
			;

		from("direct:ConfigList")
			.routeId("ConfigList")
			.beanRef("configList", "getConfigList")
			;
	}
}
