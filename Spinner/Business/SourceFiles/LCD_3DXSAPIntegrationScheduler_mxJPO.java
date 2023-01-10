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
import java.util.List;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import com.aspose.diagram.sc;
import com.dassault_systemes.enovia.changeaction.impl.ChangeAction;
import com.dassault_systemes.enovia.changeaction.servicesimpl.ChangeActionServices;
import com.dassault_systemes.enovia.changeaction.webservice.services.ChangeActionJsonUtilities;
import com.dassault_systemes.enovia.changeaction.webservice.services.ChangeActionJsonUtilities.ChangeActionFacets;
import com.dassault_systemes.enovia.enterprisechangemgt.util.ChangeUtil;
import com.dassault_systemes.plm.config.exposed.factory.ConfigurationExposedFilterablesFactory;
import com.dassault_systemes.plm.config.exposed.interfaces.IConfigurationExposedFilterables;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.DomainRelationship;
import com.matrixone.apps.domain.util.ContextUtil;
import com.matrixone.apps.domain.util.EnoviaResourceBundle;
import com.matrixone.apps.domain.util.FrameworkException;
import com.matrixone.apps.domain.util.FrameworkUtil;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.MqlUtil;
import com.matrixone.apps.domain.util.PropertyUtil;
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
import matrix.db.JPO;
import matrix.util.MatrixException;
import matrix.util.Pattern;
import matrix.util.StringList;

public class LCD_3DXSAPIntegrationScheduler_mxJPO extends LCD_Constants_mxJPO {

	private static final Base64.Encoder BASE64_ENCODER = Base64.getEncoder();

	public static final String LCD_3DX_SAP_INTEGRATION_KEY = "LCD_3DXSAPStringResource_en";

	public static final String TYPE_LCD_BOM_ANCHOR_OBJECT = "LCD_BOMAnchorObject";
	public static final String NAME_LCD_ANCHOR_OBJECT = "LCD_AnchorObject";
	public static final String REV_LCD_ANCHOR_OBJECT = "A";
	public static final String POLICY_LCD_3DX_SAP_INTEGRATION = "LCD_3DXSAPIntegration";
	public static final String REL_LCD_SAP_BOM_INTERFACE = "LCD_SAPBOMInterface";
	public static final String VAULT_ESERVICE_PRODUCTION = "eService Production";

	public static final String ATTR_LCD_PROCESS_STATUS_FLAG = "LCD_ProcessStatusFlag";
	public static final String ATTR_LCD_REASON_FOR_FAILURE = "LCD_ReasonforFailure";
	public static final String ATTR_LCD_CAID = "LCD_CAID";
	public static final String ATTR_MODIFIED = "modified";

	public static final String STATUS_COMPLETE = "Complete";
	public static final String STATUS_IN_WORK = "In Work";
	public static final String STATUS_FAILED = "Failed";
	public static final String STATUS_WAITING = "Waiting";

	private static final String MSG_JSON_FORMAT_VALIDATION_FAILED = "JSON Format Validation Failed";
	private static final String MSG_JSON_FORMAT_VALIDATION_COMPLETED = "JSON Format Validation Completed";

	public static final String KEY_CONNECTION_ID = "ConnectionID";
	public static final String KEY_BOM_COMPONENT_ID = "BOMComponentID";
	public static final String KEY_BOM_NAME = "BOMName";
	public static final String KEY_CA_ID = "CAID";
	public static final long TIME_CONVERSION = 86400000;
	public static final long DAYS_IN_YEAR = 365;
	public static final String MSG_NETWORK_FAILURE = "Network Failure";

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
	private static boolean subContractStatus;
	private static String subContractRelId;
	private static String CA_START_DATE;
	private static List<String> LIST_REALIZED_CHANGES;

	private static HashMap<String, JsonObjectBuilder> changeActionMap = new HashMap<>();

	/**
	 * Main Method.
	 * 
	 * @param context
	 * @return
	 * @throws Exception
	 */
	public void scheduledPushToSAP(Context context, String[] strArgs) throws Exception {

		boolean isPresent = checkAncObjPresent(context);
		if (isPresent) {
			MapList bomComponentsMapList = findRelevantObjectsConnectedToAnchorObject(context);
			pushToSAP(context, bomComponentsMapList);
		} else {
			DomainObject domObjBomAncOcj = DomainObject.newInstance(context, TYPE_LCD_BOM_ANCHOR_OBJECT);
			System.out.println("Anchor Object created." + domObjBomAncOcj);
			domObjBomAncOcj.createObject(context, TYPE_LCD_BOM_ANCHOR_OBJECT, NAME_LCD_ANCHOR_OBJECT,
					REV_LCD_ANCHOR_OBJECT, POLICY_LCD_3DX_SAP_INTEGRATION, VAULT_ESERVICE_PRODUCTION);
			System.out.println("Anchor Object created -> " + domObjBomAncOcj);
		}
	}

	/**
	 * This Method is check if Anchor Object is present.
	 * 
	 * @param context
	 * @return
	 * @throws Exception
	 */
	private boolean checkAncObjPresent(Context context) throws MatrixException {
		boolean isPresent;

		BusinessObject busObjAchor = new BusinessObject(TYPE_LCD_BOM_ANCHOR_OBJECT, NAME_LCD_ANCHOR_OBJECT,
				REV_LCD_ANCHOR_OBJECT, VAULT_ESERVICE_PRODUCTION);

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
	private void pushToSAP(Context context, MapList bomComponentsMapList) throws Exception {

		Iterator<?> iterMAsMaplist = bomComponentsMapList.iterator();
		int intResponseCode = 0;
		try {
			loggerInitialization(context);
            callGETService(context);

			while (iterMAsMaplist.hasNext()) {
				Map<?, ?> item = (Map<?, ?>) iterMAsMaplist.next();
				String strConnectionId = (String) (item.get(DomainRelationship.SELECT_ID));
				String strBOMComponentId = (String) (item.get(DomainConstants.SELECT_ID));
				String strBOMComponentType = (String) (item.get(DomainConstants.SELECT_TYPE));
				String strbomModified = (String) (item.get(ATTR_MODIFIED));

				DomainRelationship domRelBOMComponents = DomainRelationship.newInstance(context, strConnectionId);
				String strProcessStatusFlag = domRelBOMComponents.getAttributeValue(context, ATTR_LCD_PROCESS_STATUS_FLAG);
				String strCAID = domRelBOMComponents.getAttributeValue(context, ATTR_LCD_CAID);

				if (strProcessStatusFlag.equalsIgnoreCase(STATUS_WAITING)) {
					if (TYPE_MANUFACTURINGASSEMBLY.equalsIgnoreCase(strBOMComponentType)) {
						intResponseCode = sendManAssToSAP(context, strBOMComponentId, strBOMComponentType, strCAID);
						processWebServiceResponseForManufacturingAssembly(context, intResponseCode, strConnectionId);

					} else {
						if (UIUtil.isNotNullAndNotEmpty(strBOMComponentId))
						{
							Map<?, ?> mCadPartDetails = getCADPartDetails(context, strBOMComponentId);
							String strProcurementIntent = (String) mCadPartDetails.get(ATTR__PROCUREMENTINTENT_VPMREFERENCE);
							if (SUBCONTRACT.equalsIgnoreCase(strProcurementIntent)) {
								intResponseCode = processSubContractPart(context, mCadPartDetails, strConnectionId);
								processWebServiceResponseForSubContract(context, intResponseCode, strConnectionId);
							}
						}
					}
				} else if (strProcessStatusFlag.equalsIgnoreCase(STATUS_COMPLETE)) {
					disconnectExpiredObjectsFromAnchorObject(context, strConnectionId, strbomModified);
				}
			}
		} catch (Exception e) {
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
	private MapList findRelevantObjectsConnectedToAnchorObject(Context context) throws Exception {

		BusinessObject busObjAchor = new BusinessObject(TYPE_LCD_BOM_ANCHOR_OBJECT, // String Type
				NAME_LCD_ANCHOR_OBJECT, // String Name
				REV_LCD_ANCHOR_OBJECT, // String Revision
				VAULT_ESERVICE_PRODUCTION); // String Vault

		DomainObject domObj = DomainObject.newInstance(context, busObjAchor);

		StringList slObjectSelect = new StringList();
		slObjectSelect.add(DomainConstants.SELECT_ID);
		slObjectSelect.add(DomainConstants.SELECT_TYPE);

		StringList slRelSelect = new StringList();
		slRelSelect.add(DomainRelationship.SELECT_ID);
		slRelSelect.add(ATTR_MODIFIED);

		return (domObj.getRelatedObjects(context, // context
				REL_LCD_SAP_BOM_INTERFACE, // Relationship Pattern
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
	 * This Method is called by webService To RePush Failed BOM Components to SAP.
	 * 
	 * @param context
	 * @param args
	 * @throws Exception
	 */
	public String rePushFailedBomComponentsToSAP(Context context, String[] args) throws Exception {

		HashMap<?, ?> programMap = (HashMap<?, ?>) JPO.unpackArgs(args);
		int intResponseCode = 0;
		String strResponse = "";
		try {
			loggerInitialization(context);

			intResponseCode = callGETService(context);

			if (intResponseCode == HttpStatus.SC_OK) {
				String strConnectionId = (String) programMap.get(KEY_CONNECTION_ID);
				String strBOMComponentId = (String) programMap.get(KEY_BOM_COMPONENT_ID);
				String strCAID = (String) programMap.get(KEY_CA_ID);

				DomainObject domObj = DomainObject.newInstance(context, strBOMComponentId);
				StringList slObjectSelect = new StringList();
				slObjectSelect.add(DomainConstants.SELECT_TYPE);

				Map<?, ?> bomMap = domObj.getInfo(context, slObjectSelect);
				String strBOMComponentType = (String) bomMap.get(DomainConstants.SELECT_TYPE);

				if (TYPE_MANUFACTURINGASSEMBLY.equalsIgnoreCase(strBOMComponentType)) {
					intResponseCode = sendManAssToSAP(context, strBOMComponentId, strBOMComponentType, strCAID);
					strResponse = strResponse.valueOf(intResponseCode);
				} else {
					
					if (UIUtil.isNotNullAndNotEmpty(strBOMComponentId))
					{
						Map<?, ?> mCadPartDetails = getCADPartDetails(context, strBOMComponentId);
						String strProcurementIntent = (String) mCadPartDetails.get(ATTR__PROCUREMENTINTENT_VPMREFERENCE);
						if (SUBCONTRACT.equalsIgnoreCase(strProcurementIntent)) {
							intResponseCode = processSubContractPart(context, mCadPartDetails, strConnectionId);
							strResponse = strResponse.valueOf(intResponseCode);
						}
					}
				}
			} else {
				strResponse = MSG_NETWORK_FAILURE;
			}
			// STEP : Retrieving Input Arguments
		}catch (Exception e) {
			e.printStackTrace();
		}
		return strResponse;
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
		long differenceInDays = (differenceInTime / TIME_CONVERSION) % DAYS_IN_YEAR;

		if (differenceInDays > 7) {
			DomainRelationship.disconnect(context, strConnectionId);
		}
	}

	private int sendManAssToSAP(Context context, String strBOMComponentId, String strBOMComponentType, String caId)
			throws Exception {

		String strRequestString = null;
		int intResponseCode = 0;

		try {
			logger.writeLog("sendToSAP calledd <<<<<<<<< ");

			// STEP :Get input Arguments
			ChangeAction changeAction = ChangeActionServices.getChangeAction(context, caId);
			if (null != changeAction) {
				// STEP : Using Service API getting JSON object for Change Action
				ChangeActionFacets changeActionFacets = new ChangeActionFacets();
				changeActionFacets.attributes = true;
				changeActionFacets.realized = true;
				changeActionFacets.applicability = true;
				String changeActionJsonDetails = ChangeActionJsonUtilities.changeActionToJson(context, changeAction,changeActionFacets);

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

						// STEP : Invoking SAP Web service using GET request method to get the x-csrf-token and cookies
						if (UIUtil.isNotNullAndNotEmpty(xcsrfToken) && UIUtil.isNotNullAndNotEmpty(cookie)) {
							// STEP : Adding Change Action Header in JSON

							JsonObjectBuilder jEachPayloadBuilder = Json.createObjectBuilder();

							if (changeActionMap.containsKey(caId)) {
								jEachPayloadBuilder = changeActionMap.get(caId);
							} else {
								jEachPayloadBuilder = ChangeActionJSONPayload(context, caId);
								changeActionMap.put(caId, jEachPayloadBuilder);
							}

							if (null != jEachPayloadBuilder) {

								// STEP : Traversing Change Action to get the realized items
								JsonObject realizedItem;
								JsonArray realizedItems = getChangeActionJson().getJsonObject("changeaction").getJsonArray("realized");
								String strObjProcIntent;
								String strHasConfig;
								String strObjTitle;
								String strSAPMBOMUpdatedOn;
								String strSAPUniqueID;
								DomainObject domRealizedItemObj;
								Map<?, ?> mobjectDetails = null;
								JsonObjectBuilder jHeaderPartObjectBuilder;
								JsonObject jEachPayloadObj;
								String realizedItemid;

								if (UIUtil.isNotNullAndNotEmpty(strBOMComponentId)) {
									// STEP : Retrieving realized item's object details
									domRealizedItemObj = DomainObject.newInstance(context, strBOMComponentId);
									StringList objectSelects = new StringList();
									objectSelects.add(DomainConstants.SELECT_ID);
									objectSelects.add(DomainConstants.SELECT_NAME);
									objectSelects.add(DomainConstants.SELECT_TYPE);
									objectSelects.add(ATTR_V_NAME);
									objectSelects.add(ATTR_V_DESCRIPTION);
									objectSelects.add(ATTR__PROCUREMENTINTENT_MANUFACTURINGASSEMBLY);
									objectSelects.add(ATTR__PLANTCODE_MANUFACTURINGASSEMBLY);
									objectSelects.add(ATTR__SERVICEABLEITEM_MANUFACTURINGASSEMBLY);
									objectSelects.add(ATTR__PARTINTERCHANGEABILITY_MANUFACTURINGASSEMBLY);
									objectSelects.add(ATTR__HASCONFIGCONTEXT_VPMREFERENCE);
									objectSelects.add(ATTR__SAPMBOMUPDATEDON_MANUFACTURINGASSEMBLY);
									objectSelects.add(ATTR__SAPUNIQUEID_MANUFACTURINGASSEMBLY);

									mobjectDetails = domRealizedItemObj.getInfo(context, objectSelects);
									realizedItemid = (String) mobjectDetails.get(DomainConstants.SELECT_ID);
									strObjProcIntent = (String) mobjectDetails.get(ATTR__PROCUREMENTINTENT_MANUFACTURINGASSEMBLY);
									strHasConfig = (String) mobjectDetails.get(ATTR__HASCONFIGCONTEXT_VPMREFERENCE);
									strObjTitle = (String) mobjectDetails.get(ATTR_V_NAME);

									// STEP : Check the realized item is Discrete Make Manufacturing Assembly
									if (TYPE_MANUFACTURINGASSEMBLY.equalsIgnoreCase(strBOMComponentType)){
										if(FALSE.equalsIgnoreCase(strHasConfig)){
											logger.writeLog("INFO ::Realized Item << " + strObjTitle + " >> is with Procurement Intent : << " + strObjProcIntent + " >> ");
											setConfiguredFlag(false);
										}
										else if(TRUE.equalsIgnoreCase(strHasConfig)){
											logger.writeLog("INFO ::configured Realized Item is << " + strObjTitle + " >> is with Procurement Intent : << " + strObjProcIntent + " >> ");
											setConfiguredFlag(true);
											realizedItem = realizedItemJson(context, realizedItems, strBOMComponentId);
											setCAStartDate();
											if(null != realizedItem) {
												setRealizedItemList(context ,realizedItem);
											}
										}

										// STEP : Adding Manufacturing Assembly Header in JSON
										logger.writeLog("Processing Realized Item << " + strObjTitle + " >> Procurement Intent : << " + strObjProcIntent + " >> ");
										jHeaderPartObjectBuilder = addRealizedItemHeader(context, mobjectDetails);
										if (null != jHeaderPartObjectBuilder) 
										{
											jEachPayloadBuilder.add(HEADER_PART, jHeaderPartObjectBuilder.build());

											// STEP : Invoking SAP WebService by attaching JSON
											jEachPayloadObj = jEachPayloadBuilder.build();
											strRequestString = jEachPayloadObj.toString();
											jsonLogger.writeLog("JSON Payload Request for : <<< " + strObjTitle + " : >>> \n " + strRequestString + " \n ");
											logger.writeLog("JSON Payload Request for  : <<< " + strObjTitle + " : >>> \n " + strRequestString + " \n ");

											if (UIUtil.isNotNullAndNotEmpty(jEachPayloadObj.toString())) {
												intResponseCode = callPostService(context, jEachPayloadObj);
												jsonLogger.writeLog("Response for  : <<< " + strObjTitle+ " :  Payload from SAP WebService >>> \n "+ intResponseCode + " \n ");

											} else
												logger.writeLog("ERROR :: Failed to generate JSON Payload for realized item  : << "+ strObjTitle + ">> header for Change Action : << "+ changeActionName);

										} else
											logger.writeLog("ERROR :: Failed to add realized item  : << "
													+ strObjTitle + ">> header for Change Action : << "
													+ changeActionName);
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
						} else {
							intResponseCode = 500;
							logger.writeLog("ERROR : Failed to get x-csrf-token and cookies from SAP Webservice ");
						}
					} else
						logger.writeLog("ERROR : Failed to get change action JSON object using Service API");
				} else
					logger.writeLog("ERROR : Failed to get change action JSON using Service API");
			} else
				logger.writeLog("ERROR : Failed to input arguments");
		} catch (Exception e) {
			e.printStackTrace();
		} 
		return intResponseCode;
	}

	private JsonObject realizedItemJson(Context context, JsonArray realizedItems, String strBOMComponentId)
			throws FrameworkException {
		JsonObject joRealizedItem = null;
		for (int i = 0; i < realizedItems.size(); i++) {
			JsonObject joItem = realizedItems.getJsonObject(i);
			String realizedItemPhyId = joItem.getJsonObject("where").get("id").toString().split(":")[1].replace("\"", "");

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
	private JsonObjectBuilder ChangeActionJSONPayload(Context context, String caId) throws Exception {
		logger.writeLog("addChangeActionHeader...Start");

		// STEP : Creating JSON object Builder to add the change action object details
		JsonObjectBuilder jEachPayloadBuilder = Json.createObjectBuilder();
		try {

			if (UIUtil.isNullOrEmpty(caId)) {
				// STEP : Adding Change Action object details in JSONObjectBuilder

				jEachPayloadBuilder.add(TAG_TITLE, NOT_APPLICABLE);

				jEachPayloadBuilder.add(TAG_OID, NOT_APPLICABLE);

				jEachPayloadBuilder.add(TAG_NAME, NOT_APPLICABLE);

				jEachPayloadBuilder.add(TAG_DESCRIPTION, NOT_APPLICABLE);

				jEachPayloadBuilder.add(TAG_CATEGORY_OF_CHANGE, NOT_APPLICABLE);

				jEachPayloadBuilder.add(TAG_CHANGEDOMAIN, NOT_APPLICABLE);

				jEachPayloadBuilder.add(TAG_CHANGETYPE, NOT_APPLICABLE);

				jEachPayloadBuilder.add(TAG_REASONFORCHANGE, NOT_APPLICABLE);

				jEachPayloadBuilder.add(TAG_PLATFORM, NOT_APPLICABLE);
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
					logger.writeLog("Change Action Name  << " + changeActionName + " >> Change Action Description : << " + changeActionDescription + " >>");
				}

				// STEP : Retrieving Change Action Details from Input Change Action JSON
				JsonArray caAttributes = changeActionObject.getJsonArray("attributes");
				String attrName;
				String attrValue;
				JsonObject caAttribute;
				if (caAttributes != null) {
					for (int i = 0; i < caAttributes.size(); i++) {
						caAttribute = caAttributes.getJsonObject(i);
						attrName = caAttribute.getString("name");
						if (TITLE_CHANGEACTION.equalsIgnoreCase(attrName) || CHANGETYPE_CHANGEACTION.equalsIgnoreCase(attrName) || CHANGEDOMAIN_CHANGEACTION.equalsIgnoreCase(attrName)
								|| PLATFORM_CHANGEACTION.equalsIgnoreCase(attrName) || REASONFORCHANGE_CHANGEACTION.equalsIgnoreCase(attrName) || CATEGORYOFCHANGE_CHANGEACTION.equalsIgnoreCase(attrName)
								|| RELEASEDDATE_CHANGEACTION.equalsIgnoreCase(attrName))  {
							attrValue = caAttribute.getString("value");
							if (UIUtil.isNotNullAndNotEmpty(attrValue))
								mcaDetails.put(attrName, attrValue);
							else
								mcaDetails.put(attrName, " ");
						}
					}
				}
				String strCACompletionDate = mcaDetails.get(RELEASEDDATE_CHANGEACTION);
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
				if (UIUtil.isNotNullAndNotEmpty(mcaDetails.get(TITLE_CHANGEACTION)))
					jEachPayloadBuilder.add(TAG_TITLE, mcaDetails.get(TITLE_CHANGEACTION));

				if (UIUtil.isNotNullAndNotEmpty(changeActionObjId))
					jEachPayloadBuilder.add(TAG_OID, changeActionObjId);
				else
					jEachPayloadBuilder.add(TAG_OID, " ");

				if (UIUtil.isNotNullAndNotEmpty(changeActionName))
					jEachPayloadBuilder.add(TAG_NAME, changeActionName);
				else
					jEachPayloadBuilder.add(TAG_NAME, " ");

				if (UIUtil.isNotNullAndNotEmpty(changeActionDescription))
					jEachPayloadBuilder.add(TAG_DESCRIPTION, changeActionDescription);
				else
					jEachPayloadBuilder.add(TAG_DESCRIPTION, " ");

				if (UIUtil.isNotNullAndNotEmpty(mcaDetails.get(CATEGORYOFCHANGE_CHANGEACTION)))
					jEachPayloadBuilder.add(TAG_CATEGORY_OF_CHANGE, mcaDetails.get(CATEGORYOFCHANGE_CHANGEACTION));

				if (UIUtil.isNotNullAndNotEmpty(mcaDetails.get(CHANGEDOMAIN_CHANGEACTION)))
					jEachPayloadBuilder.add(TAG_CHANGEDOMAIN, mcaDetails.get(CHANGEDOMAIN_CHANGEACTION));

				if (UIUtil.isNotNullAndNotEmpty(mcaDetails.get(CHANGETYPE_CHANGEACTION)))
					jEachPayloadBuilder.add(TAG_CHANGETYPE, mcaDetails.get(CHANGETYPE_CHANGEACTION));

				if (UIUtil.isNotNullAndNotEmpty(mcaDetails.get(REASONFORCHANGE_CHANGEACTION)))
					jEachPayloadBuilder.add(TAG_REASONFORCHANGE, mcaDetails.get(REASONFORCHANGE_CHANGEACTION));

				if (UIUtil.isNotNullAndNotEmpty(mcaDetails.get(PLATFORM_CHANGEACTION)))
					jEachPayloadBuilder.add(TAG_PLATFORM, mcaDetails.get(PLATFORM_CHANGEACTION));

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
	private JsonObjectBuilder addRealizedItemHeader(Context context, Map mobjectDetails) throws Exception {
		logger.writeLog("addRealizedItemHeader...START");

		// STEP : Creating JSON object Builder to add the realized item details
		JsonObjectBuilder jHeaderPartObjectBuilder = Json.createObjectBuilder();
		try {
			// STEP : Retrieving realized item Details
			String strObjProcurementIntent = (String) mobjectDetails.get(ATTR__PROCUREMENTINTENT_MANUFACTURINGASSEMBLY);
			String strObjTitle = (String) mobjectDetails.get(ATTR_V_NAME);
			String strObjId = (String) mobjectDetails.get(DomainConstants.SELECT_ID);
			String strObjdescription = (String) mobjectDetails.get(ATTR_V_DESCRIPTION);
			String strName = (String) mobjectDetails.get(DomainConstants.SELECT_NAME);
			String strObjPlantCode = (String) mobjectDetails.get(ATTR__PLANTCODE_MANUFACTURINGASSEMBLY);
			String strObjServiceable = (String) mobjectDetails.get(ATTR__SERVICEABLEITEM_MANUFACTURINGASSEMBLY);
			String strObjPartInterchangeability = (String) mobjectDetails.get(ATTR__PARTINTERCHANGEABILITY_MANUFACTURINGASSEMBLY);

			// STEP :Adding realized item object details in JSONObjectBuilder
			if (UIUtil.isNotNullAndNotEmpty(strObjTitle))
				jHeaderPartObjectBuilder.add(TAG_TITLE, strObjTitle);
			else
				jHeaderPartObjectBuilder.add(TAG_TITLE, " ");

			if (UIUtil.isNotNullAndNotEmpty(strName))
				jHeaderPartObjectBuilder.add(TAG_NAME, strName);
			else
				jHeaderPartObjectBuilder.add(TAG_NAME, " ");

			if (UIUtil.isNotNullAndNotEmpty(strObjId))
				jHeaderPartObjectBuilder.add(TAG_OID, strObjId);
			else
				jHeaderPartObjectBuilder.add(TAG_OID, " ");

			jHeaderPartObjectBuilder.add(TAG_REL_ID, NOT_APPLICABLE);

			if (UIUtil.isNotNullAndNotEmpty(strObjdescription))
				jHeaderPartObjectBuilder.add(TAG_DESCRIPTION, strObjdescription);
			else
				jHeaderPartObjectBuilder.add(TAG_DESCRIPTION, " ");

			if (UIUtil.isNotNullAndNotEmpty(strObjProcurementIntent))
				jHeaderPartObjectBuilder.add(TAG_PROCUREMENTINTENT, strObjProcurementIntent);
			else
				jHeaderPartObjectBuilder.add(TAG_PROCUREMENTINTENT, " ");

			if (UIUtil.isNotNullAndNotEmpty(strObjServiceable))
				jHeaderPartObjectBuilder.add(TAG_SERVICEABLEITEM, strObjServiceable);
			else
				jHeaderPartObjectBuilder.add(TAG_SERVICEABLEITEM, " ");

			if (UIUtil.isNotNullAndNotEmpty(strObjPlantCode))
				jHeaderPartObjectBuilder.add(TAG_PLANTCODE, strObjPlantCode);
			else
				jHeaderPartObjectBuilder.add(TAG_PLANTCODE, " ");

			if (UIUtil.isNotNullAndNotEmpty(strObjPartInterchangeability))
				jHeaderPartObjectBuilder.add(TAG_PARTINTERCHANGEABILITY, strObjPartInterchangeability);
			else
				jHeaderPartObjectBuilder.add(TAG_PARTINTERCHANGEABILITY, " ");

			jHeaderPartObjectBuilder.add(TAG_VARIAENT_Effectivity, NOT_APPLICABLE);
			jHeaderPartObjectBuilder.add(TAG_DATEFROM, NOT_APPLICABLE);
			jHeaderPartObjectBuilder.add(TAG_DATETO, NOT_APPLICABLE);
			jHeaderPartObjectBuilder.add(TAG_REALIZED_DATA, true);

			// STEP : Adding child of Manufacturing Assembly Header in JSON
			if (UIUtil.isNotNullAndNotEmpty(strObjId))
				addRealizedItemChildren(context, strObjId, jHeaderPartObjectBuilder);

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
	private void addRealizedItemChildren(Context context, String strObjId, JsonObjectBuilder jHeaderPartObjectBuilder)
			throws Exception {
		logger.writeLog("getRealizedItemDetails : START !!");
		try {
			// STEP : Retrieving realized item child details
			MapList mlMBOMDetails = getMBOMDetails(context, strObjId);
			logger.writeLog("Number of Provided Part under  Make Manufacturing Assembly  :: " + mlMBOMDetails.size());
			JsonArrayBuilder jsonMBOMPartArrayBuilder = Json.createArrayBuilder();

			// STEP : Processing child of realized item
			Map mPartDetails;
			String sType;
			String strTitle;
			boolean isAssembly;
			for (int jdx = 0; jdx < mlMBOMDetails.size(); jdx++) {
				mPartDetails = (Map<?, ?>) mlMBOMDetails.get(jdx);
				sType = (String) mPartDetails.get(DomainConstants.SELECT_TYPE);
				strTitle = (String) mPartDetails.get(ATTR_V_NAME);

				JsonObjectBuilder jMBOMPartBuilder = Json.createObjectBuilder();
				if (sType.equalsIgnoreCase(TYPE_MANUFACTURINGASSEMBLY)) {
					// STEP : Adding Manufacturing Assembly child of Manufacturing Assembly Header in JSON
					isAssembly = true;
					addChildPartHeader(context, mPartDetails, jMBOMPartBuilder, isAssembly);
				} else {
					// STEP : Adding child part of Manufacturing Assembly Header in JSON
					isAssembly = false;
					addChildPartHeader(context, mPartDetails, jMBOMPartBuilder, isAssembly);
				}
				// STEP : adding child part JSON Object in JSON Array
				if (null != jMBOMPartBuilder) {
					logger.writeLog("JSON Header for child :: << " + strTitle + ">> <<"+ jMBOMPartBuilder.build().toString() + ">>>>>>>>>");
					jsonMBOMPartArrayBuilder.add(jMBOMPartBuilder.build());
				}
			}
			// STEP : adding child parts JSON Array in Header Part JSON object
			if (null != jsonMBOMPartArrayBuilder)
				jHeaderPartObjectBuilder.add(HEADER_CHILDREN, jsonMBOMPartArrayBuilder.build());
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
	private MapList getMBOMDetails(Context context, String strObjectId) throws Exception {
		logger.writeLog("getMBOMDetails : START!!");
		MapList mlMBOMList = null;
		try {
			// STEP : Defining BusinessObject Pattern and Relationship Pattern
			Pattern relPattern = new Pattern(REL_PROVIDE);
			relPattern.addPattern(REL_PROCESSINSTANCECONTINUOUS);
			Pattern typePattern = new Pattern(TYPE_PROVIDE);
			typePattern.addPattern(TYPE_MANUFACTURINGASSEMBLY);
			typePattern.addPattern(TYPE_FASTEN);
			typePattern.addPattern(TYPE_PROCESSINSTANCECONTINUOUS);

			// STEP : Defining BusinessObject Selectables
			StringList objectSelects = new StringList();
			objectSelects.add(ATTR_V_NAME);
			objectSelects.add(DomainConstants.SELECT_ID);
			objectSelects.add(ATTR_V_DESCRIPTION);
			objectSelects.add(DomainConstants.SELECT_NAME);
			objectSelects.add(ATTR__PROCUREMENTINTENT_MANUFACTURINGASSEMBLY);
			objectSelects.add(ATTR__PLANTCODE_MANUFACTURINGASSEMBLY);
			objectSelects.add(ATTR__SERVICEABLEITEM_MANUFACTURINGASSEMBLY);
			objectSelects.add(ATTR__PARTINTERCHANGEABILITY_MANUFACTURINGASSEMBLY);
			objectSelects.add(ATTR__HASCONFIGCONTEXT_VPMREFERENCE);

			// STEP : Defining Relationship Selectables
			StringList relSelects = new StringList();
			relSelects.add(DomainConstants.SELECT_RELATIONSHIP_ID);
			relSelects.add(ATTR__HASCONFIGEFFECTIVITY);
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

			mlMBOMList.sort(ATTR__TREEORDER_PLMINSTANCE, "ascending", "real");
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
			boolean isAssembly) throws Exception {
		logger.writeLog("addChildPartHeader : START !!");
		Map<?, ?> mLinkedCadPart = null;
		try {
			// STEP : Retrieving Child Part Details
			String strProcurementIntent = null;
			String strPartInterchangeability = null;
			String strServiceable = null;
			String strUnitOfMeasure = null;
			String strLinkedVPMReferenceId = null;
			String strObjectID = (String) mPartDetails.get(DomainConstants.SELECT_ID);
			String sName = (String) mPartDetails.get(DomainConstants.SELECT_NAME);
			String strTitle = (String) mPartDetails.get(ATTR_V_NAME);
			String strdescription = (String) mPartDetails.get(ATTR_V_DESCRIPTION);
			String strRelId = (String) mPartDetails.get(DomainConstants.SELECT_RELATIONSHIP_ID);

			String strRelPhyId = (String) mPartDetails.get(DomainConstants.SELECT_PHYSICAL_ID);
			String strhasConfigEffectivity = (String) mPartDetails.get(ATTR__HASCONFIGEFFECTIVITY);

			// STEP : If Child is Manufacturing Assembly ,Retrieving Procurement Intent
			// ,serviceable ,PartInterchangeability from Object details
			if (isAssembly) {
				strProcurementIntent = (String) mPartDetails.get(ATTR__PROCUREMENTINTENT_MANUFACTURINGASSEMBLY);
				strServiceable = (String) mPartDetails.get(ATTR__SERVICEABLEITEM_MANUFACTURINGASSEMBLY);
				strPartInterchangeability = (String) mPartDetails.get(ATTR__PARTINTERCHANGEABILITY_MANUFACTURINGASSEMBLY);
				strUnitOfMeasure = EACH;
			} else {
				if (null != strObjectID && !strObjectID.isEmpty()) {
					// STEP : If Child is other than Manufacturing Assembly ,Retrieving Procurement
					// Intent , serviceable ,PartInterchangeability from corresponding Linked CAD
					// PART
					// STEP : Retrieving Child Part Details from corresponding Linked CAD PART
					mLinkedCadPart = getLinkedCADPartFromMBOMPart(context, strObjectID, REL_PROVIDE);
					if (null != mLinkedCadPart) {
						strProcurementIntent = (String) mLinkedCadPart.get(ATTR__PROCUREMENTINTENT_VPMREFERENCE);
						strServiceable = (String) mLinkedCadPart.get(ATTR__SERVICEABLEITEM_VPMREFERENCE);
						strPartInterchangeability = (String) mLinkedCadPart.get(ATTR__PARTINTERCHANGEABILITY_VPMREFERENCE);
						strLinkedVPMReferenceId = (String) mLinkedCadPart.get(DomainConstants.SELECT_ID);
						strUnitOfMeasure = (String) mLinkedCadPart.get(ATTR__UNITOFMEASURE_VPMREFERENCE);
					}
				}
			}

			String strVariantEffectivity = "";
			String strEffectivityObject = "";
			String strDateEffectivity = "";
			String strCADReleaseDateIn = "";
			String strCADReleaseDateOut = "";

			if (TRUE.equals(strhasConfigEffectivity)) {
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

						if (strEffectivityObject.contains(EFFECTIVITY_VARIANT + ":")) {
							strVariantEffectivity = strEffectivityObject.substring(strEffectivityObject.indexOf(EFFECTIVITY_VARIANT + ":") + 20,strEffectivityObject.indexOf("ExpressionFormat") - 1);
							strVariantEffectivity = sortOptionCode(strVariantEffectivity);
						}
					}
				}
			}

			// STEP : Adding Child Part Details in JsonObjectBuilder
			if (UIUtil.isNotNullAndNotEmpty(strTitle))
				jMBOMPartBuilder.add(TAG_TITLE, strTitle);
			else
				jMBOMPartBuilder.add(TAG_TITLE, " ");

			if (UIUtil.isNotNullAndNotEmpty(sName))
				jMBOMPartBuilder.add(TAG_NAME, sName);
			else
				jMBOMPartBuilder.add(TAG_NAME, " ");

			if (UIUtil.isNotNullAndNotEmpty(strObjectID))
				jMBOMPartBuilder.add(TAG_OID, strObjectID);
			else
				jMBOMPartBuilder.add(TAG_OID, " ");

			if (UIUtil.isNotNullAndNotEmpty(strRelId))
				jMBOMPartBuilder.add(TAG_REL_ID, strRelId);
			else
				jMBOMPartBuilder.add(TAG_REL_ID, " ");

			if (UIUtil.isNotNullAndNotEmpty(strdescription))
				jMBOMPartBuilder.add(TAG_DESCRIPTION, strdescription);
			else
				jMBOMPartBuilder.add(TAG_DESCRIPTION, " ");

			if (UIUtil.isNotNullAndNotEmpty(strUnitOfMeasure))
				jMBOMPartBuilder.add(TAG_UNIT_OF_MEASURE, strUnitOfMeasure);
			else
				jMBOMPartBuilder.add(TAG_UNIT_OF_MEASURE, " ");

			if (UIUtil.isNotNullAndNotEmpty(strProcurementIntent))
				jMBOMPartBuilder.add(TAG_PROCUREMENTINTENT, strProcurementIntent);
			else
				jMBOMPartBuilder.add(TAG_PROCUREMENTINTENT, " ");

			if (UIUtil.isNotNullAndNotEmpty(strServiceable))
				jMBOMPartBuilder.add(TAG_SERVICEABLEITEM, strServiceable);
			else
				jMBOMPartBuilder.add(TAG_SERVICEABLEITEM, " ");

			if (UIUtil.isNotNullAndNotEmpty(strPartInterchangeability))
				jMBOMPartBuilder.add(TAG_PARTINTERCHANGEABILITY, strPartInterchangeability);
			else
				jMBOMPartBuilder.add(TAG_PARTINTERCHANGEABILITY, " ");

			if (UIUtil.isNotNullAndNotEmpty(strVariantEffectivity))
				jMBOMPartBuilder.add(TAG_VARIAENT_Effectivity, strVariantEffectivity);
			else
				jMBOMPartBuilder.add(TAG_VARIAENT_Effectivity, " ");
			if(getConfiguredFlag())
			{
				//False Effectivity Case :: In JSON { "Start Date" : Start date of CA and "End Date" = Start date of CA }
				if(getFalseEffectivityFlag()) {
					jMBOMPartBuilder.add(TAG_DATEFROM, getCAStartDate());
					jMBOMPartBuilder.add(TAG_DATETO, getCAStartDate());
				}
				else 
				{
					SimpleDateFormat format1 = new SimpleDateFormat("yyyy-MM-dd");
					SimpleDateFormat format2 = new SimpleDateFormat("MM-dd-yyyy");
					if (UIUtil.isNotNullAndNotEmpty(strCADReleaseDateIn)){
						Date date = format1.parse(strCADReleaseDateIn);
						String formatDate = format2.format(date);
						jMBOMPartBuilder.add(TAG_DATEFROM,formatDate);
					}
					else
						jMBOMPartBuilder.add(TAG_DATEFROM," ");

					if (UIUtil.isNotNullAndNotEmpty(strCADReleaseDateOut)){

						if(strCADReleaseDateOut.equals("INF")){
							strCADReleaseDateOut = "12-31-9999";
							jMBOMPartBuilder.add(TAG_DATETO,strCADReleaseDateOut);
						}
						else{
							// Single day active in PLM case ::: In JSON { "Start Date": start date of effectivity  and "End Date" = next day date }
							if(strCADReleaseDateOut.equals(strCADReleaseDateIn)) {
								String dt = strCADReleaseDateOut;  // Start date
								SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
								Calendar c = Calendar.getInstance();
								c.setTime(sdf.parse(dt));
								c.add(Calendar.DATE, 1);  // number of days to add
								dt = sdf.format(c.getTime());  // dt is now the new date
								Date date = format1.parse(dt);
								String formatDate = format2.format(date);
								jMBOMPartBuilder.add(TAG_DATETO,formatDate);

							}
							else{
								Date date = format1.parse(strCADReleaseDateOut);
								String formatDate = format2.format(date);
								jMBOMPartBuilder.add(TAG_DATETO,formatDate);
							}
						}
					}
					else
						jMBOMPartBuilder.add(TAG_DATETO," ");
				}
				List<String> lstRealizedChanges = new ArrayList<String>();
				lstRealizedChanges = getRealizedItemList();
				if(lstRealizedChanges.size() != 0){
					if(lstRealizedChanges.contains(strRelPhyId))
						jMBOMPartBuilder.add(TAG_REALIZED_DATA, true);
					else
						jMBOMPartBuilder.add(TAG_REALIZED_DATA, false);
				}
			} else {
				if (UIUtil.isNotNullAndNotEmpty(getCACompletionDate()))
					jMBOMPartBuilder.add(TAG_DATEFROM, getCACompletionDate());
				else
					jMBOMPartBuilder.add(TAG_DATEFROM, "");

				jMBOMPartBuilder.add(TAG_DATETO, "12-31-9999");
				jMBOMPartBuilder.add(TAG_REALIZED_DATA, true);
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
			
			//ContextUtil.pushContext(context,ROLE_ADMIN,DomainConstants.EMPTY_STRING,DomainConstants.EMPTY_STRING);
			// ContextUtil.pushContext(context);
			ConfigurationExposedFilterablesFactory configurationExposedFilterablesactory = new ConfigurationExposedFilterablesFactory();
			IConfigurationExposedFilterables iConfigurationExposedFilterables = configurationExposedFilterablesactory
					.getIPublicConfigurationFilterablesServices();
			List<String> objects = new ArrayList<>();
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
	private Map<?, ?> getCADPartDetails(Context context, String strOBJId) throws Exception {
		logger.writeLog("getCADPartDetails: START!!");
		Map<?, ?> objInfoMap = new HashMap<>();
		try {
			if (null != strOBJId && !strOBJId.isEmpty()) {
				// Object Selectables
				StringList objectSelects = new StringList();
				objectSelects.add(DomainConstants.SELECT_ID);
				objectSelects.add(DomainConstants.SELECT_NAME);
				objectSelects.add(ATTR_V_NAME);
				objectSelects.add(ATTR_V_DESCRIPTION);
				objectSelects.add(ATTR__SERVICEABLEITEM_VPMREFERENCE); // "Serviceable Item";
				objectSelects.add(ATTR__PROCUREMENTINTENT_VPMREFERENCE); // "Procurement Intent"
				objectSelects.add(ATTR__PARTINTERCHANGEABILITY_VPMREFERENCE); // "Part Interchangeability ";
				objectSelects.add(ATTR__UNITOFMEASURE_VPMREFERENCE); // "Unit of Measure";

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
	private Map getLinkedCADPartFromMBOMPart(Context context, String strOBJId, String strRel) throws Exception {
		logger.writeLog("getLinkedCADPartFromMBOMPart: START!!");
		Map<String, String> objInfoMap = null;
		try {
			if (null != strOBJId && !strOBJId.isEmpty()) {
				// Object Selectables
				StringList objectSelects = new StringList();
				objectSelects.add(DomainConstants.SELECT_ID);
				objectSelects.add(DomainConstants.SELECT_NAME);
				objectSelects.add(ATTR_V_NAME);
				objectSelects.add(ATTR_V_DESCRIPTION);
				objectSelects.add(ATTR__SERVICEABLEITEM_VPMREFERENCE); // "Serviceable Item";
				objectSelects.add(ATTR__PROCUREMENTINTENT_VPMREFERENCE); // "Procurement Intent"
				objectSelects.add(ATTR__PARTINTERCHANGEABILITY_VPMREFERENCE); // "Part Interchangeability ";
				objectSelects.add(ATTR__UNITOFMEASURE_VPMREFERENCE); // "Unit of Measure";

				// RelationShip Selectables
				StringList relInfoList = new StringList();
				relInfoList.add(DomainConstants.SELECT_ID);
				relInfoList.add(DomainConstants.SELECT_TO_ID);

				// Mql command to get CADConnectionPhysicalID
				String strCADConnectionPhysicalID = MqlUtil.mqlCommand(context, "print bus " + strOBJId + " select relationship[" + strRel + "].paths.path.element.physicalid dump |");

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
	private int processSubContractPart(Context context, Map<?, ?> mCadPartDetails, String strConnectionId)
			throws Exception {
		logger.writeLog("processSubContractPart : START !!");
		int intCADPartResponseString = 0;
		try {
			if (UIUtil.isNotNullAndNotEmpty(xcsrfToken) && UIUtil.isNotNullAndNotEmpty(cookie)) {
			if(mCadPartDetails != null)
				intCADPartResponseString = sendSubContractPartPayload(context, mCadPartDetails,strConnectionId);
			}
			else {
				intCADPartResponseString = 500;
				logger.writeLog("ERROR : Failed to get x-csrf-token and cookies from SAP Webservice ");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		logger.writeLog("processSubContractPart : END !!");
		return intCADPartResponseString;
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
	private MapList getAllLevelSubContract(Context context, String strObjectId) throws Exception {
		logger.writeLog("getAllLevelSubContract: START!!");
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
				objectSelects.add(ATTR_V_NAME);
				objectSelects.add(ATTR_V_DESCRIPTION);
				objectSelects.add(ATTR__SERVICEABLEITEM_VPMREFERENCE); // "Serviceable Item";
				objectSelects.add(ATTR__PROCUREMENTINTENT_VPMREFERENCE); // "Procurement Intent"
				objectSelects.add(ATTR__PARTINTERCHANGEABILITY_VPMREFERENCE); // "Part Interchangeability ";
				objectSelects.add(ATTR__UNITOFMEASURE_VPMREFERENCE); // "Unit of Measure";

				// RelationShip Selectables
				StringList relSelects = new StringList();
				relSelects.add(DomainConstants.SELECT_RELATIONSHIP_ID);
				relSelects.add(ATTR__HASCONFIGEFFECTIVITY);

				// STEP : Expanding the Input Linked CAD Part to get all children of it
				DomainObject partDomObj = DomainObject.newInstance(context, strObjectId);
				MapList ebomList = partDomObj.getRelatedObjects(context, TYPE_VPMINSTANCE, // relationship pattern
						TYPE_VPMREFERENCE, // object pattern
						objectSelects, // object selects
						relSelects, // relationship selects
						false, // to direction
						true, // from direction
						(short) 0, // recursion level
						DomainConstants.EMPTY_STRING, // object where clause
						DomainConstants.EMPTY_STRING, 0);

				ebomList.sort(ATTR__TREEORDER_PLMINSTANCE, "ascending", "real");
				// STEP : Collecting only BuySubC part from expanded childs
				Map<?, ?> tempMap;
				if (!ebomList.isEmpty()) {
					for (int b = 0; b < ebomList.size(); b++) {
						tempMap = (Map<?, ?>) ebomList.get(b);
						strProcurementIntent = (String) tempMap.get(ATTR__PROCUREMENTINTENT_VPMREFERENCE);
						if (SUBCONTRACT.equalsIgnoreCase(strProcurementIntent)) {
							mlObjects.add(tempMap);
						}
					}
				}
			}
		} catch (Exception expMsg) {
		}
		logger.writeLog("getAllLevelSubContract: END!!");
		return mlObjects;
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

	private int sendSubContractPartPayload(Context context, Map<?, ?> mSubContractPartDetails,String strConnectionId) throws Exception {
		logger.writeLog("sendSubContractPartPayload : START !!");
		int intCADPartResponseString = 0;

		try {

			// STEP : Get Change Action Header
			JsonObjectBuilder jEachSubContractPayloadBuilder = ChangeActionJSONPayload(context, "");
			if (null != jEachSubContractPayloadBuilder) {
				// STEP : Get SubContract part Header

				JsonObjectBuilder jSubContractBuilder = getSubContractPartHeader(context, mSubContractPartDetails);
				if (null != jSubContractBuilder) {

					// STEP : Retrieving all level BuySUBC Cad part , single level SubContract Cad
					// part and adding in JSON array
					JsonArrayBuilder jsubContractChildPartArrayBuilder = Json.createArrayBuilder();
					String strObjectID = (String) mSubContractPartDetails.get(DomainConstants.SELECT_ID);
					String strTitle = (String) mSubContractPartDetails.get(ATTR_V_NAME);
					MapList mlObjects = getFirstLevelChildren(context, strObjectID);
					if (mlObjects != null) {
						MapList mlBuySubCObjects = null;
						Map<?, ?> mchildPartDetails;
						String strProcIntent;
						String strObjID;

						// STEP : Processing First level Children of SubContract
						JsonObjectBuilder jChildBuilder = Json.createObjectBuilder();
						for (int kdx = 0; kdx < mlObjects.size(); kdx++) {
							mchildPartDetails = (Map<?, ?>) mlObjects.get(kdx);
							strProcIntent = (String) mchildPartDetails.get(ATTR__PROCUREMENTINTENT_VPMREFERENCE);
							strObjID = (String) mchildPartDetails.get(DomainConstants.SELECT_ID);

							// STEP : adding SUBCONTRACT part in array of Child JSON
							if (SUBCONTRACT.equalsIgnoreCase(strProcIntent)) {
								jChildBuilder = addCADChildPartHeader(context, mchildPartDetails);
								if (null != jChildBuilder)
									jsubContractChildPartArrayBuilder.add(jChildBuilder.build());
							} else {
								// STEP : adding all level BUYSUBC part in array of Child JSON
								if (BUYSUBC.equalsIgnoreCase(strProcIntent)) {
									jChildBuilder = addCADChildPartHeader(context, mchildPartDetails);
									if (null != jChildBuilder)
										jsubContractChildPartArrayBuilder.add(jChildBuilder.build());
								}

								mlBuySubCObjects = getAllLevelBuySubC(context, strObjID);
								if (mlBuySubCObjects != null) {
									for (int idx = 0; idx < mlBuySubCObjects.size(); idx++) {
										mchildPartDetails = (Map<?, ?>) mlBuySubCObjects.get(idx);
										strProcIntent = (String) mchildPartDetails.get(ATTR__PROCUREMENTINTENT_VPMREFERENCE);
										if (BUYSUBC.equalsIgnoreCase(strProcIntent)) {
											jChildBuilder = addCADChildPartHeader(context, mchildPartDetails);
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
							jSubContractBuilder.add(HEADER_CHILDREN, jsubContractChildPartArrayBuilder.build());
						}
						if (null != jSubContractBuilder)
							jEachSubContractPayloadBuilder.add(HEADER_PART, jSubContractBuilder.build());

						JsonObject jSubContractPayloadObj = jEachSubContractPayloadBuilder.build();
						if (jSubContractPayloadObj.containsKey(HEADER_PART)) {
							String jsonEachPayloadString = jSubContractPayloadObj.toString();
							jsonLogger.writeLog("JSON Payload Request for SubContract : <<< " + strTitle + " : >>> \n " + jsonEachPayloadString + " \n ");
							intCADPartResponseString = callPostService(context, jSubContractPayloadObj);
							jsonLogger.writeLog("Response for SubContract : <<< " + strTitle + " :  Payload from SAP WebService >>> \n " + intCADPartResponseString + " \n ");
						}
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		logger.writeLog("sendSubContractPartPayload : END !!");
		return intCADPartResponseString;
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

	private JsonObjectBuilder getSubContractPartHeader(Context context, Map<?, ?> mSubContractPartDetails) throws Exception {
		logger.writeLog("addSubContractPartHeader : START !!");
		JsonObjectBuilder jSubContractBuilder = Json.createObjectBuilder();
		try {
			// STEP : Retrieving necessary information of SubContract Part
			String strObjectID = (String) mSubContractPartDetails.get(DomainConstants.SELECT_ID);
			String sName = (String) mSubContractPartDetails.get(DomainConstants.SELECT_NAME);
			String strTitle = (String) mSubContractPartDetails.get(ATTR_V_NAME);
			String strdescription = (String) mSubContractPartDetails.get(ATTR_V_DESCRIPTION);
			String strProcurementIntent = (String) mSubContractPartDetails.get(ATTR__PROCUREMENTINTENT_VPMREFERENCE);
			String strPartInterchangeability = (String) mSubContractPartDetails.get(ATTR__PARTINTERCHANGEABILITY_VPMREFERENCE);
			String strServiceable = (String) mSubContractPartDetails.get(ATTR__SERVICEABLEITEM_VPMREFERENCE);

			// STEP : Adding information of SubContract Part IN JSON Object
			if (UIUtil.isNotNullAndNotEmpty(strTitle))
				jSubContractBuilder.add(TAG_TITLE, strTitle);
			else
				jSubContractBuilder.add(TAG_TITLE, " ");

			if (UIUtil.isNotNullAndNotEmpty(sName))
				jSubContractBuilder.add(TAG_NAME, sName);
			else
				jSubContractBuilder.add(TAG_NAME, " ");

			if (UIUtil.isNotNullAndNotEmpty(strObjectID))
				jSubContractBuilder.add(TAG_OID, strObjectID);
			else
				jSubContractBuilder.add(TAG_OID, " ");

			jSubContractBuilder.add(TAG_REL_ID, NOT_APPLICABLE);

			if (UIUtil.isNotNullAndNotEmpty(strdescription))
				jSubContractBuilder.add(TAG_DESCRIPTION, strdescription);
			else
				jSubContractBuilder.add(TAG_DESCRIPTION, " ");

			if (UIUtil.isNotNullAndNotEmpty(strProcurementIntent))
				jSubContractBuilder.add(TAG_PROCUREMENTINTENT, strProcurementIntent);
			else
				jSubContractBuilder.add(TAG_PROCUREMENTINTENT, " ");

			if (UIUtil.isNotNullAndNotEmpty(strServiceable))
				jSubContractBuilder.add(TAG_SERVICEABLEITEM, strServiceable);
			else
				jSubContractBuilder.add(TAG_SERVICEABLEITEM, " ");

			jSubContractBuilder.add(TAG_PLANTCODE, NOT_APPLICABLE);

			if (UIUtil.isNotNullAndNotEmpty(strPartInterchangeability))
				jSubContractBuilder.add(TAG_PARTINTERCHANGEABILITY, strPartInterchangeability);
			else
				jSubContractBuilder.add(TAG_PARTINTERCHANGEABILITY, " ");

			jSubContractBuilder.add(TAG_VARIAENT_Effectivity, NOT_APPLICABLE);
			jSubContractBuilder.add(TAG_DATEFROM, NOT_APPLICABLE);
			jSubContractBuilder.add(TAG_DATETO, NOT_APPLICABLE);
			jSubContractBuilder.add(TAG_REALIZED_DATA, true);

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
	private MapList getFirstLevelChildren(Context context, String strObjectId) throws Exception {
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
				objectSelects.add(ATTR_V_NAME);
				objectSelects.add(ATTR_V_DESCRIPTION);
				objectSelects.add(ATTR__SERVICEABLEITEM_VPMREFERENCE); // "Serviceable Item";
				objectSelects.add(ATTR__PROCUREMENTINTENT_VPMREFERENCE); // "Procurement Intent"
				objectSelects.add(ATTR__PARTINTERCHANGEABILITY_VPMREFERENCE); // "Part Interchangeability ";
				objectSelects.add(ATTR__UNITOFMEASURE_VPMREFERENCE); // "Unit of Measure";

				// RelationShip Selectables
				StringList relSelects = new StringList();
				relSelects.add(DomainConstants.SELECT_RELATIONSHIP_ID);
				relSelects.add(ATTR__HASCONFIGEFFECTIVITY);

				// STEP : Expanding the Input Linked CAD Part to get all children of it
				DomainObject partDomObj = DomainObject.newInstance(context, strObjectId);
				mlObjects = partDomObj.getRelatedObjects(context, TYPE_VPMINSTANCE, // relationship pattern
						TYPE_VPMREFERENCE, // object pattern
						objectSelects, // object selects
						relSelects, // relationship selects
						false, // to direction
						true, // from direction
						(short) 1, // recursion level
						DomainConstants.EMPTY_STRING, // object where clause
						DomainConstants.EMPTY_STRING, 0);

				mlObjects.sort(ATTR__TREEORDER_PLMINSTANCE, "ascending", "real");

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
	private JsonObjectBuilder addCADChildPartHeader(Context context, Map<?, ?> mchildPartDetails) throws Exception {
		logger.writeLog("addBuySubCPartHeader : START!!");
		JsonObjectBuilder jCadPartBuilder = Json.createObjectBuilder();
		try {
			// STEP : Retrieving necessary information of BuySubC Part
			String strVName = (String) mchildPartDetails.get(ATTR_V_NAME);
			String strName = (String) mchildPartDetails.get(DomainConstants.SELECT_NAME);
			String strObjectID = (String) mchildPartDetails.get(DomainObject.SELECT_ID);
			String strRId = (String) mchildPartDetails.get(DomainConstants.SELECT_RELATIONSHIP_ID);
			String strdesc = (String) mchildPartDetails.get(ATTR_V_DESCRIPTION);
			String strUnitOfMeasure = (String) mchildPartDetails.get(ATTR__UNITOFMEASURE_VPMREFERENCE);
			String strProcIntent = (String) mchildPartDetails.get(ATTR__PROCUREMENTINTENT_VPMREFERENCE);
			String strServiceItem = (String) mchildPartDetails.get(ATTR__SERVICEABLEITEM_VPMREFERENCE);
			String strPartability = (String) mchildPartDetails.get(ATTR__PARTINTERCHANGEABILITY_VPMREFERENCE);
			String strhasConfigEffectivity = (String) mchildPartDetails.get(ATTR__HASCONFIGEFFECTIVITY);

			String strVariantEffectivity = null;
			String strEffectivityObject = null;
			String strDateEffectivity = "";
			String strCADReleaseDateIn = "";
			String strCADReleaseDateOut = "";

			if (TRUE.equals(strhasConfigEffectivity)) {
				Map<?, ?> mEffectivity = getEffectivity(context, strRId);
				if (mEffectivity != null) {
					strEffectivityObject = mEffectivity.toString();

					if (strEffectivityObject.contains("Effectivity_Current_Evolution:")) {
						strDateEffectivity = strEffectivityObject.substring(strEffectivityObject.indexOf("Effectivity_Current_Evolution:") + 30,strEffectivityObject.indexOf("]") + 1);

						String[] result = strDateEffectivity.split(" - ");
						if (result.length >= 2) {
							strCADReleaseDateIn = result[0];
							strCADReleaseDateIn = strCADReleaseDateIn.substring(13);

							if (strCADReleaseDateIn.length() >= 19)
								strCADReleaseDateIn = strCADReleaseDateIn.substring(0,strCADReleaseDateIn.length() - 9);

							strCADReleaseDateOut = result[1];
							strCADReleaseDateOut = strCADReleaseDateOut.substring(0,strCADReleaseDateOut.length() - 1);

							if (strCADReleaseDateOut.length() >= 19)
								strCADReleaseDateOut = strCADReleaseDateOut.substring(0,strCADReleaseDateOut.length() - 9);

						}
					}
				}
			}
			// STEP : Adding information of each BuySubC Part IN JSON Object

			if (UIUtil.isNotNullAndNotEmpty(strVName))
				jCadPartBuilder.add(TAG_TITLE, strVName);
			else
				jCadPartBuilder.add(TAG_TITLE, " ");

			if (UIUtil.isNotNullAndNotEmpty(strName))
				jCadPartBuilder.add(TAG_NAME, strName);
			else
				jCadPartBuilder.add(TAG_NAME, " ");

			if (UIUtil.isNotNullAndNotEmpty(strObjectID))
				jCadPartBuilder.add(TAG_OID, strObjectID);
			else
				jCadPartBuilder.add(TAG_OID, " ");

			if (UIUtil.isNotNullAndNotEmpty(strRId))
				jCadPartBuilder.add(TAG_REL_ID, strRId);
			else
				jCadPartBuilder.add(TAG_REL_ID, " ");

			if (UIUtil.isNotNullAndNotEmpty(strdesc))
				jCadPartBuilder.add(TAG_DESCRIPTION, strdesc);
			else
				jCadPartBuilder.add(TAG_DESCRIPTION, " ");

			if (UIUtil.isNotNullAndNotEmpty(strUnitOfMeasure))
				jCadPartBuilder.add(TAG_UNIT_OF_MEASURE, strUnitOfMeasure);
			else
				jCadPartBuilder.add(TAG_UNIT_OF_MEASURE, " ");

			if (UIUtil.isNotNullAndNotEmpty(strProcIntent))
				jCadPartBuilder.add(TAG_PROCUREMENTINTENT, strProcIntent);
			else
				jCadPartBuilder.add(TAG_PROCUREMENTINTENT, " ");

			if (UIUtil.isNotNullAndNotEmpty(strServiceItem))
				jCadPartBuilder.add(TAG_SERVICEABLEITEM, strServiceItem);
			else
				jCadPartBuilder.add(TAG_SERVICEABLEITEM, " ");

			if (UIUtil.isNotNullAndNotEmpty(strPartability))
				jCadPartBuilder.add(TAG_PARTINTERCHANGEABILITY, strPartability);
			else
				jCadPartBuilder.add(TAG_PARTINTERCHANGEABILITY, " ");

			jCadPartBuilder.add(TAG_VARIAENT_Effectivity, NOT_APPLICABLE);

			SimpleDateFormat format1 = new SimpleDateFormat("yyyy-MM-dd");
			SimpleDateFormat format2 = new SimpleDateFormat("MM-dd-yyyy");
			if (UIUtil.isNotNullAndNotEmpty(strCADReleaseDateIn)){
				Date date = format1.parse(strCADReleaseDateIn);
				String formatDate = format2.format(date);
				jCadPartBuilder.add(TAG_DATEFROM,formatDate);
			}
			else
				jCadPartBuilder.add(TAG_DATEFROM," ");

			if (UIUtil.isNotNullAndNotEmpty(strCADReleaseDateOut)){
				if(strCADReleaseDateOut.equals("INF")){
					strCADReleaseDateOut = "12-31-9999";
					jCadPartBuilder.add(TAG_DATETO,strCADReleaseDateOut);
				}
				else{
					Date date = format1.parse(strCADReleaseDateOut);
					String formatDate = format2.format(date);
					jCadPartBuilder.add(TAG_DATETO,formatDate);
				}
			}
			else
				jCadPartBuilder.add(TAG_DATETO," ");

			jCadPartBuilder.add(TAG_REALIZED_DATA, true);

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
	private MapList getAllLevelBuySubC(Context context, String strObjectId) throws Exception {
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
				objectSelects.add(ATTR_V_NAME);
				objectSelects.add(ATTR_V_DESCRIPTION);
				objectSelects.add(ATTR__SERVICEABLEITEM_VPMREFERENCE); // "Serviceable Item";
				objectSelects.add(ATTR__PROCUREMENTINTENT_VPMREFERENCE); // "Procurement Intent"
				objectSelects.add(ATTR__PARTINTERCHANGEABILITY_VPMREFERENCE); // "Part Interchangeability ";
				objectSelects.add(ATTR__UNITOFMEASURE_VPMREFERENCE); // "Unit of Measure";

				// RelationShip Selectables
				StringList relSelects = new StringList();
				relSelects.add(DomainConstants.SELECT_RELATIONSHIP_ID);
				relSelects.add(ATTR__HASCONFIGEFFECTIVITY);

				// STEP : Expanding the Input Linked CAD Part to get all children of it
				DomainObject partDomObj = DomainObject.newInstance(context, strObjectId);
				MapList ebomList = partDomObj.getRelatedObjects(context, TYPE_VPMINSTANCE, // relationship pattern
						TYPE_VPMREFERENCE, // object pattern
						objectSelects, // object selects
						relSelects, // relationship selects
						false, // to direction
						true, // from direction
						(short) 0, // recursion level
						DomainConstants.EMPTY_STRING, // object where clause
						DomainConstants.EMPTY_STRING, 0);

				ebomList.sort(ATTR__TREEORDER_PLMINSTANCE, "ascending", "real");
				// STEP : Collecting only BuySubC part from expanded childs
				Map<?, ?> tempMap;
				if (!ebomList.isEmpty()) {
					for (int b = 0; b < ebomList.size(); b++) {
						tempMap = (Map<?, ?>) ebomList.get(b);
						strProcurementIntent = (String) tempMap.get(ATTR__PROCUREMENTINTENT_VPMREFERENCE);
						if (BUYSUBC.equalsIgnoreCase(strProcurementIntent)) {
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
	private int callPostService(Context context, JsonObject jEachPayloadObj) throws Exception {
		logger.writeLog("callPostService()..... START");
		// STEP : Creating HttpPost Object with SAP webService URL
		HttpPost postURL = new HttpPost(url);
		String result = null;
		int intResponseCode = 0;
		try {
			// STEP : Creating request header for SAP WebService POST Method call
			String xcsrftoken = EnoviaResourceBundle.getProperty(context, LCD_3DX_SAP_INTEGRATION_KEY,
					"LCD_3DXSAPStringResource_en.SAP.XCSRFTOKEN", language);
			String cookie = EnoviaResourceBundle.getProperty(context, LCD_3DX_SAP_INTEGRATION_KEY,
					"LCD_3DXSAPStringResource_en.SAP.COOKIE", language);
			String contentType = EnoviaResourceBundle.getProperty(context, LCD_3DX_SAP_INTEGRATION_KEY,
					"LCD_3DXSAPStringResource_en.SAP.CONTENTTYPE", language);

			StringEntity params = new StringEntity(jEachPayloadObj.toString());
			postURL.setHeader(cookie, cookie);
			postURL.setHeader(contentType, "application/json");
			postURL.setHeader(xcsrftoken, xcsrfToken);
			postURL.setEntity(params);

			// STEP : Invoking SAP webService with Post Method
			HttpResponse response = httpClient.execute(postURL);
			logger.writeLog("postURL -- " + postURL);
			intResponseCode = response.getStatusLine().getStatusCode();

			if (intResponseCode == HttpStatus.SC_OK) { // success
				// STEP : Collecting the acknowledgement from SAP webService
				logger.writeLog("POST request successful , response code ==  " + response.getStatusLine().getStatusCode());
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
	private void processWebServiceResponseForSubContract(Context context, int intResponseCode, String strConnectionId)
			throws Exception {
		logger.writeLog("ProcessWebServiceResponseForSubContract START");
		String sbfErrMes = "";

		SimpleDateFormat DateFormat = new SimpleDateFormat("MM/dd/yyyy hh.mm.ss aa");
		try {
			ContextUtil.pushContext(context);

			DomainRelationship domRelBomConnectedToAnchorObj = DomainRelationship.newInstance(context, strConnectionId);

			if (intResponseCode == 200) {
				domRelBomConnectedToAnchorObj.setAttributeValue(context, ATTR_LCD_PROCESS_STATUS_FLAG, STATUS_IN_WORK);
				domRelBomConnectedToAnchorObj.setAttributeValue(context, ATTR_LCD_REASON_FOR_FAILURE,MSG_JSON_FORMAT_VALIDATION_COMPLETED);
			} 
			else if(intResponseCode == 417)
			{
				domRelBomConnectedToAnchorObj.setAttributeValue(context, ATTR_LCD_PROCESS_STATUS_FLAG, STATUS_FAILED);
				domRelBomConnectedToAnchorObj.setAttributeValue(context, ATTR_LCD_REASON_FOR_FAILURE,MSG_JSON_FORMAT_VALIDATION_FAILED);
			}
			else
			{
				domRelBomConnectedToAnchorObj.setAttributeValue(context, ATTR_LCD_PROCESS_STATUS_FLAG, STATUS_FAILED);
				domRelBomConnectedToAnchorObj.setAttributeValue(context, ATTR_LCD_REASON_FOR_FAILURE,MSG_NETWORK_FAILURE);
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
	private void processWebServiceResponseForManufacturingAssembly(Context context, int intResponseCode,String strConnectionId) throws NullPointerException {
		logger.writeLog("ProcessWebServiceResponseForManufacturingAssembly START");
		try {
			ContextUtil.pushContext(context);
			DomainRelationship domRelBomConnectedToAnchorObj = DomainRelationship.newInstance(context, strConnectionId);

			if (intResponseCode == 200) {
				domRelBomConnectedToAnchorObj.setAttributeValue(context, ATTR_LCD_PROCESS_STATUS_FLAG, STATUS_IN_WORK);
				domRelBomConnectedToAnchorObj.setAttributeValue(context, ATTR_LCD_REASON_FOR_FAILURE,
						MSG_JSON_FORMAT_VALIDATION_COMPLETED);
			} 
			else if(intResponseCode == 417)
			{
				domRelBomConnectedToAnchorObj.setAttributeValue(context, ATTR_LCD_PROCESS_STATUS_FLAG, STATUS_FAILED);
				domRelBomConnectedToAnchorObj.setAttributeValue(context, ATTR_LCD_REASON_FOR_FAILURE,MSG_JSON_FORMAT_VALIDATION_FAILED);
			}
			else
			{
				domRelBomConnectedToAnchorObj.setAttributeValue(context, ATTR_LCD_PROCESS_STATUS_FLAG, STATUS_FAILED);
				domRelBomConnectedToAnchorObj.setAttributeValue(context, ATTR_LCD_REASON_FOR_FAILURE,MSG_NETWORK_FAILURE);
			}
			ContextUtil.popContext(context);
		} catch (Exception e) {
			e.printStackTrace();
		}
		logger.writeLog("ProcessWebServiceResponseForManufacturingAssembly END");
	}

	/**
	 * This Method is to add the history on Object
	 *
	 * @param context
	 * @param String  strId : Object Id
	 * @param String  strComment : History Comment
	 * @throws Exception
	 */
	/*	private void addObjectHistory(Context context, String strId, String strComment) throws Exception {
		logger.writeLog("addObjectHistory...Start");
		try {
			if (UIUtil.isNotNullAndNotEmpty(strId)) {
				String strError = "Error From SAP Web Service - ";
				ChangeUtil.addHistory(context, strId, strError, strComment);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		logger.writeLog("addObjectHistory...Finish");
	}
	 */
	/**
	 * This Method is to add the history on Relationship
	 *
	 * @param context
	 * @param String  strRelId : Relationship Id
	 * @param String  strComment : History Comment
	 * @throws Exception
	 */
	/*		private void addRelationshipHistory(Context context, String strRelId, String strComment) throws Exception {
		logger.writeLog("addRelationshipHistory...Start");
		try {
			if (UIUtil.isNotNullAndNotEmpty(strRelId)) {
				String mql1 = "modify connection $1 add history $2 comment $3";
				String strError = "Error From SAP Web Service - ";
				MqlUtil.mqlCommand(context, mql1, false, strRelId, strError, strComment.toString());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		logger.writeLog("addRelationshipHistory...Finish");
	}*/

	/**
	 * This Method is to log the SAP WebService Connection Error on Object
	 *
	 * @param context
	 * @param String  strResponseStatusLine : SAP WebService Connection Error with
	 *                Response Code
	 * @throws Exception
	 */
	/*private void LogWebServiceConnectionErrorOnHeader(Context context, String strResponseStatusLine) throws Exception {
		logger.writeLog("LogWebServiceConnectionErrorOnHeader...Start");
		try {
			JsonArray realizedItems = getChangeActionJson().getJsonObject("changeaction").getJsonArray("realized");
			if (!realizedItems.isEmpty()) {
				StringList objectSelects = new StringList();
				JsonObject realizedItem;
				String realizedItemType;
				String realizedItemId;
				Map<?, ?> mobjectDetails;
				String strObjProcureIntent;
				String strHasConfig;
				String strObjId;
				String strTitle;
				DomainObject domRealizedItemObj;
				for (int i = 0; i < realizedItems.size(); i++) {
					realizedItem = realizedItems.getJsonObject(i);
					realizedItemType = realizedItem.getJsonObject("where").getJsonObject("info").getString("type");
					realizedItemId = realizedItem.getJsonObject("where").get("id").toString().split(":")[1]
							.replace("\"", "");
					if (UIUtil.isNotNullAndNotEmpty(realizedItemId)) {
						domRealizedItemObj = DomainObject.newInstance(context, realizedItemId);

						objectSelects.add(ATTR_V_NAME);
						objectSelects.add(DomainConstants.SELECT_ID);
						objectSelects.add(ATTR__PROCUREMENTINTENT_MANUFACTURINGASSEMBLY);
						objectSelects.add(ATTR__HASCONFIGCONTEXT_VPMREFERENCE);
						objectSelects.add(ATTR__SAPMBOMUPDATEDON_MANUFACTURINGASSEMBLY);

						mobjectDetails = domRealizedItemObj.getInfo(context, objectSelects);
						strObjProcureIntent = (String) mobjectDetails
								.get(ATTR__PROCUREMENTINTENT_MANUFACTURINGASSEMBLY);
						strHasConfig = (String) mobjectDetails.get(ATTR__HASCONFIGCONTEXT_VPMREFERENCE);

						if (realizedItemType.equalsIgnoreCase(TYPE_MANUFACTURINGASSEMBLY)
								&& strObjProcureIntent.equalsIgnoreCase(DESCRETE)
								&& "FALSE".equalsIgnoreCase(strHasConfig)) {
							strObjId = (String) mobjectDetails.get(DomainConstants.SELECT_ID);
							strTitle = (String) mobjectDetails.get(ATTR_V_NAME);
							if (UIUtil.isNotNullAndNotEmpty(strObjId)
									&& UIUtil.isNotNullAndNotEmpty(strResponseStatusLine)
									&& UIUtil.isNotNullAndNotEmpty(strTitle))

								strResponseStatusLine = strTitle + ":" + strResponseStatusLine;
							addObjectHistory(context, strObjId, strResponseStatusLine);

						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		logger.writeLog("LogWebServiceConnectionErrorOnHeader...End");
	}*/

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
	 * This Method is to Set the Cookie for the Session
	 *
	 * @param context
	 * @param strCookie : Cookie for the Session
	 * @throws Exception
	 */
	/*	private static int setSubContractFailedFlag(boolean isFailed) {
		subContractStatus = isFailed;
		return 0;
	}
	 */
	/**
	 * This Method is to Get the Cookie for the Session
	 *
	 * @param context
	 * @return COOKIE : Cookie for the Session
	 * @throws Exception
	 */
	/*	private static boolean getSubContractFailedFlag() {
		return subContractStatus;
	}
	 */
	/**
	 * This Method is to Set the Change Action Completion Date
	 *
	 * @param context
	 * @return CA_RELEASED_DATE : Change Action Completion Date
	 * @throws Exception
	 */
	/*	private void setRootSubContractRelId(String strSubContractRelId) {
		subContractRelId = strSubContractRelId;
	}*/

	/**
	 * This Method is to Get the Change Action Completion Date
	 *
	 * @param context
	 * @return CA_RELEASED_DATE : Change Action Completion Date
	 * @throws Exception
	 */
	/*	private static String getRootSubContractRelId() {
		return subContractRelId;
	}*/

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
	private void setFalseEffectivityFlag(boolean bFalseEffectivityFlag)throws Exception {
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
	private void setConfiguredFlag(boolean bConfigFlag) throws Exception{
		CONFIGURABLE_STATUS = bConfigFlag;
	}

	/**
	 * This Method is to Set the configured flag of object
	 *
	 * @param context
	 * @return CA_RELEASED_DATE : Change Action Completion Date
	 * @throws Exception
	 */
	private void setRealizedItemList(Context context , JsonObject realizedItem) throws Exception {
		logger.writeLog("setRealizedItemList()..... START");
		try {
			List<String> lstRealizedChanges = new ArrayList<String>();		
			String relPhyId = "";
			JsonArray amos = realizedItem.getJsonArray("amo");
			for(int a = 0; a < amos.size(); a++) {
				JsonObject amo = amos.getJsonObject(a);
				if(amo.containsKey("before")) {
					relPhyId = amo.getJsonObject("before").get("id").toString().split(":")[1].replaceAll("\"", "");
					if (UIUtil.isNotNullAndNotEmpty(relPhyId)){
						lstRealizedChanges.add(relPhyId);

					}
				}
				if(amo.containsKey("after")) {
					relPhyId = amo.getJsonObject("after").get("id").toString().split(":")[1].replaceAll("\"", "");
					if (UIUtil.isNotNullAndNotEmpty(relPhyId)){
						lstRealizedChanges.add(relPhyId);
					}
				}

			}
			if(lstRealizedChanges.size()!=0){
				LIST_REALIZED_CHANGES =  lstRealizedChanges;
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
		return LIST_REALIZED_CHANGES;
	}

	/**
	 * Invoking SAP Web service with GET Method to get the x-csrf-token and cookies
	 * error or send empty string.
	 *
	 * @param CloseableHttpClient
	 * @return String Array in arguments ( x-csrf-token and cookies )
	 * @throws Exception
	 */
	private int callGETService(Context context) throws Exception {
		logger.writeLog("callGETService()..... START");

		String strStatusLine = null;
		int responseCode = 0;

		String userName = EnoviaResourceBundle.getProperty(context, LCD_3DX_SAP_INTEGRATION_KEY,
				"LCD_3DXSAPStringResource_en.SAP.UserName", language);

		String password = EnoviaResourceBundle.getProperty(context, LCD_3DX_SAP_INTEGRATION_KEY,
				"LCD_3DXSAPStringResource_en.SAP.Password", language);

		url = EnoviaResourceBundle.getProperty(context, LCD_3DX_SAP_INTEGRATION_KEY,
				"LCD_3DXSAPStringResource_en.SAP.WebServiceURL.DEV", language);

		httpClient = HttpClients.createDefault();

		try {
			// STEP : Creating Authorization Header for calling SAP webService using GET
			// method
			String authString = userName + ":" + password;
			String authStringEnc = BASE64_ENCODER.encodeToString(authString.getBytes());

			// STEP : Creating HttpGet Object with SAP webService URL
			HttpGet getURL = new HttpGet(url);

			String authorization = EnoviaResourceBundle.getProperty(context, LCD_3DX_SAP_INTEGRATION_KEY,
					"LCD_3DXSAPStringResource_en.SAP.Authorization", language);
			xcsrfToken = EnoviaResourceBundle.getProperty(context, LCD_3DX_SAP_INTEGRATION_KEY,
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
					logger.writeLog("GET request successful , Response code ==  " + response.getStatusLine().getStatusCode());
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
					strTempOptionCodeToSort = strTempOptionCodeToSort.substring(1,strTempOptionCodeToSort.length() - 1);

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
			String strCAStartDate =null ;
			String formatDate =null ;
			String changeActionName = getChangeActionJson().getJsonObject("changeaction").getString("name");
			String changeActionApplicabilityTxt = getChangeActionJson().getJsonObject("changeaction").getJsonObject("applicability").getString("expressionTXT");
			if(UIUtil.isNotNullAndNotEmpty(changeActionApplicabilityTxt)) {
				String changeActionApplicability = changeActionApplicabilityTxt.substring(changeActionApplicabilityTxt.indexOf("[")+1, changeActionApplicabilityTxt.indexOf("]"));
				logger.writeLog("changeActionApplicability :" + changeActionApplicability);
				String[] changeActionApplicabilityDate = changeActionApplicability.split(" - ");
				if(changeActionApplicabilityDate.length >=2){
					strCAStartDate = changeActionApplicabilityDate[0];
					if(strCAStartDate.length() >= 19)
						strCAStartDate = strCAStartDate.substring(0, strCAStartDate.length() - 9);

					SimpleDateFormat format1 = new SimpleDateFormat("yyyy-MM-dd");
					SimpleDateFormat format2 = new SimpleDateFormat("MM-dd-yyyy");
					if (UIUtil.isNotNullAndNotEmpty(strCAStartDate)){
						Date date = format1.parse(strCAStartDate);
						formatDate = format2.format(date);
					} 
				}
			}
			if(UIUtil.isNotNullAndNotEmpty(formatDate))
				CA_START_DATE = formatDate;

		}
		catch (Exception e) {
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
	private void loggerInitialization(Context context) throws Exception {
		try {
			language = context.getSession().getLanguage();

			String strCompleteLogger = EnoviaResourceBundle.getProperty(context, LCD_3DX_SAP_INTEGRATION_KEY,
					"LCD_3DXSAPStringResource_en.logger.CompleteLogger", language);
			String strJSONLogger = EnoviaResourceBundle.getProperty(context, LCD_3DX_SAP_INTEGRATION_KEY,
					"LCD_3DXSAPStringResource_en.logger.JSONLogger", language);
			//			String strDateFormat = EnoviaResourceBundle.getProperty(context, LCD_3DX_SAP_INTEGRATION_KEY,
			//					"LCD_3DXSAPStringResource_en.logger.DateFormat", getLanguage());
			String strExtension = EnoviaResourceBundle.getProperty(context, LCD_3DX_SAP_INTEGRATION_KEY,
					"LCD_3DXSAPStringResource_en.logger.Extension", language);
			String strUnderscore = EnoviaResourceBundle.getProperty(context, LCD_3DX_SAP_INTEGRATION_KEY,
					"LCD_3DXSAPStringResource_en.logger.UnderScore", language);

			// STEP : Log Creation
			SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yy_hh_mm_ss");
			String strCurrentDate = dateFormat.format(new Date());
			System.out.println("dateFormat<<<<<<<<< " + dateFormat);

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

			String strLogPath = EnoviaResourceBundle.getProperty(context, LCD_3DX_SAP_INTEGRATION_KEY,
					"LCD_3DXSAPStringResource_en.logger.logPath", language);
			File tmpDir = new File(strLogPath);
			boolean exists = tmpDir.exists();

			System.out.println("strLogPath 123<<<<<<<<< " + strLogPath);
			if (!exists) {
				tmpDir.mkdirs();
			}
			//			System.out.println("getChangeActionJson() <<<<<<<<< " + getChangeActionJson());
			//			System.out.println("getChangeActionJson().getJsonObject(\"changeaction\") <<<<<<<<< "
			//					+ getChangeActionJson().getJsonObject("changeaction"));

			//			String changeActionName = getChangeActionJson().getJsonObject("changeaction").getString("name");
			//			System.out.println("changeActionName <<<<<<<<< " + changeActionName);
			logger = new SimpleLogger(strLogPath + strUnderscore + strCompleteLogName);
			System.out.println("logger <<<<<<<<< " + logger);
			jsonLogger = new SimpleLogger(strLogPath + strUnderscore + strJSONLogName);

			System.out.println("jsonLogger <<<<<<<<< " + jsonLogger);

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
