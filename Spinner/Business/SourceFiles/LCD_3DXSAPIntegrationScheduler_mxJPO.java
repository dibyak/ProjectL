import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
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

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

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
import matrix.util.DateFormatUtil;
import matrix.util.Pattern;
import matrix.util.StringList;

public class LCD_3DXSAPIntegrationScheduler_mxJPO extends LCD_Constants_mxJPO {

	private static final Base64.Encoder BASE64_ENCODER = Base64.getEncoder();

	public static final String LCD_3DX_SAP_INTEGRATION_KEY = "LCD_3DXSAPStringResource_en";

	public static final String TYPE_LCD_BOM_ANCHOR_OBJECT = "LCD_BOMAnchorObject";
	public static final String NAME_LCD_ANCHOR_OBJECT = "LCD_AnchorObject";
	public static final String REV_LCD_ANCHOR_OBJECT = "A";
	public static final String REL_LCD_SAP_BOM_INTERFACE = "LCD_SAPBOMInterface";
	public static final String VAULT_ESERVICE_PRODUCTION = "eService Production";

	public static final String ATTR_LCD_PROCESS_STATUS_FLAG = "LCD_ProcessStatusFlag";
	public static final String ATTR_LCD_REASON_FOR_FAILURE = "LCD_ReasonforFailure";
	public static final String ATTR_LCD_CAID = "LCD_CAID";
	public static final String ATTR_MODIFIED = "modified";

	public static final String STATUS_COMPLETE = "Complete";
	public static final String STATUS_IN_WORK = "In Work";
	public static final String STATUS_FAILED = "Failed";

	public static final String KEY_CONNECTION_ID = "ConnectionID";
	public static final String KEY_BOM_COMPONENT_ID = "BOMComponentID";
	public static final String KEY_BOM_NAME = "BOMName";
	public static final String KEY_CA_ID = "CAID";
	public static final long TIME_CONVERSION = 86400000;
	public static final long DAYS_IN_YEAR = 365;

	private static String language;
	private static String userName;
	private static String password;
	private static String url;
	private static String xcsrfToken;
	private static String cookie;
	private static SimpleLogger logger;
	private static SimpleLogger jsonLogger;
	private static CloseableHttpClient httpClient;
	private static JsonObject changeActionJson;
	private static String caReleasedDate;
	private static boolean subContractStatus;
	private static JsonObjectBuilder changeActionJsonBuilder;

	private static String subContractRelId;

	private static HashMap<String, JsonObjectBuilder> changeActionMap = new HashMap<>();


	/**
	 * This Method is to get Relevant BOM Components which are connected with Anchor
	 * Object and Send it to SAP.
	 * 
	 * @param context
	 * @return
	 * @throws Exception
	 */

	public void FindRelevantObjectsConnectedToAnchorObject(Context context) throws Exception {

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

		MapList manAssMapList = domObj.getRelatedObjects(context, // context
				REL_LCD_SAP_BOM_INTERFACE, // Relationship Pattern
				"*", // Type Pattern
				slObjectSelect, // Object Select
				slRelSelect, // Relationship Select
				false, // To Side
				true, // from Side
				(short) 1, // Recursion Level
				"", // Object Where clause
				"", // Relationship Where clause
				0); // limit

		Iterator<?> iterMAsMaplist = manAssMapList.iterator();

		while (iterMAsMaplist.hasNext()) {
			Map<?, ?> item = (Map<?, ?>) iterMAsMaplist.next();
			String strConnectionId = (String) (item.get(DomainRelationship.SELECT_ID));
			String strBOMComponentId = (String) (item.get(DomainConstants.SELECT_ID));
			String strBOMComponentType = (String) (item.get(DomainConstants.SELECT_TYPE));
			String strbomModified = (String) (item.get(ATTR_MODIFIED));

			DomainRelationship domRelBOMComponents = DomainRelationship.newInstance(context, strConnectionId);

			String strProcessStatusFlag = domRelBOMComponents.getAttributeValue(context, ATTR_LCD_PROCESS_STATUS_FLAG);
			String strCAID = domRelBOMComponents.getAttributeValue(context, ATTR_LCD_CAID);

			if (!(strProcessStatusFlag.equalsIgnoreCase(STATUS_COMPLETE)
					&& strProcessStatusFlag.equalsIgnoreCase(STATUS_IN_WORK)
					&& strProcessStatusFlag.equalsIgnoreCase(STATUS_FAILED))) {
				if (TYPE_MANUFACTURINGASSEMBLY.equalsIgnoreCase(strBOMComponentType)) {
					SendToSAP(context, strBOMComponentId, strBOMComponentType, strCAID, strProcessStatusFlag,
							strConnectionId);
				} else {
					Map<?, ?> mLinkedCadPart = getLinkedCADPartFromMBOMPart(context, strBOMComponentId, REL_PROVIDE);
					String strProcurementIntent = (String) mLinkedCadPart.get(ATTR__PROCUREMENTINTENT_VPMREFERENCE);
					if (SUBCONTRACT.equalsIgnoreCase(strProcurementIntent)) {
						processSubContractPart(context, mLinkedCadPart, strConnectionId);
					}
				}
			} else if (strProcessStatusFlag.equalsIgnoreCase(STATUS_COMPLETE)) {
				DisconnectExpiredObjectsFromAnchorObject(context, strConnectionId, strbomModified);
			}
		}
	}

	/**
	 * This Method is called by webService To RePush Failed BOM Components to SAP.
	 * 
	 * @param context
	 * @param args
	 * @throws Exception
	 */

	public HashMap<?, ?> RePushFailedBomComponentsToSAP(Context context, String args[]) throws Exception {

		HashMap<?, ?> programMap = (HashMap<?, ?>) JPO.unpackArgs(args);
		HashMap<String, String> responseMap = new HashMap<>();
		String strResponseString = "";

		// STEP : Retrieving Input Arguments
		String strConnectionId = (String) programMap.get(KEY_CONNECTION_ID);
		String strBOMComponentId = (String) programMap.get(KEY_BOM_COMPONENT_ID);
		String strCAID = (String) programMap.get(KEY_CA_ID);

		DomainObject domObj = DomainObject.newInstance(context, strBOMComponentId);

		StringList slObjectSelect = new StringList();
		slObjectSelect.add(DomainConstants.SELECT_TYPE);

		Map bomMap = domObj.getInfo(context, slObjectSelect);

		String strBOMComponentType = (String) bomMap.get(DomainConstants.SELECT_TYPE);

		if (TYPE_MANUFACTURINGASSEMBLY.equalsIgnoreCase(strBOMComponentType)) {
			strResponseString = SendToSAP(context, strBOMComponentId, strBOMComponentType, strCAID, STATUS_FAILED,
					strConnectionId);
			System.out.println("RePushFailedBomComponentsToSAP strResponseString ------> " + strResponseString);
			responseMap = ProcessWebServiceResponseForManufacturingAssembly(context, strResponseString,
					strConnectionId);
		} else {
			Map<?, ?> mLinkedCadPart = getLinkedCADPartFromMBOMPart(context, strBOMComponentId, REL_PROVIDE);
			String strProcurementIntent = (String) mLinkedCadPart.get(ATTR__PROCUREMENTINTENT_VPMREFERENCE);
			if (SUBCONTRACT.equalsIgnoreCase(strProcurementIntent)) {
			}
		}
		logger.writeLog("response from SAP for Re-Push faild components >>>> " + responseMap);
		return responseMap;
	}

	/**
	 * This Method is to disconnect Completed BOM Components from Anchor Objects
	 * after 7 days Object and Send it to SAP.
	 * 
	 * @param context
	 * @return
	 * @throws Exception
	 */

	public void DisconnectExpiredObjectsFromAnchorObject(Context context, String strConnectionId,
			String strbomModified) throws Exception {

		SimpleDateFormat sdf = new SimpleDateFormat(eMatrixDateFormat.getEMatrixDateFormat(), context.getLocale());
		Date da = sdf.parse(strbomModified);
		Date currentDate = new Date();
		long differenceInTime = currentDate.getTime() - da.getTime();
		long differenceInDays = (differenceInTime / TIME_CONVERSION) % DAYS_IN_YEAR;

		if (differenceInDays > 7) {
			DomainRelationship.disconnect(context, strConnectionId);
		}
	}

	private String SendToSAP(Context context, String strBOMComponentId, String strBOMComponentType, String caId,
			String processStatusFlag, String strConnectionId) throws Exception {

		String strRequestString = null;
		int intResponseString = 0;
		String strResponseString = "";
		try {
			System.out.println("sendToSAP calledd <<<<<<<<< ");

			language = context.getSession().getLanguage();

			userName = EnoviaResourceBundle.getProperty(context, LCD_3DX_SAP_INTEGRATION_KEY,
					"LCD_3DXSAPStringResource_en.SAP.UserName", language);

			password = EnoviaResourceBundle.getProperty(context, LCD_3DX_SAP_INTEGRATION_KEY,
					"LCD_3DXSAPStringResource_en.SAP.Password", language);

			url = EnoviaResourceBundle.getProperty(context, LCD_3DX_SAP_INTEGRATION_KEY,
					"LCD_3DXSAPStringResource_en.SAP.WebServiceURL.DEV", language);

			httpClient = HttpClients.createDefault();

			// STEP :Get input Arguments
			ChangeAction changeAction = ChangeActionServices.getChangeAction(context, caId);
			if (null != changeAction) {
				// STEP : Using Service API getting JSON object for Change Action
				ChangeActionFacets changeActionFacets = new ChangeActionFacets();
				changeActionFacets.attributes = true;
				changeActionFacets.realized = true;
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
						// STEP :Log Initialization
						LoggerInitialization(context);

						// Step : Retrieving Change Action Name from JSON Object of Change Action
						// [output of service API]
						String changeActionName = getChangeActionJson().getJsonObject("changeaction").getString("name");

						// STEP : Invoking SAP Web service using GET request method to get the
						// x-csrf-token and cookies
						String strResponseStatusLine = callGETService(context);
						if (UIUtil.isNotNullAndNotEmpty(xcsrfToken) && UIUtil.isNotNullAndNotEmpty(cookie)) {
							// STEP : Adding Change Action Header in JSON
//							JsonObjectBuilder jEachPayloadBuilder = addChangeActionHeader(context);

							JsonObjectBuilder jEachPayloadBuilder = Json.createObjectBuilder();

							if (changeActionMap.containsKey(caId)) {
								jEachPayloadBuilder = changeActionMap.get(caId);
							} else {
								jEachPayloadBuilder = ChangeActionJSONPayload(context, caId);
								changeActionMap.put(caId, jEachPayloadBuilder);
							}

							if (null != jEachPayloadBuilder) {

								// STEP : Traversing Change Action to get the realized items
								String strObjProcIntent;
								String strHasConfig;
								String strObjTitle;
								String strSAPMBOMUpdatedOn;
								String strSAPUniqueID;
								DomainObject domRealizedItemObj;
								Map<?, ?> mobjectDetails;
								JsonObjectBuilder jHeaderPartObjectBuilder;
								JsonObject jEachPayloadObj;

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
									strObjProcIntent = (String) mobjectDetails
											.get(ATTR__PROCUREMENTINTENT_MANUFACTURINGASSEMBLY);
									strHasConfig = (String) mobjectDetails.get(ATTR__HASCONFIGCONTEXT_VPMREFERENCE);
									strObjTitle = (String) mobjectDetails.get(ATTR_V_NAME);
//									strBOMComponentType = (String) mobjectDetails.get(DomainConstants.SELECT_TYPE);

									// STEP : Check the realized item is Discrete Make Manufacturing Assembly
									if (TYPE_MANUFACTURINGASSEMBLY.equalsIgnoreCase(strBOMComponentType)
											&& DESCRETE.equalsIgnoreCase(strObjProcIntent)
											&& FALSE.equalsIgnoreCase(strHasConfig)) {
										strSAPMBOMUpdatedOn = (String) mobjectDetails
												.get(ATTR__SAPMBOMUPDATEDON_MANUFACTURINGASSEMBLY);
										strSAPUniqueID = (String) mobjectDetails
												.get(ATTR__SAPUNIQUEID_MANUFACTURINGASSEMBLY);

										// STEP : Added to handle Re push only Failed and new discrete M11
										if (UIUtil.isNullOrEmpty(strSAPMBOMUpdatedOn)
												&& UIUtil.isNullOrEmpty(strSAPUniqueID)) {
											// Mark SubContract Failed Flag False at start of each Make Assembly
											setSubContractFailedFlag(false);

											// STEP : Adding Manufacturing Assembly Header in JSON
											logger.writeLog("Processing Realized Item << " + strObjTitle
													+ " >> Procurement Intent : << " + strObjProcIntent + " >> ");
											jHeaderPartObjectBuilder = addRealizedItemHeader(context, mobjectDetails);
											if (null != jHeaderPartObjectBuilder) {
												jEachPayloadBuilder.add(HEADER_PART, jHeaderPartObjectBuilder.build());

												// STEP : Invoking SAP WebService by attaching JSON
												jEachPayloadObj = jEachPayloadBuilder.build();
												strRequestString = jEachPayloadObj.toString();
												jsonLogger.writeLog("JSON Payload Request for Discrete : <<< "
														+ strObjTitle + " : >>> \n " + strRequestString + " \n ");
												logger.writeLog("JSON Payload Request for Discrete : <<< " + strObjTitle
														+ " : >>> \n " + strRequestString + " \n ");

												if (UIUtil.isNotNullAndNotEmpty(jEachPayloadObj.toString())) {
													strResponseString = callPostService(context, jEachPayloadObj);

													jsonLogger.writeLog("Response for Discrete : <<< " + strObjTitle
															+ " :  Payload from SAP WebService >>> \n "
															+ intResponseString + " \n ");

												} else
													logger.writeLog(
															"ERROR :: Failed to generate JSON Payload for realized item  : << "
																	+ strObjTitle + ">> header for Change Action : << "
																	+ changeActionName);

											} else
												logger.writeLog("ERROR :: Failed to add realized item  : << "
														+ strObjTitle + ">> header for Change Action : << "
														+ changeActionName);
										} else
											logger.writeLog("INFO ::Realized Item << " + strObjTitle
													+ " >> is with Procurement Intent : << " + strObjProcIntent
													+ " >> is already processed from Change Action : << "
													+ changeActionName + " >> ");
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
							logger.writeLog("ERROR : Failed to get x-csrf-token and cookies from SAP Webservice ");
							// add history on M11 with Status Line
							if (UIUtil.isNotNullAndNotEmpty(strResponseStatusLine))
								LogWebServiceConnectionErrorOnHeader(context, strResponseStatusLine);
						}
					} else
						logger.writeLog("ERROR : Failed to get change action JSON object using Service API");
				} else
					logger.writeLog("ERROR : Failed to get change action JSON using Service API");
			} else
				logger.writeLog("ERROR : Failed to input arguments");
		} catch (

		Exception e) {
			e.printStackTrace();
		} finally {
			httpClient.close();
		}

		return strResponseString;
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
			// STEP : Retrieving Change Action Details from Input Change Action JSON

			DomainObject domObjChangeAction = DomainObject.newInstance(context, caId);

			StringList slObjectSelect = new StringList();
			slObjectSelect.add(DomainConstants.SELECT_NAME);
			slObjectSelect.add(DomainConstants.ATTRIBUTE_DESCRIPTION);
			slObjectSelect.add(TITLE_CHANGEACTION);
			slObjectSelect.add(CATEGORYOFCHANGE_CHANGEACTION);
			slObjectSelect.add(CHANGEDOMAIN_CHANGEACTION);
			slObjectSelect.add(PLATFORM_CHANGEACTION);
			slObjectSelect.add(CHANGETYPE_CHANGEACTION);
			slObjectSelect.add(REASONFORCHANGE_CHANGEACTION);
			slObjectSelect.add(RELEASEDDATE_CHANGEACTION);

			Map<?, ?> changeActionAttrDetails = domObjChangeAction.getInfo(context, slObjectSelect);

			// STEP : Retrieving Change Action Details from Input Change Action JSON
			caReleasedDate = (String) changeActionAttrDetails.get(RELEASEDDATE_CHANGEACTION);
			if (UIUtil.isNotNullAndNotEmpty(caReleasedDate)) {
				caReleasedDate = caReleasedDate.substring(0, caReleasedDate.length() - 11);
				if (UIUtil.isNotNullAndNotEmpty(caReleasedDate)) {
					SimpleDateFormat format1 = new SimpleDateFormat("MM/dd/yyyy");
					SimpleDateFormat format2 = new SimpleDateFormat("MM-dd-yyyy");
					Date date = format1.parse(caReleasedDate);
					String formatDate = format2.format(date);
					setCACompletionDate(formatDate);
				}
			}

			// STEP : Adding Change Action object details in JSONObjectBuilder
			if (UIUtil.isNotNullAndNotEmpty((String) changeActionAttrDetails.get(TITLE_CHANGEACTION)))
				jEachPayloadBuilder.add(TAG_TITLE, (String) changeActionAttrDetails.get(TITLE_CHANGEACTION));

			if (UIUtil.isNotNullAndNotEmpty(caId))
				jEachPayloadBuilder.add(TAG_OID, caId);
			else
				jEachPayloadBuilder.add(TAG_OID, " ");

			if (UIUtil.isNotNullAndNotEmpty((String) changeActionAttrDetails.get(DomainConstants.SELECT_NAME)))
				jEachPayloadBuilder.add(TAG_NAME, (String) changeActionAttrDetails.get(DomainConstants.SELECT_NAME));
			else
				jEachPayloadBuilder.add(TAG_NAME, " ");

			if (UIUtil
					.isNotNullAndNotEmpty((String) changeActionAttrDetails.get(DomainConstants.ATTRIBUTE_DESCRIPTION)))
				jEachPayloadBuilder.add(TAG_DESCRIPTION,
						(String) changeActionAttrDetails.get(DomainConstants.ATTRIBUTE_DESCRIPTION));
			else
				jEachPayloadBuilder.add(TAG_DESCRIPTION, " ");

			if (UIUtil.isNotNullAndNotEmpty((String) changeActionAttrDetails.get(CATEGORYOFCHANGE_CHANGEACTION)))
				jEachPayloadBuilder.add(TAG_CATEGORY_OF_CHANGE,
						(String) changeActionAttrDetails.get(CATEGORYOFCHANGE_CHANGEACTION));

			if (UIUtil.isNotNullAndNotEmpty((String) changeActionAttrDetails.get(CHANGEDOMAIN_CHANGEACTION)))
				jEachPayloadBuilder.add(TAG_CHANGEDOMAIN,
						(String) changeActionAttrDetails.get(CHANGEDOMAIN_CHANGEACTION));

			if (UIUtil.isNotNullAndNotEmpty((String) changeActionAttrDetails.get(CHANGETYPE_CHANGEACTION)))
				jEachPayloadBuilder.add(TAG_CHANGETYPE, (String) changeActionAttrDetails.get(CHANGETYPE_CHANGEACTION));

			if (UIUtil.isNotNullAndNotEmpty((String) changeActionAttrDetails.get(REASONFORCHANGE_CHANGEACTION)))
				jEachPayloadBuilder.add(TAG_REASONFORCHANGE,
						(String) changeActionAttrDetails.get(REASONFORCHANGE_CHANGEACTION));

			if (UIUtil.isNotNullAndNotEmpty((String) changeActionAttrDetails.get(PLATFORM_CHANGEACTION)))
				jEachPayloadBuilder.add(TAG_PLATFORM, (changeActionAttrDetails.get(PLATFORM_CHANGEACTION)).toString());

			logger.writeLog("Change Action Header :: << " + jEachPayloadBuilder.build().toString() + ">>");
			// Set Change Action Header in JSON
			setChangeActionHeader(jEachPayloadBuilder);

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
			String strObjPartInterchangeability = (String) mobjectDetails
					.get(ATTR__PARTINTERCHANGEABILITY_MANUFACTURINGASSEMBLY);

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
				jHeaderPartObjectBuilder.add(TAG_PLANTCODE, strObjPlantCode + "5");
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
					// STEP : Adding Manufacturing Assembly child of Manufacturing Assembly Header
					// in JSON
					isAssembly = true;
					addChildPartHeader(context, mPartDetails, jMBOMPartBuilder, isAssembly);
				} else {
					// STEP : Adding child part of Manufacturing Assembly Header in JSON
					isAssembly = false;
					addChildPartHeader(context, mPartDetails, jMBOMPartBuilder, isAssembly);
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
			String strObjectID = (String) mPartDetails.get(DomainConstants.SELECT_ID);
			String sName = (String) mPartDetails.get(DomainConstants.SELECT_NAME);
			String strTitle = (String) mPartDetails.get(ATTR_V_NAME);
			String strdescription = (String) mPartDetails.get(ATTR_V_DESCRIPTION);
			String strRelId = (String) mPartDetails.get(DomainConstants.SELECT_RELATIONSHIP_ID);

			// STEP : If Child is Manufacturing Assembly ,Retrieving Procurement Intent
			// ,serviceable ,PartInterchangeability from Object details
			if (isAssembly) {
				strProcurementIntent = (String) mPartDetails.get(ATTR__PROCUREMENTINTENT_MANUFACTURINGASSEMBLY);
				strServiceable = (String) mPartDetails.get(ATTR__SERVICEABLEITEM_MANUFACTURINGASSEMBLY);
				strPartInterchangeability = (String) mPartDetails
						.get(ATTR__PARTINTERCHANGEABILITY_MANUFACTURINGASSEMBLY);
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
						strPartInterchangeability = (String) mLinkedCadPart
								.get(ATTR__PARTINTERCHANGEABILITY_VPMREFERENCE);
						strUnitOfMeasure = (String) mLinkedCadPart.get(ATTR__UNITOFMEASURE_VPMREFERENCE);
					}
				}
			}

			String strVariantEffectivity = "";
			String strEffectivityObject = "";
			Map<?, ?> mEffectivity = getEffectivity(context, strRelId);
			strEffectivityObject = mEffectivity.toString();

			if (strEffectivityObject.contains(EFFECTIVITY_VARIANT + ":")) {
				strVariantEffectivity = strEffectivityObject.substring(
						strEffectivityObject.indexOf(EFFECTIVITY_VARIANT + ":") + 20,
						strEffectivityObject.indexOf("ExpressionFormat") - 1);
				// strVariantEffectivity = sortOptionCode(strVariantEffectivity);
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
			if (UIUtil.isNotNullAndNotEmpty(getCACompletionDate()))
				jMBOMPartBuilder.add(TAG_DATEFROM, getCACompletionDate());
			else
				jMBOMPartBuilder.add(TAG_DATEFROM, "");

			jMBOMPartBuilder.add(TAG_DATETO, "12-31-9999");
			jMBOMPartBuilder.add(TAG_REALIZED_DATA, true);

//				if (false == isAssembly) {
//					if( null != mLinkedCadPart ){
//						if (SUBCONTRACT.equalsIgnoreCase(strProcurementIntent)) {
//							processSubContractPart(context , mLinkedCadPart );
//						}
//					}
//				}
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
			// ContextUtil.pushContext(context, ROLE_ADMIN, DomainConstants.EMPTY_STRING,
			// DomainConstants.EMPTY_STRING);
//			ContextUtil.pushContext(context);
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
//			ContextUtil.popContext(context);
		}
		logger.writeLog("getEffectivity : END !!");
		return mEffectivity;
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
									StringList slRelId = (StringList) relInfoMap.get(DomainConstants.SELECT_ID);
									String strRelId = slRelId.get(0);
									if (UIUtil.isNotNullAndNotEmpty(strRelId))
										setRootSubContractRelId(strRelId);

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

	private int processSubContractPart(Context context, Map<?, ?> mLinkedCadPart, String strConnectionId)
			throws Exception {
		logger.writeLog("processSubContractPart : START !!");
		int intCADPartResponseString = 0;
		try {

			// STEP : Retrieving necessary information of SubContract Part
			// STEP : Corresponding Sub-contract part and its child with Procurement intent
			// "Buy-SubC" :: Write this info in another JSON Payload
			String strObjectID = (String) mLinkedCadPart.get(DomainConstants.SELECT_ID);
			String strRelId = getRootSubContractRelId();
			if (UIUtil.isNotNullAndNotEmpty(strObjectID)) {
				MapList mlSubContractList = new MapList();
				mlSubContractList = getAllLevelSubContract(context, strObjectID);
				if (!mlSubContractList.isEmpty()) {
					logger.writeLog("AllLevelSubContract Count <<<<<<<<<<<<<<<<< " + mlSubContractList.size());

					Map<?, ?> mSubContractPartDetails;
					String strProcIntent;
					for (int kdx = mlSubContractList.size() - 1; kdx >= 0; --kdx) {
						mSubContractPartDetails = (Map<?, ?>) mlSubContractList.get(kdx);
						logger.writeLog(" SubContract  <<<<<<<<<<<<<<<<< " + mSubContractPartDetails);

						strProcIntent = (String) mSubContractPartDetails.get(ATTR__PROCUREMENTINTENT_VPMREFERENCE);
						if (SUBCONTRACT.equalsIgnoreCase(strProcIntent)) {
							strRelId = (String) mSubContractPartDetails.get(DomainConstants.SELECT_RELATIONSHIP_ID);
							intCADPartResponseString = sendSubContractPartPayload(context, mSubContractPartDetails,
									strRelId, strConnectionId);
						}
					}
				}
				intCADPartResponseString = sendSubContractPartPayload(context, mLinkedCadPart, strRelId,
						strConnectionId);
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

	private int sendSubContractPartPayload(Context context, Map<?, ?> mSubContractPartDetails, String strRelId,
			String strConnectionId) throws Exception {
		logger.writeLog("sendSubContractPartPayload : START !!");
		int intCADPartResponseString = 0;

		try {

			// STEP : Get Change Action Header
			JsonObjectBuilder jEachSubContractPayloadBuilder = getChangeActionHeader();
			if (null != jEachSubContractPayloadBuilder) {
				// STEP : Get SubContract part Header

				JsonObjectBuilder jSubContractBuilder = getSubContractPartHeader(context, mSubContractPartDetails,
						strRelId);
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
										strProcIntent = (String) mchildPartDetails
												.get(ATTR__PROCUREMENTINTENT_VPMREFERENCE);
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
							jsonLogger.writeLog("JSON Payload Request for SubContract : <<< " + strTitle + " : >>> \n "
									+ jsonEachPayloadString + " \n ");
//							intCADPartResponseString = callPostService(context, jSubContractPayloadObj);
							jsonLogger.writeLog("Response for SubContract : <<< " + strTitle
									+ " :  Payload from SAP WebService >>> \n " + intCADPartResponseString + " \n ");
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

	private JsonObjectBuilder getSubContractPartHeader(Context context, Map<?, ?> mSubContractPartDetails,
			String strRelId) throws Exception {
		logger.writeLog("addSubContractPartHeader : START !!");
		JsonObjectBuilder jSubContractBuilder = Json.createObjectBuilder();
		try {
			// STEP : Retrieving necessary information of SubContract Part
			String strObjectID = (String) mSubContractPartDetails.get(DomainConstants.SELECT_ID);
			String sName = (String) mSubContractPartDetails.get(DomainConstants.SELECT_NAME);
			String strTitle = (String) mSubContractPartDetails.get(ATTR_V_NAME);
			String strdescription = (String) mSubContractPartDetails.get(ATTR_V_DESCRIPTION);
			String strProcurementIntent = (String) mSubContractPartDetails.get(ATTR__PROCUREMENTINTENT_VPMREFERENCE);
			String strPartInterchangeability = (String) mSubContractPartDetails
					.get(ATTR__PARTINTERCHANGEABILITY_VPMREFERENCE);
			String strServiceable = (String) mSubContractPartDetails.get(ATTR__SERVICEABLEITEM_VPMREFERENCE);

			String strVariantEffectivity = null;
			String strEffectivityObject = null;
			Map<?, ?> mEffectivity = getEffectivity(context, strRelId);
			strEffectivityObject = mEffectivity.toString();

			if (strEffectivityObject.contains(EFFECTIVITY_VARIANT + ":")) {
				strVariantEffectivity = strEffectivityObject.substring(
						strEffectivityObject.indexOf(EFFECTIVITY_VARIANT + ":") + 20,
						strEffectivityObject.indexOf("ExpressionFormat") - 1);
			}

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

			if (UIUtil.isNotNullAndNotEmpty(strRelId))
				jSubContractBuilder.add(TAG_REL_ID, strRelId);
			else
				jSubContractBuilder.add(TAG_REL_ID, " ");

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

			if (UIUtil.isNotNullAndNotEmpty(strVariantEffectivity))
				jSubContractBuilder.add(TAG_VARIAENT_Effectivity, strVariantEffectivity);
			else
				jSubContractBuilder.add(TAG_VARIAENT_Effectivity, " ");

			jSubContractBuilder.add(TAG_DATEFROM, getCACompletionDate());
			jSubContractBuilder.add(TAG_DATETO, "12-31-9999");
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

			String strVariantEffectivity = null;
			String strEffectivityObject = null;
			Map<?, ?> mEffectivity = getEffectivity(context, strRId);
			strEffectivityObject = mEffectivity.toString();

			if (strEffectivityObject.contains(EFFECTIVITY_VARIANT + ":")) {
				strVariantEffectivity = strEffectivityObject.substring(
						strEffectivityObject.indexOf(EFFECTIVITY_VARIANT + ":") + 20,
						strEffectivityObject.indexOf("ExpressionFormat") - 1);
				// strVariantEffectivity = sortOptionCode(strVariantEffectivity);
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

			if (UIUtil.isNotNullAndNotEmpty(strVariantEffectivity))
				jCadPartBuilder.add(TAG_VARIAENT_Effectivity, strVariantEffectivity);
			else
				jCadPartBuilder.add(TAG_VARIAENT_Effectivity, " ");

			jCadPartBuilder.add(TAG_DATEFROM, getCACompletionDate());
			jCadPartBuilder.add(TAG_DATETO, "12-31-9999");
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
	private String callPostService(Context context, JsonObject jEachPayloadObj) throws Exception {
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
			System.out.println("postURL -- " + postURL);
			intResponseCode = response.getStatusLine().getStatusCode();

			if (intResponseCode == HttpStatus.SC_OK) { // success
				// STEP : Collecting the acknowledgement from SAP webService
				logger.writeLog(
						"POST request successful , response code ==  " + response.getStatusLine().getStatusCode());
			} else {
				logger.writeLog("POST request failed , response code ==  " + response.getStatusLine().getStatusCode());
			}
			result = EntityUtils.toString(response.getEntity());
			System.out.println("LCD Result--------------- " + result);
		} catch (Exception e) {
			e.printStackTrace();
		}
		logger.writeLog("callPostService()..... END");
		return result;
	}

	/**
	 * This Method is to process the response from POST call of SAP WebService
	 *
	 * @param context
	 * @param String  responseString : Response from SAP webService
	 * @throws Exception
	 */
	private void ProcessWebServiceResponseForSubContract(Context context, String responseString, String strConnectionId)
			throws Exception {
		logger.writeLog("ProcessWebServiceResponseForSubContract START");
		String sbfErrMes = "";

		SimpleDateFormat DateFormat = new SimpleDateFormat("MM/dd/yyyy hh.mm.ss aa");
		try (JsonReader jsonReader = Json.createReader(new StringReader(responseString))) {
			JsonObject jWebServiceResponse = jsonReader.readObject();
			if (null != jWebServiceResponse) {
				if (jWebServiceResponse.containsKey(HEADER_PART)) {
					JsonObject jPayloadObject = jWebServiceResponse.getJsonObject(HEADER_PART);
					if (null != jPayloadObject) {
						String resultType = jWebServiceResponse.getString(TAG_TYPE);
						if (UIUtil.isNotNullAndNotEmpty(resultType) && resultType.equalsIgnoreCase(FAIL)) {
							setSubContractFailedFlag(true);
							String strErrorMessage = jPayloadObject.getString(TAG_ERROR_MESSAGE);
							String strRelID = jPayloadObject.getString(TAG_REL_ID);
							String strObjTitle = jPayloadObject.getString(TAG_TITLE);

							if (UIUtil.isNotNullAndNotEmpty(strRelID) && UIUtil.isNotNullAndNotEmpty(strErrorMessage)) {
//								addRelationshipHistory(context, strRelID, strErrorMessage);
								sbfErrMes = strObjTitle + ":" + strErrorMessage + "\n";
							}

							JsonArray jArrayOfChildParts = jPayloadObject.getJsonArray(HEADER_CHILDREN);
							if (null != jArrayOfChildParts) {
								JsonObject jchildPart;
								String strPartRelID;
								String sErrorMessage;
								String strPartTitle;
								for (int i = 0; i < jArrayOfChildParts.size(); i++) {
									jchildPart = jArrayOfChildParts.getJsonObject(i);
									strPartTitle = jchildPart.getString(TAG_TITLE);
									strPartRelID = jchildPart.getString(TAG_REL_ID);
									sErrorMessage = jchildPart.getString(TAG_ERROR_MESSAGE);

									if (UIUtil.isNotNullAndNotEmpty(strPartRelID)
											&& UIUtil.isNotNullAndNotEmpty(sErrorMessage))
										sbfErrMes.concat(strPartTitle + ":" + sErrorMessage + "\n");
//										addRelationshipHistory(context, strPartRelID, sErrorMessage);
								}
							}
							DomainRelationship domRel = DomainRelationship.newInstance(context, strConnectionId);
							domRel.setAttributeValue(context, ATTR_LCD_PROCESS_STATUS_FLAG, STATUS_FAILED);
							domRel.setAttributeValue(context, ATTR_LCD_REASON_FOR_FAILURE, sbfErrMes);

						} else if (UIUtil.isNotNullAndNotEmpty(resultType) && resultType.equalsIgnoreCase(SUCCESS)) {
							String strRelID = jPayloadObject.getString(TAG_REL_ID);
							if (UIUtil.isNotNullAndNotEmpty(strRelID)) {
								DomainRelationship domRel = DomainRelationship.newInstance(context, strRelID);
								// Push context as no Manufacturing Assembly access to 3DXLeader in release
								// state in respective policy.
								try {
									Date date = new Date();
									ContextUtil.pushContext(context,
											PropertyUtil.getSchemaProperty(context, "person_UserAgent"),
											DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
									domRel.setAttributeValue(context, ATTR__SAP_CAD_INSATNCE_UPDATED_ON,
											DateFormat.format(date));

									DomainRelationship domRelPhysicalProduct = DomainRelationship.newInstance(context,
											strConnectionId);
									domRelPhysicalProduct.setAttributeValue(context, ATTR_LCD_PROCESS_STATUS_FLAG,
											STATUS_FAILED);
									domRelPhysicalProduct.setAttributeValue(context, ATTR_LCD_REASON_FOR_FAILURE,
											sbfErrMes);

									ContextUtil.popContext(context);
								} catch (Exception me) {
									throw me;
								}
							}
							JsonArray jArrayOfChildParts = jPayloadObject.getJsonArray(HEADER_CHILDREN);
							if (null != jArrayOfChildParts) {
								// int size = jArrayOfChildParts.size();
								JsonObject jchildPart;
								String sRelID;
								DomainRelationship domRelationship;
								for (int i = 0; i < jArrayOfChildParts.size(); i++) {
									jchildPart = jArrayOfChildParts.getJsonObject(i);
									sRelID = jchildPart.getString(TAG_REL_ID);
									if (UIUtil.isNotNullAndNotEmpty(sRelID)) {
										domRelationship = DomainRelationship.newInstance(context, sRelID);
										try {
											Date date = new Date();
											ContextUtil.pushContext(context,
													PropertyUtil.getSchemaProperty(context, "person_UserAgent"),
													DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
											domRelationship.setAttributeValue(context,
													ATTR__SAP_CAD_INSATNCE_UPDATED_ON, DateFormat.format(date));
											ContextUtil.popContext(context);
										} catch (Exception me) {
											throw me;
										}
									}
								}
							}
						}
					} else
						logger.writeLog("ERROR : Invalid Response from SAP WebService");
				} else
					logger.writeLog("Failed to get Header of SubContract from response JSON object ");
			} else
				logger.writeLog("ERROR : Invalid Response from SAP WebService");
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
	private HashMap<String, String> ProcessWebServiceResponseForManufacturingAssembly(Context context,
			String responseString, String strConnectionId) throws Exception {
		logger.writeLog("ProcessWebServiceResponseForManufacturingAssembly START");

		StringBuffer sbfErrMes = new StringBuffer();
		SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy hh.mm.ss aa");
		HashMap<String, String> responseMap = new HashMap<>();
		try (JsonReader jsonReader = Json.createReader(new StringReader(responseString))) {
			System.out.println("In try with resource for ProcessWebServiceResponseForManufacturingAssembly-----");
			JsonObject jWebServiceResponse = jsonReader.readObject();
			if (null != jWebServiceResponse) {
				if (jWebServiceResponse.containsKey(HEADER_PART)) {
					JsonObject jPayloadObject = jWebServiceResponse.getJsonObject(HEADER_PART);
					if (null != jPayloadObject) {
						String resultType = jWebServiceResponse.getString(TAG_TYPE);

						if (UIUtil.isNotNullAndNotEmpty(resultType) && resultType.equalsIgnoreCase(FAIL)) {
							String strErrorMessage = jPayloadObject.getString(TAG_ERROR_MESSAGE);
							String strObjID = jPayloadObject.getString(TAG_OID);
							String strObjTitle = jPayloadObject.getString(TAG_TITLE);

							if (UIUtil.isNotNullAndNotEmpty(strObjID) && UIUtil.isNotNullAndNotEmpty(strErrorMessage)) {
								sbfErrMes.append(strObjTitle + " : " + strErrorMessage + "\n");
//								addObjectHistory(context, strObjID, strErrorMessage);
							}

							JsonArray jArrayOfChildParts = jPayloadObject.getJsonArray(HEADER_CHILDREN);
							if (null != jArrayOfChildParts) {
								JsonObject jchildPart;
								String strPartTitle;
								String strPartRelID;
								String sErrorMessage;
								for (int i = 0; i < jArrayOfChildParts.size(); i++) {
									jchildPart = jArrayOfChildParts.getJsonObject(i);
									strPartTitle = jchildPart.getString(TAG_TITLE);
									strPartRelID = jchildPart.getString(TAG_REL_ID);
									sErrorMessage = jchildPart.getString(TAG_ERROR_MESSAGE);

									if (UIUtil.isNotNullAndNotEmpty(sErrorMessage)) {
//										addRelationshipHistory(context, strPartRelID, sErrorMessage); // adding history on MBOM Instance

										sbfErrMes.append(strPartTitle + ":" + sErrorMessage + "\n");
//										addObjectHistory(context, strObjID, strErrorMessage); // Adding history on M11 Object
									}
								}
							}
							responseMap.put("status", resultType);
							responseMap.put("ErrorMessage", sbfErrMes.toString());
							DomainRelationship domRelMA = DomainRelationship.newInstance(context, strConnectionId);
							domRelMA.setAttributeValue(context, ATTR_LCD_PROCESS_STATUS_FLAG, STATUS_FAILED);
							domRelMA.setAttributeValue(context, ATTR_LCD_REASON_FOR_FAILURE, sbfErrMes.toString());

						} else if (UIUtil.isNotNullAndNotEmpty(resultType) && resultType.equalsIgnoreCase(SUCCESS)) {
							String strObjID = jPayloadObject.getString(TAG_OID);
							if (UIUtil.isNotNullAndNotEmpty(strObjID)) {
								DomainObject HeaderObject = new DomainObject(strObjID);
								String strSAPUniqueID = jPayloadObject.getString(TAG_SAPUNIQUE_ID);
								// Push context as no Manufacturing Assembly access to 3DXLeader in release
								// state in respective policy.
								try {
									if (getSubContractFailedFlag() == false) {
										Date date = new Date();
										ContextUtil.pushContext(context,
												PropertyUtil.getSchemaProperty(context, "person_UserAgent"),
												DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
										HeaderObject.setAttributeValue(context, ATTR__SAP_UNIQUEID, strSAPUniqueID);
										HeaderObject.setAttributeValue(context, ATTR__SAPMBOM_UPDATED_ON,
												dateFormat.format(date));
										DomainRelationship domRel = DomainRelationship.newInstance(context,
												strConnectionId);
										domRel.setAttributeValue(context, ATTR_LCD_PROCESS_STATUS_FLAG,
												VALUE_STATUS_COMPLETE);
										domRel.setAttributeValue(context, ATTR_LCD_REASON_FOR_FAILURE,
												sbfErrMes.toString());
										ContextUtil.popContext(context);
										responseMap.put("status", resultType);
										responseMap.put("ErrorMessage", sbfErrMes.toString());
										System.out.println(
												"ProcessWebServiceResponseForManufacturingAssembly ---- sbfErrMes "
														+ sbfErrMes.toString());
									}
								} catch (Exception e) {
									throw e;
								}
//								JsonArray jArrayOfChildParts = jPayloadObject.getJsonArray(HEADER_CHILDREN);
//								if (null != jArrayOfChildParts) {
//									JsonObject jchildPart;
//									String sRelID;
//									DomainRelationship domRel;
//									for (int i = 0; i < jArrayOfChildParts.size(); i++) {
//										jchildPart = jArrayOfChildParts.getJsonObject(i);
//										sRelID = jchildPart.getString(TAG_REL_ID);
//										if (UIUtil.isNotNullAndNotEmpty(sRelID)) {
//											domRel = DomainRelationship.newInstance(context, sRelID);
//											try {
//												Date date = new Date();
//												ContextUtil.pushContext(context,
//														PropertyUtil.getSchemaProperty(context, "person_UserAgent"),
//														DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
//												domRel.setAttributeValue(context, ATTR__SAP_INSATNCE_UPDATED_ON,
//														dateFormat.format(date));
//												ContextUtil.popContext(context);
//											} catch (Exception me) {
//												throw me;
//											}
//										}
//									}
//								}
							}
						} else // Error of Invalid Response format
							logger.writeLog("ERROR : Invalid Response from SAP WebService");
					} else
						logger.writeLog("Failed to get Header of Manufacturing Assembly from response JSON object ");
				} else
					logger.writeLog("Failed to get response JSON object");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		logger.writeLog("ProcessWebServiceResponseForManufacturingAssembly END");
		return responseMap;
	}

	/**
	 * This Method is to add the history on Object
	 *
	 * @param context
	 * @param String  strId : Object Id
	 * @param String  strComment : History Comment
	 * @throws Exception
	 */
	private void addObjectHistory(Context context, String strId, String strComment) throws Exception {
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

	/**
	 * This Method is to add the history on Relationship
	 *
	 * @param context
	 * @param String  strRelId : Relationship Id
	 * @param String  strComment : History Comment
	 * @throws Exception
	 */
	private void addRelationshipHistory(Context context, String strRelId, String strComment) throws Exception {
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
	}

	/**
	 * This Method is to log the SAP WebService Connection Error on Object
	 *
	 * @param context
	 * @param String  strResponseStatusLine : SAP WebService Connection Error with
	 *                Response Code
	 * @throws Exception
	 */
	private void LogWebServiceConnectionErrorOnHeader(Context context, String strResponseStatusLine) throws Exception {
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
	 * This Method is to Set the Cookie for the Session
	 *
	 * @param context
	 * @param strCookie : Cookie for the Session
	 * @throws Exception
	 */
	private static int setSubContractFailedFlag(boolean isFailed) {
		subContractStatus = isFailed;
		return 0;
	}

	/**
	 * This Method is to Get the Cookie for the Session
	 *
	 * @param context
	 * @return COOKIE : Cookie for the Session
	 * @throws Exception
	 */
	private static boolean getSubContractFailedFlag() {
		return subContractStatus;
	}

	/**
	 * This Method is to Set the Change Action Completion Date
	 *
	 * @param context
	 * @return CA_RELEASED_DATE : Change Action Completion Date
	 * @throws Exception
	 */
	private void setRootSubContractRelId(String strSubContractRelId) {
		subContractRelId = strSubContractRelId;
	}

	/**
	 * This Method is to Get the Change Action Completion Date
	 *
	 * @param context
	 * @return CA_RELEASED_DATE : Change Action Completion Date
	 * @throws Exception
	 */
	private static String getRootSubContractRelId() {
		return subContractRelId;
	}

	/**
	 * This Method is to Set the Cookie for the Session
	 *
	 * @param context
	 * @param strCookie : Cookie for the Session
	 * @throws Exception
	 */
	private static int setChangeActionHeader(JsonObjectBuilder jEachPayloadBuilder) {
		changeActionJsonBuilder = jEachPayloadBuilder;
		return 0;
	}

	/**
	 * This Method is to Get the Cookie for the Session
	 *
	 * @param context
	 * @return COOKIE : Cookie for the Session
	 * @throws Exception
	 */
	private static JsonObjectBuilder getChangeActionHeader() {
		return changeActionJsonBuilder;
	}

	/**
	 * Invoking SAP Web service with GET Method to get the x-csrf-token and cookies
	 * error or send empty string.
	 *
	 * @param CloseableHttpClient
	 * @return String Array in arguments ( x-csrf-token and cookies )
	 * @throws Exception
	 */
	private String callGETService(Context context) throws Exception {
		logger.writeLog("callGETService()..... START");

		String strStatusLine = null;
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

			System.out.println("callGETService getURL ---- " + getURL);
			// STEP : Invoking SAP webService with GET Method
			HttpResponse response = httpClient.execute(getURL);

			// STEP : Collecting the acknowledgement from SAP webService
			int responseCode = response.getStatusLine().getStatusCode();
			strStatusLine = response.getStatusLine().toString();
			System.out.println("callGETService responseCode ---- " + responseCode);
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
		return strStatusLine;
	}

	/**
	 * This Method is to Initialized the logger
	 *
	 * @throws Exception
	 */
	private void LoggerInitialization(Context context) throws Exception {
		try {
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
			System.out.println("getChangeActionJson() <<<<<<<<< " + getChangeActionJson());
			System.out.println("getChangeActionJson().getJsonObject(\"changeaction\") <<<<<<<<< "
					+ getChangeActionJson().getJsonObject("changeaction"));

			String changeActionName = getChangeActionJson().getJsonObject("changeaction").getString("name");
			System.out.println("changeActionName <<<<<<<<< " + changeActionName);
			logger = new SimpleLogger(strLogPath + changeActionName + strUnderscore + strCompleteLogName);
			System.out.println("logger <<<<<<<<< " + logger);
			jsonLogger = new SimpleLogger(strLogPath + changeActionName + strUnderscore + strJSONLogName);

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
