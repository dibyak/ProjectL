package com.lcd.sapintegration;

import java.io.StringReader;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import com.dassault_systemes.platform.restServices.RestService;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.DomainRelationship;

@Path("/LCDPushToSAPServices")
public class LCDSAPIntegrationPushToSAPServices extends RestService {

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
	@POST
	@Path("/PushToSAP")
	@Consumes({ "application/json", "application/ds-json" })
	public Response DataToRepush(@javax.ws.rs.core.Context HttpServletRequest request, String ParamString)
			throws Exception {
		Response res = null;
		String RESPONSE = "";
		String FAILURE_REASON = "";
		String BOM_ID = "";
		String BOM_NAME = "";
		String CONNECTION_ID = "";

		try {
			boolean isSCMandatory = false;

			matrix.db.Context context = getAuthenticatedContext(request, isSCMandatory);

			try (JsonReader jsonReader = Json.createReader(new StringReader(ParamString))) {
				JsonObject ResObject = jsonReader.readObject();
				CONNECTION_ID = ResObject.get("ConnectionID").toString();
				BOM_ID = ResObject.get("BOMComponentID").toString();
				BOM_NAME = ResObject.get("MaName").toString();
				String CAID = ResObject.get("CAID").toString();
				
				System.out.println("----------Connection ID--------"+CONNECTION_ID);
				System.out.println("----------BOM_ID--------"+BOM_ID);
				System.out.println("----------BOM_NAME--------"+BOM_NAME);
				System.out.println("----------CAID--------"+CAID);
				System.out.println("-----------paramString----------"+ParamString);
				
				DomainRelationship relEbom = DomainRelationship.newInstance(context, CONNECTION_ID.replaceAll("\"", ""));
				
				System.out.println("-----------LCD_ProcessStatusFlag----------"+relEbom.getAttributeValue(context, "LCD_ProcessStatusFlag"));
				
				relEbom.setAttributeValue(context, "LCD_ProcessStatusFlag", "In Work");

				Thread.sleep(2000);

				if (BOM_ID.equalsIgnoreCase("40332.27195.13716.56452")) {
					RESPONSE = "Failed";
					FAILURE_REASON = "Network Connection Failed";
					relEbom.setAttributeValue(context, "LCD_ProcessStatusFlag", RESPONSE);
					relEbom.setAttributeValue(context, "LCD_ReasonforFailure", FAILURE_REASON);
				} else {
					RESPONSE = "Success";
					relEbom.setAttributeValue(context, "LCD_ProcessStatusFlag", "Success");
				}
			}

			JsonObjectBuilder jobRes = Json.createObjectBuilder();

			jobRes.add("Status", RESPONSE);
			jobRes.add("Reason", FAILURE_REASON);
			jobRes.add("MaName", BOM_NAME);

			String strOutput = jobRes.build().toString();

			res = Response.ok(strOutput).type(TYPE_APPLICATION_FORMAT).build();
		} catch (Exception e) {
			System.out.println(EXCEPTION_MESSAGE);
			e.printStackTrace();
			res = Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
		}
		return res;
	}

}
