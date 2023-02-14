package com.lcd.sapintegration.util;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.logging.Logger;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.ws.rs.core.Response;

import org.apache.jena.rdfxml.xmloutput.impl.SimpleLogger;

//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.DomainRelationship;
import com.matrixone.apps.domain.util.ContextUtil;
import com.matrixone.apps.framework.ui.UIUtil;

import matrix.db.Context;
import matrix.util.StringList;

public class LCDSAPIntegrationProceesAfterEbomAndMbomCreation {
	private static SimpleLogger logger;
//	private static final Logger loggerDebug = LoggerFactory.getLogger("LCD3DXSAPIntegrationCallFromSAP");
	static Logger loggerDebug = Logger.getLogger("LCD3DXSAPIntegrationCallFromSAP");
	
												  	
	public static String processAfterMBOMCreation(Context context, JsonObject jWebServiceResponse, String resultType,
			String strConnectionId, LCDSAPIntegration3DExpConstants lcdSAPInteg3DExpConstants) throws Exception {
		loggerDebug.info("\n\n\n3DX WebService invoked from SAP after MBOM creation");
		SimpleDateFormat logDateFormat = new SimpleDateFormat("dd-MM-yy_hh_mm_ss");
		String strCurrentDate = logDateFormat.format(new Date());
		try {
			Date date = new Date();
			SimpleDateFormat dateFormat = new SimpleDateFormat(LCDSAPIntegrationDataConstants.DATE_FORMAT);
			String strErrorMsg = null;
			boolean bIsTransActive = ContextUtil.isTransactionActive(context);
			
			if(!bIsTransActive) {
				ContextUtil.startTransaction(context, true);
				bIsTransActive = true;
				}
			
			if (UIUtil.isNotNullAndNotEmpty(jWebServiceResponse
					.getString(LCDSAPIntegrationDataConstants.PROPERTY_SAP_RESPONSE_ERROR_MESSAGE))) {
				strErrorMsg = jWebServiceResponse
						.getString(LCDSAPIntegrationDataConstants.PROPERTY_SAP_RESPONSE_ERROR_MESSAGE);
			}
			
			DomainRelationship domRelBomToAnchorObj = DomainRelationship.newInstance(context, strConnectionId);

			String strCaId = domRelBomToAnchorObj.getAttributeValue(context,
					lcdSAPInteg3DExpConstants.ATTRIBUTE_LCD_CAID);

			String strObjID = getBomComponentId(context, strConnectionId);

			StringList slObjectSelect = new StringList();
			slObjectSelect.add(DomainConstants.SELECT_TYPE);
			slObjectSelect.add(DomainConstants.SELECT_NAME);
			slObjectSelect.add(lcdSAPInteg3DExpConstants.SELECT_ATTRIBUTE_HASCONFIGCONTEXT_VPMREFERENCE);
			slObjectSelect.add(lcdSAPInteg3DExpConstants.SELECT_ATTRIBUTE_PLMENTITY_V_NAME);

			DomainObject domObjBomComponent = new DomainObject(strObjID);

			Map<?, ?> mBomDetails = domObjBomComponent.getInfo(context, slObjectSelect);
			String strBomType = (String) mBomDetails.get(DomainConstants.SELECT_TYPE);
			String strBomTitle = (String) mBomDetails.get(lcdSAPInteg3DExpConstants.SELECT_ATTRIBUTE_PLMENTITY_V_NAME);
			
			loggerInitialization(context, lcdSAPInteg3DExpConstants, strBomTitle, "MBOM");
			loggerDebug.info("Json Request from SAP after MBOM creation : [" + strBomTitle + "] : " + jWebServiceResponse.toString());
			logger.writeLog("\n\n\n" + strCurrentDate + "  |  Json Request from SAP after MBOM creation : [" + strBomTitle + "] : \n" + jWebServiceResponse.toString());
			
			
			
			try {
				if (strBomType.equalsIgnoreCase(lcdSAPInteg3DExpConstants.TYPE_MANUFACTURING_ASSEMBLY)) {

					if (UIUtil.isNotNullAndNotEmpty(resultType)
							&& resultType.equalsIgnoreCase(LCDSAPIntegrationDataConstants.VALUE_FAILURE)) {
						domRelBomToAnchorObj.setAttributeValue(context,
								lcdSAPInteg3DExpConstants.ATTRIBUTE_LCD_PROCESS_STATUS_FLAG,
								LCDSAPIntegrationDataConstants.VALUE_PROCESS_STATUS_FLAG_FAILED);
						if (UIUtil.isNotNullAndNotEmpty(strErrorMsg)) {
							domRelBomToAnchorObj.setAttributeValue(context,
									lcdSAPInteg3DExpConstants.ATTRIBUTE_LCD_SAP_FEEDBACK_MESSAGE, strErrorMsg);
						} else {
							domRelBomToAnchorObj.setAttributeValue(context,
									lcdSAPInteg3DExpConstants.ATTRIBUTE_LCD_SAP_FEEDBACK_MESSAGE,
									LCDSAPIntegrationDataConstants.MSG_MBOM_CREATION_FAILED);
						}
					} else if (UIUtil.isNotNullAndNotEmpty(resultType)
							&& resultType.equalsIgnoreCase(LCDSAPIntegrationDataConstants.VALUE_SUCCESS)) {
						domObjBomComponent.setAttributeValue(context,
								lcdSAPInteg3DExpConstants.ATTRIBUTE_LCD_MF_SAPMBOMUpdatedOn, dateFormat.format(date));
						
							JsonObject joManufacturingAssembly = LCDSAPIntegrationGenrateJsonPayload.getManAssemblyJSON(context,
									strObjID, strBomType, strCaId, strConnectionId, false);

							ArrayList<String> listObjID = getObjIdWhereRealizedDataTrue(joManufacturingAssembly);
							for (int i = 0; i < listObjID.size(); i++) {
								DomainRelationship domRel = DomainRelationship.newInstance(context,
										listObjID.get(i).replace("\"", ""));

								domRel.setAttributeValue(context, lcdSAPInteg3DExpConstants.ATTRIBUTE_SAP_INSATNCE_UPDATED_ON,
										dateFormat.format(date));
							}
						
						domRelBomToAnchorObj.setAttributeValue(context,
								lcdSAPInteg3DExpConstants.ATTRIBUTE_LCD_PROCESS_STATUS_FLAG,
								LCDSAPIntegrationDataConstants.VALUE_PROCESS_STATUS_FLAG_COMPLETE);
						domRelBomToAnchorObj.setAttributeValue(context,
								lcdSAPInteg3DExpConstants.ATTRIBUTE_LCD_SAP_FEEDBACK_MESSAGE,
								LCDSAPIntegrationDataConstants.MSG_MBOM_CREATED_SUCCESSFULLY);
					}

				} else {
					if (UIUtil.isNotNullAndNotEmpty(resultType)
							&& resultType.equalsIgnoreCase(LCDSAPIntegrationDataConstants.VALUE_FAILURE)) {
						domRelBomToAnchorObj.setAttributeValue(context,
								lcdSAPInteg3DExpConstants.ATTRIBUTE_LCD_PROCESS_STATUS_FLAG,
								LCDSAPIntegrationDataConstants.VALUE_PROCESS_STATUS_FLAG_FAILED);
						domRelBomToAnchorObj.setAttributeValue(context,
								lcdSAPInteg3DExpConstants.ATTRIBUTE_LCD_SAP_FEEDBACK_MESSAGE, strErrorMsg);
					} else if (UIUtil.isNotNullAndNotEmpty(resultType)
							&& resultType.equalsIgnoreCase(LCDSAPIntegrationDataConstants.VALUE_SUCCESS)) {
						domObjBomComponent.setAttributeValue(context,
								lcdSAPInteg3DExpConstants.ATTRIBUTE_LCD_VPMREFERENCE_SAP_MBOM_UPDATED_ON,
								dateFormat.format(date));

						Map<?, ?> mCadPartDetails = LCDSAPIntegrationGenrateJsonPayload.getCADPartDetails(context, strObjID,
								lcdSAPInteg3DExpConstants);
						JsonObject joPhysicalProduct = LCDSAPIntegrationGenrateJsonPayload.getPhysicalProductJSON(context,
								mCadPartDetails, strConnectionId, lcdSAPInteg3DExpConstants, false);

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
								lcdSAPInteg3DExpConstants.ATTRIBUTE_LCD_SAP_FEEDBACK_MESSAGE, strErrorMsg);
					}
				}
				
				if(bIsTransActive) {
					ContextUtil.commitTransaction(context);
					}
			} catch (Exception e) {
				loggerDebug.info("JSON from SAP after MBOM creation Not Valid");
				if(bIsTransActive) {
					ContextUtil.abortTransaction(context);
					}
				e.printStackTrace();
				return e.getMessage();
			}
			loggerDebug.info("3DX WebService invoked from SAP after MBOM creation Executed succesfully");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return LCDSAPIntegrationDataConstants.VALUE_SUCCESS;
	}

	public static String processAfterEBOMCreation(Context context, JsonObject jWebServiceResponse, String resultType,
			String strConnectionId, LCDSAPIntegration3DExpConstants lcdSAPInteg3DExpConstants) throws Exception {
		loggerDebug.info("\n\n\n3DX WebService invoked from SAP after EBOM creation");
		SimpleDateFormat logDateFormat = new SimpleDateFormat("dd-MM-yy_hh_mm_ss");
		String strCurrentDate = logDateFormat.format(new Date());
		try {
			String strErrorMessage = null;
			StringBuffer sbfErrMes = new StringBuffer();
			
			JsonObject joHeaderPart = jWebServiceResponse
					.getJsonObject(LCDSAPIntegrationDataConstants.PROPERTY_HEADER_PART);
			
			loggerInitialization(context, lcdSAPInteg3DExpConstants,joHeaderPart.getString(LCDSAPIntegrationDataConstants.PROPERTY_TITLE), "EBOM");
			loggerDebug.info("Json Request from SAP after EBOM creation : [" + joHeaderPart.getString(LCDSAPIntegrationDataConstants.PROPERTY_TITLE) + "] : " + jWebServiceResponse.toString());
			logger.writeLog("\n\n\n" + strCurrentDate + "  |  Json Request from SAP after EBOM creation : [" + joHeaderPart.getString(LCDSAPIntegrationDataConstants.PROPERTY_TITLE) + "] : \n" + jWebServiceResponse.toString());
			
			if (null != joHeaderPart) {

				if (UIUtil.isNotNullAndNotEmpty(resultType)
						&& resultType.equalsIgnoreCase(LCDSAPIntegrationDataConstants.VALUE_FAILURE)) {

					if (UIUtil.isNotNullAndNotEmpty(joHeaderPart
							.getString(LCDSAPIntegrationDataConstants.PROPERTY_SAP_RESPONSE_ERROR_MESSAGE))) {
						strErrorMessage = joHeaderPart
								.getString(LCDSAPIntegrationDataConstants.PROPERTY_SAP_RESPONSE_ERROR_MESSAGE);
					}

					String strObjTitle = joHeaderPart.getString(LCDSAPIntegrationDataConstants.PROPERTY_TITLE);
					if (UIUtil.isNotNullAndNotEmpty(strErrorMessage)) {
						sbfErrMes.append(strObjTitle + LCDSAPIntegrationDataConstants.COLON_SEP + strErrorMessage
								+ LCDSAPIntegrationDataConstants.NEW_LINE);
					}
					if (null != joHeaderPart.getJsonArray(LCDSAPIntegrationDataConstants.PROPERTY_CHILDREN)) {
						JsonArray jArrayOfChildParts = joHeaderPart
								.getJsonArray(LCDSAPIntegrationDataConstants.PROPERTY_CHILDREN);
						if (null != jArrayOfChildParts) {
							JsonObject jchildPart;
							String strPartTitle;
							String sErrorMessage;	
							for (int i = 0; i < jArrayOfChildParts.size(); i++) {
								jchildPart = jArrayOfChildParts.getJsonObject(i);
								strPartTitle = jchildPart.getString(LCDSAPIntegrationDataConstants.PROPERTY_TITLE);
								sErrorMessage = jchildPart
										.getString(LCDSAPIntegrationDataConstants.PROPERTY_SAP_RESPONSE_ERROR_MESSAGE);

								if (UIUtil.isNotNullAndNotEmpty(sErrorMessage)) {
									sbfErrMes.append(strPartTitle).append(LCDSAPIntegrationDataConstants.COLON_SEP)
											.append(sErrorMessage).append(LCDSAPIntegrationDataConstants.NEW_LINE);
								}
							}
						}
					}
					DomainRelationship domRelMA = DomainRelationship.newInstance(context, strConnectionId);

					domRelMA.setAttributeValue(context, lcdSAPInteg3DExpConstants.ATTRIBUTE_LCD_PROCESS_STATUS_FLAG,
							LCDSAPIntegrationDataConstants.VALUE_PROCESS_STATUS_FLAG_FAILED);
					if (UIUtil.isNotNullAndNotEmpty(sbfErrMes.toString())) {
						domRelMA.setAttributeValue(context, lcdSAPInteg3DExpConstants.ATTRIBUTE_LCD_SAP_FEEDBACK_MESSAGE,
								sbfErrMes.toString());
					} else {
						domRelMA.setAttributeValue(context, lcdSAPInteg3DExpConstants.ATTRIBUTE_LCD_SAP_FEEDBACK_MESSAGE,
								LCDSAPIntegrationDataConstants.MSG_EBOM_CREATION_FAILED);
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

						if (strBomType.equalsIgnoreCase(lcdSAPInteg3DExpConstants.TYPE_MANUFACTURING_ASSEMBLY)) {

							String strSAPUniqueID = joHeaderPart
									.getString(LCDSAPIntegrationDataConstants.PROPERTY_SAPUNIQUE_ID);

							domObjBomComponent.setAttributeValue(context,
									lcdSAPInteg3DExpConstants.ATTRIBUTE_LCD_MF_SAP_UNIQUEID, strSAPUniqueID);
						}

						DomainRelationship domRel = DomainRelationship.newInstance(context, strConnectionId);

						domRel.setAttributeValue(context, lcdSAPInteg3DExpConstants.ATTRIBUTE_LCD_PROCESS_STATUS_FLAG,
								LCDSAPIntegrationDataConstants.VALUE_PROCESS_STATUS_FLAG_IN_WORK);
						domRel.setAttributeValue(context, lcdSAPInteg3DExpConstants.ATTRIBUTE_LCD_SAP_FEEDBACK_MESSAGE,
								LCDSAPIntegrationDataConstants.MSG_EBOM_CREATED_SUCCESSFULLY);
					}
				} else {
					throw new Exception();
				}
			}
			loggerDebug.info("3DX WebService invoked from SAP after EBOM creation Executed succesfully");
		} catch (Exception e) {
			loggerDebug.info("JSON from SAP after EBOM creation Not Valid");
			e.printStackTrace();
			return e.getMessage();
		}
		return LCDSAPIntegrationDataConstants.VALUE_SUCCESS;

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
						String strRealizedData = jchildPart.get(LCDSAPIntegrationDataConstants.PROPERTY_REALIZED_DATA)
								.toString();
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
	private static void loggerInitialization(Context context, LCDSAPIntegration3DExpConstants lcdSAPInteg3DExpConstants, String obj, String name) throws Exception {
		try {
			// STEP : Log Creation
			
			SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yy");
			String strCurrentDate = dateFormat.format(new Date());
	
			logger = new SimpleLogger("C:\\Temp\\Sync_To_SAP\\log_SAPIntegration\\Request_from_SAP_" + strCurrentDate + ".log");

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static class SimpleLogger {
		private File logFile;
		private PrintWriter pr;
		private FileWriter fw; 
		private BufferedWriter bw; 
		private boolean console = true;

		public SimpleLogger(String logPath) throws IOException {
			logFile = new File(logPath);
			new File(logFile.getParent()).mkdirs();
			if(!logFile.exists()) {
				logFile.createNewFile();
			}
			fw = new FileWriter(logFile, true); 
			bw = new BufferedWriter(fw); 
			pr = new PrintWriter(bw);
		}

		

		public void writeLog(String log) {
			pr.println(log);
			pr.flush();
			if (console) {
				System.out.println(log);
			}
		}
	}
}
