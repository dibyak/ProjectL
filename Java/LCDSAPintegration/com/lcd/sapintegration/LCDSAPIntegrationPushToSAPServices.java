package com.lcd.sapintegration;

import java.io.StringReader;
import java.util.HashMap;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.json.JsonValue;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import com.dassault_systemes.platform.restServices.RestService;
import com.matrixone.apps.domain.DomainRelationship;

import matrix.db.JPO;

@Path("/LCDPushToSAPServices")
public class LCDSAPIntegrationPushToSAPServices extends RestService {

	private static final String TYPE_APPLICATION_FORMAT = "application/json";  
	
	private static final String KEY_CONNECTION_ID = "ConnectionID"; 
	private static final String KEY_BOM_COMPONENT_ID = "BOMComponentID"; 
	private static final String KEY_BOM_NAME = "BOMName"; 
	private static final String KEY_CA_ID = "CAID";
	private static final String KEY_SUCCESS = "Success"; 
	private static final String KEY_RESPONSE = "response";
	private static final String KEY_ERROR_MESSAGE = "ErrorMessage";
	private static final String KEY_STATUS = "status";
	
	private static final String ATTR_LCD_PROCESS_STATUS_FLAG = "LCD_ProcessStatusFlag";
	private static final String ATTR_LCD_REASON_FOR_FAILURE = "LCD_ReasonforFailure";
	
	private static final String STATUS_IN_WORK = "In Work";
	private static final String STATUS_FAILED = "Failed";
	
	private static final String MSG_SCHEDULER_PROCESSING = "Scheduler Processing";
	private static final String MSG_JSON_FORMAT_VALIDATION_FAILED = "JSON Format Validation Failed";
	private static final String MSG_JSON_FORMAT_VALIDATION_COMPLETED = "JSON Format Validation Completed";
	
	private static final String JPO_LCD_3DXSAP_INTEGRATION_SCHEDULER = "LCD_3DXSAPIntegrationScheduler";
	private static final String METHOD_REPUSH_FAILED_BOM_COMPONENTS_TO_SAP = "RePushFailedBomComponentsToSAP";



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
	public Response sendFailedDataToSap(@javax.ws.rs.core.Context HttpServletRequest request, String paramString)
			throws Exception {
		Response res = null;
		String bomName;
		HashMap<?, ?> responseMap = new HashMap<>();

		try {
			boolean isSCMandatory = false;
			matrix.db.Context context = getAuthenticatedContext(request, isSCMandatory);

			JsonReader jsonReader = Json.createReader(new StringReader(paramString));
			JsonObject joRequest = jsonReader.readObject();
			
			String connectionId = ((JsonValue) joRequest.get(KEY_CONNECTION_ID)).toString().replace("\"", "");
			String bomId = ((JsonValue) joRequest.get(KEY_BOM_COMPONENT_ID)).toString().replace("\"", "");
			bomName = ((JsonValue) joRequest.get(KEY_BOM_NAME)).toString().replace("\"", "");
			String caId = ((JsonValue) joRequest.get(KEY_CA_ID)).toString().replace("\"", "");
			jsonReader.close();
			
			if(connectionId.equals("") || connectionId.isEmpty()) {
				throw new NullPointerException(); 
			}
			if(bomId.equals("") || bomId.isEmpty()) {
				throw new NullPointerException(); 
			}
			if(bomName.equals("") || bomName.isEmpty()) {
				throw new NullPointerException(); 
			}
			if(caId.equals("") || caId.isEmpty()) {
				throw new NullPointerException(); 
			}
			
			DomainRelationship domRelBomConnectedToAnchorObj = DomainRelationship.newInstance(context, connectionId);

			domRelBomConnectedToAnchorObj.setAttributeValue(context, ATTR_LCD_PROCESS_STATUS_FLAG, STATUS_IN_WORK);
			domRelBomConnectedToAnchorObj.setAttributeValue(context, ATTR_LCD_REASON_FOR_FAILURE, MSG_SCHEDULER_PROCESSING);

			HashMap<String, String> params = new HashMap<>();

			params.put(KEY_CONNECTION_ID, connectionId); 
			params.put(KEY_BOM_COMPONENT_ID, bomId);  
			params.put(KEY_BOM_NAME, bomName);  
			params.put(KEY_CA_ID, caId); 

			String[] args = JPO.packArgs(params);
			
			responseMap = (HashMap<?, ?>) JPO.invoke(context, JPO_LCD_3DXSAP_INTEGRATION_SCHEDULER, null, METHOD_REPUSH_FAILED_BOM_COMPONENTS_TO_SAP,
					args, HashMap.class);
		
			if(responseMap == null) {
				throw new NullPointerException(); 
			}
			String strErrMsg = (String) responseMap.get(KEY_ERROR_MESSAGE); 
			String strStatus = (String) responseMap.get(KEY_STATUS); 
			System.out.println("sendFailedDataToSap ---- ErrorMessage -- "+strErrMsg);
			System.out.println("sendFailedDataToSap ---- Type -- "+strStatus);
			
			if (strStatus.equalsIgnoreCase(KEY_SUCCESS)) { 
				domRelBomConnectedToAnchorObj.setAttributeValue(context, ATTR_LCD_REASON_FOR_FAILURE,
						MSG_JSON_FORMAT_VALIDATION_COMPLETED);
				JsonObjectBuilder jobRes = Json.createObjectBuilder();
				jobRes.add(KEY_RESPONSE, KEY_SUCCESS);
				jobRes.add(KEY_BOM_NAME, bomName);
				jobRes.add(KEY_ERROR_MESSAGE, strErrMsg);
				jobRes.add(KEY_STATUS, strStatus); 
				
				res = Response.ok(jobRes.build().toString()).type(TYPE_APPLICATION_FORMAT).build();
			} else {
				domRelBomConnectedToAnchorObj.setAttributeValue(context, ATTR_LCD_PROCESS_STATUS_FLAG, STATUS_FAILED);
				domRelBomConnectedToAnchorObj.setAttributeValue(context, ATTR_LCD_REASON_FOR_FAILURE,
						MSG_JSON_FORMAT_VALIDATION_FAILED);
				JsonObjectBuilder jobRes = Json.createObjectBuilder();
				jobRes.add(KEY_RESPONSE, STATUS_FAILED);
				jobRes.add(KEY_BOM_NAME, bomName);
				jobRes.add(KEY_ERROR_MESSAGE, strErrMsg); 
				jobRes.add(KEY_STATUS, strStatus);
				
				res = Response.status(Response.Status.BAD_REQUEST).entity(jobRes.build().toString()).build();
			}
			
			

		} catch (Exception e) {
			e.printStackTrace();
		} 
		return res;
	}
}
