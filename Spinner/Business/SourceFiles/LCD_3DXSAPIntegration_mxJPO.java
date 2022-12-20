
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
import com.matrixone.apps.domain.Job;
import com.matrixone.apps.domain.util.ContextUtil;
import com.matrixone.apps.domain.util.EnoviaResourceBundle;
import com.matrixone.apps.domain.util.FrameworkUtil;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.MqlUtil;
import com.matrixone.apps.domain.util.PropertyUtil;
import com.matrixone.apps.framework.ui.UIUtil;
import matrix.util.List;
import java.util.Calendar;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import matrix.db.Context;
import matrix.db.JPO;
import matrix.util.Pattern;
import matrix.util.StringList;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

/*
        Author 		: Akash THAKUR
        Version		: 1.0
        Description	: This program is used for 3DX SAP MBOM Integration
 */

public class LCD_3DXSAPIntegration_mxJPO extends LCD_Constants_mxJPO {
	private static final Base64.Encoder BASE64_ENCODER = Base64.getEncoder();
	private static final String LCD_3DXSAPIntegrationKey = "LCD_3DXSAPStringResource_en";

	private static String LANGUAGE;
	private static String USERNAME;
	private static String PASSWORD;
	private static String URL;
	private static String XCSRFTOKEN;
	private static String COOKIE;
	private static SimpleLogger logger;
	private static SimpleLogger JSON_logger;
	private static CloseableHttpClient CLIENT;
	private static JsonObject CHANGE_ACTION_JSON;
	private static String CA_RELEASED_DATE;
	private static boolean SUBCONTRACT_STATUS;
	private static JsonObjectBuilder CHANGE_ACTION_JSON_BUILDER ;

	private static String SUBCONTRACT_RELID ;

	public LCD_3DXSAPIntegration_mxJPO() {}

	/**
	 * This method is being invoked from background Job This Method is to generate
	 * the JSON Pay load and invoking SAP web service
	 *
	 * @param context The ematrix context of the request.
	 * @param args    contain Change Action Object Id
	 * @throws Exception
	 */
	public void sendToSAP(Context context, String args[]) throws Exception {
		boolean isContextPushed = false;
		try {
			ContextUtil.pushContext(context);
			isContextPushed = true;
			// STEP :Set method to Populate UserName for SAP WebService
			setUserName(context);

			// STEP :Set method to Populate Password for SAP WebService
			setPassword(context);

			// STEP :Set method to Populate URL for SAP WebService
			setURL(context);

			// STEP :Set method to Populate Closeable Http Client to call SAP WebService
			setCloseableHttpClient();

			// STEP :Get input Arguments
			ChangeAction changeAction = ChangeActionServices.getChangeAction(context, args[0]);
			if (null != changeAction) {
				// STEP : Using Service API getting JSON object for Change Action
				ChangeActionFacets changeActionFacets = new ChangeActionFacets();
				changeActionFacets.attributes = true;
				changeActionFacets.realized = true;
				String changeActionJsonDetails = ChangeActionJsonUtilities.changeActionToJson(context, changeAction, changeActionFacets);

				if (UIUtil.isNotNullAndNotEmpty(changeActionJsonDetails)) {
					JsonReader jsonReader = Json.createReader(new StringReader(changeActionJsonDetails));
					JsonObject changeActionJson = jsonReader.readObject();
					jsonReader.close();

					if (null != changeActionJson) {
						// STEP :Set method to Populate the JSON Object of Change Action [output of
						// service API]
						setChangeActionJson(changeActionJson);

						// STEP :Log Initialization
						LoggerInitialization(context);

						// Step : Retrieving Change Action Name from JSON Object of Change Action
						// [output of service API]
						String changeActionName = getChangeActionJson().getJsonObject("changeaction").getString("name");

						// STEP : Invoking SAP Web service using GET request method to get the
						// x-csrf-token and cookies
						String strResponseStatusLine = callGETService(context);
						if (UIUtil.isNotNullAndNotEmpty(getCSRFToken()) && UIUtil.isNotNullAndNotEmpty(getSessionCookie())) {
							// STEP : Adding Change Action Header in JSON
							JsonObjectBuilder jEachPayloadBuilder = addChangeActionHeader(context);
							if (null != jEachPayloadBuilder) {
								logger.writeLog("                                                                                ");
                                logger.writeLog("*******************	SAP WebService URL ************ : << " + getURL() + " >> ");
								logger.writeLog("                                                                                ");
								// STEP : Traversing Change Action to get the realized items
								JsonObject realizedItem;
								JsonArray realizedItems = getChangeActionJson().getJsonObject("changeaction").getJsonArray("realized");
								String realizedItemType;
								String realizedItemId;
								String strObjProcIntent;
								String strHasConfig;
								String strObjTitle;
								String strSAPMBOMUpdatedOn;
								String strSAPUniqueID;
								DomainObject domRealizedItemObj;
								Map mobjectDetails;
								JsonObjectBuilder jHeaderPartObjectBuilder;
								JsonObject jEachPayloadObj ;
								String requestString ;
								String responseString ;
								if (realizedItems.size() > 0) {
									for (int i = 0; i < realizedItems.size(); i++) {
										realizedItem = realizedItems.getJsonObject(i);
										if (null != realizedItem) {
											realizedItemType = realizedItem.getJsonObject("where").getJsonObject("info").getString("type");
											realizedItemId = realizedItem.getJsonObject("where").get("id").toString().split(":")[1].replaceAll("\"", "");
											if (UIUtil.isNotNullAndNotEmpty(realizedItemId)) {
												// STEP : Retrieving realized item's object details
												domRealizedItemObj = DomainObject.newInstance(context, realizedItemId);
												StringList objectSelects = new StringList();
												objectSelects.add(DomainConstants.SELECT_ID);
												objectSelects.add(DomainConstants.SELECT_NAME);
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
												strObjProcIntent = (String) mobjectDetails.get(ATTR__PROCUREMENTINTENT_MANUFACTURINGASSEMBLY);
												strHasConfig = (String) mobjectDetails.get(ATTR__HASCONFIGCONTEXT_VPMREFERENCE);
												strObjTitle = (String) mobjectDetails.get(ATTR_V_NAME);

												// STEP : Check the realized item is Discrete Make Manufacturing Assembly
												// Information : To Identify and Mark Manufacturing Assembly as discrete ,
												// 1st Check : Type == "CreateAssembly" and No Configuration 
												if (TYPE_MANUFACTURINGASSEMBLY.equalsIgnoreCase(realizedItemType) && FALSE.equalsIgnoreCase(strHasConfig)) {

													// 2nd Check : Procurement Intent == "Make" or Procurement Intent == "Phantom"
													if ( DESCRETE.equalsIgnoreCase(strObjProcIntent) ||  PHANTOM.equalsIgnoreCase(strObjProcIntent)) {
														strSAPMBOMUpdatedOn = (String) mobjectDetails.get(ATTR__SAPMBOMUPDATEDON_MANUFACTURINGASSEMBLY);
														strSAPUniqueID = (String) mobjectDetails.get(ATTR__SAPUNIQUEID_MANUFACTURINGASSEMBLY);

														// STEP : Added to handle Re push only Failed and new discrete M11
														if (UIUtil.isNullOrEmpty(strSAPMBOMUpdatedOn) && UIUtil.isNullOrEmpty(strSAPUniqueID)) {
															// Mark SubContract Failed Flag False at start of each Make Assembly
															setSubContractFailedFlag(false);

															// STEP : Adding Manufacturing Assembly Header in JSON
															logger.writeLog("Processing Realized Item << " + strObjTitle + " >> Procurement Intent : << " + strObjProcIntent + " >> ");
															jHeaderPartObjectBuilder = addRealizedItemHeader(context, mobjectDetails);
															if (null != jHeaderPartObjectBuilder) {
																jEachPayloadBuilder.add(HEADER_PART, jHeaderPartObjectBuilder.build());

																// STEP : Invoking SAP WebService by attaching JSON
																jEachPayloadObj = jEachPayloadBuilder.build();
																requestString = jEachPayloadObj.toString();
																JSON_logger.writeLog("JSON Payload Request for Discrete : <<< " + strObjTitle + " : >>> \n " + requestString + " \n ");
																logger.writeLog("JSON Payload Request for Discrete : <<< " + strObjTitle + " : >>> \n " + requestString + " \n ");

																if (UIUtil.isNotNullAndNotEmpty(jEachPayloadObj.toString())) {
																	responseString = callPostService(context, jEachPayloadObj);
																	JSON_logger.writeLog("Response for Discrete : <<< " + strObjTitle + " :  Payload from SAP WebService >>> \n " + responseString + " \n ");

																	// STEP : Processing Response of SAP web Service
																	if (UIUtil.isNotNullAndNotEmpty(responseString))
																		ProcessWebServiceResponseForManufacturingAssembly(context, responseString);
																	else
																		logger.writeLog("INFO :: No response received from SAP Webservice for Realized Item << " + strObjTitle + " >> is with Procurement Intent : << " + strObjProcIntent
																				+ " >> from Change Action : << " + changeActionName + " >> ");
																} else
																	logger.writeLog("ERROR :: Failed to generate JSON Payload for realized item  : << " + strObjTitle + ">> header for Change Action : << " + changeActionName);
															} else
																logger.writeLog("ERROR :: Failed to add realized item  : << " + strObjTitle + ">> header for Change Action : << " + changeActionName);
														} else
															logger.writeLog("INFO ::Realized Item << " + strObjTitle + " >> is with Procurement Intent : << " + strObjProcIntent + " >> is already processed from Change Action : << "
																	+ changeActionName + " >> ");
													} else
														logger.writeLog("INFO ::Realized Item << " + strObjTitle + " >> is with Procurement Intent : << " + strObjProcIntent + " >> ");
												}
												else
													logger.writeLog("INFO ::Realized Item << " + strObjTitle + " >> is with Procurement Intent : << " + strObjProcIntent + " >> ");

											} else
												logger.writeLog("ERROR : Failed to get realized Item Id from Change Action : << " + changeActionName + " >> ");
										} else
											logger.writeLog("ERROR : Failed to get realized Item Id from Change Action : << " + changeActionName + " >>  ");
									}
								} else
									logger.writeLog("INFO : There are no relaized items under Change Action : << " + changeActionName + " >> ");
							} else
								logger.writeLog("ERROR : Failed to add change action header in JSON Object from Change Action : << " + changeActionName + " >> ");
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
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			getCloseableHttpClient().close();
			if (isContextPushed) {
				ContextUtil.popContext(context);
			}
		}
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
	private JsonObjectBuilder addChangeActionHeader(Context context) throws Exception {
		logger.writeLog("addChangeActionHeader...Start");

		// STEP : Creating JSON object Builder to add the change action object details
		JsonObjectBuilder jEachPayloadBuilder = Json.createObjectBuilder();
		try {
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
							|| RELEASEDDATE_CHANGEACTION.equalsIgnoreCase(attrName)) {
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
			if(null != jEachPayloadBuilder){
				//Set Change Action Header in JSON 
				setChangeActionHeader(jEachPayloadBuilder);
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
	private void addRealizedItemChildren(Context context, String strObjId, JsonObjectBuilder jHeaderPartObjectBuilder) throws Exception {
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

				logger.writeLog("Details of child part  :: " + mPartDetails);
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
					logger.writeLog("JSON Header for child :: << " + strTitle + ">> <<" + jMBOMPartBuilder.build().toString() + ">>>>>>>>>");
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
	 * This Method is to add child part information in Header Part
	 *
	 * @param context
	 * @param MAP     : child part information
	 * @return JsonObjectBuilder : After adding child part information returning
	 *         JsonObjectBuilder
	 * @throws Exception
	 */
	private void addChildPartHeader(Context context, Map mPartDetails, JsonObjectBuilder jMBOMPartBuilder, boolean isAssembly) throws Exception {
		logger.writeLog("addChildPartHeader : START !!");
		Map<?, ?> mLinkedCadPart = null;
		try {


			// STEP : Retrieving Child Part Details
			String strProcurementIntent = null;
			String strPartInterchangeability = null;
			String strServiceable = null;
			String strLinkedVPMReferenceId = null;
			String strObjectID = (String) mPartDetails.get(DomainConstants.SELECT_ID);
			String sName = (String) mPartDetails.get(DomainConstants.SELECT_NAME);
			String strTitle = (String) mPartDetails.get(ATTR_V_NAME);
			String strdescription = (String) mPartDetails.get(ATTR_V_DESCRIPTION);

			String strRelId = (String) mPartDetails.get(DomainConstants.SELECT_RELATIONSHIP_ID);
			String strhasConfigEffectivity = (String) mPartDetails.get(ATTR__HASCONFIGEFFECTIVITY);
			String strUnitOfMeasure = null;

			// STEP : If Child is Manufacturing Assembly ,Retrieving Procurement Intent
			// ,serviceable ,PartInterchangeability from Object details
			if (isAssembly) {
				strProcurementIntent = (String) mPartDetails.get(ATTR__PROCUREMENTINTENT_MANUFACTURINGASSEMBLY);
				strServiceable = (String) mPartDetails.get(ATTR__SERVICEABLEITEM_MANUFACTURINGASSEMBLY);
				strPartInterchangeability = (String) mPartDetails.get(ATTR__PARTINTERCHANGEABILITY_MANUFACTURINGASSEMBLY);
				strUnitOfMeasure = EACH;
			} else {
				if (null != strObjectID && !strObjectID.isEmpty()) {
					// STEP : If Child is other than Manufacturing Assembly ,Retrieving Procurement Intent , serviceable ,PartInterchangeability from corresponding Linked CAD PART
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
			if(TRUE.equals(strhasConfigEffectivity) )
			{
				Map<?, ?> mEffectivity = getEffectivity(context, strRelId);
				if(mEffectivity != null)
				{
					strEffectivityObject = mEffectivity.toString();

					if (strEffectivityObject.contains(EFFECTIVITY_VARIANT + ":")) {
						strVariantEffectivity = strEffectivityObject.substring(strEffectivityObject.indexOf(EFFECTIVITY_VARIANT + ":") + 20, strEffectivityObject.indexOf("ExpressionFormat") - 1);
						//strVariantEffectivity = sortOptionCode(strVariantEffectivity);
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

			jMBOMPartBuilder.add(TAG_DATEFROM, getCACompletionDate());
			jMBOMPartBuilder.add(TAG_DATETO, "12-31-9999");
			jMBOMPartBuilder.add(TAG_REALIZED_DATA, true);

			if (false == isAssembly) {
				if( null != mLinkedCadPart ){
					if (SUBCONTRACT.equalsIgnoreCase(strProcurementIntent)) {
						processSubContractPart(context , mLinkedCadPart );
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		logger.writeLog("addChildPartHeader : END !!");
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

	private void processSubContractPart(Context context, Map mLinkedCadPart) throws Exception {
		logger.writeLog("processSubContractPart : START !!");
		try {

			// STEP : Retrieving necessary information of SubContract Part
			// STEP : Corresponding Sub-contract part and its child with Procurement intent
			// "Buy-SubC" :: Write this info in another JSON Payload
			String strObjectID = (String) mLinkedCadPart.get(DomainConstants.SELECT_ID);
			String strRelId = getRootSubContractRelId();
			if (UIUtil.isNotNullAndNotEmpty(strObjectID)){
				MapList mlSubContractList = new MapList();
				mlSubContractList = getAllLevelSubContract(context, strObjectID);
				if(mlSubContractList.size() != 0) {
					logger.writeLog("AllLevelSubContract Count <<<<<<<<<<<<<<<<< " + mlSubContractList.size());

					Map mSubContractPartDetails ;
					String strProcIntent ;
					String strObjID ;
					for (int kdx = mlSubContractList.size()-1; kdx >= 0; --kdx) {
						mSubContractPartDetails = (Map<?, ?>) mlSubContractList.get(kdx);
						logger.writeLog(" SubContract  <<<<<<<<<<<<<<<<< " + mSubContractPartDetails);

						strProcIntent = (String) mSubContractPartDetails.get(ATTR__PROCUREMENTINTENT_VPMREFERENCE);
						if (SUBCONTRACT.equalsIgnoreCase(strProcIntent)) {
							strRelId = (String) mSubContractPartDetails.get(DomainConstants.SELECT_RELATIONSHIP_ID);
							sendSubContractPartPayload(context , mSubContractPartDetails ,strRelId);
						}   
					}
					//sendSubContractPartPayload(context , mLinkedCadPart,strRelId );
				}
				sendSubContractPartPayload(context , mLinkedCadPart,strRelId );
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		logger.writeLog("processSubContractPart : END !!");
		return ;
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

	private void sendSubContractPartPayload(Context context, Map mSubContractPartDetails ,String strRelId  ) throws Exception {
		logger.writeLog("sendSubContractPartPayload : START !!");

		try {

			//STEP : Get Change Action Header 
			JsonObjectBuilder jEachSubContractPayloadBuilder = getChangeActionHeader();
			if(null != jEachSubContractPayloadBuilder){
				//STEP : Get SubContract part Header

				JsonObjectBuilder jSubContractBuilder  = getSubContractPartHeader(context  , mSubContractPartDetails ,strRelId);
				if(null != jSubContractBuilder){

					// STEP : Retrieving all level BuySUBC Cad part , single level SubContract Cad part and adding in JSON array
					JsonArrayBuilder jsubContractChildPartArrayBuilder = Json.createArrayBuilder();
					MapList mlObjects = new MapList();
					String strObjectID = (String)mSubContractPartDetails.get(DomainConstants.SELECT_ID);	
					String strTitle = (String) mSubContractPartDetails.get(ATTR_V_NAME);
					mlObjects = getFirstLevelChildren(context, strObjectID);
					if (mlObjects != null) {
						MapList mlBuySubCObjects = new MapList();
						Map mchildPartDetails;
						String strProcIntent ;
						String strObjID ;

						// STEP : Processing First level Children of SubContract 
						JsonObjectBuilder jChildBuilder = Json.createObjectBuilder() ;
						for (int kdx = 0; kdx < mlObjects.size(); kdx++) { 
							mchildPartDetails = (Map<?, ?>) mlObjects.get(kdx);
							strProcIntent = (String) mchildPartDetails.get(ATTR__PROCUREMENTINTENT_VPMREFERENCE);
							strObjID = (String) mchildPartDetails.get(DomainConstants.SELECT_ID);

							// STEP : adding SUBCONTRACT part in array of Child JSON 
							if (SUBCONTRACT.equalsIgnoreCase(strProcIntent) ) {
								jChildBuilder = addCADChildPartHeader(context, mchildPartDetails);
								if(null != jChildBuilder)
									jsubContractChildPartArrayBuilder.add(jChildBuilder.build());
							}
							else 
							{
								// STEP : adding all level BUYSUBC part in array of Child JSON 
								if(BUYSUBC.equalsIgnoreCase(strProcIntent)){
									jChildBuilder = addCADChildPartHeader(context, mchildPartDetails);
									if(null != jChildBuilder)
										jsubContractChildPartArrayBuilder.add(jChildBuilder.build());
								}

								mlBuySubCObjects = getAllLevelBuySubC(context, strObjID);
								if (mlBuySubCObjects != null) {
									for (int idx = 0; idx < mlBuySubCObjects.size(); idx++) {
										mchildPartDetails = (Map<?, ?>) mlBuySubCObjects.get(idx);
										strProcIntent = (String) mchildPartDetails.get(ATTR__PROCUREMENTINTENT_VPMREFERENCE);
										if (BUYSUBC.equalsIgnoreCase(strProcIntent)) {
											jChildBuilder = addCADChildPartHeader(context, mchildPartDetails);
											if(null != jChildBuilder)
												jsubContractChildPartArrayBuilder.add(jChildBuilder.build());
										}   
									}
								} 
							}
						}
					}
					//STEP : creating  the  SubContract Part header  JSON Object
					if(null != jsubContractChildPartArrayBuilder) {
						if(!jsubContractChildPartArrayBuilder.build().isEmpty()){
							jSubContractBuilder.add(HEADER_CHILDREN, jsubContractChildPartArrayBuilder.build());
						}
						if(null != jSubContractBuilder)
							jEachSubContractPayloadBuilder.add(HEADER_PART ,jSubContractBuilder.build());

						JsonObject jSubContractPayloadObj = jEachSubContractPayloadBuilder.build();
						if(jSubContractPayloadObj.containsKey(HEADER_PART)){

							String jsonEachPayloadString = jSubContractPayloadObj.toString();
							JSON_logger.writeLog("JSON Payload Request for SubContract : <<< " + strTitle +" : >>> \n " + jsonEachPayloadString + " \n ");
							String responseString = callPostService(context ,jSubContractPayloadObj );
							JSON_logger.writeLog("Response for SubContract : <<< " + strTitle +" :  Payload from SAP WebService >>> \n " + responseString + " \n ");

							if (UIUtil.isNotNullAndNotEmpty(responseString))
								ProcessWebServiceResponseForSubContract(context , responseString) ;
							else
								logger.writeLog("ERROR :: No response received from SAP Webservice for SubContract << " + strTitle + " >>");
						}
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		logger.writeLog("sendSubContractPartPayload : END !!");

	}


	/**
	 * This Method is to create  the  SubContract Part header  in JSON
	 * 
	 * @param context
	 * @param Details of SubContract Part 
	 * @return  JsonObjectBuilder [after adding SubContract Part and its child BuySubC Part information in JSON]
	 * @throws Exception
	 */

	private JsonObjectBuilder getSubContractPartHeader( Context context , Map mSubContractPartDetails ,String strRelId ) throws Exception
	{
		logger.writeLog("addSubContractPartHeader : START !!");
		JsonObjectBuilder jSubContractBuilder = Json.createObjectBuilder();
		try {
			//STEP : Retrieving necessary information of SubContract Part
			String strObjectID = (String)mSubContractPartDetails.get(DomainConstants.SELECT_ID);						
			String sName = (String) mSubContractPartDetails.get(DomainConstants.SELECT_NAME);
			String strTitle = (String) mSubContractPartDetails.get(ATTR_V_NAME);
			String strdescription = (String) mSubContractPartDetails.get(ATTR_V_DESCRIPTION);
			String strProcurementIntent = (String) mSubContractPartDetails.get(ATTR__PROCUREMENTINTENT_VPMREFERENCE);
			String strPartInterchangeability =  (String) mSubContractPartDetails.get(ATTR__PARTINTERCHANGEABILITY_VPMREFERENCE);
			String strServiceable =  (String) mSubContractPartDetails.get(ATTR__SERVICEABLEITEM_VPMREFERENCE);
			String strhasConfigEffectivity = (String) mSubContractPartDetails.get(ATTR__HASCONFIGEFFECTIVITY);

			String strVariantEffectivity= null;
			String strEffectivityObject = null;
			if(TRUE.equals(strhasConfigEffectivity) )
			{
				Map<?, ?> mEffectivity = getEffectivity(context,strRelId);
				if(mEffectivity != null)
				{
					strEffectivityObject = mEffectivity.toString();

					if(strEffectivityObject.contains(EFFECTIVITY_VARIANT + ":")){
						strVariantEffectivity = strEffectivityObject.substring(strEffectivityObject.indexOf(EFFECTIVITY_VARIANT + ":")+20, strEffectivityObject.indexOf("ExpressionFormat")- 1);
						//strVariantEffectivity = sortOptionCode(strVariantEffectivity);
					}
				}
			}

			//STEP : Adding information of SubContract Part IN JSON Object
			if (UIUtil.isNotNullAndNotEmpty(strTitle))
				jSubContractBuilder.add(TAG_TITLE,strTitle);
			else
				jSubContractBuilder.add(TAG_TITLE," ");

			if (UIUtil.isNotNullAndNotEmpty(sName))
				jSubContractBuilder.add(TAG_NAME,sName);
			else
				jSubContractBuilder.add(TAG_NAME," ");

			if (UIUtil.isNotNullAndNotEmpty(strObjectID))
				jSubContractBuilder.add(TAG_OID,strObjectID);
			else
				jSubContractBuilder.add(TAG_OID," ");

			if (UIUtil.isNotNullAndNotEmpty(strRelId))
				jSubContractBuilder.add(TAG_REL_ID,strRelId);
			else
				jSubContractBuilder.add(TAG_REL_ID," ");

			if (UIUtil.isNotNullAndNotEmpty(strdescription))
				jSubContractBuilder.add(TAG_DESCRIPTION,strdescription);
			else
				jSubContractBuilder.add(TAG_DESCRIPTION," ");

			if (UIUtil.isNotNullAndNotEmpty(strProcurementIntent))
				jSubContractBuilder.add(TAG_PROCUREMENTINTENT,strProcurementIntent);
			else
				jSubContractBuilder.add(TAG_PROCUREMENTINTENT," ");

			if (UIUtil.isNotNullAndNotEmpty(strServiceable))
				jSubContractBuilder.add(TAG_SERVICEABLEITEM,strServiceable);
			else
				jSubContractBuilder.add(TAG_SERVICEABLEITEM," ");

			jSubContractBuilder.add(TAG_PLANTCODE,NOT_APPLICABLE);

			if (UIUtil.isNotNullAndNotEmpty(strPartInterchangeability))
				jSubContractBuilder.add(TAG_PARTINTERCHANGEABILITY,strPartInterchangeability);
			else
				jSubContractBuilder.add(TAG_PARTINTERCHANGEABILITY," ");

			if (UIUtil.isNotNullAndNotEmpty(strVariantEffectivity))
				jSubContractBuilder.add(TAG_VARIAENT_Effectivity,strVariantEffectivity);
			else
				jSubContractBuilder.add(TAG_VARIAENT_Effectivity," ");

			jSubContractBuilder.add(TAG_DATEFROM,getCACompletionDate());
			jSubContractBuilder.add(TAG_DATETO,"12-31-9999");
			jSubContractBuilder.add(TAG_REALIZED_DATA,true);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		logger.writeLog("addSubContractPartHeader : END !!");
		return jSubContractBuilder;
	}

	/**
	 * This Method is to add the BuySubC Part information in JSON
	 *
	 * @param context
	 * @param Details of BuySubC childs
	 * @return JsonArrayBuilder [after adding BuySubC Part information in JSON]
	 * @throws Exception
	 */
	private JsonObjectBuilder addCADChildPartHeader(Context context, Map mchildPartDetails) throws Exception {
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

			if(TRUE.equals(strhasConfigEffectivity) )
			{
				Map<?, ?> mEffectivity = getEffectivity(context, strRId);
				if(mEffectivity != null)
				{
					strEffectivityObject = mEffectivity.toString();

					if (strEffectivityObject.contains(EFFECTIVITY_VARIANT + ":")) {
						strVariantEffectivity = strEffectivityObject.substring(strEffectivityObject.indexOf(EFFECTIVITY_VARIANT + ":") + 20, strEffectivityObject.indexOf("ExpressionFormat") - 1);
						//strVariantEffectivity = sortOptionCode(strVariantEffectivity);
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
			mlMBOMList = partDomObj.getRelatedObjects(context,
					relPattern.getPattern(), // relationship pattern
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
	 * This Method Accepts Relationship ID to get its effectivity (Varient
	 * Effectivity and Date Effectivity)
	 *
	 * @param context
	 * @param strRelId :Relationship Id
	 * @return Effectivity Details of Relationship
	 * @throws Exception
	 */

	public Map getEffectivity(Context context, String strRelId)  throws Exception
	{		
		logger.writeLog("getEffectivity : START!!");
		Map<?,?> mEffectivity= null;
		try
		{			
			ContextUtil.pushContext(
					context,
					ROLE_ADMIN,
					DomainConstants.EMPTY_STRING,
					DomainConstants.EMPTY_STRING);
			ConfigurationExposedFilterablesFactory configurationExposedFilterablesactory = new ConfigurationExposedFilterablesFactory();
			IConfigurationExposedFilterables iConfigurationExposedFilterables = configurationExposedFilterablesactory.getIPublicConfigurationFilterablesServices();

			List<String> objects = new List<String>();			  
			objects.add(strRelId);

			//StringList RelIDList = new StringList();
			//RelIDList.add(strRelId);			

			mEffectivity=iConfigurationExposedFilterables.getEffectivitiesContent(context, objects, com.dassault_systemes.plm.config.exposed.constants.Domain.ALL, com.dassault_systemes.plm.config.exposed.constants.Format.TXT, com.dassault_systemes.plm.config.exposed.constants.View.ALL);				
		}
		catch(Exception ex)
		{
			String strException = ex.toString();
			if(strException.contains(CFG_EXPOSED_EXCEPTION)){
				return null;
			}
			else{
				logger.writeLog("getEffectivity :exception ::: " + ex.toString());
				ex.printStackTrace();
				throw ex;
			}
		}
		finally{
			ContextUtil.popContext(context);
		}		
		logger.writeLog("getEffectivity : END!!");
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
	private Map getLinkedCADPartFromMBOMPart(Context context, String strOBJId, String strRel ) throws Exception {
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
						strCADConnectionPathObjectId = (String) strListCADConnectionPathIDSplit.get(strListCADConnectionPathIDSplit.size() - 1);
						if (UIUtil.isNotNullAndNotEmpty(strCADConnectionPathObjectId)) {
							try {

								DomainRelationship domRel = new DomainRelationship(strCADConnectionPathObjectId);
								java.util.Hashtable relInfoMap = domRel.getRelationshipData(context, relInfoList);

								StringList slVPMReferenceId =(StringList) relInfoMap.get(DomainConstants.SELECT_TO_ID);
								String ostrVPMReferenceId =(String) slVPMReferenceId.get(0);
								if(UIUtil.isNotNullAndNotEmpty(ostrVPMReferenceId)){
									StringList slRelId =(StringList) relInfoMap.get(DomainConstants.SELECT_ID);
									String strRelId =(String) slRelId.get(0);
									if (UIUtil.isNotNullAndNotEmpty(strRelId))
										setRootSubContractRelId(strRelId);

									DomainObject boCADObj = DomainObject.newInstance(context,ostrVPMReferenceId);
									objInfoMap  = boCADObj.getInfo(context,objectSelects);
									if(!objInfoMap.isEmpty()){
										//System.out.println("objInfoMap " + objInfoMap);
										return objInfoMap;
									}
									else
										return objInfoMap;
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
				MapList ebomList = partDomObj.getRelatedObjects(context,
						TYPE_VPMINSTANCE, // relationship pattern
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
				Map tempMap;
				if (ebomList.size() > 0) {
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
				mlObjects = partDomObj.getRelatedObjects(context,
						TYPE_VPMINSTANCE, // relationship pattern
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
				MapList ebomList = partDomObj.getRelatedObjects(context,
						TYPE_VPMINSTANCE, // relationship pattern
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
				Map tempMap;
				if (ebomList.size() > 0) {
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
			String authString = getUserName() + ":" + getPassword();
			String authStringEnc = BASE64_ENCODER.encodeToString(authString.getBytes());

			// STEP : Creating HttpGet Object with SAP webService URL
			HttpGet getURL = new HttpGet(getURL());
            logger.writeLog("WebService URL " + getURL);
			String AUTHORIZATION = EnoviaResourceBundle.getProperty(context, LCD_3DXSAPIntegrationKey, "LCD_3DXSAPStringResource_en.SAP.Authorization", getLanguage());
			String XCSRFTOKEN = EnoviaResourceBundle.getProperty(context, LCD_3DXSAPIntegrationKey, "LCD_3DXSAPStringResource_en.SAP.XCSRFTOKEN", getLanguage());

			// STEP : Creating request header for SAP WebService GET Method call
			getURL.setHeader(AUTHORIZATION, "Basic " + authStringEnc);
			getURL.setHeader(XCSRFTOKEN, "fetch");

			// STEP : Invoking SAP webService with GET Method
			HttpResponse response = getCloseableHttpClient().execute(getURL);

			// STEP : Collecting the acknowledgement from SAP webService
			int responseCode = response.getStatusLine().getStatusCode();
			strStatusLine = response.getStatusLine().toString();
			if (responseCode == HttpStatus.SC_OK) { // success

				// STEP : Collecting the x-csrf-token and cookies from SAP webService Response
				Header[] resHeaders = response.getAllHeaders();
				int index = -1;
				index = resHeaders[1].getValue().indexOf(":");
				String strCookie = resHeaders[1].getValue().substring(index + 1);
				String xcsrftoken = null;
				for (Header header : resHeaders) {
					if (XCSRFTOKEN.equalsIgnoreCase(header.getName())) {
						xcsrftoken = header.getValue();
					}
				}

				if (UIUtil.isNotNullAndNotEmpty(xcsrftoken) && UIUtil.isNotNullAndNotEmpty(xcsrftoken)) {
					logger.writeLog(XCSRFTOKEN + xcsrftoken);
					logger.writeLog("cookies" + strCookie);
					logger.writeLog("GET request successful , Response code ==  " + response.getStatusLine().getStatusCode());
					logger.writeLog("GET request Successful , Response Status Line==  " + strStatusLine);
					setCSRFToken(xcsrftoken);
					setSessionCookie(strCookie);
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
	 * This Method Accepts xcsrftoken and cookie retrieved from GET SAP webService
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
		HttpPost postURL = new HttpPost(getURL());
		String result = null;
		try {
			// STEP : Creating request header for SAP WebService POST Method call
			String XCSRFTOKEN = EnoviaResourceBundle.getProperty(context, LCD_3DXSAPIntegrationKey, "LCD_3DXSAPStringResource_en.SAP.XCSRFTOKEN", getLanguage());
			String COOKIE = EnoviaResourceBundle.getProperty(context, LCD_3DXSAPIntegrationKey, "LCD_3DXSAPStringResource_en.SAP.COOKIE", getLanguage());
			String CONTENTTYPE = EnoviaResourceBundle.getProperty(context, LCD_3DXSAPIntegrationKey, "LCD_3DXSAPStringResource_en.SAP.CONTENTTYPE", getLanguage());

			StringEntity params = new StringEntity(jEachPayloadObj.toString());
			postURL.setHeader(COOKIE, getSessionCookie());
			postURL.setHeader(CONTENTTYPE, "application/json");
			postURL.setHeader(XCSRFTOKEN, getCSRFToken());
			postURL.setEntity(params);

			// STEP : Invoking SAP webService with Post Method
			long startTime = Calendar.getInstance().getTimeInMillis();
			logger.writeLog("Time before calling SAP webservice using POST method ==  " + startTime);
			HttpResponse response = getCloseableHttpClient().execute(postURL);
			long endTime = Calendar.getInstance().getTimeInMillis();
			logger.writeLog("Time after execution of  SAP webservice using POST method ==  " + endTime);
			logger.writeLog("Total time taken by SAP Webservice for processing ==  "+ (endTime - startTime ) + " ms");
			float sec = (endTime - startTime) / 1000F;
			logger.writeLog("Total time taken by SAP Webservice for processing ==  "+ (sec) + " seconds");


			int responseCode = response.getStatusLine().getStatusCode();
			if (responseCode == HttpStatus.SC_OK) { // success
				// STEP : Collecting the acknowledgement from SAP webService
				logger.writeLog("POST request successful , response code ==  " + response.getStatusLine().getStatusCode());
			} else {
				logger.writeLog("POST request failed , response code ==  " + response.getStatusLine().getStatusCode());
			}
			result = EntityUtils.toString(response.getEntity());

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
	private void ProcessWebServiceResponseForSubContract(Context context, String responseString) throws Exception {
		logger.writeLog("ProcessWebServiceResponseForSubContract START");
		SimpleDateFormat DateFormat = new SimpleDateFormat("MM/dd/yyyy hh.mm.ss aa");
		try {
			JsonReader jsonReader = Json.createReader(new StringReader(responseString));
			JsonObject jWebServiceResponse = jsonReader.readObject();
			jsonReader.close();
			if (null != jWebServiceResponse) {
				if (jWebServiceResponse.containsKey(HEADER_PART)) {
					JsonObject jPayloadObject = jWebServiceResponse.getJsonObject(HEADER_PART);
					if (null != jPayloadObject) {
						String resultType = jWebServiceResponse.getString(TAG_TYPE);
						if (UIUtil.isNotNullAndNotEmpty(resultType) && resultType.equalsIgnoreCase(FAIL)) {
							setSubContractFailedFlag(true);
							String strErrorMessage = jPayloadObject.getString(TAG_ERROR_MESSAGE);
							String strRelID = jPayloadObject.getString(TAG_REL_ID);

							if (UIUtil.isNotNullAndNotEmpty(strRelID) && UIUtil.isNotNullAndNotEmpty(strErrorMessage))
								addRelationshipHistory(context, strRelID, strErrorMessage);

							JsonArray jArrayOfChildParts = jPayloadObject.getJsonArray(HEADER_CHILDREN);
							if (null != jArrayOfChildParts) {
								JsonObject jchildPart;
								String strPartRelID;
								String sErrorMessage;
								for (int i = 0; i < jArrayOfChildParts.size(); i++) {
									jchildPart = jArrayOfChildParts.getJsonObject(i);
									strPartRelID = jchildPart.getString(TAG_REL_ID);
									sErrorMessage = jchildPart.getString(TAG_ERROR_MESSAGE);

									if (UIUtil.isNotNullAndNotEmpty(strPartRelID) && UIUtil.isNotNullAndNotEmpty(sErrorMessage))
										addRelationshipHistory(context, strPartRelID, sErrorMessage);
								}
							}
						} else if (UIUtil.isNotNullAndNotEmpty(resultType) && resultType.equalsIgnoreCase(SUCCESS)) {
							String strRelID = jPayloadObject.getString(TAG_REL_ID);
							if (UIUtil.isNotNullAndNotEmpty(strRelID)) {
								DomainRelationship domRel = DomainRelationship.newInstance(context, strRelID);
								// Push context as no Manufacturing Assembly access to 3DXLeader in release
								// state in respective policy.
								try {
									Date date = new Date();
									ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "person_UserAgent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
									domRel.setAttributeValue(context, ATTR__SAP_CAD_INSATNCE_UPDATED_ON, DateFormat.format(date));
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
											ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "person_UserAgent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
											domRelationship.setAttributeValue(context, ATTR__SAP_CAD_INSATNCE_UPDATED_ON, DateFormat.format(date));
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
	 * @throws Exception
	 */
	private void ProcessWebServiceResponseForManufacturingAssembly(Context context, String responseString) throws Exception {
		logger.writeLog("ProcessWebServiceResponseForManufacturingAssembly START");

		SimpleDateFormat DateFormat = new SimpleDateFormat("MM/dd/yyyy hh.mm.ss aa");
		try {
			JsonReader jsonReader = Json.createReader(new StringReader(responseString));
			JsonObject jWebServiceResponse = jsonReader.readObject();
			jsonReader.close();
			if (null != jWebServiceResponse) {
				if (jWebServiceResponse.containsKey(HEADER_PART)) {
					JsonObject jPayloadObject = jWebServiceResponse.getJsonObject(HEADER_PART);
					if (null != jPayloadObject) {
						String resultType = jWebServiceResponse.getString(TAG_TYPE);


						if (UIUtil.isNotNullAndNotEmpty(resultType) && resultType.equalsIgnoreCase(FAIL)) {
							String strErrorMessage = jPayloadObject.getString(TAG_ERROR_MESSAGE);
							String strObjID = jPayloadObject.getString(TAG_OID);
							String strObjTitle = jPayloadObject.getString(TAG_TITLE);

							if(UIUtil.isNotNullAndNotEmpty(strObjID) && UIUtil.isNotNullAndNotEmpty(strErrorMessage))
							{
								strErrorMessage = strObjTitle + ":" + strErrorMessage;
								addObjectHistory(context,strObjID, strErrorMessage);
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

									if (UIUtil.isNotNullAndNotEmpty(strPartRelID) && UIUtil.isNotNullAndNotEmpty(sErrorMessage)) {
										addRelationshipHistory(context, strPartRelID, sErrorMessage); // adding history on MBOM Instance

										strErrorMessage = strPartTitle + ":" + sErrorMessage;
										addObjectHistory(context, strObjID, strErrorMessage); // Adding history on M11 Object
									}
								}
							}
						} else if (UIUtil.isNotNullAndNotEmpty(resultType) && resultType.equalsIgnoreCase(SUCCESS)) {
							String strObjID = jPayloadObject.getString(TAG_OID);
							if (UIUtil.isNotNullAndNotEmpty(strObjID)) {
								DomainObject HeaderObject = new DomainObject(strObjID);
								String strSAPUniqueID = jPayloadObject.getString(TAG_SAPUNIQUE_ID);
								// Push context as no Manufacturing Assembly access to 3DXLeader in release state in respective policy.
								try {
									if (getSubContractFailedFlag() == false) 
									{
										Date date = new Date();
										ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "person_UserAgent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
										HeaderObject.setAttributeValue(context, ATTR__SAP_UNIQUEID, strSAPUniqueID);
										HeaderObject.setAttributeValue(context, ATTR__SAPMBOM_UPDATED_ON, DateFormat.format(date));
										ContextUtil.popContext(context);
									}
								} catch (Exception e) {
									throw e;
								}
								JsonArray jArrayOfChildParts = jPayloadObject.getJsonArray(HEADER_CHILDREN);
								if (null != jArrayOfChildParts) {
									JsonObject jchildPart;
									String sRelID;
									DomainRelationship domRel ;
									for (int i = 0; i < jArrayOfChildParts.size(); i++) {
										jchildPart = jArrayOfChildParts.getJsonObject(i);
										sRelID = jchildPart.getString(TAG_REL_ID);
										if (UIUtil.isNotNullAndNotEmpty(sRelID)) {
											domRel = DomainRelationship.newInstance(context, sRelID);
											try {
												Date date = new Date();
												ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "person_UserAgent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
												domRel.setAttributeValue(context, ATTR__SAP_INSATNCE_UPDATED_ON, DateFormat.format(date));
												ContextUtil.popContext(context);
											} catch (Exception me) {
												throw me;
											}
										}
									}
								}
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
				ChangeUtil.addHistory(context, strId, strError, strComment.toString());
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

	/***
	 * This method is to sort the Option code
	 *
	 * @param strOptionCode
	 * @return
	 * @throws Exception
	 */
	private String sortOptionCode(String strOptionCode) throws Exception {
		String strSortedOptionCode = null;
		try {
			Character charStartingBraces = new Character('{');
			Character charClosingBraces = new Character('}');

			for (int i = 0; i < strOptionCode.length(); i++) {
				if (charStartingBraces.equals(strOptionCode.charAt(i)) || charClosingBraces.equals(strOptionCode.charAt(i))) {
					String strTempOptionCodeToSort = null;
					String strTemp = null;
					int icount = 0;

					for (int j = i; j < strOptionCode.length(); j++) {
						strTempOptionCodeToSort = strTempOptionCodeToSort + strOptionCode.charAt(j);

						if (charClosingBraces.equals(strOptionCode.charAt(j))) {
							break;
						}
						icount++;
					}

					// Removing Starting Braces and Closing Braces from Option Code
					strTempOptionCodeToSort = strTempOptionCodeToSort.substring(1, strTempOptionCodeToSort.length() - 1);

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
	 * Background job Method for Synch MBOM To SAP Returns nothing
	 *
	 * @param context
	 * @param args
	 * @return
	 * @throws Exception
	 */
	public void callSyncToSAPJob(Context context, String[] args) throws Exception {
		String sFinalErrorMessage = callWSSyncToSAPJob(context, args);
		if (UIUtil.isNotNullAndNotEmpty(sFinalErrorMessage)) {
			throw new Exception(sFinalErrorMessage);
		}
	}

	/**
	 * Background job Method for Synch MBOM To SAP Returns Error string message if
	 * any error or send empty string.
	 *
	 * @param context
	 * @param args
	 * @return
	 * @throws Exception
	 */
	private String callWSSyncToSAPJob(Context context, String[] args) throws Exception {
		setLanguage(context); // Setting Session Language

		String strStatus = DomainConstants.EMPTY_STRING;
		HashMap<?, ?> programMap = (HashMap<?, ?>) JPO.unpackArgs(args);

		// STEP : Retrieving Input Arguments
		String strCAId = (String) programMap.get("objectId");
		if (UIUtil.isNullOrEmpty(strCAId)) {
			return "Input args are empty";
		}

		DomainObject domCAObj = DomainObject.newInstance(context, strCAId);
		String changeActionName = domCAObj.getInfo(context, DomainConstants.SELECT_NAME);

		String[] arrJPOArgs = new String[1];
		if (UIUtil.isNotNullAndNotEmpty(strCAId)) {
			arrJPOArgs[0] = strCAId;
		}

		// STEP : Check for if background Job is already running
		boolean isJobRunning = checkIfJobIsRunning(context);
		if (isJobRunning) {
			String strBackgroundJobInfo = EnoviaResourceBundle.getProperty(context, LCD_3DXSAPIntegrationKey, "LCD_3DXSAPStringResource_en.Alert.Msg.JobAlreadyRunning", getLanguage());
			emxContextUtilBase_mxJPO.mqlNotice(context, strBackgroundJobInfo);
			return strBackgroundJobInfo;
		}

		try {
			// STEP : Collecting JPO Name and Method name from properties file to invoke it
			String strBackgroundJobTitle = EnoviaResourceBundle.getProperty(context, LCD_3DXSAPIntegrationKey, "LCD_3DXSAPStringResource_en.JOB.Title", getLanguage());
			String strBackgroundJobDescription = EnoviaResourceBundle.getProperty(context, LCD_3DXSAPIntegrationKey, "LCD_3DXSAPStringResource_en.JOB.Description", getLanguage());
			String strJPOName = EnoviaResourceBundle.getProperty(context, LCD_3DXSAPIntegrationKey, "LCD_3DXSAPStringResource_en.JPO.NAME", getLanguage());
			String strJPOMethodName = EnoviaResourceBundle.getProperty(context, LCD_3DXSAPIntegrationKey, "LCD_3DXSAPStringResource_en.JPO.METHOD", getLanguage());
			ContextUtil.startTransaction(context, true);
			Job job = new Job(strJPOName, strJPOMethodName, arrJPOArgs);
			job.setName(changeActionName);
			job.setTitle(strBackgroundJobTitle);
			job.setDescription(strBackgroundJobDescription);
			job.createAndSubmit(context);
		} catch (Exception e) {
			ContextUtil.abortTransaction(context);
			String strBackgroundJobFail = EnoviaResourceBundle.getProperty(context, LCD_3DXSAPIntegrationKey, "LCD_3DXSAPStringResource_en.Alert.Msg.JobProcessingFailed", getLanguage());
			emxContextUtilBase_mxJPO.mqlNotice(context, strBackgroundJobFail);
			e.printStackTrace();
		} finally {
			ContextUtil.commitTransaction(context);
			String strBackgroundJobSuccess = EnoviaResourceBundle.getProperty(context, LCD_3DXSAPIntegrationKey, "LCD_3DXSAPStringResource_en.Alert.Msg.JobProcessedSuccessfully", getLanguage());
			emxContextUtilBase_mxJPO.mqlNotice(context, strBackgroundJobSuccess);
		}
		return strStatus;
	}

	/**
	 * Background job Method for checking job is already running error or send empty
	 * string.
	 *
	 * @param context
	 * @return status in String
	 * @throws Exception
	 */
	private boolean checkIfJobIsRunning(Context context) throws Exception {
		MapList mJobList = emxJobBase_mxJPO.getBackgroundJobs(context, "Current");

		if (null != mJobList) {
			HashMap<?, ?> mJobs = null;
			String sJobId = null;
			DomainObject objJob = null;
			String sTitle = null;
			String strBackgroundJobTitle = EnoviaResourceBundle.getProperty(context, LCD_3DXSAPIntegrationKey, "LCD_3DXSAPStringResource_en.JOB.Title", getLanguage());
			for (Iterator<?> iterator = mJobList.iterator(); iterator.hasNext();) {
				mJobs = (HashMap<?, ?>) iterator.next();
				sJobId = (String) mJobs.get("id");
				objJob = DomainObject.newInstance(context, sJobId);
				sTitle = objJob.getInfo(context, "attribute[Title]");
				if (sTitle.equalsIgnoreCase(strBackgroundJobTitle)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * This Method is to Initialized the logger
	 *
	 * @throws Exception
	 */
	private void LoggerInitialization(Context context) throws Exception {
		try {
			String strCompleteLogger = EnoviaResourceBundle.getProperty(context, LCD_3DXSAPIntegrationKey, "LCD_3DXSAPStringResource_en.logger.CompleteLogger", getLanguage());
			String strJSONLogger = EnoviaResourceBundle.getProperty(context, LCD_3DXSAPIntegrationKey, "LCD_3DXSAPStringResource_en.logger.JSONLogger", getLanguage());
			String strDateFormat = EnoviaResourceBundle.getProperty(context, LCD_3DXSAPIntegrationKey, "LCD_3DXSAPStringResource_en.logger.DateFormat", getLanguage());
			String strExtension = EnoviaResourceBundle.getProperty(context, LCD_3DXSAPIntegrationKey, "LCD_3DXSAPStringResource_en.logger.Extension", getLanguage());
			String strUnderscore = EnoviaResourceBundle.getProperty(context, LCD_3DXSAPIntegrationKey, "LCD_3DXSAPStringResource_en.logger.UnderScore", getLanguage());

			// STEP : Log Creation
			SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yy_hh_mm_ss");
			String strCurrentDate = dateFormat.format(new Date());
			System.out.println("dateFormat<<<<<<<<< " + dateFormat );

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

			String strLogPath = EnoviaResourceBundle.getProperty(context, LCD_3DXSAPIntegrationKey, "LCD_3DXSAPStringResource_en.logger.logPath", getLanguage());
			File tmpDir = new File(strLogPath);
			boolean exists = tmpDir.exists();

			if (!exists) {
				System.out.println("Directory doesn't exist!!");
				tmpDir.mkdirs();
			} 
			String changeActionName = getChangeActionJson().getJsonObject("changeaction").getString("name");
			logger = new SimpleLogger(strLogPath + changeActionName + strUnderscore + strCompleteLogName);
			JSON_logger = new SimpleLogger(strLogPath + changeActionName + strUnderscore + strJSONLogName);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return;
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
			if (realizedItems.size() > 0) {
				StringList objectSelects = new StringList();
				JsonObject realizedItem;
				String realizedItemType;
				String realizedItemId;
				Map mobjectDetails;
				String strObjProcureIntent;
				String strHasConfig;
				String strObjId;
				String strTitle  ;
				DomainObject domRealizedItemObj;
				for (int i = 0; i < realizedItems.size(); i++) {
					realizedItem = realizedItems.getJsonObject(i);
					realizedItemType = realizedItem.getJsonObject("where").getJsonObject("info").getString("type");
					realizedItemId = realizedItem.getJsonObject("where").get("id").toString().split(":")[1].replaceAll("\"", "");
					if (UIUtil.isNotNullAndNotEmpty(realizedItemId)) {
						domRealizedItemObj = DomainObject.newInstance(context, realizedItemId);

						objectSelects.add(ATTR_V_NAME);
						objectSelects.add(DomainConstants.SELECT_ID);
						objectSelects.add(ATTR__PROCUREMENTINTENT_MANUFACTURINGASSEMBLY);
						objectSelects.add(ATTR__HASCONFIGCONTEXT_VPMREFERENCE);
						objectSelects.add(ATTR__SAPMBOMUPDATEDON_MANUFACTURINGASSEMBLY);

						mobjectDetails = domRealizedItemObj.getInfo(context, objectSelects);
						strObjProcureIntent = (String) mobjectDetails.get(ATTR__PROCUREMENTINTENT_MANUFACTURINGASSEMBLY);
						strHasConfig = (String) mobjectDetails.get(ATTR__HASCONFIGCONTEXT_VPMREFERENCE);

						if (realizedItemType.equalsIgnoreCase(TYPE_MANUFACTURINGASSEMBLY) && strObjProcureIntent.equalsIgnoreCase(DESCRETE) && "FALSE".equalsIgnoreCase(strHasConfig)) {
							strObjId = (String) mobjectDetails.get(DomainConstants.SELECT_ID);
							strTitle = (String) mobjectDetails.get(ATTR_V_NAME);
							if (UIUtil.isNotNullAndNotEmpty(strObjId) && UIUtil.isNotNullAndNotEmpty(strResponseStatusLine))

								if(UIUtil.isNotNullAndNotEmpty(strTitle) )
								{
									strResponseStatusLine = strTitle + ":" + strResponseStatusLine;
									addObjectHistory(context,strObjId, strResponseStatusLine);
								}

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
	 * This Method is to set the session Language
	 *
	 * @param context
	 * @throws Exception
	 */
	private static int setLanguage(Context context) throws Exception {
		LANGUAGE = context.getSession().getLanguage();
		return 0;
	}

	/**
	 * This Method is to get the session Language
	 *
	 * @param context
	 * @return LANGUAGE : session Language
	 * @throws Exception
	 */
	private static String getLanguage() throws Exception {
		return LANGUAGE;
	}

	/**
	 * This Method is to set the SAP WebService Authentication User
	 *
	 * @param context
	 * @throws Exception
	 */
	private static int setUserName(Context context) throws Exception {
		USERNAME = EnoviaResourceBundle.getProperty(context, LCD_3DXSAPIntegrationKey, "LCD_3DXSAPStringResource_en.SAP.UserName", getLanguage());
		return 0;
	}

	/**
	 * This Method is to get the SAP WebService Authentication User
	 *
	 * @param context
	 * @return USERNAME : SAP WebService Authentication User
	 * @throws Exception
	 */
	private static String getUserName() throws Exception {
		return USERNAME;
	}

	/**
	 * This Method is to set the SAP WebService Authentication Password
	 *
	 * @param context
	 * @throws Exception
	 */
	private static int setPassword(Context context) throws Exception {
		PASSWORD = EnoviaResourceBundle.getProperty(context, LCD_3DXSAPIntegrationKey, "LCD_3DXSAPStringResource_en.SAP.Password", getLanguage());
		return 0;
	}

	/**
	 * This Method is to get the SAP WebService Authentication Password
	 *
	 * @param context
	 * @return PASSWORD : SAP WebService Authentication Password
	 * @throws Exception
	 */
	private static String getPassword() throws Exception {
		return PASSWORD;
	}

	/**
	 * This Method is to set the SAP WebService URL
	 *
	 * @param context
	 * @throws Exception
	 */
	private static int setURL(Context context) throws Exception {
		URL = EnoviaResourceBundle.getProperty(context, LCD_3DXSAPIntegrationKey, "LCD_3DXSAPStringResource_en.SAP.WebServiceURL.PROD", getLanguage());
		return 0;
	}

	/**
	 * This Method is to get the SAP WebService URL
	 *
	 * @param context
	 * @return URL : SAP WebService URL
	 * @throws Exception
	 */
	private static String getURL() throws Exception {
		return URL;
	}

	/**
	 * This Method is to Set the Change Action JSON
	 *
	 * @param context
	 * @param JsonObject changeActionJson : SAP WebService URL
	 * @throws Exception
	 */
	private static int setChangeActionJson(JsonObject changeActionJson) throws Exception {
		CHANGE_ACTION_JSON = changeActionJson;
		return 0;
	}

	/**
	 * This Method is to Get the Change Action JSON
	 *
	 * @param context
	 * @return CHANGE_ACTION_JSON : SAP WebService URL
	 * @throws Exception
	 */
	private static JsonObject getChangeActionJson() throws Exception {
		return CHANGE_ACTION_JSON;
	}

	/**
	 * This Method is to Set the Closeable Http Client
	 *
	 * @param context
	 * @throws Exception
	 */
	private static int setCloseableHttpClient() throws Exception {
		CLIENT = HttpClients.createDefault();
		return 0;
	}

	/**
	 * This Method is to Get the Closeable Http Client
	 *
	 * @param context
	 * @return CLIENT : Closeable Http Client
	 * @throws Exception
	 */
	private static CloseableHttpClient getCloseableHttpClient() throws Exception {
		return CLIENT;
	}

	/**
	 * This Method is to Set the CSRFToken for the Session
	 *
	 * @param context
	 * @param xcsrftoken : CSRFToken
	 * @throws Exception
	 */
	private static int setCSRFToken(String xcsrftoken) throws Exception {
		XCSRFTOKEN = xcsrftoken;
		return 0;
	}

	/**
	 * This Method is to Get the CSRFToken for the Session
	 *
	 * @param context
	 * @return XCSRFTOKEN : CSRFToken
	 * @throws Exception
	 */
	private static String getCSRFToken() throws Exception {
		return XCSRFTOKEN;
	}

	/**
	 * This Method is to Set the Cookie for the Session
	 *
	 * @param context
	 * @param strCookie : Cookie for the Session
	 * @throws Exception
	 */
	private static int setSessionCookie(String strCookie) throws Exception {
		COOKIE = strCookie;
		return 0;
	}

	/**
	 * This Method is to Get the Cookie for the Session
	 *
	 * @param context
	 * @return COOKIE : Cookie for the Session
	 * @throws Exception
	 */
	private static String getSessionCookie() throws Exception {
		return COOKIE;
	}

	/**
	 * This Method is to Set the Change Action Completion Date
	 *
	 * @param context
	 * @return CA_RELEASED_DATE : Change Action Completion Date
	 * @throws Exception
	 */
	private void setCACompletionDate(String strCACompletionDate) {
		CA_RELEASED_DATE = strCACompletionDate;
	}

	/**
	 * This Method is to Get the Change Action Completion Date
	 *
	 * @param context
	 * @return CA_RELEASED_DATE : Change Action Completion Date
	 * @throws Exception
	 */
	private static String getCACompletionDate() throws Exception {
		return CA_RELEASED_DATE;
	}

	/**
	 * This Method is to Set the Cookie for the Session
	 *
	 * @param context
	 * @param strCookie : Cookie for the Session
	 * @throws Exception
	 */
	private static int setSubContractFailedFlag(boolean isFailed) throws Exception {
		SUBCONTRACT_STATUS = isFailed;
		return 0;
	}

	/**
	 * This Method is to Get the Cookie for the Session
	 *
	 * @param context
	 * @return COOKIE : Cookie for the Session
	 * @throws Exception
	 */
	private static boolean getSubContractFailedFlag() throws Exception {
		return SUBCONTRACT_STATUS;
	}

	/**
	 * This Method is to Set the Change Action Completion Date
	 *
	 * @param context
	 * @return CA_RELEASED_DATE : Change Action Completion Date
	 * @throws Exception
	 */
	private void setRootSubContractRelId(String strSubContractRelId) {
		SUBCONTRACT_RELID = strSubContractRelId;
	}

	/**
	 * This Method is to Get the Change Action Completion Date
	 *
	 * @param context
	 * @return CA_RELEASED_DATE : Change Action Completion Date
	 * @throws Exception
	 */
	private static String getRootSubContractRelId() throws Exception {
		return SUBCONTRACT_RELID;
	}

	/**
	 * This Method is to Set the Cookie for the Session
	 *
	 * @param context
	 * @param strCookie : Cookie for the Session
	 * @throws Exception
	 */

	/**
	 * This Method is to Set the Cookie for the Session
	 *
	 * @param context
	 * @param strCookie : Cookie for the Session
	 * @throws Exception
	 */
	private static int setChangeActionHeader(JsonObjectBuilder jEachPayloadBuilder) throws Exception {
		CHANGE_ACTION_JSON_BUILDER = jEachPayloadBuilder;
		return 0;
	}

	/**
	 * This Method is to Get the Cookie for the Session
	 *
	 * @param context
	 * @return COOKIE : Cookie for the Session
	 * @throws Exception
	 */
	private static JsonObjectBuilder getChangeActionHeader() throws Exception {
		return CHANGE_ACTION_JSON_BUILDER;
	}

	/**
	 * This class is for the logger Utilities
	 *
	 */

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
