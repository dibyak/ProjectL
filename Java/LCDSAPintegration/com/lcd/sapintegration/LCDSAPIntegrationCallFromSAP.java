package com.lcd.sapintegration;

import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.dassault_systemes.platform.restServices.RestService;
import com.lcd.sapintegration.util.LCDSAPIntegration3DExpConstants;
import com.lcd.sapintegration.util.LCDSAPIntegrationDataConstants;
import com.lcd.sapintegration.util.LCDSAPIntegrationGenrateJsonPayload;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.DomainRelationship;
import com.matrixone.apps.domain.util.ContextUtil;
import com.matrixone.apps.framework.ui.UIUtil;

import matrix.db.Context;
import matrix.util.StringList;

@Path("/LCDGetFromSAPServices")
public class LCDSAPIntegrationCallFromSAP extends RestService {

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
	@Path("/callFromSAP")
	@Consumes({ "application/json", "application/ds-json" })
	public Response processRequestFromSAP(@javax.ws.rs.core.Context HttpServletRequest request, String paramString)
			throws Exception {

		System.out.println(">>>>>LCDSAPIntegrationCallFromSAP---processRequestFromSAP()-----STARTED");
		Response res = null;
		boolean bContextPushed = false;

		boolean isSCMandatory = false;
		matrix.db.Context context = getAuthenticatedContext(request, isSCMandatory);
		
		LCDSAPIntegration3DExpConstants lcdSAPInteg3DExpConstants = new LCDSAPIntegration3DExpConstants(context);

		ContextUtil.pushContext(context);
		bContextPushed = true;

		StringBuffer sbfErrMes = new StringBuffer();
		SimpleDateFormat dateFormat = new SimpleDateFormat(LCDSAPIntegrationDataConstants.DATE_FORMAT);

		try (JsonReader jsonReader = Json.createReader(new StringReader(paramString))) {
			JsonObject jWebServiceResponse = jsonReader.readObject();

			String resultType = jWebServiceResponse.getString(LCDSAPIntegrationDataConstants.PROPERTY_TYPE);
			String strConnectionId = jWebServiceResponse.getString(LCDSAPIntegrationDataConstants.PROPERTY_OID);

			if (null != jWebServiceResponse) {
				if (jWebServiceResponse.containsKey(LCDSAPIntegrationDataConstants.PROPERTY_HEADER_PART)) {
					JsonObject joHeaderPart = jWebServiceResponse
							.getJsonObject(LCDSAPIntegrationDataConstants.PROPERTY_HEADER_PART);
					if (null != joHeaderPart) {

						if (UIUtil.isNotNullAndNotEmpty(resultType)
								&& resultType.equalsIgnoreCase(LCDSAPIntegrationDataConstants.VALUE_FAILURE)) {

							String strErrorMessage = joHeaderPart
									.getString(LCDSAPIntegrationDataConstants.PROPERTY_SAP_RESPONSE_ERROR_MESSAGE);
							String strObjTitle = joHeaderPart.getString(LCDSAPIntegrationDataConstants.PROPERTY_TITLE);

							if (UIUtil.isNotNullAndNotEmpty(strErrorMessage)) {
								sbfErrMes.append(strObjTitle + LCDSAPIntegrationDataConstants.COLON_SEP
										+ strErrorMessage + LCDSAPIntegrationDataConstants.NEW_LINE);
							}
							JsonArray jArrayOfChildParts = joHeaderPart
									.getJsonArray(LCDSAPIntegrationDataConstants.PROPERTY_CHILDREN);
							if (null != jArrayOfChildParts) {
								JsonObject jchildPart;
								String strPartTitle;
								String sErrorMessage;
								for (int i = 0; i < jArrayOfChildParts.size(); i++) {
									jchildPart = jArrayOfChildParts.getJsonObject(i);
									strPartTitle = jchildPart.getString(LCDSAPIntegrationDataConstants.PROPERTY_TITLE);
									sErrorMessage = jchildPart.getString(
											LCDSAPIntegrationDataConstants.PROPERTY_SAP_RESPONSE_ERROR_MESSAGE);

									if (UIUtil.isNotNullAndNotEmpty(sErrorMessage)) {
										sbfErrMes.append(strPartTitle).append(LCDSAPIntegrationDataConstants.COLON_SEP)
												.append(sErrorMessage).append(LCDSAPIntegrationDataConstants.NEW_LINE);
									}
								}
							}
							DomainRelationship domRelMA = DomainRelationship.newInstance(context, strConnectionId);

							domRelMA.setAttributeValue(context,
									lcdSAPInteg3DExpConstants.ATTRIBUTE_LCD_PROCESS_STATUS_FLAG,
									LCDSAPIntegrationDataConstants.VALUE_PROCESS_STATUS_FLAG_FAILED);
							if (UIUtil.isNotNullAndNotEmpty(sbfErrMes.toString())) {
								domRelMA.setAttributeValue(context,
										lcdSAPInteg3DExpConstants.ATTRIBUTE_LCD_REASON_FOR_FAILURE,
										sbfErrMes.toString());
							} else {
								domRelMA.setAttributeValue(context,
										lcdSAPInteg3DExpConstants.ATTRIBUTE_LCD_REASON_FOR_FAILURE,
										LCDSAPIntegrationDataConstants.MSG_EBOM_CREATED_SUCCESSFULLY);
							}

						} else if (UIUtil.isNotNullAndNotEmpty(resultType)
								&& resultType.equalsIgnoreCase(LCDSAPIntegrationDataConstants.VALUE_SUCCESS)) {
							String strObjID = joHeaderPart.getString(LCDSAPIntegrationDataConstants.PROPERTY_OID);

							if (UIUtil.isNotNullAndNotEmpty(strObjID)) {

								StringList slObjectSelect = new StringList();
								slObjectSelect.add(DomainConstants.SELECT_TYPE);

								DomainObject domObjBomComponent = new DomainObject(strObjID);
								Map<?, ?> mBomDetails = domObjBomComponent.getInfo(context, slObjectSelect);

								String strBomType = (String) mBomDetails.get(DomainConstants.SELECT_TYPE);

								if (strBomType
										.equalsIgnoreCase(lcdSAPInteg3DExpConstants.TYPE_MANUFACTURING_ASSEMBLY)) {

									String strSAPUniqueID = joHeaderPart
											.getString(LCDSAPIntegrationDataConstants.PROPERTY_SAPUNIQUE_ID);

									domObjBomComponent.setAttributeValue(context,
											lcdSAPInteg3DExpConstants.ATTRIBUTE_LCD_MF_SAP_UNIQUEID, strSAPUniqueID);
								}

								DomainRelationship domRel = DomainRelationship.newInstance(context, strConnectionId);

								domRel.setAttributeValue(context,
										lcdSAPInteg3DExpConstants.ATTRIBUTE_LCD_PROCESS_STATUS_FLAG,
										LCDSAPIntegrationDataConstants.VALUE_PROCESS_STATUS_FLAG_IN_WORK);
								domRel.setAttributeValue(context,
										lcdSAPInteg3DExpConstants.ATTRIBUTE_LCD_REASON_FOR_FAILURE,
										LCDSAPIntegrationDataConstants.MSG_EBOM_CREATED_SUCCESSFULLY);
							}
						}
					}
				} else {
					String strErrorMsg = jWebServiceResponse
							.getString(LCDSAPIntegrationDataConstants.PROPERTY_SAP_RESPONSE_ERROR_MESSAGE);

					DomainRelationship domRelBomToAnchorObj = DomainRelationship.newInstance(context, strConnectionId);

					String strCaId = domRelBomToAnchorObj.getAttributeValue(context,
							lcdSAPInteg3DExpConstants.ATTRIBUTE_LCD_CAID);

					String strObjID = getBomComponentId(context, strConnectionId);

					StringList slObjectSelect = new StringList();
					slObjectSelect.add(DomainConstants.SELECT_TYPE);

					DomainObject domObjBomComponent = new DomainObject(strObjID);

					Map<?, ?> mBomDetails = domObjBomComponent.getInfo(context, slObjectSelect);

					String strBomType = (String) mBomDetails.get(DomainConstants.SELECT_TYPE);

					if (strBomType.equalsIgnoreCase(lcdSAPInteg3DExpConstants.TYPE_MANUFACTURING_ASSEMBLY)) {

						if (UIUtil.isNotNullAndNotEmpty(resultType)
								&& resultType.equalsIgnoreCase(LCDSAPIntegrationDataConstants.VALUE_FAILURE)) {

							domRelBomToAnchorObj.setAttributeValue(context,
									lcdSAPInteg3DExpConstants.ATTRIBUTE_LCD_PROCESS_STATUS_FLAG,
									LCDSAPIntegrationDataConstants.VALUE_PROCESS_STATUS_FLAG_FAILED);
							domRelBomToAnchorObj.setAttributeValue(context,
									lcdSAPInteg3DExpConstants.ATTRIBUTE_LCD_REASON_FOR_FAILURE, strErrorMsg);
						} else if (UIUtil.isNotNullAndNotEmpty(resultType)
								&& resultType.equalsIgnoreCase(LCDSAPIntegrationDataConstants.VALUE_SUCCESS)) {

							Date date = new Date();
							
							JsonObject joManufacturingAssembly = LCDSAPIntegrationGenrateJsonPayload
									.getManAssemblyJSON(context, strObjID, strBomType, strCaId);

							ArrayList<String> listObjID = getObjIdWhereRealizedDataTrue(joManufacturingAssembly);
							for (int i = 0; i < listObjID.size(); i++) {
								DomainRelationship domRel = DomainRelationship.newInstance(context, listObjID.get(i).replace("\"", ""));
								domRel.setAttributeValue(context,
										lcdSAPInteg3DExpConstants.ATTRIBUTE_SAP_INSATNCE_UPDATED_ON,
										dateFormat.format(date));
								domRelBomToAnchorObj.setAttributeValue(context,
										lcdSAPInteg3DExpConstants.ATTRIBUTE_LCD_PROCESS_STATUS_FLAG,
										LCDSAPIntegrationDataConstants.VALUE_PROCESS_STATUS_FLAG_COMPLETE);
								domRelBomToAnchorObj.setAttributeValue(context,
										lcdSAPInteg3DExpConstants.ATTRIBUTE_LCD_REASON_FOR_FAILURE,
										LCDSAPIntegrationDataConstants.MSG_MBOM_CREATED_SUCCESSFULLY);
							}
						}

					} else {
						if (UIUtil.isNotNullAndNotEmpty(resultType)
								&& resultType.equalsIgnoreCase(LCDSAPIntegrationDataConstants.VALUE_FAILURE)) {

							domRelBomToAnchorObj.setAttributeValue(context,
									lcdSAPInteg3DExpConstants.ATTRIBUTE_LCD_PROCESS_STATUS_FLAG,
									LCDSAPIntegrationDataConstants.VALUE_PROCESS_STATUS_FLAG_FAILED);
							domRelBomToAnchorObj.setAttributeValue(context,
									lcdSAPInteg3DExpConstants.ATTRIBUTE_LCD_REASON_FOR_FAILURE, strErrorMsg);
						} else if (UIUtil.isNotNullAndNotEmpty(resultType)
								&& resultType.equalsIgnoreCase(LCDSAPIntegrationDataConstants.VALUE_SUCCESS)) {

							domRelBomToAnchorObj.setAttributeValue(context,
									lcdSAPInteg3DExpConstants.ATTRIBUTE_LCD_PROCESS_STATUS_FLAG,
									LCDSAPIntegrationDataConstants.VALUE_PROCESS_STATUS_FLAG_COMPLETE);
							domRelBomToAnchorObj.setAttributeValue(context,
									lcdSAPInteg3DExpConstants.ATTRIBUTE_LCD_REASON_FOR_FAILURE, strErrorMsg);

							Map<?, ?> mCadPartDetails = LCDSAPIntegrationGenrateJsonPayload.getCADPartDetails(context,
									strObjID, lcdSAPInteg3DExpConstants);
							JsonObject joPhysicalProduct = LCDSAPIntegrationGenrateJsonPayload.getPhysicalProductJSON(
									context, mCadPartDetails, strConnectionId, lcdSAPInteg3DExpConstants);

							JsonObject joHeaderPart = joPhysicalProduct
									.getJsonObject(LCDSAPIntegrationDataConstants.PROPERTY_HEADER_PART);
							String strRelID = joHeaderPart.getString(LCDSAPIntegrationDataConstants.PROPERTY_REL_ID);
							if (UIUtil.isNotNullAndNotEmpty(strRelID)) {
								DomainRelationship domRel = DomainRelationship.newInstance(context, strRelID.replace("\"", ""));
								// Push context as no Manufacturing Assembly access to 3DXLeader in release
								// state in respective policy.
								Date date = new Date();
								domRel.setAttributeValue(context,
										lcdSAPInteg3DExpConstants.ATTRIBUTE_SAP_CAD_INSATNCE_UPDATED_ON,
										dateFormat.format(date));
							}
							JsonArray jArrayOfChildParts = joHeaderPart
									.getJsonArray(LCDSAPIntegrationDataConstants.PROPERTY_CHILDREN);
							if (null != jArrayOfChildParts) {
								JsonObject jchildPart;
								String sRelID;
								DomainRelationship domRelationship;
								for (int i = 0; i < jArrayOfChildParts.size(); i++) {
									jchildPart = jArrayOfChildParts.getJsonObject(i);
									sRelID = jchildPart.getString(LCDSAPIntegrationDataConstants.PROPERTY_REL_ID);
									if (UIUtil.isNotNullAndNotEmpty(sRelID)) {
										domRelationship = DomainRelationship.newInstance(context, sRelID);

										Date date = new Date();
										domRelationship.setAttributeValue(context,
												lcdSAPInteg3DExpConstants.ATTRIBUTE_SAP_CAD_INSATNCE_UPDATED_ON,
												dateFormat.format(date));
									}
								}
							}

						}
					}

				}
			}
			System.out.println(">>>>>LCDSAPIntegrationCallFromSAP---processRequestFromSAP()-----ENDED");
			res = Response.ok(LCDSAPIntegrationDataConstants.VALUE_SUCCESS).type(MediaType.TEXT_PLAIN).build();
		} catch (Exception e) {
			e.printStackTrace();
			res = Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
		} finally {
			if (bContextPushed) {
				ContextUtil.popContext(context);
			}
		}
		return res;
	}

	private String getBomComponentId(Context context, String strConnectionId) throws Exception {

		StringList relInfoList = new StringList();
		relInfoList.add(DomainConstants.SELECT_ID);
		relInfoList.add(DomainConstants.SELECT_TO_ID);

		DomainRelationship domRel = new DomainRelationship(strConnectionId);
		java.util.Hashtable relInfoMap = domRel.getRelationshipData(context, relInfoList);

		StringList slObjID = (StringList) relInfoMap.get(DomainConstants.SELECT_TO_ID);

		return (String) slObjID.get(0);
	}

	private ArrayList<String> getObjIdWhereRealizedDataTrue(JsonObject joManufacturingAssembly) {
		ArrayList<String> listRelObjID = new ArrayList<>();

		if (null != joManufacturingAssembly
				&& joManufacturingAssembly.containsKey(LCDSAPIntegrationDataConstants.PROPERTY_HEADER_PART)) {

			JsonObject jPayloadObject = joManufacturingAssembly
					.getJsonObject(LCDSAPIntegrationDataConstants.PROPERTY_HEADER_PART);
			if (null != jPayloadObject) {
				JsonArray jArrayOfChildParts = jPayloadObject
						.getJsonArray(LCDSAPIntegrationDataConstants.PROPERTY_CHILDREN);
				if (null != jArrayOfChildParts) {
					JsonObject jchildPart;
					for (int i = 0; i < jArrayOfChildParts.size(); i++) {
						jchildPart = jArrayOfChildParts.getJsonObject(i);
						String strRealizedData = jchildPart.get(LCDSAPIntegrationDataConstants.PROPERTY_REALIZED_DATA).toString();
						String strRelId = jchildPart.get(LCDSAPIntegrationDataConstants.PROPERTY_REL_ID).toString();

						if (strRealizedData.equalsIgnoreCase(LCDSAPIntegrationDataConstants.TRUE)) {
							listRelObjID.add(strRelId);
						}
					}
				}
			}

		}

		return listRelObjID;
	}

}
