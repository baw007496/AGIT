package com.centurylink.icl.admin.activemq.util;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

import javax.management.MBeanServer;
import javax.management.MBeanServerInvocationHandler;
import javax.management.ObjectName;

import org.apache.activemq.broker.jmx.BrokerViewMBean;
import org.apache.activemq.broker.jmx.QueueViewMBean;
import org.apache.activemq.broker.jmx.SubscriptionViewMBean;

public class BorkerStatistics {
	
	private static MBeanServer connection = ManagementFactory.getPlatformMBeanServer();
	
	public static BrokerViewMBean getBrokerViewMBean(String brokerName) throws Exception
	{	
		ObjectName name = new ObjectName("org.apache.activemq:BrokerName=" + brokerName + ",Type=Broker");

		BrokerViewMBean mbean = (BrokerViewMBean) MBeanServerInvocationHandler.newProxyInstance(connection, name, BrokerViewMBean.class, true);

		return mbean;
	}
	
	public static List<QueueViewMBean> getQueueList(String brokerName) throws Exception
	{
		List<QueueViewMBean> response = new ArrayList<QueueViewMBean>();
		
		for (ObjectName name : getBrokerViewMBean(brokerName).getQueues()) {
		    QueueViewMBean queueMbean = (QueueViewMBean) MBeanServerInvocationHandler.newProxyInstance(connection, name, QueueViewMBean.class, true);
		    response.add(queueMbean);
		} 
		
		return response;
	}
	
	public static List<SubscriptionViewMBean> getSubscriberListFromQueue(QueueViewMBean queue)throws Exception
	{
		List<SubscriptionViewMBean> response = new ArrayList<SubscriptionViewMBean>();
		
		for (ObjectName name : queue.getSubscriptions()) {
			SubscriptionViewMBean subscriberView = MBeanServerInvocationHandler.newProxyInstance(connection, name, SubscriptionViewMBean.class, true);
			response.add(subscriberView);
		}
		
		return response;
	}

}
