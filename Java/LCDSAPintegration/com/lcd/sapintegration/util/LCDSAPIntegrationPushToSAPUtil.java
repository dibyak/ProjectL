package com.lcd.sapintegration.util;

import java.io.StringReader;
import java.util.HashMap;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.dassault_systemes.platform.restServices.RestService;
import com.matrixone.apps.domain.DomainRelationship;
import com.matrixone.apps.domain.util.ContextUtil;

import matrix.db.Context;
import matrix.db.JPO;

public class LCDSAPIntegrationPushToSAPUtil extends RestService {

	public static Response sendFailedDataToSap(Context context, String strParamString)
			throws NullPointerException {
		LCDSAPIntegration3DExpConstants lcdSAPInteg3DExpConstants = new LCDSAPIntegration3DExpConstants(context);
		Response res = null;
		String bomName;
		String strResponse = "";
		JsonReader jsonReader = null;
		try {
			ContextUtil.pushContext(context);

			jsonReader = Json.createReader(new StringReader(strParamString));
			JsonObject joRequest = jsonReader.readObject();

			String connectionId =  joRequest.get(LCDSAPIntegrationDataConstants.PROPERTY_CONNECTION_ID).toString().replace("\"", "");
			String bomId = joRequest.get(LCDSAPIntegrationDataConstants.PROPERTY_BOM_COMPONENT_ID).toString().replace("\"", "");
			bomName = joRequest.get(LCDSAPIntegrationDataConstants.PROPERTY_BOM_COMPONENT_NAME).toString().replace("\"", "");
			String caId = joRequest.get(LCDSAPIntegrationDataConstants.PROPERTY_CA_ID).toString().replace("\"", "");

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
					lcdSAPInteg3DExpConstants.ATTRIBUTE_LCD_PROCESS_STATUS_FLAG);

			if (strCurrentStatus.equals(LCDSAPIntegrationDataConstants.VALUE_PROCESS_STATUS_FAILED)) {

				domRelBomConnectedToAnchorObj.setAttributeValue(context, lcdSAPInteg3DExpConstants.ATTRIBUTE_LCD_PROCESS_STATUS_FLAG, LCDSAPIntegrationDataConstants.VALUE_PROCESS_STATUS_FLAG_IN_WORK);
				domRelBomConnectedToAnchorObj.setAttributeValue(context, lcdSAPInteg3DExpConstants.ATTRIBUTE_LCD_REASON_FOR_FAILURE,
						LCDSAPIntegrationDataConstants.MSG_SCHEDULER_PROCESSING);

				HashMap<String, String> params = new HashMap<>();

				params.put(LCDSAPIntegrationDataConstants.PROPERTY_CONNECTION_ID, connectionId);
				params.put(LCDSAPIntegrationDataConstants.PROPERTY_BOM_COMPONENT_ID, bomId);
				params.put(LCDSAPIntegrationDataConstants.PROPERTY_BOM_COMPONENT_NAME, bomName);
				params.put(LCDSAPIntegrationDataConstants.PROPERTY_CA_ID, caId);

				String[] args = JPO.packArgs(params);

				strResponse = (String) JPO.invoke(context, LCDSAPIntegrationDataConstants.JPO_LCD_3DXSAP_INTEGRATION_SCHEDULER, null,
						LCDSAPIntegrationDataConstants.METHOD_REPUSH_FAILED_BOM_COMPONENTS_TO_SAP, args, String.class);

				if (strResponse == null) {
					throw new NullPointerException("Null Response from SAP");
				} else {
					if (strResponse.equalsIgnoreCase(LCDSAPIntegrationDataConstants.RESPONSE_STATUS_CODE_OK)) {
						domRelBomConnectedToAnchorObj.setAttributeValue(context, lcdSAPInteg3DExpConstants.ATTRIBUTE_LCD_REASON_FOR_FAILURE,
								LCDSAPIntegrationDataConstants.MSG_JSON_FORMAT_VALIDATION_COMPLETED);
						JsonObjectBuilder jobRes = Json.createObjectBuilder();
						jobRes.add(LCDSAPIntegrationDataConstants.PROPERTY_STATUS, LCDSAPIntegrationDataConstants.VALUE_SUCCESS);
						jobRes.add(LCDSAPIntegrationDataConstants.PROPERTY_RESPONSE, strResponse);

						res = Response.ok(jobRes.build().toString()).type(MediaType.APPLICATION_JSON).build();
					} else {
						domRelBomConnectedToAnchorObj.setAttributeValue(context, lcdSAPInteg3DExpConstants.ATTRIBUTE_LCD_PROCESS_STATUS_FLAG,
								LCDSAPIntegrationDataConstants.VALUE_PROCESS_STATUS_FAILED);
						domRelBomConnectedToAnchorObj.setAttributeValue(context, lcdSAPInteg3DExpConstants.ATTRIBUTE_LCD_REASON_FOR_FAILURE,
								LCDSAPIntegrationDataConstants.MSG_JSON_FORMAT_VALIDATION_FAILED);
						JsonObjectBuilder jobRes = Json.createObjectBuilder();
						jobRes.add(LCDSAPIntegrationDataConstants.PROPERTY_STATUS, LCDSAPIntegrationDataConstants.VALUE_PROCESS_STATUS_FAILED);
						jobRes.add(LCDSAPIntegrationDataConstants.PROPERTY_RESPONSE, strResponse);
						jobRes.add(LCDSAPIntegrationDataConstants.PROPERTY_BOM_COMPONENT_NAME, bomName);

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
