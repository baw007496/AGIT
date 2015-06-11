package com.centurylink.icl.admin.logconfig;

import org.apache.camel.builder.RouteBuilder;

public class LogConfigServiceRoute extends RouteBuilder{

	@Override
	public void configure() throws Exception {


		
		from("direct:SetLogLevel")
			.routeId("SetLogLevelRoute")
			.beanRef("setLogLevel", "setLogLevel")
			;
		
		from("direct:GetLogLevel")
		.routeId("GetLogLevelRoute")
		.beanRef("getLogLevel", "getLogLevel");
		
	}

}
