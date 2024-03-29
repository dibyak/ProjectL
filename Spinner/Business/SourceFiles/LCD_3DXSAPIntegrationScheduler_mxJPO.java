import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import matrix.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import com.dassault_systemes.enovia.changeaction.impl.ChangeAction;
import com.dassault_systemes.enovia.changeaction.servicesimpl.ChangeActionServices;
import com.dassault_systemes.enovia.changeaction.webservice.services.ChangeActionJsonUtilities;
import com.dassault_systemes.enovia.changeaction.webservice.services.ChangeActionJsonUtilities.ChangeActionFacets;
import com.dassault_systemes.plm.config.exposed.factory.ConfigurationExposedFilterablesFactory;
import com.dassault_systemes.plm.config.exposed.interfaces.IConfigurationExposedFilterables;
import com.lcd.sapintegration.util.LCDSAPIntegration3DExpConstants;
import com.lcd.sapintegration.util.LCDSAPIntegrationDataConstants;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.DomainRelationship;
import com.matrixone.apps.domain.util.ContextUtil;
import com.matrixone.apps.domain.util.EnoviaResourceBundle;
import com.matrixone.apps.domain.util.FrameworkException;
import com.matrixone.apps.domain.util.FrameworkUtil;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.MqlUtil;
import com.matrixone.apps.domain.util.eMatrixDateFormat;
import com.matrixone.apps.framework.ui.UIUtil;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;

import matrix.db.BusinessObject;
import matrix.db.Context;
import matrix.util.MatrixException;
import matrix.util.Pattern;
import matrix.util.StringList;

public class LCD_3DXSAPIntegrationScheduler_mxJPO {

	private static final Base64.Encoder BASE64_ENCODER = Base64.getEncoder();

	private static String language;
	private static String url;
	private static String xcsrfToken;
	private static String cookie;
	private static SimpleLogger logger;
	private static SimpleLogger jsonLogger;
	private static CloseableHttpClient httpClient;
	private static JsonObject changeActionJson;
	private static String caReleasedDate;
	private static boolean FALSE_EFFECTIVITY_STATUS;
	private static boolean CONFIGURABLE_STATUS;
	private static String CA_START_DATE;
	private static List<String> LIST_REALIZED_CHANGES;
	private static String partReleasedDate;
	

	private static HashMap<String, JsonObjectBuilder> changeActionMap = new HashMap<>();
	
	
	Logger loggerDebug = Logger.getLogger("LCD_3DXSAPIntegrationScheduler_mxJPO");

	/**
	 * Main Method.
	 * 
	 * @param context
	 * @return
	 * @throws Exception
	 */
	public void scheduledPushToSAP(Context context, String[] strArgs) throws Exception {
		LCDSAPIntegration3DExpConstants lcdSAPInteg3DExpConstants = new LCDSAPIntegration3DExpConstants(context);
		loggerInitialization(context, lcdSAPInteg3DExpConstants);

		boolean isPresent = checkAncObjPresent(context, lcdSAPInteg3DExpConstants);
		if (isPresent) {
			logger.writeLog("Anchor Object already Present");
			MapList bomComponentsMapList = findRelevantObjectsConnectedToAnchorObject(context, lcdSAPInteg3DExpConstants);
			bomComponentsMapList.sort("originated", "ascending","date");
			if(bomComponentsMapList != null && bomComponentsMapList.size() > 0) {
				pushToSAP(context, bomComponentsMapList, lcdSAPInteg3DExpConstants);
			}else 
				logger.writeLog("No BOM component is connected with Anchor Object");
		} else {
			DomainObject domObjBomAncOcj = DomainObject.newInstance(context, lcdSAPInteg3DExpConstants.TYPE_LCD_BOM_ANCHOR_OBJECT);
			logger.writeLog("Anchor Object created." + domObjBomAncOcj);
			domObjBomAncOcj.createObject(context, lcdSAPInteg3DExpConstants.TYPE_LCD_BOM_ANCHOR_OBJECT, lcdSAPInteg3DExpConstants.NAME_LCD_BOM_ANCHOR_OBJECT,
					lcdSAPInteg3DExpConstants.REVISION_LCD_BOM_ANCHOR_OBJECT, lcdSAPInteg3DExpConstants.POLICY_LCD_3DX_SAP_INTEGRATION, lcdSAPInteg3DExpConstants.VAULT_ESERVICE_PRODUCTION);
			logger.writeLog("Anchor Object created -> " + domObjBomAncOcj);
		}
	}

	/**
	 * This Method is check if Anchor Object is present.
	 * 
	 * @param context
	 * @return
	 * @throws Exception
	 */
	private boolean checkAncObjPresent(Context context, LCDSAPIntegration3DExpConstants lcdSAPInteg3DExpConstants) throws MatrixException {
		boolean isPresent;

		BusinessObject busObjAchor = new BusinessObject(lcdSAPInteg3DExpConstants.TYPE_LCD_BOM_ANCHOR_OBJECT, lcdSAPInteg3DExpConstants.NAME_LCD_BOM_ANCHOR_OBJECT,
				lcdSAPInteg3DExpConstants.REVISION_LCD_BOM_ANCHOR_OBJECT, lcdSAPInteg3DExpConstants.VAULT_ESERVICE_PRODUCTION);

		if (busObjAchor.exists(context)) {
			isPresent = true;
		} else {
			isPresent = false;
		}
		return isPresent;
	}

	/**
	 * This Method is to Push BOM Components to SAP.
	 * 
	 * @param context
	 * @return
	 * @throws Exception
	 */
	private void pushToSAP(Context context, MapList bomComponentsMapList, LCDSAPIntegration3DExpConstants lcdSAPInteg3DExpConstants) throws Exception {

		Iterator<?> iterMAsMaplist = bomComponentsMapList.iterator();
		int intResponseCode = 0;
		try {
			JsonObject joBomPayload = null;

			
			//callGETService(context, lcdSAPInteg3DExpConstants);

			//if (UIUtil.isNotNullAndNotEmpty(xcsrfToken) && UIUtil.isNotNullAndNotEmpty(cookie)) {
				while (iterMAsMaplist.hasNext()) {
					Map<?, ?> item = (Map<?, ?>) iterMAsMaplist.next();
					String strConnectionId = (String) (item.get(DomainRelationship.SELECT_ID));
					String strBOMComponentId = (String) (item.get(DomainConstants.SELECT_ID));
					String strBOMComponentType = (String) (item.get(DomainConstants.SELECT_TYPE));
					String strbomModified = (String) (item.get("modified"));
					String strbomOriginated = (String) (item.get("originated"));
					String strBOMComponentName = (String) (item.get(DomainConstants.SELECT_NAME));
					String strBOMComponentTitle = (String) (item.get(lcdSAPInteg3DExpConstants.SELECT_ATTRIBUTE_PLMENTITY_V_NAME));
					logger.writeLog("id------->> "+strBOMComponentId+" _____ originated-----> "+ strbomOriginated);
					DomainRelationship domRelBOMComponents = DomainRelationship.newInstance(context, strConnectionId);
					String strProcessStatusFlag = domRelBOMComponents.getAttributeValue(context,lcdSAPInteg3DExpConstants.ATTRIBUTE_LCD_PROCESS_STATUS_FLAG);
					String strCAID = domRelBOMComponents.getAttributeValue(context, lcdSAPInteg3DExpConstants.ATTRIBUTE_LCD_CAID);
                    String strRequestString ="";
					if (UIUtil.isNotNullAndNotEmpty(strBOMComponentId)) {
						if (LCDSAPIntegrationDataConstants.VALUE_PROCESS_STATUS_FLAG_Waiting.equalsIgnoreCase(strProcessStatusFlag)) 
						{
							if (lcdSAPInteg3DExpConstants.TYPE_MANUFACTURING_ASSEMBLY.equalsIgnoreCase(strBOMComponentType)) {
								if (UIUtil.isNotNullAndNotEmpty(strCAID)) {
									joBomPayload = getManAssemblyJSON(context, strBOMComponentId,strBOMComponentType, strCAID, strConnectionId, lcdSAPInteg3DExpConstants);
									if (UIUtil.isNotNullAndNotEmpty(joBomPayload.toString())) {
										  strRequestString = joBomPayload.toString();
										 jsonLogger.writeLog("JSON Payload Request for : <<< " + strBOMComponentTitle+ " : >>> \n " + strRequestString + " \n ");
										 logger.writeLog("JSON Payload Request for  : <<< " + strBOMComponentTitle + " : >>> \n "+ strRequestString + " \n ");
										intResponseCode = callPostService(context, joBomPayload, lcdSAPInteg3DExpConstants);
									} else {
										logger.writeLog(
												"ERROR :: Failed to generate JSON Payload for " + strBOMComponentName);
									}
									processWebServiceResponseForManufacturingAssembly(context, intResponseCode,
											strConnectionId, lcdSAPInteg3DExpConstants);
								}
							} else if (lcdSAPInteg3DExpConstants.TYPE_VPM_REFERENCE.equalsIgnoreCase(strBOMComponentType)){
								Map<?, ?> mCadPartDetails = getCADPartDetails(context, strBOMComponentId, lcdSAPInteg3DExpConstants);
								String strProcurementIntent = (String) mCadPartDetails.get(lcdSAPInteg3DExpConstants.SELECT_ATTRIBUTE_PROCUREMENTINTENT_VPMREFERENCE);
								if (LCDSAPIntegrationDataConstants.SUBCONTRACT.equalsIgnoreCase(strProcurementIntent)) {
									String strReleasedDate = (String) mCadPartDetails.get("attribute[XP_VPMReference_Ext.AtievaActualReleasedDate]");
									logger.writeLog("Part released 1st date  " + strReleasedDate);
									if (UIUtil.isNotNullAndNotEmpty(strReleasedDate)) {
										
										strReleasedDate = strReleasedDate.substring(0, strReleasedDate.length() - 11);
										logger.writeLog("Part released 2nd date  " + strReleasedDate);
										if (UIUtil.isNotNullAndNotEmpty(strReleasedDate)) {
											
											SimpleDateFormat format1 = new SimpleDateFormat("MM/dd/yyyy");
											SimpleDateFormat format2 = new SimpleDateFormat("MM-dd-yyyy");
											Date date = format1.parse(strReleasedDate);
											String formatDate = format2.format(date);
											logger.writeLog("Part released format date  " + formatDate);
											setPartReleasedDate(formatDate);
										}
									}
									

									joBomPayload = getPhysicalProductJSON(context, mCadPartDetails, strConnectionId, lcdSAPInteg3DExpConstants);
									 strRequestString = joBomPayload.toString();
									 jsonLogger.writeLog("JSON Payload Request for : <<< " + strBOMComponentTitle+ " : >>> \n " + strRequestString + " \n ");
									 logger.writeLog("JSON Payload Request for  : <<< " + strBOMComponentTitle + " : >>> \n "+ strRequestString + " \n ");
									intResponseCode = callPostService(context, joBomPayload, lcdSAPInteg3DExpConstants);
									processWebServiceResponseForSubContract(context, intResponseCode, strConnectionId, lcdSAPInteg3DExpConstants);
								}
							}
						}
						} else if (LCDSAPIntegrationDataConstants.VALUE_PROCESS_STATUS_FLAG_COMPLETE.equalsIgnoreCase(strProcessStatusFlag)) {
							disconnectExpiredObjectsFromAnchorObject(context, strConnectionId, strbomModified);
						}
					}

				}
			//} else {
			//	logger.writeLog("ERROR : Failed to get x-csrf-token and cookies from SAP Webservice ");
			//}
          catch (Exception e) {
			e.printStackTrace();
		} finally {
			httpClient.close();
		}
	}

	/**
	 * This Method is to get Relevant BOM Components which are connected with Anchor
	 * Object and Send it to SAP.
	 * 
	 * @param context
	 * @return
	 * @throws Exception
	 */
	private MapList findRelevantObjectsConnectedToAnchorObject(Context context, LCDSAPIntegration3DExpConstants lcdSAPInteg3DExpConstants) throws Exception {

			BusinessObject busObjAchor = new BusinessObject(lcdSAPInteg3DExpConstants.TYPE_LCD_BOM_ANCHOR_OBJECT, // String Type
					lcdSAPInteg3DExpConstants.NAME_LCD_BOM_ANCHOR_OBJECT, // String Name
					lcdSAPInteg3DExpConstants.REVISION_LCD_BOM_ANCHOR_OBJECT, // String Revision
					lcdSAPInteg3DExpConstants.VAULT_ESERVICE_PRODUCTION); // String Vault

			DomainObject domObj = DomainObject.newInstance(context, busObjAchor);

			StringList slObjectSelect = new StringList();
			slObjectSelect.add(DomainConstants.SELECT_ID);
			slObjectSelect.add(DomainConstants.SELECT_TYPE);
			slObjectSelect.add(DomainConstants.SELECT_NAME);
			slObjectSelect.add(lcdSAPInteg3DExpConstants.SELECT_ATTRIBUTE_PLMENTITY_V_NAME);

			StringList slRelSelect = new StringList();
			slRelSelect.add(DomainRelationship.SELECT_ID);
			slRelSelect.add("modified");
			slRelSelect.add("originated");

			return (domObj.getRelatedObjects(context, // context
					lcdSAPInteg3DExpConstants.RELATIONSHIP_LCD_SAP_BOM_INTERFACE, // Relationship Pattern
					"*", // Type Pattern
					slObjectSelect, // Object Select
					slRelSelect, // Relationship Select
					false, // To Side
					true, // from Side
					(short) 1, // Recursion Level
					"", // Object Where clause
					"", // Relationship Where clause
					0)); // limit;
	}

	/**
	 * This Method is to disconnect Completed BOM Components from Anchor Objects
	 * after 7 days Object and Send it to SAP.
	 * 
	 * @param context
	 * @return
	 * @throws Exception
	 */

	public void disconnectExpiredObjectsFromAnchorObject(Context context, String strConnectionId, String strbomModified)
			throws Exception {

		SimpleDateFormat sdf = new SimpleDateFormat(eMatrixDateFormat.getEMatrixDateFormat(), context.getLocale());
		Date da = sdf.parse(strbomModified);
		Date currentDate = new Date();
		long differenceInTime = currentDate.getTime() - da.getTime();
		long differenceInDays = differenceInTime / LCDSAPIntegrationDataConstants.TIME_CONVERSION;

		if (differenceInDays > 7) {
			DomainRelationship.disconnect(context, strConnectionId);
		}
	}

	private JsonObject getManAssemblyJSON(Context context, String strBOMComponentId, String strBOMComponentType,
			String caId, String strConnectionId, LCDSAPIntegration3DExpConstants lcdSAPInteg3DExpConstants) throws Exception {

		String strRequestString = null;
		JsonObject jEachPayloadObj = null;

		try {
			logger.writeLog("getManAssemblyJSON calledd <<<<<<<<< ");

			// STEP :Get input Arguments
			ChangeAction changeAction = ChangeActionServices.getChangeAction(context, caId);
			if (null != changeAction) {
				// STEP : Using Service API getting JSON object for Change Action
				ChangeActionFacets changeActionFacets = new ChangeActionFacets();
				changeActionFacets.attributes = true;
				changeActionFacets.realized = true;
				changeActionFacets.applicability = true;
				String changeActionJsonDetails = ChangeActionJsonUtilities.changeActionToJson(context, changeAction,
						changeActionFacets);

				if (UIUtil.isNotNullAndNotEmpty(changeActionJsonDetails)) {
					JsonReader jsonReader = Json.createReader(new StringReader(changeActionJsonDetails));
					JsonObject joChangeAction = jsonReader.readObject();
					jsonReader.close();

					if (null != joChangeAction) {
						// STEP :Set method to Populate the JSON Object of Change Action [output of
						// service API]
						setChangeActionJson(joChangeAction);

						// Step : Retrieving Change Action Name from JSON Object of Change Action
						// [output of service API]
						String changeActionName = getChangeActionJson().getJsonObject("changeaction").getString("name");

						loggerDebug.info("getChangeActionJson()------------->"+getChangeActionJson());
						// STEP : Invoking SAP Web service using GET request method to get the
						// x-csrf-token and cookies
						// STEP : Adding Change Action Header in JSON

						JsonObjectBuilder jEachPayloadBuilder = Json.createObjectBuilder();

//						if (changeActionMap.containsKey(caId)) {
//							jEachPayloadBuilder = changeActionMap.get(caId);
//						} else {
							jEachPayloadBuilder = ChangeActionJSONPayload(context, caId, strConnectionId, lcdSAPInteg3DExpConstants);
//							changeActionMap.put(caId, jEachPayloadBuilder);
//						}

						if (null != jEachPayloadBuilder) {

							// STEP : Traversing Change Action to get the realized items
							JsonObject realizedItem;
							JsonArray realizedItems = getChangeActionJson().getJsonObject("changeaction")
									.getJsonArray("realized");
							String strObjProcIntent;
							String strHasConfig;
							String strObjTitle;
							DomainObject domRealizedItemObj;
							Map<?, ?> mobjectDetails = null;
							JsonObjectBuilder jHeaderPartObjectBuilder;

							if (UIUtil.isNotNullAndNotEmpty(strBOMComponentId)) {
								// STEP : Retrieving realized item's object details
								domRealizedItemObj = DomainObject.newInstance(context, strBOMComponentId);
								StringList objectSelects = new StringList();
								objectSelects.add(DomainConstants.SELECT_ID);
								objectSelects.add(DomainConstants.SELECT_NAME);
								objectSelects.add(DomainConstants.SELECT_TYPE);
								objectSelects.add(lcdSAPInteg3DExpConstants.SELECT_ATTRIBUTE_PLMENTITY_V_NAME);
								objectSelects.add(lcdSAPInteg3DExpConstants.SELECT_ATTRIBUTE_PLMENTITY_V_DESCRIPTION);
								objectSelects.add(lcdSAPInteg3DExpConstants.SELECT_ATTRIBUTE_PROCUREMENT_INTENT_MANUFACTURING_ASSEMBLY);
								objectSelects.add(lcdSAPInteg3DExpConstants.SELECT_ATTRIBUTE_PLANT_CODE_MANUFACTURING_ASSEMBLY);
								objectSelects.add(lcdSAPInteg3DExpConstants.SELECT_ATTRIBUTE_SERVICEABLE_ITEM_MANUFACTURING_ASSEMBLY);
								objectSelects.add(lcdSAPInteg3DExpConstants.SELECT_ATTRIBUTE_PART_INTERCHANGE_ABILITY_MANUFACTURING_ASSEMBLY);
								objectSelects.add(lcdSAPInteg3DExpConstants.SELECT_ATTRIBUTE_HASCONFIGCONTEXT_VPMREFERENCE);
								objectSelects.add(lcdSAPInteg3DExpConstants.SELECT_ATTRIBUTE_SAP_BOM_UPDATED_ON_MANUFACTURING_ASSEMBLY);
								objectSelects.add(lcdSAPInteg3DExpConstants.SELECT_ATTRIBUTE_SAP_UNIQUE_ID_MANUFACTURING_ASSEMBLY);

								mobjectDetails = domRealizedItemObj.getInfo(context, objectSelects);
								strObjProcIntent = (String) mobjectDetails
										.get(lcdSAPInteg3DExpConstants.SELECT_ATTRIBUTE_PROCUREMENT_INTENT_MANUFACTURING_ASSEMBLY);
								strHasConfig = (String) mobjectDetails.get(lcdSAPInteg3DExpConstants.SELECT_ATTRIBUTE_HASCONFIGCONTEXT_VPMREFERENCE);
								strObjTitle = (String) mobjectDetails.get(lcdSAPInteg3DExpConstants.SELECT_ATTRIBUTE_PLMENTITY_V_NAME);

								// STEP : Check the realized item is Discrete Make Manufacturing Assembly
								if (lcdSAPInteg3DExpConstants.TYPE_MANUFACTURING_ASSEMBLY.equalsIgnoreCase(strBOMComponentType)) {
									if (LCDSAPIntegrationDataConstants.VALUE_FALSE.equalsIgnoreCase(strHasConfig)) {
										logger.writeLog("INFO ::Realized Item << " + strObjTitle
												+ " >> is with Procurement Intent : << " + strObjProcIntent + " >> "+ " >> Has conifg : << " + strHasConfig + " >> ");
										setConfiguredFlag(false);
									} else if (LCDSAPIntegrationDataConstants.TRUE.equalsIgnoreCase(strHasConfig)) {
										logger.writeLog("INFO ::configured Realized Item is << " + strObjTitle
												+ " >> is with Procurement Intent : << " + strObjProcIntent + " >> ");
										setConfiguredFlag(true);
										realizedItem = realizedItemJson(context, realizedItems, strBOMComponentId);

										setCAStartDate();
										if (null != realizedItem) {
											logger.writeLog("test ::configured Realized Item JSON  << "
													+ realizedItem.toString() + " >> ");
											setRealizedItemList(context, realizedItem);
										}
									}

									// STEP : Adding Manufacturing Assembly Header in JSON
									logger.writeLog("Processing Realized Item << " + strObjTitle
											+ " >> Procurement Intent : << " + strObjProcIntent + " >> ");
									jHeaderPartObjectBuilder = addRealizedItemHeader(context, mobjectDetails, lcdSAPInteg3DExpConstants);
									if (null != jHeaderPartObjectBuilder) {
										jEachPayloadBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_HEADER_PART, jHeaderPartObjectBuilder.build());

										// STEP : Invoking SAP WebService by attaching JSON
										jEachPayloadObj = jEachPayloadBuilder.build();
										//strRequestString = jEachPayloadObj.toString();
										//jsonLogger.writeLog("JSON Payload Request for : <<< " + strObjTitle+ " : >>> \n " + strRequestString + " \n ");
										//logger.writeLog("JSON Payload Request for  : <<< " + strObjTitle + " : >>> \n "+ strRequestString + " \n ");
									} else
										logger.writeLog("ERROR :: Failed to add realized item  : << " + strObjTitle
												+ ">> header for Change Action : << " + changeActionName);
								} else
									logger.writeLog("INFO ::Realized Item << " + strObjTitle
											+ " >> is with Procurement Intent : << " + strObjProcIntent + " >> ");
							} else
								logger.writeLog("ERROR : Failed to get realized Item Id from Change Action : << "
										+ changeActionName + " >> ");
						} else
							logger.writeLog(
									"ERROR : Failed to add change action header in JSON Object from Change Action : << "
											+ changeActionName + " >> ");
					} else
						logger.writeLog("ERROR : Failed to get change action JSON object using Service API");
				} else
					logger.writeLog("ERROR : Failed to get change action JSON using Service API");
			} else
				logger.writeLog("ERROR : Failed to get input arguments");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return jEachPayloadObj;
	}

	private JsonObject realizedItemJson(Context context, JsonArray realizedItems, String strBOMComponentId)
			throws FrameworkException {
		JsonObject joRealizedItem = null;
		for (int i = 0; i < realizedItems.size(); i++) {
			JsonObject joItem = realizedItems.getJsonObject(i);
			String realizedItemPhyId = joItem.getJsonObject("where").get("id").toString().split(":")[1].replace("\"",
					"");

			DomainObject domRealizedItemObj = DomainObject.newInstance(context, realizedItemPhyId);
			StringList objectSelects = new StringList();
			objectSelects.add(DomainConstants.SELECT_ID);

			Map mobjectDetails = domRealizedItemObj.getInfo(context, objectSelects);
			String realizedItemBusID = (String) mobjectDetails.get(DomainConstants.SELECT_ID);
			if (realizedItemBusID.equals(strBOMComponentId)) {
				joRealizedItem = joItem;
			}
		}
		return joRealizedItem;
	}

	/**
	 * This Method is to add the Change Action object details in JSON Header
	 *
	 * @param context
	 * @param JsonObject : Input Change Action JSON
	 * @return JsonObjectBuilder : After adding Change Action information returning
	 *         JsonObjectBuilder
	 * @throws Exception
	 */
	private JsonObjectBuilder ChangeActionJSONPayload(Context context, String caId, String strConnectionId, LCDSAPIntegration3DExpConstants lcdSAPInteg3DExpConstants) throws Exception {
		logger.writeLog("addChangeActionHeader...Start");

		// STEP : Creating JSON object Builder to add the change action object details
		JsonObjectBuilder jEachPayloadBuilder = Json.createObjectBuilder();
		try {

			if (UIUtil.isNullOrEmpty(caId)) {
				// STEP : Adding Change Action object details in JSONObjectBuilder

				jEachPayloadBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_TITLE, LCDSAPIntegrationDataConstants.VALUE_NOT_APPLICABLE);

				jEachPayloadBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_OID, strConnectionId);

				jEachPayloadBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_NAME, LCDSAPIntegrationDataConstants.VALUE_NOT_APPLICABLE);

				jEachPayloadBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_DESCRIPTION, LCDSAPIntegrationDataConstants.VALUE_NOT_APPLICABLE);

				jEachPayloadBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_CATEGORY_OF_CHANGE, LCDSAPIntegrationDataConstants.VALUE_NOT_APPLICABLE);

				jEachPayloadBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_CHANGEDOMAIN, LCDSAPIntegrationDataConstants.VALUE_NOT_APPLICABLE);

				jEachPayloadBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_CHANGETYPE, LCDSAPIntegrationDataConstants.VALUE_NOT_APPLICABLE);

				jEachPayloadBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_REASONFORCHANGE, LCDSAPIntegrationDataConstants.VALUE_NOT_APPLICABLE);

				jEachPayloadBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_PLATFORM, LCDSAPIntegrationDataConstants.VALUE_NOT_APPLICABLE);
				
				//jEachPayloadBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_CA_APPLICABILITY_START_DATE, LCDSAPIntegrationDataConstants.VALUE_NOT_APPLICABLE);
				
				//jEachPayloadBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_CA_APPLICABILITY_END_DATE, LCDSAPIntegrationDataConstants.VALUE_NOT_APPLICABLE);
			} else {

				// STEP : Retrieving Change Action Details from Input Change Action JSON
				String changeActionName = null;
				String changeObjPhyId = null;
				String changeActionDescription = null;
				JsonObject changeActionObject = getChangeActionJson().getJsonObject("changeaction");
				Map<String, String> mcaDetails = new HashMap<>();
				if (null != changeActionObject) {
					changeActionName = changeActionObject.getString("name");
					changeObjPhyId = changeActionObject.getString("id").toString().split(":")[1].replaceAll("\"", "");
					changeActionDescription = changeActionObject.getString("description");
					logger.writeLog("Change Action Name  << " + changeActionName + " >> Change Action Description : << "
							+ changeActionDescription + " >>");
				}

				// STEP : Retrieving Change Action Details from Input Change Action JSON
				JsonArray caAttributes = changeActionObject.getJsonArray("attributes");
				logger.writeLog("Change Action attribute json << "+ caAttributes.toString() + " >>");
				String attrName;
				String attrValue;
				JsonObject caAttribute;
				if (caAttributes != null) {
					for (int i = 0; i < caAttributes.size(); i++) {
						caAttribute = caAttributes.getJsonObject(i);
						attrName = caAttribute.getString("name");
						if (lcdSAPInteg3DExpConstants.ATTRIBUTE_CHANGE_ACTION_TITLE.equalsIgnoreCase(attrName)
								|| lcdSAPInteg3DExpConstants.ATTRIBUTE_CHANGE_ACTION_CHANGETYPE.equalsIgnoreCase(attrName)
								|| lcdSAPInteg3DExpConstants.ATTRIBUTE_CHANGE_ACTION_CHANGEDOMAIN.equalsIgnoreCase(attrName)
								|| lcdSAPInteg3DExpConstants.ATTRIBUTE_CHANGE_ACTION_PLATFORM.equalsIgnoreCase(attrName)
								|| lcdSAPInteg3DExpConstants.ATTRIBUTE_CHANGE_ACTION_REASONFORCHANGE.equalsIgnoreCase(attrName)
								|| lcdSAPInteg3DExpConstants.ATTRIBUTE_CHANGE_ACTION_CATEGORY_OF_CHANGE.equalsIgnoreCase(attrName)
								|| lcdSAPInteg3DExpConstants.ATTRIBUTE_CHANGE_ACTION_RELEASED_DATE.equalsIgnoreCase(attrName)
								|| lcdSAPInteg3DExpConstants.ATTRIBUTE_PROPOSED_APPLICABILITY_START_DATE.equalsIgnoreCase(attrName)
								|| lcdSAPInteg3DExpConstants.ATTRIBUTE_PROPOSED_APPLICABILITY_END_DATE.equalsIgnoreCase(attrName)) {
							attrValue = caAttribute.getString("value");
							if (UIUtil.isNotNullAndNotEmpty(attrValue))
								mcaDetails.put(attrName, attrValue);
							else
								mcaDetails.put(attrName, " ");
						}
					}
				}

				String strCACompletionDate = mcaDetails.get(lcdSAPInteg3DExpConstants.ATTRIBUTE_CHANGE_ACTION_RELEASED_DATE);
				if (UIUtil.isNotNullAndNotEmpty(strCACompletionDate)) {
					strCACompletionDate = strCACompletionDate.substring(0, strCACompletionDate.length() - 11);
					if (UIUtil.isNotNullAndNotEmpty(strCACompletionDate)) {
						SimpleDateFormat format1 = new SimpleDateFormat("MM/dd/yyyy");
						SimpleDateFormat format2 = new SimpleDateFormat("MM-dd-yyyy");
						Date date = format1.parse(strCACompletionDate);
						String formatDate = format2.format(date);
						setCACompletionDate(formatDate);
					}
				}
				
			
				// STEP : Retrieving Change Action object ID from Change Action Physical ID
				String changeActionObjId = null;
				if (UIUtil.isNotNullAndNotEmpty(changeObjPhyId)) {
					DomainObject domCAObj = DomainObject.newInstance(context, changeObjPhyId);
					changeActionObjId = domCAObj.getInfo(context, DomainConstants.SELECT_ID);
				}

				// STEP : Adding Change Action object details in JSONObjectBuilder
				if (UIUtil.isNotNullAndNotEmpty(mcaDetails.get(lcdSAPInteg3DExpConstants.ATTRIBUTE_CHANGE_ACTION_TITLE)))
					jEachPayloadBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_TITLE, mcaDetails.get(lcdSAPInteg3DExpConstants.ATTRIBUTE_CHANGE_ACTION_TITLE));

				if (UIUtil.isNotNullAndNotEmpty(changeActionObjId))
					jEachPayloadBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_OID, strConnectionId);
				else
					jEachPayloadBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_OID, " ");

				if (UIUtil.isNotNullAndNotEmpty(changeActionName))
					jEachPayloadBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_NAME, changeActionName);
				else
					jEachPayloadBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_NAME, " ");

				if (UIUtil.isNotNullAndNotEmpty(changeActionDescription))
					jEachPayloadBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_DESCRIPTION, changeActionDescription);
				else
					jEachPayloadBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_DESCRIPTION, " ");

				if (UIUtil.isNotNullAndNotEmpty(mcaDetails.get(lcdSAPInteg3DExpConstants.ATTRIBUTE_CHANGE_ACTION_CATEGORY_OF_CHANGE)))
					jEachPayloadBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_CATEGORY_OF_CHANGE, mcaDetails.get(lcdSAPInteg3DExpConstants.ATTRIBUTE_CHANGE_ACTION_CATEGORY_OF_CHANGE));

				if (UIUtil.isNotNullAndNotEmpty(mcaDetails.get(lcdSAPInteg3DExpConstants.ATTRIBUTE_CHANGE_ACTION_CHANGEDOMAIN)))
					jEachPayloadBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_CHANGEDOMAIN, mcaDetails.get(lcdSAPInteg3DExpConstants.ATTRIBUTE_CHANGE_ACTION_CHANGEDOMAIN));

				if (UIUtil.isNotNullAndNotEmpty(mcaDetails.get(lcdSAPInteg3DExpConstants.ATTRIBUTE_CHANGE_ACTION_CHANGETYPE)))
					jEachPayloadBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_CHANGETYPE, mcaDetails.get(lcdSAPInteg3DExpConstants.ATTRIBUTE_CHANGE_ACTION_CHANGETYPE));

				if (UIUtil.isNotNullAndNotEmpty(mcaDetails.get(lcdSAPInteg3DExpConstants.ATTRIBUTE_CHANGE_ACTION_REASONFORCHANGE)))
					jEachPayloadBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_REASONFORCHANGE, mcaDetails.get(lcdSAPInteg3DExpConstants.ATTRIBUTE_CHANGE_ACTION_REASONFORCHANGE));

				if (UIUtil.isNotNullAndNotEmpty(mcaDetails.get(lcdSAPInteg3DExpConstants.ATTRIBUTE_CHANGE_ACTION_PLATFORM)))
					jEachPayloadBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_PLATFORM, mcaDetails.get(lcdSAPInteg3DExpConstants.ATTRIBUTE_CHANGE_ACTION_PLATFORM));
				
				/*if (UIUtil.isNotNullAndNotEmpty(mcaDetails.get(lcdSAPInteg3DExpConstants.ATTRIBUTE_PROPOSED_APPLICABILITY_START_DATE)))
					jEachPayloadBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_CA_APPLICABILITY_START_DATE, mcaDetails.get(lcdSAPInteg3DExpConstants.ATTRIBUTE_PROPOSED_APPLICABILITY_START_DATE));
				else
					jEachPayloadBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_CA_APPLICABILITY_START_DATE, "");
				
				if (UIUtil.isNotNullAndNotEmpty(mcaDetails.get(lcdSAPInteg3DExpConstants.ATTRIBUTE_PROPOSED_APPLICABILITY_END_DATE)))
					jEachPayloadBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_CA_APPLICABILITY_END_DATE, mcaDetails.get(lcdSAPInteg3DExpConstants.ATTRIBUTE_PROPOSED_APPLICABILITY_END_DATE));
				else
					jEachPayloadBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_CA_APPLICABILITY_END_DATE, "");*/

				logger.writeLog("Change Action Header :: << " + jEachPayloadBuilder.build().toString() + ">>");

			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		logger.writeLog("addChangeActionHeader...END");
		return jEachPayloadBuilder;
	}

	/**
	 * This Method is to add the realized item details in JSON Header
	 *
	 * @param context
	 * @param Map     :Object Details of realized item
	 * @return JsonObjectBuilder : After adding realized item information returning
	 *         JsonObjectBuilder
	 * @throws Exception
	 */
	private JsonObjectBuilder addRealizedItemHeader(Context context, Map mobjectDetails, LCDSAPIntegration3DExpConstants lcdSAPInteg3DExpConstants) throws Exception {
		logger.writeLog("addRealizedItemHeader...START");

		// STEP : Creating JSON object Builder to add the realized item details
		JsonObjectBuilder jHeaderPartObjectBuilder = Json.createObjectBuilder();
		try {
			// STEP : Retrieving realized item Details
			String strObjProcurementIntent = (String) mobjectDetails.get(lcdSAPInteg3DExpConstants.SELECT_ATTRIBUTE_PROCUREMENT_INTENT_MANUFACTURING_ASSEMBLY);
			String strObjTitle = (String) mobjectDetails.get(lcdSAPInteg3DExpConstants.SELECT_ATTRIBUTE_PLMENTITY_V_NAME);
			String strObjId = (String) mobjectDetails.get(DomainConstants.SELECT_ID);
			String strObjdescription = (String) mobjectDetails.get(lcdSAPInteg3DExpConstants.SELECT_ATTRIBUTE_PLMENTITY_V_DESCRIPTION);
			String strName = (String) mobjectDetails.get(DomainConstants.SELECT_NAME);
			String strObjPlantCode = (String) mobjectDetails.get(lcdSAPInteg3DExpConstants.SELECT_ATTRIBUTE_PLANT_CODE_MANUFACTURING_ASSEMBLY);
			String strObjServiceable = (String) mobjectDetails.get(lcdSAPInteg3DExpConstants.SELECT_ATTRIBUTE_SERVICEABLE_ITEM_MANUFACTURING_ASSEMBLY);
			String strObjPartInterchangeability = (String) mobjectDetails
					.get(lcdSAPInteg3DExpConstants.SELECT_ATTRIBUTE_PART_INTERCHANGE_ABILITY_MANUFACTURING_ASSEMBLY);

			// STEP :Adding realized item object details in JSONObjectBuilder
			if (UIUtil.isNotNullAndNotEmpty(strObjTitle))
				jHeaderPartObjectBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_TITLE, strObjTitle);
			else
				jHeaderPartObjectBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_TITLE, " ");

			if (UIUtil.isNotNullAndNotEmpty(strName))
				jHeaderPartObjectBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_NAME, strName);
			else
				jHeaderPartObjectBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_NAME, " ");

			if (UIUtil.isNotNullAndNotEmpty(strObjId))
				jHeaderPartObjectBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_OID, strObjId);
			else
				jHeaderPartObjectBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_OID, " ");

			jHeaderPartObjectBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_REL_ID, LCDSAPIntegrationDataConstants.VALUE_NOT_APPLICABLE);

			if (UIUtil.isNotNullAndNotEmpty(strObjdescription))
				jHeaderPartObjectBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_DESCRIPTION, strObjdescription);
			else
				jHeaderPartObjectBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_DESCRIPTION, " ");

			if (UIUtil.isNotNullAndNotEmpty(strObjProcurementIntent))
				jHeaderPartObjectBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_PROCUREMENTINTENT, strObjProcurementIntent);
			else
				jHeaderPartObjectBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_PROCUREMENTINTENT, " ");

			if (UIUtil.isNotNullAndNotEmpty(strObjServiceable))
				jHeaderPartObjectBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_SERVICEABLEITEM, strObjServiceable);
			else
				jHeaderPartObjectBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_SERVICEABLEITEM, " ");

			if (UIUtil.isNotNullAndNotEmpty(strObjPlantCode))
				jHeaderPartObjectBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_PLANTCODE, strObjPlantCode);
			else
				jHeaderPartObjectBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_PLANTCODE, " ");

			if (UIUtil.isNotNullAndNotEmpty(strObjPartInterchangeability))
				jHeaderPartObjectBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_PARTINTERCHANGEABILITY, strObjPartInterchangeability);
			else
				jHeaderPartObjectBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_PARTINTERCHANGEABILITY, " ");

			jHeaderPartObjectBuilder.add("Effectivity_Option_Code", LCDSAPIntegrationDataConstants.VALUE_NOT_APPLICABLE);
			jHeaderPartObjectBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_DATEFROM, LCDSAPIntegrationDataConstants.VALUE_NOT_APPLICABLE);
			jHeaderPartObjectBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_DATETO, LCDSAPIntegrationDataConstants.VALUE_NOT_APPLICABLE);
			jHeaderPartObjectBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_REALIZED_DATA, true);

			// STEP : Adding child of Manufacturing Assembly Header in JSON
			if (UIUtil.isNotNullAndNotEmpty(strObjId))
				addRealizedItemChildren(context, strObjId, jHeaderPartObjectBuilder, lcdSAPInteg3DExpConstants);

			// STEP :sending back realized item object details in JSONObjectBuilder
			logger.writeLog("Manufacturing JSON Header :: << " + jHeaderPartObjectBuilder.build().toString() + ">>");
		} catch (Exception e) {
			e.printStackTrace();
		}
		logger.writeLog("addRealizedItemHeader...END");
		return jHeaderPartObjectBuilder;
	}

	/**
	 * This Method is to get the realized item details
	 *
	 * @param context
	 * @param String  : Object Id of Realized Item
	 * @return JsonObjectBuilder : After adding realized item information returning
	 *         JsonObjectBuilder
	 * @throws Exception
	 */
	private void addRealizedItemChildren(Context context, String strObjId, JsonObjectBuilder jHeaderPartObjectBuilder, LCDSAPIntegration3DExpConstants lcdSAPInteg3DExpConstants)
			throws Exception {
		logger.writeLog("getRealizedItemDetails : START !!");
		try {
			// STEP : Retrieving realized item child details
			MapList mlMBOMDetails = getMBOMDetails(context, strObjId, lcdSAPInteg3DExpConstants);
			logger.writeLog("Number of Provided Part under  Make Manufacturing Assembly  :: " + mlMBOMDetails.size());
			JsonArrayBuilder jsonMBOMPartArrayBuilder = Json.createArrayBuilder();

			// STEP : Processing child of realized item
			Map mPartDetails;
			String sType;
			String strTitle;
			boolean isAssembly;
			for (int jdx = 0; jdx < mlMBOMDetails.size(); jdx++) {
				setFalseEffectivityFlag(false);
				mPartDetails = (Map<?, ?>) mlMBOMDetails.get(jdx);
				sType = (String) mPartDetails.get(DomainConstants.SELECT_TYPE);
				strTitle = (String) mPartDetails.get(lcdSAPInteg3DExpConstants.SELECT_ATTRIBUTE_PLMENTITY_V_NAME);

				JsonObjectBuilder jMBOMPartBuilder = Json.createObjectBuilder();
				if (sType.equalsIgnoreCase(lcdSAPInteg3DExpConstants.TYPE_MANUFACTURING_ASSEMBLY)) {
					// STEP : Adding Manufacturing Assembly child of Manufacturing Assembly Header
					// in JSON
					isAssembly = true;
					addChildPartHeader(context, mPartDetails, jMBOMPartBuilder, isAssembly, lcdSAPInteg3DExpConstants);
				} else {
					// STEP : Adding child part of Manufacturing Assembly Header in JSON
					isAssembly = false;
					addChildPartHeader(context, mPartDetails, jMBOMPartBuilder, isAssembly, lcdSAPInteg3DExpConstants);
				}
				// STEP : adding child part JSON Object in JSON Array
				if (null != jMBOMPartBuilder) {
					logger.writeLog("JSON Header for child :: << " + strTitle + ">> <<"
							+ jMBOMPartBuilder.build().toString() + ">>>>>>>>>");
					jsonMBOMPartArrayBuilder.add(jMBOMPartBuilder.build());
				}
			}
			// STEP : adding child parts JSON Array in Header Part JSON object
			if (null != jsonMBOMPartArrayBuilder)
				jHeaderPartObjectBuilder.add("children", jsonMBOMPartArrayBuilder.build());
			else
				logger.writeLog("ERROR :: Unable to add children header under Manufacturing Assembly ");

		} catch (Exception e) {
			e.printStackTrace();
		}
		logger.writeLog("getRealizedItemDetails : END !!");
	}

	/**
	 * This Method Accepts MBOM ObjectID to expand it
	 *
	 * @param context
	 * @param strObjectId : MBOM ObjectID to expand it
	 * @return Details of childs of input Object
	 * @throws Exception
	 */
	private MapList getMBOMDetails(Context context, String strObjectId, LCDSAPIntegration3DExpConstants lcdSAPInteg3DExpConstants) throws Exception {
		logger.writeLog("getMBOMDetails : START!!");
		MapList mlMBOMList = null;
		try {
			// STEP : Defining BusinessObject Pattern and Relationship Pattern
			Pattern relPattern = new Pattern(lcdSAPInteg3DExpConstants.RELATIONSHIP_DELFMI_FUNCTION_IDENTIFIED_INSTANCE);
			relPattern.addPattern(lcdSAPInteg3DExpConstants.RELATIONSHIP_PROCESS_INSTANCE_CONTINUOUS);
			Pattern typePattern = new Pattern(lcdSAPInteg3DExpConstants.TYPE_PROVIDE);
			typePattern.addPattern(lcdSAPInteg3DExpConstants.TYPE_MANUFACTURING_ASSEMBLY);
			typePattern.addPattern(lcdSAPInteg3DExpConstants.TYPE_FASTEN);
			typePattern.addPattern(lcdSAPInteg3DExpConstants.TYPE_PROCESS_INSTANCE_CONTINUOUS);

			// STEP : Defining BusinessObject Selectables
			StringList objectSelects = new StringList();
			objectSelects.add(lcdSAPInteg3DExpConstants.SELECT_ATTRIBUTE_PLMENTITY_V_NAME);
			objectSelects.add(DomainConstants.SELECT_ID);
			objectSelects.add(lcdSAPInteg3DExpConstants.SELECT_ATTRIBUTE_PLMENTITY_V_DESCRIPTION);
			objectSelects.add(DomainConstants.SELECT_NAME);
			objectSelects.add(lcdSAPInteg3DExpConstants.SELECT_ATTRIBUTE_PROCUREMENT_INTENT_MANUFACTURING_ASSEMBLY);
			objectSelects.add(lcdSAPInteg3DExpConstants.SELECT_ATTRIBUTE_PLANT_CODE_MANUFACTURING_ASSEMBLY);
			objectSelects.add(lcdSAPInteg3DExpConstants.SELECT_ATTRIBUTE_SERVICEABLE_ITEM_MANUFACTURING_ASSEMBLY);
			objectSelects.add(lcdSAPInteg3DExpConstants.SELECT_ATTRIBUTE_PART_INTERCHANGE_ABILITY_MANUFACTURING_ASSEMBLY);
			objectSelects.add(lcdSAPInteg3DExpConstants.SELECT_ATTRIBUTE_HASCONFIGCONTEXT_VPMREFERENCE);

			// STEP : Defining Relationship Selectables
			StringList relSelects = new StringList();
			relSelects.add(DomainConstants.SELECT_RELATIONSHIP_ID);
			relSelects.add(DomainConstants.SELECT_PHYSICAL_ID);
			relSelects.add(lcdSAPInteg3DExpConstants.SELECT_ATTRIBUTE_HAS_CONFIG_EFFECTIVITY);
			DomainObject partDomObj = DomainObject.newInstance(context, strObjectId);

			// STEP : Expanding the input object to get all its child
			mlMBOMList = partDomObj.getRelatedObjects(context, relPattern.getPattern(), // relationship pattern
					typePattern.getPattern(), // object pattern
					objectSelects, // object selects
					relSelects, // relationship selects
					false, // to direction
					true, // from direction //
					(short) 1, // recursion level
					DomainConstants.EMPTY_STRING, // object where clause
					DomainConstants.EMPTY_STRING, 0);

			mlMBOMList.sort(lcdSAPInteg3DExpConstants.SELECT_ATTRIBUTE_TREEORDER_PLMINSTANCE, "ascending", "real");
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		logger.writeLog("getMBOMDetails : END!!");
		return mlMBOMList;
	}

	/**
	 * This Method is to add child part information in Header Part
	 *
	 * @param context
	 * @param MAP     : child part information
	 * @return JsonObjectBuilder : After adding child part information returning
	 *         JsonObjectBuilder
	 * @throws Exception
	 */
	private void addChildPartHeader(Context context, Map<?, ?> mPartDetails, JsonObjectBuilder jMBOMPartBuilder,
			boolean isAssembly, LCDSAPIntegration3DExpConstants lcdSAPInteg3DExpConstants) throws Exception {
		logger.writeLog("addChildPartHeader : START !!");
		Map<?, ?> mLinkedCadPart = null;
		try {
			// STEP : Retrieving Child Part Details
			String strProcurementIntent = null;
			String strPartInterchangeability = null;
			String strServiceable = null;
			String strUnitOfMeasure = null;
			String strObjectID = (String) mPartDetails.get(DomainConstants.SELECT_ID);
			String sName = (String) mPartDetails.get(DomainConstants.SELECT_NAME);
			String strTitle = (String) mPartDetails.get(lcdSAPInteg3DExpConstants.SELECT_ATTRIBUTE_PLMENTITY_V_NAME);
			String strdescription = (String) mPartDetails.get(lcdSAPInteg3DExpConstants.SELECT_ATTRIBUTE_PLMENTITY_V_DESCRIPTION);
			String strRelId = (String) mPartDetails.get(DomainConstants.SELECT_RELATIONSHIP_ID);

			String strRelPhyId = (String) mPartDetails.get(DomainConstants.SELECT_PHYSICAL_ID);
			String strhasConfigEffectivity = (String) mPartDetails.get(lcdSAPInteg3DExpConstants.SELECT_ATTRIBUTE_HAS_CONFIG_EFFECTIVITY);

			// STEP : If Child is Manufacturing Assembly ,Retrieving Procurement Intent
			// ,serviceable ,PartInterchangeability from Object details
			if (isAssembly) {
				strProcurementIntent = (String) mPartDetails.get(lcdSAPInteg3DExpConstants.SELECT_ATTRIBUTE_PROCUREMENT_INTENT_MANUFACTURING_ASSEMBLY);
				strServiceable = (String) mPartDetails.get(lcdSAPInteg3DExpConstants.SELECT_ATTRIBUTE_SERVICEABLE_ITEM_MANUFACTURING_ASSEMBLY);
				strPartInterchangeability = (String) mPartDetails
						.get(lcdSAPInteg3DExpConstants.SELECT_ATTRIBUTE_PART_INTERCHANGE_ABILITY_MANUFACTURING_ASSEMBLY);
				strUnitOfMeasure = LCDSAPIntegrationDataConstants.EACH;
			} else {
				if (null != strObjectID && !strObjectID.isEmpty()) {
					// STEP : If Child is other than Manufacturing Assembly ,Retrieving Procurement
					// Intent , serviceable ,PartInterchangeability from corresponding Linked CAD
					// PART
					// STEP : Retrieving Child Part Details from corresponding Linked CAD PART
					mLinkedCadPart = getLinkedCADPartFromMBOMPart(context, strObjectID, lcdSAPInteg3DExpConstants.RELATIONSHIP_DELFMI_FUNCTION_IDENTIFIED_INSTANCE, lcdSAPInteg3DExpConstants);
					if (null != mLinkedCadPart) {
						strProcurementIntent = (String) mLinkedCadPart.get(lcdSAPInteg3DExpConstants.SELECT_ATTRIBUTE_PROCUREMENTINTENT_VPMREFERENCE);
						strServiceable = (String) mLinkedCadPart.get(lcdSAPInteg3DExpConstants.SELECT_ATTRIBUTE_SERVICEABLEITEM_VPMREFERENCE);
						strPartInterchangeability = (String) mLinkedCadPart
								.get(lcdSAPInteg3DExpConstants.SELECT_ATTRIBUTE_PARTINTERCHANGEABILITY_VPMREFERENCE);
						strUnitOfMeasure = (String) mLinkedCadPart.get("attribute[XP_VPMReference_Ext.AtievaUnitofMeasure]");
					}
				}
			}

			String strVariantEffectivity = "";
			String strEffectivityObject = "";
			String strDateEffectivity = "";
			String strCADReleaseDateIn = "";
			String strCADReleaseDateOut = "";

			if (LCDSAPIntegrationDataConstants.TRUE.equals(strhasConfigEffectivity)) {
				Map<?, ?> mEffectivity = getEffectivity(context, strRelId);
				if (mEffectivity != null) {
					strEffectivityObject = mEffectivity.toString();

					if (strEffectivityObject.contains("Effectivity_Current_Evolution:")) {
						strDateEffectivity = strEffectivityObject.substring(
								strEffectivityObject.indexOf("Effectivity_Current_Evolution:") + 30,
								strEffectivityObject.indexOf("]") + 1);

						// To Identify False evolution Effectivity of instance (Delete case)
						if (strDateEffectivity.contains("false")) {
							setFalseEffectivityFlag(true);
						} else {
							String[] result = strDateEffectivity.split(" - ");
							if (result.length >= 2) {
								strCADReleaseDateIn = result[0];
								strCADReleaseDateIn = strCADReleaseDateIn.substring(13);

								if (strCADReleaseDateIn.length() >= 19)
									strCADReleaseDateIn = strCADReleaseDateIn.substring(0,
											strCADReleaseDateIn.length() - 9);

								strCADReleaseDateOut = result[1];
								strCADReleaseDateOut = strCADReleaseDateOut.substring(0,
										strCADReleaseDateOut.length() - 1);

								if (strCADReleaseDateOut.length() >= 19)
									strCADReleaseDateOut = strCADReleaseDateOut.substring(0,
											strCADReleaseDateOut.length() - 9);
							}
						}

						if (strEffectivityObject.contains(lcdSAPInteg3DExpConstants.EFFECTIVITY_VARIANT + ":")) {
							strVariantEffectivity = strEffectivityObject.substring(
									strEffectivityObject.indexOf(lcdSAPInteg3DExpConstants.EFFECTIVITY_VARIANT + ":") + 20,
									strEffectivityObject.indexOf("ExpressionFormat") - 1);
							strVariantEffectivity = sortOptionCode(strVariantEffectivity);
						}
					}
				}
			}

			// STEP : Adding Child Part Details in JsonObjectBuilder
			if (UIUtil.isNotNullAndNotEmpty(strTitle))
				jMBOMPartBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_TITLE, strTitle);
			else
				jMBOMPartBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_TITLE, " ");

			if (UIUtil.isNotNullAndNotEmpty(sName))
				jMBOMPartBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_NAME, sName);
			else
				jMBOMPartBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_NAME, " ");

			if (UIUtil.isNotNullAndNotEmpty(strObjectID))
				jMBOMPartBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_OID, strObjectID);
			else
				jMBOMPartBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_OID, " ");

			if (UIUtil.isNotNullAndNotEmpty(strRelId))
				jMBOMPartBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_REL_ID, strRelId);
			else
				jMBOMPartBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_REL_ID, " ");

			if (UIUtil.isNotNullAndNotEmpty(strdescription))
				jMBOMPartBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_DESCRIPTION, strdescription);
			else
				jMBOMPartBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_DESCRIPTION, " ");

			if (UIUtil.isNotNullAndNotEmpty(strUnitOfMeasure))
				jMBOMPartBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_UNIT_OF_MEASURE, strUnitOfMeasure);
			else
				jMBOMPartBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_UNIT_OF_MEASURE, " ");

			if (UIUtil.isNotNullAndNotEmpty(strProcurementIntent))
				jMBOMPartBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_PROCUREMENTINTENT, strProcurementIntent);
			else
				jMBOMPartBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_PROCUREMENTINTENT, " ");

			if (UIUtil.isNotNullAndNotEmpty(strServiceable))
				jMBOMPartBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_SERVICEABLEITEM, strServiceable);
			else
				jMBOMPartBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_SERVICEABLEITEM, " ");

			if (UIUtil.isNotNullAndNotEmpty(strPartInterchangeability))
				jMBOMPartBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_PARTINTERCHANGEABILITY, strPartInterchangeability);
			else
				jMBOMPartBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_PARTINTERCHANGEABILITY, " ");

			if (UIUtil.isNotNullAndNotEmpty(strVariantEffectivity))
				jMBOMPartBuilder.add("Effectivity_Option_Code", strVariantEffectivity);
			else
				jMBOMPartBuilder.add("Effectivity_Option_Code", " ");

			if (getConfiguredFlag()) {
				// False Effectivity Case :: In JSON { "Start Date" : Start date of CA and "End
				// Date" = Start date of CA }
				if (getFalseEffectivityFlag()) {
					jMBOMPartBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_DATEFROM, getCAStartDate());
					jMBOMPartBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_DATETO, getCAStartDate());
				} else {
					SimpleDateFormat format1 = new SimpleDateFormat("yyyy-MM-dd");
					SimpleDateFormat format2 = new SimpleDateFormat("MM-dd-yyyy");
					if (UIUtil.isNotNullAndNotEmpty(strCADReleaseDateIn)) {
						Date date = format1.parse(strCADReleaseDateIn);
						String formatDate = format2.format(date);
						jMBOMPartBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_DATEFROM, formatDate);
					} else
						jMBOMPartBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_DATEFROM, " ");

					if (UIUtil.isNotNullAndNotEmpty(strCADReleaseDateOut)) {

						if (strCADReleaseDateOut.equals("INF")) {
							strCADReleaseDateOut = "12-31-9999";
							jMBOMPartBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_DATETO, strCADReleaseDateOut);
						} else {
							// Single day active in PLM case ::: In JSON { "Start Date": start date of
							// effectivity and "End Date" = next day date }
							if (strCADReleaseDateOut.equals(strCADReleaseDateIn)) {
								String dt = strCADReleaseDateOut; // Start date
								SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
								Calendar c = Calendar.getInstance();
								c.setTime(sdf.parse(dt));
								c.add(Calendar.DATE, 1); // number of days to add
								dt = sdf.format(c.getTime()); // dt is now the new date
								Date date = format1.parse(dt);
								String formatDate = format2.format(date);
								logger.writeLog("Test strCADReleaseDateOut :" + formatDate);
								jMBOMPartBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_DATETO, formatDate);

							} else {
								Date date = format1.parse(strCADReleaseDateOut);
								String formatDate = format2.format(date);
								jMBOMPartBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_DATETO, formatDate);
							}
						}
					} else
						jMBOMPartBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_DATETO, " ");
				}
				List<String> lstRealizedChanges = new List<String>();
				lstRealizedChanges = getRealizedItemList();

				if (lstRealizedChanges.contains(strRelPhyId)) {
					jMBOMPartBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_REALIZED_DATA, true);
				}
				else
					jMBOMPartBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_REALIZED_DATA, false);

			} else {
				if (UIUtil.isNotNullAndNotEmpty(getCACompletionDate()))
					jMBOMPartBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_DATEFROM, getCACompletionDate());
				else
					jMBOMPartBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_DATEFROM, "");

				jMBOMPartBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_DATETO, "12-31-9999");
				jMBOMPartBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_REALIZED_DATA, true);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		logger.writeLog("addChildPartHeader : END !!");
	}

	/**
	 * This Method Accepts Relationship ID to get its effectivity (Varient
	 * Effectivity and Date Effectivity)
	 *
	 * @param context
	 * @param strRelId :Relationship Id
	 * @return Effectivity Details of Relationship
	 * @throws Exception
	 */
	private Map<?, ?> getEffectivity(Context context, String strRelId) throws Exception {
		logger.writeLog("getEffectivity : START!!");
		Map<?, ?> mEffectivity;
		try {

			// ContextUtil.pushContext(context,ROLE_ADMIN,DomainConstants.EMPTY_STRING,DomainConstants.EMPTY_STRING);
			// ContextUtil.pushContext(context);
			ConfigurationExposedFilterablesFactory configurationExposedFilterablesactory = new ConfigurationExposedFilterablesFactory();
			IConfigurationExposedFilterables iConfigurationExposedFilterables = configurationExposedFilterablesactory
					.getIPublicConfigurationFilterablesServices();
			List<String> objects = new List<String>();
			objects.add(strRelId);
			mEffectivity = iConfigurationExposedFilterables.getEffectivitiesContent(context, objects,
					com.dassault_systemes.plm.config.exposed.constants.Domain.ALL,
					com.dassault_systemes.plm.config.exposed.constants.Format.TXT,
					com.dassault_systemes.plm.config.exposed.constants.View.ALL);
		} catch (Exception ex) {
			ex.printStackTrace();
			throw ex;
		} finally {
			// ContextUtil.popContext(context);
		}
		logger.writeLog("getEffectivity : END !!");
		return mEffectivity;
	}

	/**
	 * This Method Accepts Provide Part object id to get corresponding Linked CAD
	 * Part details
	 *
	 * @param context
	 * @param strObjectId :Provide Part Object Id
	 * @param strRel      : Relationship Name
	 * @return Details of Linked CAD Part
	 * @throws Exception
	 */
	private Map<?, ?> getCADPartDetails(Context context, String strOBJId, LCDSAPIntegration3DExpConstants lcdSAPInteg3DExpConstants) throws Exception {
		logger.writeLog("getCADPartDetails: START!!");
		Map<?, ?> objInfoMap = new HashMap<>();
		try {
			if (null != strOBJId && !strOBJId.isEmpty()) {
				// Object Selectables
				StringList objectSelects = new StringList();
				objectSelects.add(DomainConstants.SELECT_ID);
				objectSelects.add(DomainConstants.SELECT_NAME);
				objectSelects.add(lcdSAPInteg3DExpConstants.SELECT_ATTRIBUTE_PLMENTITY_V_NAME);
				objectSelects.add(lcdSAPInteg3DExpConstants.SELECT_ATTRIBUTE_PLMENTITY_V_DESCRIPTION);
				objectSelects.add(lcdSAPInteg3DExpConstants.SELECT_ATTRIBUTE_SERVICEABLEITEM_VPMREFERENCE); // "Serviceable Item";
				objectSelects.add(lcdSAPInteg3DExpConstants.SELECT_ATTRIBUTE_PROCUREMENTINTENT_VPMREFERENCE); // "Procurement Intent"
				objectSelects.add(lcdSAPInteg3DExpConstants.SELECT_ATTRIBUTE_PARTINTERCHANGEABILITY_VPMREFERENCE); // "Part Interchangeability ";
				objectSelects.add("attribute[XP_VPMReference_Ext.AtievaUnitofMeasure]"); // "Unit of Measure";
				objectSelects.add("attribute[XP_VPMReference_Ext.AtievaActualReleasedDate]"); //Released data of Part
				
				DomainObject boCADObj = DomainObject.newInstance(context, strOBJId);
				objInfoMap = boCADObj.getInfo(context, objectSelects);
				return objInfoMap;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		logger.writeLog("getCADPartDetails: END!!");
		return objInfoMap;
	}

	/**
	 * This Method Accepts Provide Part object id and Relationship Name to get
	 * corresponding Linked CAD Part details
	 *
	 * @param context
	 * @param strObjectId :Provide Part Object Id
	 * @param strRel      : Relationship Name
	 * @return Details of Linked CAD Part
	 * @throws Exception
	 */
	private Map getLinkedCADPartFromMBOMPart(Context context, String strOBJId, String strRel, LCDSAPIntegration3DExpConstants lcdSAPInteg3DExpConstants) throws Exception {
		logger.writeLog("getLinkedCADPartFromMBOMPart: START!!");
		Map<String, String> objInfoMap = null;
		try {
			if (null != strOBJId && !strOBJId.isEmpty()) {
				// Object Selectables
				StringList objectSelects = new StringList();
				objectSelects.add(DomainConstants.SELECT_ID);
				objectSelects.add(DomainConstants.SELECT_NAME);
				objectSelects.add(lcdSAPInteg3DExpConstants.SELECT_ATTRIBUTE_PLMENTITY_V_NAME);
				objectSelects.add(lcdSAPInteg3DExpConstants.SELECT_ATTRIBUTE_PLMENTITY_V_DESCRIPTION);
				objectSelects.add(lcdSAPInteg3DExpConstants.SELECT_ATTRIBUTE_SERVICEABLEITEM_VPMREFERENCE); // "Serviceable Item";
				objectSelects.add(lcdSAPInteg3DExpConstants.SELECT_ATTRIBUTE_PROCUREMENTINTENT_VPMREFERENCE); // "Procurement Intent"
				objectSelects.add(lcdSAPInteg3DExpConstants.SELECT_ATTRIBUTE_PARTINTERCHANGEABILITY_VPMREFERENCE); // "Part Interchangeability ";
				objectSelects.add("attribute[XP_VPMReference_Ext.AtievaUnitofMeasure]"); // "Unit of Measure";

				// RelationShip Selectables
				StringList relInfoList = new StringList();
				relInfoList.add(DomainConstants.SELECT_ID);
				relInfoList.add(DomainConstants.SELECT_TO_ID);

				// Mql command to get CADConnectionPhysicalID
				String strCADConnectionPhysicalID = MqlUtil.mqlCommand(context, "print bus " + strOBJId
						+ " select relationship[" + strRel + "].paths.path.element.physicalid dump |");

				if (UIUtil.isNotNullAndNotEmpty(strCADConnectionPhysicalID)) {
					StringList strListCADConnectionPathIDSplit = FrameworkUtil.split(strCADConnectionPhysicalID, "|");
					String strCADConnectionPathObjectId = null;

					if (!strListCADConnectionPathIDSplit.isEmpty()) {
						strCADConnectionPathObjectId = strListCADConnectionPathIDSplit
								.get(strListCADConnectionPathIDSplit.size() - 1);
						if (UIUtil.isNotNullAndNotEmpty(strCADConnectionPathObjectId)) {
							try {

								DomainRelationship domRel = new DomainRelationship(strCADConnectionPathObjectId);
								java.util.Hashtable relInfoMap = domRel.getRelationshipData(context, relInfoList);

								StringList slVPMReferenceId = (StringList) relInfoMap.get(DomainConstants.SELECT_TO_ID);
								String ostrVPMReferenceId = slVPMReferenceId.get(0);
								if (UIUtil.isNotNullAndNotEmpty(ostrVPMReferenceId)) {
									DomainObject boCADObj = DomainObject.newInstance(context, ostrVPMReferenceId);
									objInfoMap = boCADObj.getInfo(context, objectSelects);
								}

							} catch (Exception e) {
								logger.writeLog("Issue : Provided Part  catch>>>>>>>>>>>>>> " + strOBJId);
							}
						}
					}
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			throw ex;
		}
		logger.writeLog("getLinkedCADPartFromMBOMPart: END!!");
		return objInfoMap;
	}

	/**
	 * This Method is to create the SubContract Part header in JSON
	 *
	 * @param context
	 * @param Details of SubContract Part
	 * @return JsonObjectBuilder [after adding SubContract Part and its child
	 *         BuySubC Part information in JSON]
	 * @throws Exception
	 */
	private JsonObject getPhysicalProductJSON(Context context, Map<?, ?> mCadPartDetails, String strConnectionId, LCDSAPIntegration3DExpConstants lcdSAPInteg3DExpConstants)
			throws Exception {
		logger.writeLog("processSubContractPart : START !!");
		JsonObject jSubContractPayloadObj = null;
		try {
			if (mCadPartDetails != null)
				jSubContractPayloadObj = sendSubContractPartPayload(context, mCadPartDetails, strConnectionId, lcdSAPInteg3DExpConstants);
		} catch (Exception e) {
			e.printStackTrace();
		}
		logger.writeLog("processSubContractPart : END !!");
		return jSubContractPayloadObj;
	}

	/**
	 * This Method is to create the SubContract Part header in JSON
	 *
	 * @param context
	 * @param Details of SubContract Part
	 * @return JsonObjectBuilder [after adding SubContract Part and its child
	 *         BuySubC Part information in JSON]
	 * @throws Exception
	 */

	private JsonObject sendSubContractPartPayload(Context context, Map<?, ?> mSubContractPartDetails,
			String strConnectionId, LCDSAPIntegration3DExpConstants lcdSAPInteg3DExpConstants) throws Exception {
		logger.writeLog("sendSubContractPartPayload : START !!");
		int intCADPartResponseString = 0;
		JsonObject jSubContractPayloadObj = null;

		try {

			// STEP : Get Change Action Header
			JsonObjectBuilder jEachSubContractPayloadBuilder = ChangeActionJSONPayload(context, "", strConnectionId, lcdSAPInteg3DExpConstants);
			if (null != jEachSubContractPayloadBuilder) {
				// STEP : Get SubContract part Header

				JsonObjectBuilder jSubContractBuilder = getSubContractPartHeader(context, mSubContractPartDetails, lcdSAPInteg3DExpConstants);
				if (null != jSubContractBuilder) {

					// STEP : Retrieving all level BuySUBC Cad part , single level SubContract Cad
					// part and adding in JSON array
					JsonArrayBuilder jsubContractChildPartArrayBuilder = Json.createArrayBuilder();
					String strObjectID = (String) mSubContractPartDetails.get(DomainConstants.SELECT_ID);
					String strTitle = (String) mSubContractPartDetails.get(lcdSAPInteg3DExpConstants.SELECT_ATTRIBUTE_PLMENTITY_V_NAME);
					MapList mlObjects = getFirstLevelChildren(context, strObjectID, lcdSAPInteg3DExpConstants);
					if (mlObjects != null) {
						MapList mlBuySubCObjects = null;
						Map<?, ?> mchildPartDetails;
						String strProcIntent;
						String strObjID;

						// STEP : Processing First level Children of SubContract
						JsonObjectBuilder jChildBuilder = Json.createObjectBuilder();
						for (int kdx = 0; kdx < mlObjects.size(); kdx++) {
							mchildPartDetails = (Map<?, ?>) mlObjects.get(kdx);
							strProcIntent = (String) mchildPartDetails.get(lcdSAPInteg3DExpConstants.SELECT_ATTRIBUTE_PROCUREMENTINTENT_VPMREFERENCE);
							strObjID = (String) mchildPartDetails.get(DomainConstants.SELECT_ID);

							// STEP : adding SUBCONTRACT part in array of Child JSON
							if (LCDSAPIntegrationDataConstants.SUBCONTRACT.equalsIgnoreCase(strProcIntent)) {
								jChildBuilder = addCADChildPartHeader(context, mchildPartDetails, lcdSAPInteg3DExpConstants);
								if (null != jChildBuilder)
									jsubContractChildPartArrayBuilder.add(jChildBuilder.build());
							} else {
								// STEP : adding all level BUYSUBC part in array of Child JSON
								if (LCDSAPIntegrationDataConstants.BUYSUBC.equalsIgnoreCase(strProcIntent)) {
									jChildBuilder = addCADChildPartHeader(context, mchildPartDetails, lcdSAPInteg3DExpConstants);
									if (null != jChildBuilder)
										jsubContractChildPartArrayBuilder.add(jChildBuilder.build());
								}

								mlBuySubCObjects = getAllLevelBuySubC(context, strObjID, lcdSAPInteg3DExpConstants);
								if (mlBuySubCObjects != null) {
									for (int idx = 0; idx < mlBuySubCObjects.size(); idx++) {
										mchildPartDetails = (Map<?, ?>) mlBuySubCObjects.get(idx);
										strProcIntent = (String) mchildPartDetails
												.get(lcdSAPInteg3DExpConstants.SELECT_ATTRIBUTE_PROCUREMENTINTENT_VPMREFERENCE);
										if (LCDSAPIntegrationDataConstants.BUYSUBC.equalsIgnoreCase(strProcIntent)) {
											jChildBuilder = addCADChildPartHeader(context, mchildPartDetails, lcdSAPInteg3DExpConstants);
											if (null != jChildBuilder)
												jsubContractChildPartArrayBuilder.add(jChildBuilder.build());
										}
									}
								}
							}
						}
					}
					// STEP : creating the SubContract Part header JSON Object
					if (null != jsubContractChildPartArrayBuilder) {
						if (!jsubContractChildPartArrayBuilder.build().isEmpty()) {
							jSubContractBuilder.add("children", jsubContractChildPartArrayBuilder.build());
						}
						if (null != jSubContractBuilder)
							jEachSubContractPayloadBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_HEADER_PART, jSubContractBuilder.build());

						jSubContractPayloadObj = jEachSubContractPayloadBuilder.build();
						if (jSubContractPayloadObj.containsKey(LCDSAPIntegrationDataConstants.PROPERTY_HEADER_PART)) {
							String jsonEachPayloadString = jSubContractPayloadObj.toString();
							jsonLogger.writeLog("JSON Payload Request for SubContract : <<< " + strTitle + " : >>> \n "
									+ jsonEachPayloadString + " \n ");
						}
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		logger.writeLog("sendSubContractPartPayload : END !!");
		return jSubContractPayloadObj;
	}

	/**
	 * This Method is to create the SubContract Part header in JSON
	 * 
	 * @param context
	 * @param Details of SubContract Part
	 * @return JsonObjectBuilder [after adding SubContract Part and its child
	 *         BuySubC Part information in JSON]
	 * @throws Exception
	 */

	private JsonObjectBuilder getSubContractPartHeader(Context context, Map<?, ?> mSubContractPartDetails, LCDSAPIntegration3DExpConstants lcdSAPInteg3DExpConstants)
			throws Exception {
		logger.writeLog("addSubContractPartHeader : START !!");
		JsonObjectBuilder jSubContractBuilder = Json.createObjectBuilder();
		try {
			// STEP : Retrieving necessary information of SubContract Part
			String strObjectID = (String) mSubContractPartDetails.get(DomainConstants.SELECT_ID);
			String sName = (String) mSubContractPartDetails.get(DomainConstants.SELECT_NAME);
			String strTitle = (String) mSubContractPartDetails.get(lcdSAPInteg3DExpConstants.SELECT_ATTRIBUTE_PLMENTITY_V_NAME);
			String strdescription = (String) mSubContractPartDetails.get(lcdSAPInteg3DExpConstants.SELECT_ATTRIBUTE_PLMENTITY_V_DESCRIPTION);
			String strProcurementIntent = (String) mSubContractPartDetails.get(lcdSAPInteg3DExpConstants.SELECT_ATTRIBUTE_PROCUREMENTINTENT_VPMREFERENCE);
			String strPartInterchangeability = (String) mSubContractPartDetails
					.get(lcdSAPInteg3DExpConstants.SELECT_ATTRIBUTE_PARTINTERCHANGEABILITY_VPMREFERENCE);
			String strServiceable = (String) mSubContractPartDetails.get(lcdSAPInteg3DExpConstants.SELECT_ATTRIBUTE_SERVICEABLEITEM_VPMREFERENCE);

			// STEP : Adding information of SubContract Part IN JSON Object
			if (UIUtil.isNotNullAndNotEmpty(strTitle))
				jSubContractBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_TITLE, strTitle);
			else
				jSubContractBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_TITLE, " ");

			if (UIUtil.isNotNullAndNotEmpty(sName))
				jSubContractBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_NAME, sName);
			else
				jSubContractBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_NAME, " ");

			if (UIUtil.isNotNullAndNotEmpty(strObjectID))
				jSubContractBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_OID, strObjectID);
			else
				jSubContractBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_OID, " ");

			jSubContractBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_REL_ID, LCDSAPIntegrationDataConstants.VALUE_NOT_APPLICABLE);

			if (UIUtil.isNotNullAndNotEmpty(strdescription))
				jSubContractBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_DESCRIPTION, strdescription);
			else
				jSubContractBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_DESCRIPTION, " ");

			if (UIUtil.isNotNullAndNotEmpty(strProcurementIntent))
				jSubContractBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_PROCUREMENTINTENT, strProcurementIntent);
			else
				jSubContractBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_PROCUREMENTINTENT, " ");

			if (UIUtil.isNotNullAndNotEmpty(strServiceable))
				jSubContractBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_SERVICEABLEITEM, strServiceable);
			else
				jSubContractBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_SERVICEABLEITEM, " ");

			jSubContractBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_PLANTCODE, LCDSAPIntegrationDataConstants.VALUE_NOT_APPLICABLE);

			if (UIUtil.isNotNullAndNotEmpty(strPartInterchangeability))
				jSubContractBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_PARTINTERCHANGEABILITY, strPartInterchangeability);
			else
				jSubContractBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_PARTINTERCHANGEABILITY, " ");

			jSubContractBuilder.add("Effectivity_Option_Code", LCDSAPIntegrationDataConstants.VALUE_NOT_APPLICABLE);
			jSubContractBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_DATEFROM, LCDSAPIntegrationDataConstants.VALUE_NOT_APPLICABLE);
			jSubContractBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_DATETO, LCDSAPIntegrationDataConstants.VALUE_NOT_APPLICABLE);
			jSubContractBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_REALIZED_DATA, true);

		} catch (Exception e) {
			e.printStackTrace();
		}
		logger.writeLog("addSubContractPartHeader : END !!");
		return jSubContractBuilder;
	}

	/**
	 * This Method Accepts Linked EBOM Part Object ID to expand it to get BuySubC
	 * Children
	 *
	 * @param context
	 * @param strObjectId :Linked EBOM Part Object ID
	 * @return All BuySubC children
	 * @throws Exception
	 */
	private MapList getFirstLevelChildren(Context context, String strObjectId, LCDSAPIntegration3DExpConstants lcdSAPInteg3DExpConstants) throws Exception {
		logger.writeLog("getFirstLevelChildren: START!!");
		MapList mlObjects = null;
		try {
			if (UIUtil.isNotNullAndNotEmpty(strObjectId)) {
				// Object Selectables
				StringList objectSelects = new StringList();
				objectSelects.add(DomainConstants.SELECT_ID);
				objectSelects.add(DomainConstants.SELECT_TYPE);
				objectSelects.add(DomainConstants.SELECT_NAME);
				objectSelects.add(DomainConstants.SELECT_REVISION);
				objectSelects.add(lcdSAPInteg3DExpConstants.SELECT_ATTRIBUTE_PLMENTITY_V_NAME);
				objectSelects.add(lcdSAPInteg3DExpConstants.SELECT_ATTRIBUTE_PLMENTITY_V_DESCRIPTION);
				objectSelects.add(lcdSAPInteg3DExpConstants.SELECT_ATTRIBUTE_SERVICEABLEITEM_VPMREFERENCE); // "Serviceable Item";
				objectSelects.add(lcdSAPInteg3DExpConstants.SELECT_ATTRIBUTE_PROCUREMENTINTENT_VPMREFERENCE); // "Procurement Intent"
				objectSelects.add(lcdSAPInteg3DExpConstants.SELECT_ATTRIBUTE_PARTINTERCHANGEABILITY_VPMREFERENCE); // "Part Interchangeability ";
				objectSelects.add("attribute[XP_VPMReference_Ext.AtievaUnitofMeasure]"); // "Unit of Measure";

				// RelationShip Selectables
				StringList relSelects = new StringList();
				relSelects.add(DomainConstants.SELECT_RELATIONSHIP_ID);
				relSelects.add(lcdSAPInteg3DExpConstants.SELECT_ATTRIBUTE_HAS_CONFIG_EFFECTIVITY);

				// STEP : Expanding the Input Linked CAD Part to get all children of it
				DomainObject partDomObj = DomainObject.newInstance(context, strObjectId);
				mlObjects = partDomObj.getRelatedObjects(context, lcdSAPInteg3DExpConstants.TYPE_VPM_INSTANCE, // relationship pattern
						lcdSAPInteg3DExpConstants.TYPE_VPM_REFERENCE, // object pattern
						objectSelects, // object selects
						relSelects, // relationship selects
						false, // to direction
						true, // from direction
						(short) 1, // recursion level
						DomainConstants.EMPTY_STRING, // object where clause
						DomainConstants.EMPTY_STRING, 0);

				mlObjects.sort(lcdSAPInteg3DExpConstants.SELECT_ATTRIBUTE_TREEORDER_PLMINSTANCE, "ascending", "real");

			}
		} catch (Exception expMsg) {
		}
		logger.writeLog("getFirstLevelChildren: END!!");
		return mlObjects;
	}

	/**
	 * This Method is to add the BuySubC Part information in JSON
	 *
	 * @param context
	 * @param Details of BuySubC childs
	 * @return JsonArrayBuilder [after adding BuySubC Part information in JSON]
	 * @throws Exception
	 */
	private JsonObjectBuilder addCADChildPartHeader(Context context, Map<?, ?> mchildPartDetails, LCDSAPIntegration3DExpConstants lcdSAPInteg3DExpConstants) throws Exception {
		logger.writeLog("addBuySubCPartHeader : START!!");
		JsonObjectBuilder jCadPartBuilder = Json.createObjectBuilder();
		try {
			// STEP : Retrieving necessary information of BuySubC Part
			String strVName = (String) mchildPartDetails.get(lcdSAPInteg3DExpConstants.SELECT_ATTRIBUTE_PLMENTITY_V_NAME);
			String strName = (String) mchildPartDetails.get(DomainConstants.SELECT_NAME);
			String strObjectID = (String) mchildPartDetails.get(DomainObject.SELECT_ID);
			String strRId = (String) mchildPartDetails.get(DomainConstants.SELECT_RELATIONSHIP_ID);
			String strdesc = (String) mchildPartDetails.get(lcdSAPInteg3DExpConstants.SELECT_ATTRIBUTE_PLMENTITY_V_DESCRIPTION);
			String strUnitOfMeasure = (String) mchildPartDetails.get("attribute[XP_VPMReference_Ext.AtievaUnitofMeasure]");
			String strProcIntent = (String) mchildPartDetails.get(lcdSAPInteg3DExpConstants.SELECT_ATTRIBUTE_PROCUREMENTINTENT_VPMREFERENCE);
			String strServiceItem = (String) mchildPartDetails.get(lcdSAPInteg3DExpConstants.SELECT_ATTRIBUTE_SERVICEABLEITEM_VPMREFERENCE);
			String strPartability = (String) mchildPartDetails.get(lcdSAPInteg3DExpConstants.SELECT_ATTRIBUTE_PARTINTERCHANGEABILITY_VPMREFERENCE);
			String strhasConfigEffectivity = (String) mchildPartDetails.get(lcdSAPInteg3DExpConstants.SELECT_ATTRIBUTE_HAS_CONFIG_EFFECTIVITY);

			/*String strVariantEffectivity = null;
			String strEffectivityObject = null;
			String strDateEffectivity = "";
			String strCADReleaseDateIn = "";
			String strCADReleaseDateOut = "";

			if (LCDSAPIntegrationDataConstants.TRUE.equals(strhasConfigEffectivity)) {
				Map<?, ?> mEffectivity = getEffectivity(context, strRId);
				if (mEffectivity != null) {
					strEffectivityObject = mEffectivity.toString();

					if (strEffectivityObject.contains("Effectivity_Current_Evolution:")) {
						strDateEffectivity = strEffectivityObject.substring(
								strEffectivityObject.indexOf("Effectivity_Current_Evolution:") + 30,
								strEffectivityObject.indexOf("]") + 1);

						String[] result = strDateEffectivity.split(" - ");
						if (result.length >= 2) {
							strCADReleaseDateIn = result[0];
							strCADReleaseDateIn = strCADReleaseDateIn.substring(13);

							if (strCADReleaseDateIn.length() >= 19)
								strCADReleaseDateIn = strCADReleaseDateIn.substring(0,
										strCADReleaseDateIn.length() - 9);

							strCADReleaseDateOut = result[1];
							strCADReleaseDateOut = strCADReleaseDateOut.substring(0, strCADReleaseDateOut.length() - 1);

							if (strCADReleaseDateOut.length() >= 19)
								strCADReleaseDateOut = strCADReleaseDateOut.substring(0,
										strCADReleaseDateOut.length() - 9);

						}
					}
				}
			}*/
			// STEP : Adding information of each BuySubC Part IN JSON Object

			if (UIUtil.isNotNullAndNotEmpty(strVName))
				jCadPartBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_TITLE, strVName);
			else
				jCadPartBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_TITLE, " ");

			if (UIUtil.isNotNullAndNotEmpty(strName))
				jCadPartBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_NAME, strName);
			else
				jCadPartBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_NAME, " ");

			if (UIUtil.isNotNullAndNotEmpty(strObjectID))
				jCadPartBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_OID, strObjectID);
			else
				jCadPartBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_OID, " ");

			if (UIUtil.isNotNullAndNotEmpty(strRId))
				jCadPartBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_REL_ID, strRId);
			else
				jCadPartBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_REL_ID, " ");

			if (UIUtil.isNotNullAndNotEmpty(strdesc))
				jCadPartBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_DESCRIPTION, strdesc);
			else
				jCadPartBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_DESCRIPTION, " ");

			if (UIUtil.isNotNullAndNotEmpty(strUnitOfMeasure))
				jCadPartBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_UNIT_OF_MEASURE, strUnitOfMeasure);
			else
				jCadPartBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_UNIT_OF_MEASURE, " ");

			if (UIUtil.isNotNullAndNotEmpty(strProcIntent))
				jCadPartBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_PROCUREMENTINTENT, strProcIntent);
			else
				jCadPartBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_PROCUREMENTINTENT, " ");

			if (UIUtil.isNotNullAndNotEmpty(strServiceItem))
				jCadPartBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_SERVICEABLEITEM, strServiceItem);
			else
				jCadPartBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_SERVICEABLEITEM, " ");

			if (UIUtil.isNotNullAndNotEmpty(strPartability))
				jCadPartBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_PARTINTERCHANGEABILITY, strPartability);
			else
				jCadPartBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_PARTINTERCHANGEABILITY, " ");

			jCadPartBuilder.add("Effectivity_Option_Code", LCDSAPIntegrationDataConstants.VALUE_NOT_APPLICABLE);

			//SimpleDateFormat format1 = new SimpleDateFormat("yyyy-MM-dd");
			//SimpleDateFormat format2 = new SimpleDateFormat("MM-dd-yyyy");
			/*if (UIUtil.isNotNullAndNotEmpty(strCADReleaseDateIn)) {
				Date date = format1.parse(strCADReleaseDateIn);
				String formatDate = format2.format(date);
				jCadPartBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_DATEFROM, formatDate);
			} else
				jCadPartBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_DATEFROM, " ");*/
			
			jCadPartBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_DATEFROM, getPartReleasedDate());
			
			String strCADReleaseDateOut = "12-31-9999";
			jCadPartBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_DATETO, strCADReleaseDateOut);
			
			/*if (UIUtil.isNotNullAndNotEmpty(strCADReleaseDateOut)) {
				if (strCADReleaseDateOut.equals("INF")) {
					strCADReleaseDateOut = "12-31-9999";
					jCadPartBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_DATETO, strCADReleaseDateOut);
				} else {
					Date date = format1.parse(strCADReleaseDateOut);
					String formatDate = format2.format(date);
					jCadPartBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_DATETO, formatDate);
				}
			} else
				jCadPartBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_DATETO, " ");*/

			jCadPartBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_REALIZED_DATA, true);

		} catch (Exception e) {
			e.printStackTrace();
		}
		logger.writeLog("addBuySubCPartHeader : END!!");
		return jCadPartBuilder;
	}

	/**
	 * This Method Accepts Linked EBOM Part Object ID to expand it to get BuySubC
	 * Children
	 *
	 * @param context
	 * @param strObjectId :Linked EBOM Part Object ID
	 * @return All BuySubC children
	 * @throws Exception
	 */
	private MapList getAllLevelBuySubC(Context context, String strObjectId, LCDSAPIntegration3DExpConstants lcdSAPInteg3DExpConstants) throws Exception {
		logger.writeLog("getAllLevelBuySubC: START!!");
		MapList mlObjects = new MapList();
		try {
			String strProcurementIntent = null;
			if (UIUtil.isNotNullAndNotEmpty(strObjectId)) {
				// Object Selectables
				StringList objectSelects = new StringList();
				objectSelects.add(DomainConstants.SELECT_ID);
				objectSelects.add(DomainConstants.SELECT_TYPE);
				objectSelects.add(DomainConstants.SELECT_NAME);
				objectSelects.add(DomainConstants.SELECT_REVISION);
				objectSelects.add(lcdSAPInteg3DExpConstants.SELECT_ATTRIBUTE_PLMENTITY_V_NAME);
				objectSelects.add(lcdSAPInteg3DExpConstants.SELECT_ATTRIBUTE_PLMENTITY_V_DESCRIPTION);
				objectSelects.add(lcdSAPInteg3DExpConstants.SELECT_ATTRIBUTE_SERVICEABLEITEM_VPMREFERENCE); // "Serviceable Item";
				objectSelects.add(lcdSAPInteg3DExpConstants.SELECT_ATTRIBUTE_PROCUREMENTINTENT_VPMREFERENCE); // "Procurement Intent"
				objectSelects.add(lcdSAPInteg3DExpConstants.SELECT_ATTRIBUTE_PARTINTERCHANGEABILITY_VPMREFERENCE); // "Part Interchangeability ";
				objectSelects.add("attribute[XP_VPMReference_Ext.AtievaUnitofMeasure]"); // "Unit of Measure";

				// RelationShip Selectables
				StringList relSelects = new StringList();
				relSelects.add(DomainConstants.SELECT_RELATIONSHIP_ID);
				relSelects.add(lcdSAPInteg3DExpConstants.SELECT_ATTRIBUTE_HAS_CONFIG_EFFECTIVITY);

				// STEP : Expanding the Input Linked CAD Part to get all children of it
				DomainObject partDomObj = DomainObject.newInstance(context, strObjectId);
				MapList ebomList = partDomObj.getRelatedObjects(context, lcdSAPInteg3DExpConstants.TYPE_VPM_INSTANCE, // relationship pattern
						lcdSAPInteg3DExpConstants.TYPE_VPM_REFERENCE, // object pattern
						objectSelects, // object selects
						relSelects, // relationship selects
						false, // to direction
						true, // from direction
						(short) 0, // recursion level
						DomainConstants.EMPTY_STRING, // object where clause
						DomainConstants.EMPTY_STRING, 0);

				ebomList.sort(lcdSAPInteg3DExpConstants.SELECT_ATTRIBUTE_TREEORDER_PLMINSTANCE, "ascending", "real");
				// STEP : Collecting only BuySubC part from expanded childs
				Map<?, ?> tempMap;
				if (!ebomList.isEmpty()) {
					for (int b = 0; b < ebomList.size(); b++) {
						tempMap = (Map<?, ?>) ebomList.get(b);
						strProcurementIntent = (String) tempMap.get(lcdSAPInteg3DExpConstants.SELECT_ATTRIBUTE_PROCUREMENTINTENT_VPMREFERENCE);
						if (LCDSAPIntegrationDataConstants.BUYSUBC.equalsIgnoreCase(strProcurementIntent)) {
							mlObjects.add(tempMap);
						}
					}
				}
			}
		} catch (Exception expMsg) {
		}
		logger.writeLog("getAllLevelBuySubC: END!!");
		return mlObjects;
	}

	/**
	 * This Method Accepts xcsrfToken and cookie retrieved from GET SAP webService
	 * Call calls Custom created Post web service in SAP
	 *
	 * @param Cookie              From SAP
	 * @param xcsrftoken          From SAP
	 * @param Input               JsonObject to SAP WebService
	 * @param CloseableHttpClient for Active session to call the SAP web service
	 * @return web service response
	 * @throws Exception
	 */
	private int callPostService(Context context, JsonObject jEachPayloadObj, LCDSAPIntegration3DExpConstants lcdSAPInteg3DExpConstants) throws Exception {
		logger.writeLog("callPostService()..... START");
		// STEP : Creating HttpPost Object with SAP webService URL
		url = EnoviaResourceBundle.getProperty(context, lcdSAPInteg3DExpConstants.LCD_3DX_SAP_INTEGRATION_KEY,
					"LCD_3DXSAPStringResource_en.SAP.WebServiceURL.DEV", language);
					
	    logger.writeLog("SAP PO Webservice URL ==  " + url);	
		HttpPost postURL = new HttpPost(url);
		httpClient = HttpClients.createDefault();
		String result = null;
		int intResponseCode = 0;
		try {
			//  STEP : Creating request header for SAP WebService POST Method call
		    //	String xcsrftoken = EnoviaResourceBundle.getProperty(context, lcdSAPInteg3DExpConstants.LCD_3DX_SAP_INTEGRATION_KEY,"LCD_3DXSAPStringResource_en.SAP.XCSRFTOKEN", language);
		    //	String cookie = EnoviaResourceBundle.getProperty(context, lcdSAPInteg3DExpConstants.LCD_3DX_SAP_INTEGRATION_KEY,"LCD_3DXSAPStringResource_en.SAP.COOKIE", language);
			
			String userName = EnoviaResourceBundle.getProperty(context, lcdSAPInteg3DExpConstants.LCD_3DX_SAP_INTEGRATION_KEY,
					"LCD_3DXSAPStringResource_en.SAP.UserName", language);

			String password = EnoviaResourceBundle.getProperty(context, lcdSAPInteg3DExpConstants.LCD_3DX_SAP_INTEGRATION_KEY,
					"LCD_3DXSAPStringResource_en.SAP.Password", language);

			

			String contentType = EnoviaResourceBundle.getProperty(context, lcdSAPInteg3DExpConstants.LCD_3DX_SAP_INTEGRATION_KEY,"LCD_3DXSAPStringResource_en.SAP.CONTENTTYPE", language);

			String authString = userName + ":" + password;
			String authStringEnc = BASE64_ENCODER.encodeToString(authString.getBytes());

			// STEP : Creating HttpGet Object with SAP webService UR
			String authorization = EnoviaResourceBundle.getProperty(context, lcdSAPInteg3DExpConstants.LCD_3DX_SAP_INTEGRATION_KEY,"LCD_3DXSAPStringResource_en.SAP.Authorization", language);
			
			StringEntity params = new StringEntity(jEachPayloadObj.toString());
			
			
			// STEP : Creating request header for SAP WebService GET Method call
			postURL.setHeader(authorization, "Basic " + authStringEnc);
			postURL.setHeader(contentType, "application/json");
			postURL.setEntity(params);

			logger.writeLog("Before Calling SAP Webservice Payload " + params);	
			// STEP : Invoking SAP webService with Post Method
			HttpResponse response = httpClient.execute(postURL);
			logger.writeLog("postURL -- " + postURL);
			intResponseCode = response.getStatusLine().getStatusCode();

			if (intResponseCode == HttpStatus.SC_OK) { // success
				// STEP : Collecting the acknowledgement from SAP webService
				logger.writeLog(
						"POST request successful , response code ==  " + response.getStatusLine().getStatusCode());
			} else {
				logger.writeLog("POST request failed , response code ==  " + response.getStatusLine().getStatusCode());
			}
			result = EntityUtils.toString(response.getEntity());
			logger.writeLog("POST request Result--------------- " + result);
		} catch (Exception e) {
			e.printStackTrace();
		}
		logger.writeLog("callPostService()..... END");
		return intResponseCode;
	}

	/**
	 * This Method is to process the response from POST call of SAP WebService
	 *
	 * @param context
	 * @param String  responseString : Response from SAP webService
	 * @throws Exception
	 */
	private void processWebServiceResponseForSubContract(Context context, int intResponseCode, String strConnectionId, LCDSAPIntegration3DExpConstants lcdSAPInteg3DExpConstants)
			throws Exception {
		logger.writeLog("ProcessWebServiceResponseForSubContract START");
		String sbfErrMes = "";

		SimpleDateFormat DateFormat = new SimpleDateFormat("MM/dd/yyyy hh.mm.ss aa");
		try {
			ContextUtil.pushContext(context);

			DomainRelationship domRelBomConnectedToAnchorObj = DomainRelationship.newInstance(context, strConnectionId);

			if (intResponseCode == 200 || intResponseCode == 201 || intResponseCode == 202 ) {
				domRelBomConnectedToAnchorObj.setAttributeValue(context, lcdSAPInteg3DExpConstants.ATTRIBUTE_LCD_PROCESS_STATUS_FLAG, LCDSAPIntegrationDataConstants.VALUE_PROCESS_STATUS_FLAG_IN_WORK);
				domRelBomConnectedToAnchorObj.setAttributeValue(context, lcdSAPInteg3DExpConstants.ATTRIBUTE_LCD_SAP_FEEDBACK_MESSAGE,
						LCDSAPIntegrationDataConstants.MSG_JSON_FORMAT_VALIDATION_COMPLETED);
			} else if (intResponseCode == 417) {
				domRelBomConnectedToAnchorObj.setAttributeValue(context, lcdSAPInteg3DExpConstants.ATTRIBUTE_LCD_PROCESS_STATUS_FLAG, LCDSAPIntegrationDataConstants.VALUE_PROCESS_STATUS_FLAG_FAILED);
				domRelBomConnectedToAnchorObj.setAttributeValue(context, lcdSAPInteg3DExpConstants.ATTRIBUTE_LCD_SAP_FEEDBACK_MESSAGE,
						LCDSAPIntegrationDataConstants.MSG_JSON_FORMAT_VALIDATION_FAILED);
			} else {
				domRelBomConnectedToAnchorObj.setAttributeValue(context, lcdSAPInteg3DExpConstants.ATTRIBUTE_LCD_PROCESS_STATUS_FLAG, LCDSAPIntegrationDataConstants.VALUE_PROCESS_STATUS_FLAG_FAILED);
				domRelBomConnectedToAnchorObj.setAttributeValue(context, lcdSAPInteg3DExpConstants.ATTRIBUTE_LCD_SAP_FEEDBACK_MESSAGE,
						LCDSAPIntegrationDataConstants.MSG_NETWORK_FAILURE);
			}
			ContextUtil.popContext(context);
		} catch (Exception e) {
			e.printStackTrace();
		}
		logger.writeLog("ProcessWebServiceResponseForSubContract END");
	}

	/**
	 * This Method is to process the response from POST call of SAP WebService
	 *
	 * @param context
	 * @param String  responseString : Response from SAP webService
	 * @return
	 * @throws Exception
	 */
	private void processWebServiceResponseForManufacturingAssembly(Context context, int intResponseCode,
			String strConnectionId, LCDSAPIntegration3DExpConstants lcdSAPInteg3DExpConstants) throws NullPointerException {
		logger.writeLog("ProcessWebServiceResponseForManufacturingAssembly START");
		try {
			ContextUtil.pushContext(context);
			DomainRelationship domRelBomConnectedToAnchorObj = DomainRelationship.newInstance(context, strConnectionId);

			if (intResponseCode == 200 || intResponseCode == 201 || intResponseCode == 202 ) {
				domRelBomConnectedToAnchorObj.setAttributeValue(context, lcdSAPInteg3DExpConstants.ATTRIBUTE_LCD_PROCESS_STATUS_FLAG, LCDSAPIntegrationDataConstants.VALUE_PROCESS_STATUS_FLAG_IN_WORK);
				domRelBomConnectedToAnchorObj.setAttributeValue(context, lcdSAPInteg3DExpConstants.ATTRIBUTE_LCD_SAP_FEEDBACK_MESSAGE,
						LCDSAPIntegrationDataConstants.MSG_JSON_FORMAT_VALIDATION_COMPLETED);
			} else if (intResponseCode == 417) {
				domRelBomConnectedToAnchorObj.setAttributeValue(context, lcdSAPInteg3DExpConstants.ATTRIBUTE_LCD_PROCESS_STATUS_FLAG, LCDSAPIntegrationDataConstants.VALUE_PROCESS_STATUS_FLAG_FAILED);
				domRelBomConnectedToAnchorObj.setAttributeValue(context, lcdSAPInteg3DExpConstants.ATTRIBUTE_LCD_SAP_FEEDBACK_MESSAGE,
						LCDSAPIntegrationDataConstants.MSG_JSON_FORMAT_VALIDATION_FAILED);
			} else {
				domRelBomConnectedToAnchorObj.setAttributeValue(context, lcdSAPInteg3DExpConstants.ATTRIBUTE_LCD_PROCESS_STATUS_FLAG, LCDSAPIntegrationDataConstants.VALUE_PROCESS_STATUS_FLAG_FAILED);
				domRelBomConnectedToAnchorObj.setAttributeValue(context, lcdSAPInteg3DExpConstants.ATTRIBUTE_LCD_SAP_FEEDBACK_MESSAGE,
						LCDSAPIntegrationDataConstants.MSG_NETWORK_FAILURE);
			}
			ContextUtil.popContext(context);
		} catch (Exception e) {
			e.printStackTrace();
		}
		logger.writeLog("ProcessWebServiceResponseForManufacturingAssembly END");
	}

	/**
	 * This Method is to Set the Change Action JSON
	 *
	 * @param context
	 * @param JsonObject changeActionJson : SAP WebService URL
	 * @throws Exception
	 */
	private static int setChangeActionJson(JsonObject changeAction) {
		changeActionJson = changeAction;
		return 0;
	}

	/**
	 * This Method is to Get the Change Action JSON
	 *
	 * @param context
	 * @return CHANGE_ACTION_JSON : SAP WebService URL
	 * @throws Exception
	 */
	private static JsonObject getChangeActionJson() {
		return changeActionJson;
	}

	/**
	 * This Method is to Set the Change Action Completion Date
	 *
	 * @param context
	 * @return CA_RELEASED_DATE : Change Action Completion Date
	 * @throws Exception
	 */
	private void setCACompletionDate(String strCACompletionDate) {
		caReleasedDate = strCACompletionDate;
	}

	/**
	 * This Method is to Get the Change Action Completion Date
	 *
	 * @param context
	 * @return CA_RELEASED_DATE : Change Action Completion Date
	 * @throws Exception
	 */
	private static String getCACompletionDate() {
		return caReleasedDate;
	}

	/**
	 * This Method is to Set the Change Action Completion Date
	 *
	 * @param context
	 * @return CA_RELEASED_DATE : Change Action Completion Date
	 * @throws Exception
	 */
	private void setPartReleasedDate(String strPartReleasedDate) {
		partReleasedDate = strPartReleasedDate;
	}

	/**
	 * This Method is to Get the Change Action Completion Date
	 *
	 * @param context
	 * @return CA_RELEASED_DATE : Change Action Completion Date
	 * @throws Exception
	 */
	private static String getPartReleasedDate() {
		return partReleasedDate;
	}

	/**
	 * This Method is to Get the Cookie for the Session
	 *
	 * @param context
	 * @return COOKIE : Cookie for the Session
	 * @throws Exception
	 */
	private static boolean getFalseEffectivityFlag() throws Exception {
		return FALSE_EFFECTIVITY_STATUS;
	}

	/**
	 * This Method is to Set the configured flag of object
	 *
	 * @param context
	 * @return CA_RELEASED_DATE : Change Action Completion Date
	 * @throws Exception
	 */
	private void setFalseEffectivityFlag(boolean bFalseEffectivityFlag) throws Exception {
		FALSE_EFFECTIVITY_STATUS = bFalseEffectivityFlag;
	}

	/**
	 * This Method is to Get the Cookie for the Session
	 *
	 * @param context
	 * @return COOKIE : Cookie for the Session
	 * @throws Exception
	 */
	private static boolean getConfiguredFlag() throws Exception {
		return CONFIGURABLE_STATUS;
	}

	/**
	 * This Method is to Set the configured flag of object
	 *
	 * @param context
	 * @return CA_RELEASED_DATE : Change Action Completion Date
	 * @throws Exception
	 */
	private void setConfiguredFlag(boolean bConfigFlag) throws Exception {
		CONFIGURABLE_STATUS = bConfigFlag;
	}

	/**
	 * This Method is to Set the configured flag of object
	 *
	 * @param context
	 * @return CA_RELEASED_DATE : Change Action Completion Date
	 * @throws Exception
	 */
	private void setRealizedItemList(Context context, JsonObject realizedItem) throws Exception {
		logger.writeLog("setRealizedItemList()..... START");
		try {
			List<String> lstRealizedChanges = new List<String>();
			String relPhyId = "";
			JsonArray amos = realizedItem.getJsonArray("amo");
			for (int a = 0; a < amos.size(); a++) {
				JsonObject amo = amos.getJsonObject(a);
				if (amo.containsKey("before")) {
					relPhyId = amo.getJsonObject("before").get("id").toString().split(":")[1].replaceAll("\"", "");
					if (UIUtil.isNotNullAndNotEmpty(relPhyId)) {
						lstRealizedChanges.add(relPhyId);

					}
				}
				if (amo.containsKey("after")) {
					relPhyId = amo.getJsonObject("after").get("id").toString().split(":")[1].replaceAll("\"", "");
					if (UIUtil.isNotNullAndNotEmpty(relPhyId)) {
						lstRealizedChanges.add(relPhyId);
					}
				}

			}
			logger.writeLog("lstRealizedChanges..... START <<<<<<<<<< " + lstRealizedChanges);
			if (lstRealizedChanges.size() != 0) {
				LIST_REALIZED_CHANGES = lstRealizedChanges;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		logger.writeLog("setRealizedItemList()..... END");
	}

	/**
	 * This Method is to Get the Cookie for the Session
	 *
	 * @param context
	 * @return COOKIE : Cookie for the Session
	 * @throws Exception
	 */
	private static List<String> getRealizedItemList() throws Exception {
		List<String> lstRealizedChanges = new List<String>();
		if (LIST_REALIZED_CHANGES != null && LIST_REALIZED_CHANGES.size() != 0) {
			return LIST_REALIZED_CHANGES;
		} else
			return lstRealizedChanges;

	}

	/**
	 * Invoking SAP Web service with GET Method to get the x-csrf-token and cookies
	 * error or send empty string.
	 *
	 * @param CloseableHttpClient
	 * @return String Array in arguments ( x-csrf-token and cookies )
	 * @throws Exception
	 */
	private int callGETService(Context context, LCDSAPIntegration3DExpConstants lcdSAPInteg3DExpConstants) throws Exception {
		logger.writeLog("callGETService()..... START");

		String strStatusLine = null;
		int responseCode = 0;

		String userName = EnoviaResourceBundle.getProperty(context, lcdSAPInteg3DExpConstants.LCD_3DX_SAP_INTEGRATION_KEY,
				"LCD_3DXSAPStringResource_en.SAP.UserName", language);

		String password = EnoviaResourceBundle.getProperty(context, lcdSAPInteg3DExpConstants.LCD_3DX_SAP_INTEGRATION_KEY,
				"LCD_3DXSAPStringResource_en.SAP.Password", language);

		url = EnoviaResourceBundle.getProperty(context, lcdSAPInteg3DExpConstants.LCD_3DX_SAP_INTEGRATION_KEY,
				"LCD_3DXSAPStringResource_en.SAP.WebServiceURL.DEV", language);

		httpClient = HttpClients.createDefault();

		try {
			// STEP : Creating Authorization Header for calling SAP webService using GET
			// method
			String authString = userName + ":" + password;
			String authStringEnc = BASE64_ENCODER.encodeToString(authString.getBytes());

			// STEP : Creating HttpGet Object with SAP webService URL
			HttpGet getURL = new HttpGet(url);

			String authorization = EnoviaResourceBundle.getProperty(context, lcdSAPInteg3DExpConstants.LCD_3DX_SAP_INTEGRATION_KEY,
					"LCD_3DXSAPStringResource_en.SAP.Authorization", language);
			xcsrfToken = EnoviaResourceBundle.getProperty(context, lcdSAPInteg3DExpConstants.LCD_3DX_SAP_INTEGRATION_KEY,
					"LCD_3DXSAPStringResource_en.SAP.XCSRFTOKEN", language);

			// STEP : Creating request header for SAP WebService GET Method call
			getURL.setHeader(authorization, "Basic " + authStringEnc);
			getURL.setHeader(xcsrfToken, "fetch");

			logger.writeLog("callGETService getURL ---- " + getURL);
			// STEP : Invoking SAP webService with GET Method
			HttpResponse response = httpClient.execute(getURL);

			// STEP : Collecting the acknowledgement from SAP webService
			responseCode = response.getStatusLine().getStatusCode();
			strStatusLine = response.getStatusLine().toString();
			logger.writeLog("callGETService responseCode ---- " + responseCode);
			if (responseCode == HttpStatus.SC_OK) { // success

				// STEP : Collecting the x-csrf-token and cookies from SAP webService Response
				Header[] resHeaders = response.getAllHeaders();
				int index = -1;
				index = resHeaders[1].getValue().indexOf(":");
				String strCookie = resHeaders[1].getValue().substring(index + 1);
				String xcsrftoken = null;
				for (Header header : resHeaders) {
					if (xcsrfToken.equalsIgnoreCase(header.getName())) {
						xcsrftoken = header.getValue();
					}
				}
				if (UIUtil.isNotNullAndNotEmpty(xcsrftoken)) {
					logger.writeLog(xcsrfToken + xcsrftoken);
					logger.writeLog("cookies" + strCookie);
					logger.writeLog(
							"GET request successful , Response code ==  " + response.getStatusLine().getStatusCode());
					logger.writeLog("GET request Successful , Response Status Line==  " + strStatusLine);
					xcsrfToken = xcsrftoken;
					cookie = strCookie;
				}
			} else {
				logger.writeLog("GET request Failed , Response code ==  " + response.getStatusLine().getStatusCode());
				logger.writeLog("GET request Failed , Response Status Line ==  " + strStatusLine);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		logger.writeLog("callGETService()..... END");
		return responseCode;
	}

	/***
	 * This method is to sort the Option code
	 * 
	 * @param strOptionCode
	 * @return
	 * @throws Exception
	 */
	public String sortOptionCode(String strOptionCode) throws Exception {
		String strSortedOptionCode = "";
		try {
			Character charStartingBraces = new Character('{');
			Character charClosingBraces = new Character('}');

			for (int i = 0; i < strOptionCode.length(); i++) {
				if (charStartingBraces.equals(strOptionCode.charAt(i))
						|| charClosingBraces.equals(strOptionCode.charAt(i))) {
					String strTempOptionCodeToSort = "";
					String strTemp = "";
					int icount = 0;

					for (int j = i; j < strOptionCode.length(); j++) {
						strTempOptionCodeToSort = strTempOptionCodeToSort + strOptionCode.charAt(j);

						if (charClosingBraces.equals(strOptionCode.charAt(j))) {
							break;
						}
						icount++;
					}

					// Removing Starting Braces and Closing Braces from Option Code
					strTempOptionCodeToSort = strTempOptionCodeToSort.substring(1,
							strTempOptionCodeToSort.length() - 1);

					java.util.List<String> values = sortAlphaNumeric(strTempOptionCodeToSort);

					strTemp = values.toString();

					strTemp = strTemp.substring(1, strTemp.length() - 1);

					strTemp = "{" + strTemp + "}";
					strSortedOptionCode = strSortedOptionCode + strTemp;

					i = i + icount;

				} else {
					strSortedOptionCode = strSortedOptionCode + strOptionCode.charAt(i);
				}
			}

		} catch (Exception ex) {
			ex.printStackTrace();
			throw ex;

		}
		return strSortedOptionCode;
	}

	/***
	 *
	 * @param strValue
	 * @return
	 * @throws Exception
	 */
	private java.util.List<String> sortAlphaNumeric(String strValue) throws Exception {
		String[] OptionCodeArray = strValue.split(",");

		java.util.List<String> values = new ArrayList<String>(Arrays.asList(OptionCodeArray));

		Comparator<String> com = (o1, o2) -> {
			return comparator(o1, o2);
		}; // lambda expression

		Collections.sort(values, com);

		return values;
	}

	/***
	 *
	 * @param s1
	 * @param s2
	 * @return
	 */
	private static int comparator(String s1, String s2) {
		String[] pt1 = s1.split("((?<=[a-z])(?=[0-9]))|((?<=[0-9])(?=[a-z]))");
		String[] pt2 = s2.split("((?<=[a-z])(?=[0-9]))|((?<=[0-9])(?=[a-z]))");
		// pt1 and pt2 arrays will have the string split in alphabets and numbers

		int i = 0;
		if (Arrays.equals(pt1, pt2))
			return 0;
		else {
			for (i = 0; i < Math.min(pt1.length, pt2.length); i++)
				if (!pt1[i].equals(pt2[i])) {
					if (!isNumber(pt1[i], pt2[i])) {
						if (pt1[i].compareTo(pt2[i]) > 0)
							return 1;
						else
							return -1;
					} else {
						int nu1 = Integer.parseInt(pt1[i]);
						int nu2 = Integer.parseInt(pt2[i]);
						if (nu1 > nu2)
							return 1;
						else
							return -1;
					}
				}
		}

		if (pt1.length > i)
			return 1;
		else
			return -1;
	}

	/***
	 *
	 * @param n1
	 * @param n2
	 * @return
	 */
	private static Boolean isNumber(String n1, String n2) {
		try {
			int nu1 = Integer.parseInt(n1);
			int nu2 = Integer.parseInt(n2);
			return true;
		} catch (Exception x) {
			return false;
		}
	}

	/**
	 * This Method is to Set the Change Action Completion Date
	 *
	 * @param context
	 * @return CA_RELEASED_DATE : Change Action Completion Date
	 * @throws Exception
	 */
	private void setCAStartDate() throws Exception {
		try {
			String strCAStartDate = null;
			String formatDate = null;
			String changeActionName = getChangeActionJson().getJsonObject("changeaction").getString("name");
			String changeActionApplicabilityTxt = getChangeActionJson().getJsonObject("changeaction")
					.getJsonObject("applicability").getString("expressionTXT");
			if (UIUtil.isNotNullAndNotEmpty(changeActionApplicabilityTxt)) {
				String changeActionApplicability = changeActionApplicabilityTxt.substring(
						changeActionApplicabilityTxt.indexOf("[") + 1, changeActionApplicabilityTxt.indexOf("]"));
				logger.writeLog("changeActionApplicability :" + changeActionApplicability);
				String[] changeActionApplicabilityDate = changeActionApplicability.split(" - ");
				if (changeActionApplicabilityDate.length >= 2) {
					strCAStartDate = changeActionApplicabilityDate[0];
					if (strCAStartDate.length() >= 19)
						strCAStartDate = strCAStartDate.substring(0, strCAStartDate.length() - 9);

					SimpleDateFormat format1 = new SimpleDateFormat("yyyy-MM-dd");
					SimpleDateFormat format2 = new SimpleDateFormat("MM-dd-yyyy");
					if (UIUtil.isNotNullAndNotEmpty(strCAStartDate)) {
						Date date = format1.parse(strCAStartDate);
						formatDate = format2.format(date);
					}
				}
			}
			if (UIUtil.isNotNullAndNotEmpty(formatDate))
				CA_START_DATE = formatDate;

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * This Method is to Get the Change Action Completion Date
	 *
	 * @param context
	 * @return CA_RELEASED_DATE : Change Action Completion Date
	 * @throws Exception
	 */
	private static String getCAStartDate() throws Exception {
		return CA_START_DATE;
	}

	/**
	 * This Method is to Initialized the logger
	 *
	 * @throws Exception
	 */
	private void loggerInitialization(Context context, LCDSAPIntegration3DExpConstants lcdSAPInteg3DExpConstants) throws Exception {
		try {
			language = context.getSession().getLanguage();

			String strCompleteLogger = EnoviaResourceBundle.getProperty(context, lcdSAPInteg3DExpConstants.LCD_3DX_SAP_INTEGRATION_KEY,
					"LCD_3DXSAPStringResource_en.logger.CompleteLogger", language);
			String strJSONLogger = EnoviaResourceBundle.getProperty(context, lcdSAPInteg3DExpConstants.LCD_3DX_SAP_INTEGRATION_KEY,
					"LCD_3DXSAPStringResource_en.logger.JSONLogger", language);
			// String strDateFormat = EnoviaResourceBundle.getProperty(context,
			// LCD_3DX_SAP_INTEGRATION_KEY,
			// "LCD_3DXSAPStringResource_en.logger.DateFormat", getLanguage());
			String strExtension = EnoviaResourceBundle.getProperty(context, lcdSAPInteg3DExpConstants.LCD_3DX_SAP_INTEGRATION_KEY,
					"LCD_3DXSAPStringResource_en.logger.Extension", language);
			String strUnderscore = EnoviaResourceBundle.getProperty(context, lcdSAPInteg3DExpConstants.LCD_3DX_SAP_INTEGRATION_KEY,
					"LCD_3DXSAPStringResource_en.logger.UnderScore", language);

			// STEP : Log Creation
			SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yy_hh_mm_ss");
			String strCurrentDate = dateFormat.format(new Date());
			loggerDebug.info("dateFormat<<<<<<<<< " + dateFormat);

			StringBuilder strCompleteLogName = new StringBuilder();
			strCompleteLogName.append(strCompleteLogger);
			strCompleteLogName.append(strUnderscore);
			strCompleteLogName.append(strCurrentDate);
			strCompleteLogName.append(strExtension);

			StringBuilder strJSONLogName = new StringBuilder();
			strJSONLogName.append(strJSONLogger);
			strJSONLogName.append(strUnderscore);
			strJSONLogName.append(strCurrentDate);
			strJSONLogName.append(strExtension);

			String strLogPath = EnoviaResourceBundle.getProperty(context, lcdSAPInteg3DExpConstants.LCD_3DX_SAP_INTEGRATION_KEY,
					"LCD_3DXSAPStringResource_en.logger.logPath", language);
			File tmpDir = new File(strLogPath);
			boolean exists = tmpDir.exists();

			loggerDebug.info("strLogPath 123<<<<<<<<< " + strLogPath);
			if (!exists) {
				tmpDir.mkdirs();
			}
			// System.out.println("getChangeActionJson() <<<<<<<<< " +
			// getChangeActionJson());
			// System.out.println("getChangeActionJson().getJsonObject(\"changeaction\")
			// <<<<<<<<< "
			// + getChangeActionJson().getJsonObject("changeaction"));

			// String changeActionName =
			// getChangeActionJson().getJsonObject("changeaction").getString("name");
			// System.out.println("changeActionName <<<<<<<<< " + changeActionName);
			logger = new SimpleLogger(strLogPath + strUnderscore + strCompleteLogName);
			loggerDebug.info("logger <<<<<<<<< " + logger);
			jsonLogger = new SimpleLogger(strLogPath + strUnderscore + strJSONLogName);

			loggerDebug.info("jsonLogger <<<<<<<<< " + jsonLogger);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static class SimpleLogger {
		private File logFile;
		private PrintWriter pr;
		private boolean console = true;

		public SimpleLogger(String logPath) throws IOException {
			logFile = new File(logPath);
			new File(logFile.getParent()).mkdirs();
			logFile.createNewFile();
			pr = new PrintWriter(new FileWriter(logFile));
		}

		public SimpleLogger(String logPath, boolean console) throws IOException {
			logFile = new File(logPath);
			new File(logFile.getParent()).mkdirs();
			logFile.createNewFile();
			pr = new PrintWriter(new FileWriter(logFile));
			this.console = console;
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
