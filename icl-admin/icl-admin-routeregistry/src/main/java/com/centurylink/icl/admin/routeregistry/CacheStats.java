package com.centurylink.icl.admin.routeregistry;

import net.sf.ehcache.CacheManager;

import org.apache.camel.Exchange;

import com.centurylink.icl.builder.iclaction.ICLActionResponseBuilder;
import com.centurylink.icl.builder.iclaction.ICLActionResponseDocumentBuilder;
import com.centurylink.icl.builder.iclaction.ParameterBuilder;
import com.centurylink.icl.builder.iclaction.RecordSetBuilder;
import com.centurylink.icl.builder.iclaction.RowBuilder;
import com.centurylink.icl.builder.iclaction.StatusBuilder;
import com.centurylink.icl.iclaction.ICLActionRequest;
import com.centurylink.icl.iclaction.ICLActionRequestDocument;
import com.centurylink.icl.iclaction.ICLActionResponseDocument;

public class CacheStats {
	
	private CacheManager manager;
	
	public CacheStats(String cacheConfig)
	{
		manager = CacheManager.create(cacheConfig);
	}
	
	public ICLActionResponseDocument getCacheStatistics(Exchange exchange) throws Exception
	{
		final ICLActionResponseDocumentBuilder documentBuilder = new ICLActionResponseDocumentBuilder();
		final ICLActionResponseBuilder responseBuilder = new ICLActionResponseBuilder();
		final StatusBuilder statusBuilder = new StatusBuilder();
		final ParameterBuilder parameterBuilder = new ParameterBuilder();
		final RecordSetBuilder recordSetBuilder = new RecordSetBuilder();
		final RowBuilder rowBuilder = new RowBuilder();

		Object requestDocument = exchange.getIn().getBody();
		ICLActionRequest request = ((ICLActionRequestDocument) requestDocument).getICLActionRequest();

		responseBuilder.buildICLActionResponse();
		recordSetBuilder.buildRecordSet();
		
		String[] cacheNames = manager.getCacheNames();
		
		for (String name:cacheNames)
		{
			rowBuilder.buildRow();
			parameterBuilder.buildParameter("CacheName", name);
			rowBuilder.addParameter(parameterBuilder.getParameter());
			parameterBuilder.buildParameter("InMemorySize", Long.toString(manager.getEhcache(name).calculateInMemorySize()));
			rowBuilder.addParameter(parameterBuilder.getParameter());
			parameterBuilder.buildParameter("CacheHits", Long.toString(manager.getEhcache(name).getStatistics().getCacheHits()));
			rowBuilder.addParameter(parameterBuilder.getParameter());
			parameterBuilder.buildParameter("CacheMisses", Long.toString(manager.getEhcache(name).getStatistics().getCacheMisses()));
			rowBuilder.addParameter(parameterBuilder.getParameter());
			parameterBuilder.buildParameter("MemoryStoreObjectCount", Long.toString(manager.getEhcache(name).getStatistics().getMemoryStoreObjectCount()));
			rowBuilder.addParameter(parameterBuilder.getParameter());
			recordSetBuilder.addRow(rowBuilder.getRow());
		}
		
		responseBuilder.addRecordSet(recordSetBuilder.getRecordSet());
		documentBuilder.buildICLActionResponseDocument(responseBuilder.getICLActionResponse());
		
		return documentBuilder.getICLActionResponseDocument();
	}

}
