package com.lcd.sapintegration.util;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.logging.Logger;

import javax.json.JsonArray;
import javax.json.JsonObject;

import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.DomainRelationship;
import com.matrixone.apps.framework.ui.UIUtil;

import matrix.db.Context;
import matrix.util.StringList;

public class LCDSAPIntegrationProceesAfterEbomAndMbomCreation {
	static Logger loggerDebug = Logger.getLogger("processAfterMBOMCreation");
	
	public static void processAfterMBOMCreation(Context context, JsonObject jWebServiceResponse, String resultType,
			String strConnectionId, LCDSAPIntegration3DExpConstants lcdSAPInteg3DExpConstants) {
		loggerDebug.info(">>>>>>processAfterMBOMCreation------STARTED " );
		
		try {
			Date date = new Date();
			SimpleDateFormat dateFormat = new SimpleDateFormat(LCDSAPIntegrationDataConstants.DATE_FORMAT);
			String strErrorMsg = null;
			loggerDebug.info("processAfterMBOMCreation------> jWebServiceResponse from SAP after MBOM creation------> " + jWebServiceResponse.toString());
			if(UIUtil.isNotNullAndNotEmpty(jWebServiceResponse.getString(LCDSAPIntegrationDataConstants.PROPERTY_SAP_RESPONSE_ERROR_MESSAGE))) {
				strErrorMsg = jWebServiceResponse.getString(LCDSAPIntegrationDataConstants.PROPERTY_SAP_RESPONSE_ERROR_MESSAGE);
			}
			
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
					loggerDebug.info("processAfterMBOMCreation ------> BOM ID ----> " + strObjID + " Responsefrom SAP-----> " + resultType);
					domRelBomToAnchorObj.setAttributeValue(context,
							lcdSAPInteg3DExpConstants.ATTRIBUTE_LCD_PROCESS_STATUS_FLAG,
							LCDSAPIntegrationDataConstants.VALUE_PROCESS_STATUS_FLAG_FAILED);
					domRelBomToAnchorObj.setAttributeValue(context,
							lcdSAPInteg3DExpConstants.ATTRIBUTE_LCD_REASON_FOR_FAILURE, strErrorMsg);
				} else if (UIUtil.isNotNullAndNotEmpty(resultType)
						&& resultType.equalsIgnoreCase(LCDSAPIntegrationDataConstants.VALUE_SUCCESS)) {
					loggerDebug.info("processAfterMBOMCreation ------> BOM ID ----> " + strObjID + " Responsefrom SAP-----> " + resultType);
					domObjBomComponent.setAttributeValue(context, lcdSAPInteg3DExpConstants.ATTRIBUTE_LCD_MF_SAPMBOMUpdatedOn, dateFormat.format(date));
					
					JsonObject joManufacturingAssembly = LCDSAPIntegrationGenrateJsonPayload
							.getManAssemblyJSON(context, strObjID, strBomType, strCaId, strConnectionId);

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
					loggerDebug.info("processAfterMBOMCreation ------> BOM ID ----> " + strObjID + " Responsefrom SAP-----> " + resultType);
					domRelBomToAnchorObj.setAttributeValue(context,
							lcdSAPInteg3DExpConstants.ATTRIBUTE_LCD_PROCESS_STATUS_FLAG,
							LCDSAPIntegrationDataConstants.VALUE_PROCESS_STATUS_FLAG_FAILED);
					domRelBomToAnchorObj.setAttributeValue(context,
							lcdSAPInteg3DExpConstants.ATTRIBUTE_LCD_REASON_FOR_FAILURE, strErrorMsg);
				} else if (UIUtil.isNotNullAndNotEmpty(resultType)
						&& resultType.equalsIgnoreCase(LCDSAPIntegrationDataConstants.VALUE_SUCCESS)) {
					loggerDebug.info("processAfterMBOMCreation ------> BOM ID ----> " + strObjID + " Responsefrom SAP-----> " + resultType);
					domObjBomComponent.setAttributeValue(context, lcdSAPInteg3DExpConstants.ATTRIBUTE_LCD_VPMREFERENCE_SAP_MBOM_UPDATED_ON, dateFormat.format(date));

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
								domRelationship = DomainRelationship.newInstance(context, sRelID.replace("\"", ""));

								domRelationship.setAttributeValue(context,
										lcdSAPInteg3DExpConstants.ATTRIBUTE_SAP_CAD_INSATNCE_UPDATED_ON,
										dateFormat.format(date));
							}
						}
					}
					domRelBomToAnchorObj.setAttributeValue(context,
							lcdSAPInteg3DExpConstants.ATTRIBUTE_LCD_PROCESS_STATUS_FLAG,
							LCDSAPIntegrationDataConstants.VALUE_PROCESS_STATUS_FLAG_COMPLETE);
					domRelBomToAnchorObj.setAttributeValue(context,
							lcdSAPInteg3DExpConstants.ATTRIBUTE_LCD_REASON_FOR_FAILURE, strErrorMsg);
				}
			}
			loggerDebug.info(">>>>>>processAfterMBOMCreation------ END");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void processAfterEBOMCreation(Context context, JsonObject jWebServiceResponse, String resultType, String strConnectionId, LCDSAPIntegration3DExpConstants lcdSAPInteg3DExpConstants) {
		try {
			loggerDebug.info(">>>>>>processAfterEBOMCreation------ START");
			StringBuffer sbfErrMes = new StringBuffer();
			loggerDebug.info("processAfterEBOMCreation------> jWebServiceResponse from SAP after EBOM creation------> " + jWebServiceResponse.toString());
			JsonObject joHeaderPart = jWebServiceResponse
					.getJsonObject(LCDSAPIntegrationDataConstants.PROPERTY_HEADER_PART);
			if (null != joHeaderPart) {

				if (UIUtil.isNotNullAndNotEmpty(resultType)
						&& resultType.equalsIgnoreCase(LCDSAPIntegrationDataConstants.VALUE_FAILURE)) {
					loggerDebug.info("processAfterEBOMCreation ------> Responsefrom SAP-----> " + resultType);
					String strErrorMessage = joHeaderPart
							.getString(LCDSAPIntegrationDataConstants.PROPERTY_SAP_RESPONSE_ERROR_MESSAGE);
					String strObjTitle = joHeaderPart.getString(LCDSAPIntegrationDataConstants.PROPERTY_TITLE);
					loggerDebug.info("LCDSAPIntegrationCallFromSAP------ strObjTitle " + strObjTitle + " strErrorMessage " + strErrorMessage);
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
					loggerDebug.info("processAfterMBOMCreation ------> jArrayOfChildParts -----> " + jArrayOfChildParts.toString());
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
								LCDSAPIntegrationDataConstants.MSG_EBOM_CREATION_FAILED);
					}

				} else if (UIUtil.isNotNullAndNotEmpty(resultType)
						&& resultType.equalsIgnoreCase(LCDSAPIntegrationDataConstants.VALUE_SUCCESS)) {
					loggerDebug.info("processAfterEBOMCreation ------> Responsefrom SAP-----> " + resultType);
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
			loggerDebug.info(">>>>>>processAfterEBOMCreation------ END");
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	private static String getBomComponentId(Context context, String strConnectionId) throws Exception {

		StringList relInfoList = new StringList();
		relInfoList.add(DomainConstants.SELECT_ID);
		relInfoList.add(DomainConstants.SELECT_TO_ID);

		DomainRelationship domRel = new DomainRelationship(strConnectionId);
		java.util.Hashtable relInfoMap = domRel.getRelationshipData(context, relInfoList);

		StringList slObjID = (StringList) relInfoMap.get(DomainConstants.SELECT_TO_ID);

		return (String) slObjID.get(0);
	}

	private static ArrayList<String> getObjIdWhereRealizedDataTrue(JsonObject joManufacturingAssembly) {
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
