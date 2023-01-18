package com.lcd.sapintegration.util;

import java.io.StringReader;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.http.HttpStatus;

import com.dassault_systemes.platform.restServices.RestService;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.DomainRelationship;
import com.matrixone.apps.domain.util.ContextUtil;
import com.matrixone.apps.framework.ui.UIUtil;

import matrix.db.Context;
import matrix.util.StringList;

public class LCDSAPIntegrationPushToSAPUtil extends RestService {

	public static Response sendFailedDataToSap(Context context, String strParamString) throws NullPointerException {
		LCDSAPIntegration3DExpConstants lcdSAPInteg3DExpConstants = new LCDSAPIntegration3DExpConstants(context);
		Response res = null;
		String bomName;
		JsonReader jsonReader = null;
		int intGetSerivceResponseCode = 0;
		int intPostSerivceResponseCode = 0;

		try {
			ContextUtil.pushContext(context);

			jsonReader = Json.createReader(new StringReader(strParamString));
			JsonObject joRequest = jsonReader.readObject();

			String connectionId = joRequest.get(LCDSAPIntegrationDataConstants.PROPERTY_CONNECTION_ID).toString()
					.replace("\"", "");
			String bomId = joRequest.get(LCDSAPIntegrationDataConstants.PROPERTY_BOM_COMPONENT_ID).toString()
					.replace("\"", "");
			bomName = joRequest.get(LCDSAPIntegrationDataConstants.PROPERTY_BOM_COMPONENT_NAME).toString().replace("\"",
					"");
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

			DomainRelationship domRelBomConnectedToAnchorObj = DomainRelationship.newInstance(context, connectionId);

			String strCurrentStatus = domRelBomConnectedToAnchorObj.getAttributeValue(context,
					lcdSAPInteg3DExpConstants.ATTRIBUTE_LCD_PROCESS_STATUS_FLAG);

			if (strCurrentStatus.equals(LCDSAPIntegrationDataConstants.VALUE_PROCESS_STATUS_FLAG_FAILED)) {

				domRelBomConnectedToAnchorObj.setAttributeValue(context,
						lcdSAPInteg3DExpConstants.ATTRIBUTE_LCD_PROCESS_STATUS_FLAG,
						LCDSAPIntegrationDataConstants.VALUE_PROCESS_STATUS_FLAG_IN_WORK);
				domRelBomConnectedToAnchorObj.setAttributeValue(context,
						lcdSAPInteg3DExpConstants.ATTRIBUTE_LCD_REASON_FOR_FAILURE,
						LCDSAPIntegrationDataConstants.MSG_SCHEDULER_PROCESSING);

				intGetSerivceResponseCode = LCDSAPIntegrationSendJsonToSAP.callGETService(context);
				intPostSerivceResponseCode = sendFailedBomToSAP(context, intGetSerivceResponseCode, bomId, caId,
						connectionId, lcdSAPInteg3DExpConstants);
				String strResponse = String.valueOf(intPostSerivceResponseCode);
				if (strResponse == null) {
					throw new NullPointerException("Null Response from SAP");
				} else {
					if (strResponse.equalsIgnoreCase(LCDSAPIntegrationDataConstants.RESPONSE_STATUS_CODE_OK)) {
						domRelBomConnectedToAnchorObj.setAttributeValue(context,
								lcdSAPInteg3DExpConstants.ATTRIBUTE_LCD_REASON_FOR_FAILURE,
								LCDSAPIntegrationDataConstants.MSG_JSON_FORMAT_VALIDATION_COMPLETED);
						JsonObjectBuilder jobRes = Json.createObjectBuilder();
						jobRes.add(LCDSAPIntegrationDataConstants.PROPERTY_STATUS,
								LCDSAPIntegrationDataConstants.VALUE_SUCCESS);
						jobRes.add(LCDSAPIntegrationDataConstants.PROPERTY_RESPONSE, strResponse);
						jobRes.add(LCDSAPIntegrationDataConstants.PROPERTY_BOM_COMPONENT_NAME, bomName);

						res = Response.ok(jobRes.build().toString()).type(MediaType.APPLICATION_JSON).build();
					} else {
						domRelBomConnectedToAnchorObj.setAttributeValue(context,
								lcdSAPInteg3DExpConstants.ATTRIBUTE_LCD_PROCESS_STATUS_FLAG,
								LCDSAPIntegrationDataConstants.VALUE_PROCESS_STATUS_FLAG_FAILED);
						domRelBomConnectedToAnchorObj.setAttributeValue(context,
								lcdSAPInteg3DExpConstants.ATTRIBUTE_LCD_REASON_FOR_FAILURE,
								LCDSAPIntegrationDataConstants.MSG_JSON_FORMAT_VALIDATION_FAILED);
						JsonObjectBuilder jobRes = Json.createObjectBuilder();
						jobRes.add(LCDSAPIntegrationDataConstants.PROPERTY_STATUS,
								LCDSAPIntegrationDataConstants.VALUE_PROCESS_STATUS_FLAG_FAILED);
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
		} finally {
			if (jsonReader != null) {
				jsonReader.close();
			}
		}
		return res;
	}

	private static int sendFailedBomToSAP(Context context, int intResponseCode, String bomId, String caId,
			String connectionId, LCDSAPIntegration3DExpConstants lcdSAPInteg3DExpConstants) throws Exception {
		JsonObject joBomPayload;
		if (intResponseCode == HttpStatus.SC_OK) {
			if (UIUtil.isNotNullAndNotEmpty(bomId)) {
				DomainObject domObj = DomainObject.newInstance(context, bomId);
				StringList slObjectSelect = new StringList();
				slObjectSelect.add(DomainConstants.SELECT_TYPE);

				Map<?, ?> bomMap = domObj.getInfo(context, slObjectSelect);
				String strBOMComponentType = (String) bomMap.get(DomainConstants.SELECT_TYPE);

				if (lcdSAPInteg3DExpConstants.TYPE_MANUFACTURING_ASSEMBLY.equalsIgnoreCase(strBOMComponentType)) {
					joBomPayload = LCDSAPIntegrationGetManAssemblyJson.getManAssemblyJSON(context, bomId,
							strBOMComponentType, caId);
					if (UIUtil.isNotNullAndNotEmpty(joBomPayload.toString())) {
						intResponseCode = LCDSAPIntegrationSendJsonToSAP.callPostService(context, joBomPayload);
					} else {
						System.out.println("ERROR :: Failed to generate JSON Payload for object id : " + bomId);
					}
					LCDSAPIntegrationProcessWebServiceResponse
							.processWebServiceResponseForManufacturingAssembly(context, intResponseCode, connectionId);
				} else {
					Map<?, ?> mCadPartDetails = LCDSAPIntegrationGetPhysicalProductJson.getCADPartDetails(context,
							bomId, lcdSAPInteg3DExpConstants);
					String strProcurementIntent = (String) mCadPartDetails
							.get(lcdSAPInteg3DExpConstants.SELECT_ATTRIBUTE_PROCUREMENTINTENT_VPMREFERENCE);
					if (LCDSAPIntegrationDataConstants.SUBCONTRACT.equalsIgnoreCase(strProcurementIntent)) {
						joBomPayload = LCDSAPIntegrationGetPhysicalProductJson.getPhysicalProductJSON(context,
								mCadPartDetails, connectionId, lcdSAPInteg3DExpConstants);
						if (UIUtil.isNotNullAndNotEmpty(joBomPayload.toString())) {
							intResponseCode = LCDSAPIntegrationSendJsonToSAP.callPostService(context, joBomPayload);
						} else {
							System.out.println("ERROR :: Failed to generate JSON Payload for object id : " + bomId);
						}
						LCDSAPIntegrationProcessWebServiceResponse.processWebServiceResponseForSubContract(context,
								intResponseCode, connectionId);
					}
				}
			}
		}
		return intResponseCode;
	}
}
