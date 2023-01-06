package com.lcd.sapintegration.util;

import java.io.StringReader;
import java.util.HashMap;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.json.JsonValue;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.dassault_systemes.platform.restServices.RestService;
import com.matrixone.apps.domain.DomainRelationship;
import com.matrixone.apps.domain.util.ContextUtil;

import matrix.db.Context;
import matrix.db.JPO;

public class LCDSAPIntegrationPushToSAPUtil extends RestService {

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
	private static final String METHOD_REPUSH_FAILED_BOM_COMPONENTS_TO_SAP = "rePushFailedBomComponentsToSAP";

	
	public static Response sendFailedDataToSap(Context context, String strParamString)
			throws NullPointerException {
		Response res = null;
		String bomName;
		String strResponse = "";
		JsonReader jsonReader = null;
		try {
			ContextUtil.pushContext(context);

			jsonReader = Json.createReader(new StringReader(strParamString));
			JsonObject joRequest = jsonReader.readObject();

			String connectionId =  joRequest.get(LCDSAPIntegrationDataConstants.PROPERTY_CONNECTION_ID).toString().replace("\"", "");
			String bomId = joRequest.get(KEY_BOM_COMPONENT_ID).toString().replace("\"", "");
			bomName = joRequest.get(KEY_BOM_NAME).toString().replace("\"", "");
			String caId = joRequest.get(KEY_CA_ID).toString().replace("\"", "");

			if (connectionId.equals("") || connectionId.isEmpty()) {
				throw new NullPointerException();
			}
			if (bomId.equals("") || bomId.isEmpty()) {
				throw new NullPointerException();
			}
			if (bomName.equals("") || bomName.isEmpty()) {
				throw new NullPointerException();
			}
			if (caId.equals("") || caId.isEmpty()) {
				throw new NullPointerException();
			}

			DomainRelationship domRelBomConnectedToAnchorObj = DomainRelationship.newInstance(context, connectionId);

			String strCurrentStatus = domRelBomConnectedToAnchorObj.getAttributeValue(context,
					ATTR_LCD_PROCESS_STATUS_FLAG);

			if (strCurrentStatus.equals(STATUS_FAILED)) {

				domRelBomConnectedToAnchorObj.setAttributeValue(context, ATTR_LCD_PROCESS_STATUS_FLAG, STATUS_IN_WORK);
				domRelBomConnectedToAnchorObj.setAttributeValue(context, ATTR_LCD_REASON_FOR_FAILURE,
						MSG_SCHEDULER_PROCESSING);

				HashMap<String, String> params = new HashMap<>();

				params.put(KEY_CONNECTION_ID, connectionId);
				params.put(KEY_BOM_COMPONENT_ID, bomId);
				params.put(KEY_BOM_NAME, bomName);
				params.put(KEY_CA_ID, caId);

				String[] args = JPO.packArgs(params);

				strResponse = (String) JPO.invoke(context, JPO_LCD_3DXSAP_INTEGRATION_SCHEDULER, null,
						METHOD_REPUSH_FAILED_BOM_COMPONENTS_TO_SAP, args, String.class);

				if (strResponse == null) {
					throw new NullPointerException("Null Response from SAP");
				} else {
					if (strResponse.equalsIgnoreCase(RESPONSE_STATUS_CODE_OK)) {
						domRelBomConnectedToAnchorObj.setAttributeValue(context, ATTR_LCD_REASON_FOR_FAILURE,
								MSG_JSON_FORMAT_VALIDATION_COMPLETED);
						JsonObjectBuilder jobRes = Json.createObjectBuilder();
						jobRes.add(KEY_STATUS, VALUE_SUCCESS);
						jobRes.add(PROPERTY_RESPONSE, strResponse);

						res = Response.ok(jobRes.build().toString()).type(MediaType.APPLICATION_JSON).build();
					} else {
						domRelBomConnectedToAnchorObj.setAttributeValue(context, ATTR_LCD_PROCESS_STATUS_FLAG,
								STATUS_FAILED);
						domRelBomConnectedToAnchorObj.setAttributeValue(context, ATTR_LCD_REASON_FOR_FAILURE,
								MSG_JSON_FORMAT_VALIDATION_FAILED);
						JsonObjectBuilder jobRes = Json.createObjectBuilder();
						jobRes.add(KEY_STATUS, STATUS_FAILED);
						jobRes.add(PROPERTY_RESPONSE, strResponse);
						jobRes.add(KEY_BOM_NAME, bomName);

						res = Response.ok(jobRes.build().toString()).type(MediaType.APPLICATION_JSON).build();
					}
				}
			}
			ContextUtil.popContext(context);
		} catch (Exception e) {
			e.printStackTrace();
			res = Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
		}finally {
			if(jsonReader !=null) {
				jsonReader.close();
			}
		}
		return res;
	}
}
