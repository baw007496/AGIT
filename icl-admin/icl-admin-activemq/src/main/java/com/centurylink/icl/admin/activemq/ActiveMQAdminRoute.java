package com.centurylink.icl.admin.activemq;

import org.apache.camel.builder.RouteBuilder;

public class ActiveMQAdminRoute extends RouteBuilder
{
	@Override
	public void configure() throws Exception {
				
		from("direct:BrokerList")
			.routeId("BrokerList")
			.beanRef("brokerList", "getBrokerList")
			;

	}
}
