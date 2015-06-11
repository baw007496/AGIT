package com.centurylink.icl.xref.db.transformationengine;

import java.util.ArrayList;
import java.util.HashMap;
import org.custommonkey.xmlunit.XMLTestCase;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import com.centurylink.icl.xref.helper.CktXRefDetails;

public class XrefTransformationEngineTest extends XMLTestCase
{
	XRefTransformationEngine xrefTransformationEngine;
	CktXRefDetails cktXRefDetails = null;
	
	@Before
	public void setUp() throws Exception 
	{
		super.setUp();

		xrefTransformationEngine = new XRefTransformationEngine();
		cktXRefDetails = new CktXRefDetails();
		XMLUnit.setIgnoreAttributeOrder(true);
		XMLUnit.setIgnoreComments(true);
		XMLUnit.setIgnoreWhitespace(true);
	}
	
	@After
	public void tearDown() throws Exception 
	{
		super.tearDown();
		cktXRefDetails = null;
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	public void testTransformToCim() throws Exception 
	{
		try{
			cktXRefDetails = setTestData(cktXRefDetails);
			//For Request
			ArrayList list=new ArrayList();
			list.add(cktXRefDetails);
			list.add(cktXRefDetails);
			HashMap<String, Object> ihashMap=new HashMap<String, Object>();
			ihashMap.put("FromSidType","CktXRefDetails");
			Object object = xrefTransformationEngine.transformToCim(list, ihashMap); 
			System.out.println("Actual response from transformToCim method :: " + object);
			
			//For Equility
		//	assertXMLEqual("Comparing expectedResponse with generatedResponse", expectedResponse.toString(), object.toString());

			
		} 
		catch (Exception e) 
		{
			e.printStackTrace();
			assertTrue("Exception caught in test:" + e.getMessage(), false);
		}
	}
	
	private CktXRefDetails setTestData(CktXRefDetails cktXRefDetails) {
		
		cktXRefDetails.setNewCktName("tst2");
		cktXRefDetails.setObjectID("2");
		cktXRefDetails.setSourceSystem("TSTGRC");
		cktXRefDetails.setAliasName1("A33");
		cktXRefDetails.setAliasName2("A22");
		cktXRefDetails.setRegion("wESTERN");
		cktXRefDetails.setCacCode("CAC23");
		cktXRefDetails.setCktFormat("FRMT2");
		cktXRefDetails.setMco("mc2");
		cktXRefDetails.setCustomerName("ATL ENTPERPRISES");
		cktXRefDetails.setzAddress("TSTADDRESS");
		cktXRefDetails.setzState("mN");
		
		return cktXRefDetails;
		
	}
	
}
