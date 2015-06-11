package com.centurylink.icl.admin.activemq;

import java.lang.management.ManagementFactory;
import java.util.List;


import javax.management.MBeanServer;
import javax.management.ObjectInstance;
import javax.management.ObjectName;

import org.apache.activemq.broker.jmx.BrokerViewMBean;
import org.apache.activemq.broker.jmx.QueueViewMBean;
import org.apache.activemq.broker.jmx.SubscriptionViewMBean;
import org.apache.camel.Exchange;

import com.centurylink.icl.builder.iclaction.ICLActionResponseBuilder;
import com.centurylink.icl.builder.iclaction.ICLActionResponseDocumentBuilder;
import com.centurylink.icl.builder.iclaction.ParameterBuilder;
import com.centurylink.icl.builder.iclaction.RecordSetBuilder;
import com.centurylink.icl.builder.iclaction.RowBuilder;
import com.centurylink.icl.builder.iclaction.StatusBuilder;
import com.centurylink.icl.exceptions.OSSDataNotFoundException;
import com.centurylink.icl.iclaction.ICLActionResponseDocument;

import com.centurylink.icl.admin.activemq.util.BorkerStatistics;
import com.centurylink.icl.admin.activemq.util.MBeansObjectNameQueryFilter;

public class BrokerList {

	private final ICLActionResponseDocumentBuilder documentBuilder = new ICLActionResponseDocumentBuilder();
	private final ICLActionResponseBuilder responseBuilder = new ICLActionResponseBuilder();
	private final StatusBuilder statusBuilder = new StatusBuilder();
	private final ParameterBuilder parameterBuilder = new ParameterBuilder();
	private final RecordSetBuilder recordSetBuilder = new RecordSetBuilder();
	private final RowBuilder rowBuilder = new RowBuilder();

	
	public BrokerList()
	{
	}
	
	public ICLActionResponseDocument getBrokerList(Exchange exchange) throws Exception
	{
		MBeanServer jmxConnection = ManagementFactory.getPlatformMBeanServer();
		List brokerList = (new MBeansObjectNameQueryFilter(jmxConnection)).query("Type=Broker");
		
		
		boolean dataFound = false;
		
		//Object requestDocument = exchange.getIn().getBody();
		//ICLActionRequest request = ((ICLActionRequestDocument) requestDocument).getICLActionRequest();
		RecordSetBuilder queueRecordSetBuilder = new RecordSetBuilder();
		RowBuilder queueRowBuilder = new RowBuilder();
		RecordSetBuilder subscriberRecordSetBuilder = new RecordSetBuilder();
		RowBuilder subscriberRowBuilder = new RowBuilder();

		responseBuilder.buildICLActionResponse();
		
		for (Object obj:brokerList)
		{
			dataFound = true;
			ObjectName name = ((ObjectInstance) obj).getObjectName();
			Object brokerName = jmxConnection.getAttribute(name, "BrokerName");
			recordSetBuilder.buildRecordSet(brokerName.toString());
			rowBuilder.buildRow();
			BrokerViewMBean brokerStats = BorkerStatistics.getBrokerViewMBean(brokerName.toString());
			parameterBuilder.buildParameter("BrokerID", brokerStats.getBrokerId());
			rowBuilder.addParameter(parameterBuilder.getParameter());		
			parameterBuilder.buildParameter("Slave", Boolean.toString(brokerStats.isSlave()));
			rowBuilder.addParameter(parameterBuilder.getParameter());
			parameterBuilder.buildParameter("TotalDequeueCount", Long.toString(brokerStats.getTotalDequeueCount()));
			rowBuilder.addParameter(parameterBuilder.getParameter());
			parameterBuilder.buildParameter("TotalEnqueueCount", Long.toString(brokerStats.getTotalEnqueueCount()));
			rowBuilder.addParameter(parameterBuilder.getParameter());
			parameterBuilder.buildParameter("TotalMessageCount", Long.toString(brokerStats.getTotalMessageCount()));
			rowBuilder.addParameter(parameterBuilder.getParameter());
			parameterBuilder.buildParameter("TotalConsumerCount", Long.toString(brokerStats.getTotalConsumerCount()));
			rowBuilder.addParameter(parameterBuilder.getParameter());

			List<QueueViewMBean> queues = BorkerStatistics.getQueueList(brokerName.toString());
			
			queueRecordSetBuilder.buildRecordSet("QUEUES");
			for (QueueViewMBean queue:queues)
			{
				queueRowBuilder.buildRow();
				parameterBuilder.buildParameter("Name", queue.getName());
				queueRowBuilder.addParameter(parameterBuilder.getParameter());
				
				subscriberRecordSetBuilder.buildRecordSet("SUBSCRIBERS");
				for (SubscriptionViewMBean subscriber:BorkerStatistics.getSubscriberListFromQueue(queue))
				{
					subscriberRowBuilder.buildRow();
					parameterBuilder.buildParameter("ClientId", subscriber.getClientId());
					subscriberRowBuilder.addParameter(parameterBuilder.getParameter());
					parameterBuilder.buildParameter("ConnectionId", subscriber.getConnectionId());
					subscriberRowBuilder.addParameter(parameterBuilder.getParameter());
					subscriberRecordSetBuilder.addRow(subscriberRowBuilder.getRow());
				}
				queueRowBuilder.addRecordSet(subscriberRecordSetBuilder.getRecordSet());
				
				queueRecordSetBuilder.addRow(queueRowBuilder.getRow());
			}
			rowBuilder.addRecordSet(queueRecordSetBuilder.getRecordSet());
			
			recordSetBuilder.addRow(rowBuilder.getRow());
			responseBuilder.addRecordSet(recordSetBuilder.getRecordSet());
		}
				
		if (!dataFound)
			throw new OSSDataNotFoundException();
		
		statusBuilder.buildStatus();
		responseBuilder.setStatus(statusBuilder.getStatus());
		
		documentBuilder.buildICLActionResponseDocument(responseBuilder.getICLActionResponse());
		
		return documentBuilder.getICLActionResponseDocument();
	}
	
}
