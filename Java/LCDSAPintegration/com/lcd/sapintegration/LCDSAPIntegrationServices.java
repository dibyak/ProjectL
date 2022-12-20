package com.lcd.sapintegration;

import java.util.Iterator;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import com.dassault_systemes.platform.restServices.RestService;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.DomainRelationship;
import com.matrixone.apps.domain.util.MapList;

import matrix.util.StringList;

/**
 * Class PGReportGeneratorServices is used to invoke the web service methods
 * required for report generator widget
 * 
 * @since 2018x.5
 * @author
 *
 */
@Path("/LCDSAPIntegrationService")
// New Class: LCDSAPIntegrationPushToSAPServices.java
public class LCDSAPIntegrationServices extends RestService {

	static final String TYPE_APPLICATION_FORMAT = "application/json";
	static final String EXCEPTION_MESSAGE = "Exception in DarshitServices"; 

	/**
	 * Method
	 * 
	 * @param request     : HttpServletRequest request param
	 * @param type        :
	 * @param name        :
	 * @param revision    :
	 * @param showAllCols :
	 * @return : Response of Json object with object info
	 * @throws Exception
	 */
	@GET
	@Path("/getMA")
	public Response getReportTemplateData(@javax.ws.rs.core.Context HttpServletRequest request) throws Exception {
		Response res = null;
		MapList manAssMapList;
		DomainObject domCAObj;
		String SELECT_ATTRIBUTE_ACTUAL_COMPLETION_DATE = "attribute["+DomainConstants.ATTRIBUTE_ACTUAL_COMPLETION_DATE+"]";
		
		try {
			boolean isSCMandatory = false;

			matrix.db.Context context = getAuthenticatedContext(request, isSCMandatory);

			DomainObject domObj = DomainObject.newInstance(context, "976.26903.5485.41789");

			StringList slObjectSelect = new StringList();
			slObjectSelect.add(DomainConstants.SELECT_RELATIONSHIP_ID);
			slObjectSelect.add(DomainConstants.SELECT_ID);
			slObjectSelect.add(DomainConstants.SELECT_NAME);
			slObjectSelect.add(DomainConstants.SELECT_REVISION);
			slObjectSelect.add("attribute[PLMEntity.V_Name]");
			slObjectSelect.add("attribute[PLMEntity.V_description]");
			slObjectSelect.add(DomainConstants.SELECT_CURRENT);
			slObjectSelect.add("attribute[LCDMF_ManufacturingAssembly.LCDMF_SAPMBOMUpdatedOn]");

			StringList slRelSelect = new StringList();
			slRelSelect.add(DomainRelationship.SELECT_ID);

			manAssMapList = domObj.getRelatedObjects(context, // context
					"LCD_SAPBOMInterface", // Relationship Pattern
					"*", // Type Pattern
					slObjectSelect, // Object Select
					slRelSelect, // Relationship Select
					false, // To Side
					true, // from Side
					(short) 1, // Recursion Level
					"", // Object Where clause
					"", // Relationship Where clause
					0); // limit

			Iterator<?> iterMAsMaplist = manAssMapList.iterator();

			JsonArrayBuilder jabMAs = Json.createArrayBuilder();
			while (iterMAsMaplist.hasNext()) {
				JsonObjectBuilder jobMA = Json.createObjectBuilder();
				Map<?, ?> item = (Map<?, ?>) iterMAsMaplist.next();

				String sConnectionId = (String)(item.get(DomainRelationship.SELECT_ID));
				
				DomainRelationship relEbom = DomainRelationship.newInstance(context,sConnectionId);
				
				String Status = relEbom.getAttributeValue(context, "LCD_ProcessStatusFlag");
                String ReasonForFailure = relEbom.getAttributeValue(context, "LCD_ReasonforFailure");
                String CAID = relEbom.getAttributeValue(context, "LCD_CAID");
                
				StringList CAobjectSelects = new StringList();
				CAobjectSelects.add(DomainConstants.SELECT_NAME);
				CAobjectSelects.add(SELECT_ATTRIBUTE_ACTUAL_COMPLETION_DATE);

				domCAObj = DomainObject.newInstance(context, CAID);

				Map<?, ?> CaAttrDetails = domCAObj.getInfo(context, CAobjectSelects);

				jobMA.add("ConnectionID", sConnectionId);
				jobMA.add("caID", CAID);
				jobMA.add("maID", (String) item.get(DomainConstants.SELECT_ID));
				jobMA.add("maName", (String) item.get(DomainConstants.SELECT_NAME));
				jobMA.add("status", Status);
				jobMA.add("revision", (String) item.get(DomainConstants.SELECT_REVISION));
				jobMA.add("title", (String) item.get("attribute[PLMEntity.V_Name]"));
				jobMA.add("maturity", (String) item.get(DomainConstants.SELECT_CURRENT));
				jobMA.add("description", (String) item.get("attribute[PLMEntity.V_description]"));
				jobMA.add("caCompletedTime", (String) CaAttrDetails.get(SELECT_ATTRIBUTE_ACTUAL_COMPLETION_DATE));
				jobMA.add("caName", (String) CaAttrDetails.get(DomainConstants.SELECT_NAME));
				jobMA.add("SapFeedbackTimeStamp", (String) item.get("attribute[LCDMF_ManufacturingAssembly.LCDMF_SAPMBOMUpdatedOn]"));
				jobMA.add("SapFeedbackMessage", ReasonForFailure);

				jabMAs.add(jobMA);

			}

			String strOutput = jabMAs.build().toString();

			res = Response.ok(strOutput).type(TYPE_APPLICATION_FORMAT).build();
		} catch (Exception e) {
			System.out.println(EXCEPTION_MESSAGE);
			e.printStackTrace();
			res = Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
		}
		return res;
	}
}