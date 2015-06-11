 package com.centurylink.icl.xref.db.transformationengine;


import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.centurylink.icl.builder.cim2.AmericanPropertyAddressBuilder; 
import com.centurylink.icl.builder.cim2.ConnectionTerminationPointBuilder;
import com.centurylink.icl.builder.cim2.CustomerBuilder;
import com.centurylink.icl.builder.cim2.OwnsResourceDetailsBuilder;
import com.centurylink.icl.builder.cim2.Point2PointCircuitBuilder;
import com.centurylink.icl.builder.cim2.SearchResourceResponseBuilder;
import com.centurylink.icl.builder.cim2.SearchResourceResponseDocumentBuilder;
import com.centurylink.icl.builder.cim2.SearchResponseDetailsBuilder;
import com.centurylink.icl.builder.cim2.SubNetworkConnectionBuilder;
import com.centurylink.icl.builder.util.SearchResourceRequestDocumentReader;
import com.centurylink.icl.builder.util.StringHelper;
import com.centurylink.icl.exceptions.OSSDataNotFoundException;
import com.iclnbi.iclnbiV200.SearchResourceDetails;
import com.iclnbi.iclnbiV200.SearchResourceResponseDocument;
import com.centurylink.icl.xref.helper.CktXRefDetails;
import com.centurylink.icl.xref.helper.XRefCriteria;

public class XrefTransformationEngine2
{

	private static final Log LOG = LogFactory.getLog(XrefTransformationEngine2.class);

	final private SearchResourceResponseDocumentBuilder searchResourceResponseDocumentBuilder;
	final private SearchResourceResponseBuilder searchResourceResponseBuilder;
	final private SearchResponseDetailsBuilder searchResponseDetailsBuilder;

	final private Point2PointCircuitBuilder point2PointCircuitBuilder;
	final private OwnsResourceDetailsBuilder ownsResourceDetailsBuilder;
	final private CustomerBuilder customerBuilder;
	final private ConnectionTerminationPointBuilder connectionTerminationPointBuilder;
	final private AmericanPropertyAddressBuilder americanPropertyAddressBuilder;
	final private SubNetworkConnectionBuilder subNetworkConnectionBuilder;

	public XrefTransformationEngine2() 
	{
		this.searchResourceResponseBuilder = new SearchResourceResponseBuilder();
		this.searchResourceResponseDocumentBuilder = new SearchResourceResponseDocumentBuilder();
		this.searchResponseDetailsBuilder = new SearchResponseDetailsBuilder();
		this.point2PointCircuitBuilder = new Point2PointCircuitBuilder();
		this.ownsResourceDetailsBuilder = new OwnsResourceDetailsBuilder();
		this.customerBuilder = new CustomerBuilder();
		this.connectionTerminationPointBuilder = new ConnectionTerminationPointBuilder();
		this.americanPropertyAddressBuilder = new AmericanPropertyAddressBuilder();
		this.subNetworkConnectionBuilder = new SubNetworkConnectionBuilder();
	}


	protected SearchResourceResponseDocument transformXrefInfoToCim(List<CktXRefDetails> input) throws Exception 
	{
		LOG.debug("Response Data >>>>>> " + input);
		if(null!=input && input.size()>0)
		{
			searchResponseDetailsBuilder.buildSearchResponseDetails();
			for(CktXRefDetails objCktXRefDetails:input)
			{
				if (null != objCktXRefDetails) 
				{
					
					subNetworkConnectionBuilder.buildSubNetworkConnection(objCktXRefDetails.getNewCktName(), objCktXRefDetails.getObjectID(), null, objCktXRefDetails.getSourceSystem(), null, null, null, null);
					subNetworkConnectionBuilder.setResourceAlias(objCktXRefDetails.getAliasName1(), objCktXRefDetails.getAliasName2());

					customerBuilder.buildCustomer(objCktXRefDetails.getCustomerName(), null, null, null, null, null);
					ownsResourceDetailsBuilder.buildOwnsResourceDetails();
					ownsResourceDetailsBuilder.addCustomer(customerBuilder.getCustomer());
					subNetworkConnectionBuilder.addOwnsResourceDetails(ownsResourceDetailsBuilder.getOwnsResourceDetails());

					// Region
					subNetworkConnectionBuilder.addResourceDescribedBy("Region", objCktXRefDetails.getRegion());
					// CACCode
					subNetworkConnectionBuilder.addResourceDescribedBy("CACCode", objCktXRefDetails.getCacCode());
					// CircuitFormat
					subNetworkConnectionBuilder.addResourceDescribedBy("CircuitFormat", objCktXRefDetails.getCktFormat());
					// MCO
					subNetworkConnectionBuilder.addResourceDescribedBy("MCO", objCktXRefDetails.getMco());

					americanPropertyAddressBuilder.buildAmericanPropertyAddress(objCktXRefDetails.getzAddress(), null, null, null, null, null, null, null, null, null, objCktXRefDetails.getzState());

					connectionTerminationPointBuilder.buildConnectionTerminationPoint(null, null, null);
					connectionTerminationPointBuilder.addAccessPointAddress(americanPropertyAddressBuilder.getAmericanPropertyAddress());
					subNetworkConnectionBuilder.addZEndTps(connectionTerminationPointBuilder.getConnectionTerminationPoint());
					searchResponseDetailsBuilder.addVoidCircuit(subNetworkConnectionBuilder.getSubNetworkConnection());
					

				} 
			}
			searchResourceResponseBuilder.buildSearchResourceResponse(
					searchResponseDetailsBuilder.getSearchResponseDetails(), null);
			searchResourceResponseDocumentBuilder
			.buildSearchResourceResponseDocument();
			searchResourceResponseDocumentBuilder
			.addSearchResourceResponse(searchResourceResponseBuilder
					.getSearchResourceResponse());
			LOG.debug(searchResourceResponseDocumentBuilder
					.getSearchResourceResponseDocument());
			return (SearchResourceResponseDocument) searchResourceResponseDocumentBuilder
					.getSearchResourceResponseDocument();
		}
		else 
		{
			throw new OSSDataNotFoundException();
		}

	}

	protected XRefCriteria transformXRefFromCim(SearchResourceDetails searchResourceDetails) throws Exception
	{
		XRefCriteria iclRefCriteria =new XRefCriteria();
		
		if(!StringHelper.isEmpty(SearchResourceRequestDocumentReader.getResourceCharacteristicValue(searchResourceDetails.getResourceCharacteristicValueList(), "Alias")))
		{
			iclRefCriteria.setAliasname1(SearchResourceRequestDocumentReader.getResourceCharacteristicValue(searchResourceDetails.getResourceCharacteristicValueList(), "Alias"));
		}
		if(!StringHelper.isEmpty(searchResourceDetails.getCommonName()))
		{
			iclRefCriteria.setNewCktName(searchResourceDetails.getCommonName());
		}
		if(!StringHelper.isEmpty(SearchResourceRequestDocumentReader.getResourceCharacteristicValue(searchResourceDetails.getResourceCharacteristicValueList(), "Region")))
		{
			iclRefCriteria.setRegion(SearchResourceRequestDocumentReader.getResourceCharacteristicValue(searchResourceDetails.getResourceCharacteristicValueList(), "Region"));
		}
		return iclRefCriteria;
	}

}
