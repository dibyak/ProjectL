package com.lcd.sapintegration;

import com.dassault_systemes.enovia.changeaction.impl.ChangeAction;
import com.dassault_systemes.enovia.changeaction.servicesimpl.ChangeActionServices;
import com.dassault_systemes.enovia.changeaction.webservice.services.ChangeActionJsonUtilities;
import com.dassault_systemes.enovia.changeaction.webservice.services.ChangeActionJsonUtilities.ChangeActionFacets;
import com.dassault_systemes.platform.restServices.RestService;
import com.dassault_systemes.plm.config.exposed.factory.ConfigurationExposedFilterablesFactory;
import com.dassault_systemes.plm.config.exposed.interfaces.IConfigurationExposedFilterables;
import com.lcd.sapintegration.LCDSAPIntegrationExportCSVServices;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.DomainRelationship;
import com.matrixone.apps.domain.util.FrameworkUtil;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.MqlUtil;
import com.matrixone.apps.framework.ui.UIUtil;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.json.JsonValue;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import matrix.util.Pattern;
import matrix.util.StringList;

@Path("/LCDSAPIntegrationExportCSVService")
public class LCDSAPIntegrationExportCSVServices extends RestService {
	public static final String TYPE_APPLICATION_FORMAT = "application/json";
	public static final String TYPE_CSV_FORMAT = "text/csv";
	public static final String CSV_FILE = "Export_CSV_Format.csv";
	public static final String UTF_8 = "UTF-8";
	public static final String CA_DETAILS = "Change Action Details";
	public static final String REALIZED_ITEM_DETAILS = "Realized Item Details";
	public static final String VALUE_HEADER_PART = "HeaderPart";
	public static final String VALUE_CHILDERN_PART = "children";

	public static final String ROLE_ADMIN = "admin_platform";
	public static final String TYPE_PLMENTITY = "PLMEntity";
	public static final String TYPE_PLMREFERENCE = "PLMReference";
	public static final String TYPE_PLMCOREREFERENCE = "PLMCoreReference";
	public static final String TYPE_PLMCOREREPREFERENCE = "PLMCoreRepReference";
	public static final String TYPE_LPABSTRACTREPREF = "LPAbstractRepReference";
	public static final String TYPE_VPMINSTANCE = "VPMInstance";
	public static final String TYPE_VPMREFERENCE = "VPMReference";
	public static final String TYPE_VPMREPREFERENCE = "VPMRepReference";
	public static final String TYPE_3DSHAPE = "3DShape";
	public static final String TYPE_VPMREPINSTANCE = "VPMRepInstance";
	public static final String TYPE_XCADASSEMBLYREPREFERENCE = "XCADAssemblyRepReference";
	public static final String TYPE_MANUFACTURINGASSEMBLY = "CreateAssembly";
	public static final String TYPE_PROVIDE = "Provide";
	public static final String TYPE_PROCESSINSTANCECONTINUOUS = "ProcessContinuousProvide";
	public static final String TYPE_FASTEN = "Fasten";

	public static final String REL_PROVIDE = "DELFmiFunctionIdentifiedInstance";
	public static final String REL_PROCESSINSTANCECONTINUOUS = "ProcessInstanceContinuous";

	public static final String ATTR_V_NAME = "attribute[PLMEntity.V_Name]";
	public static final String ATTR_V_DESCRIPTION = "attribute[PLMEntity.V_description]";
	public static final String ATTR_PLM_EXTERNALID = "attribute[PLMEntity.PLM_ExternalID]";

	public static final String ATTR_PROCUREMENTINTENT_MANUFACTURINGASSEMBLY = "attribute[LCDMF_ManufacturingAssembly.LCDMF_ProcurementIntent]";
	public static final String ATTR_PLANTCODE_MANUFACTURINGASSEMBLY = "attribute[XP_CreateAssembly_Ext.LCDMF_PlantCode]";
	public static final String ATTR_SERVICEABLEITEM_MANUFACTURINGASSEMBLY = "attribute[XP_CreateAssembly_Ext.LCD_ServiceableItem]";
	public static final String ATTR_PARTINTERCHANGEABILITY_MANUFACTURINGASSEMBLY = "attribute[XP_CreateAssembly_Ext.LCD_PartInterchangeability]";
	public static final String ATTR_HASCONFIGCONTEXT_MANUFACTURINGASSEMBLY = "attribute[PLMReference.V_hasConfigContext]";
	public static final String ATTR_SAPMBOMUPDATEDON_MANUFACTURINGASSEMBLY = "attribute[LCDMF_ManufacturingAssembly.LCDMF_SAPMBOMUpdatedOn]";
	public static final String ATTR_SAPUNIQUEID_MANUFACTURINGASSEMBLY = "attribute[LCDMF_ManufacturingAssembly.LCDMF_SAPUniqueID]";
	public static final String ATTR_SAPBOMUPDATEDON_MANUFACTURINGASSEMBLY = "attribute[XP_CreateAssembly_Ext.LCD_SAPBOMUpdateOn]";

	public static final String ATTR_SAP_UNIQUEID = "LCDMF_ManufacturingAssembly.LCDMF_SAPUniqueID";
	public static final String ATTR_SAPMBOM_UPDATED_ON = "LCDMF_ManufacturingAssembly.LCDMF_SAPMBOMUpdatedOn";
	public static final String ATTR_SAP_INSATNCE_UPDATED_ON = "LCD_MBOMInstance.LCD_SAPMBOMUpdatedOn";

	public static final String ATTR_SAP_CAD_INSATNCE_UPDATED_ON = "LCD_CADInstanceExt.LCD_SAPBOMUpdatedOn";

	public static final String TITLE_CHANGEACTION = "attribute[Synopsis]";
	public static final String CHANGETYPE_CHANGEACTION = "attribute[LCD_3ChangeType]";
	public static final String CHANGEDOMAIN_CHANGEACTION = "attribute[LCD_1ChangeDomain]";
	public static final String PLATFORM_CHANGEACTION = "attribute[LCD_2Platform]";
	public static final String REASONFORCHANGE_CHANGEACTION = "attribute[LCD_4ReasonForChange]";
	public static final String CATEGORYOFCHANGE_CHANGEACTION = "attribute[CategoryofChange]";
	public static final String RELEASEDDATE_CHANGEACTION = "attribute[ActualCompletionDate]";

	public static final String ATTR_V_NAME_VPMREFERENCE = "attribute[V_Name]";
	public static final String ATTR_PROCUREMENTINTENT_VPMREFERENCE = "attribute[XP_VPMReference_Ext.AtievaProcurementIntent]";
	public static final String ATTR_SERVICEABLEITEM_VPMREFERENCE = "attribute[XP_VPMReference_Ext.AtievaSpareProduct]";
	public static final String ATTR_PARTINTERCHANGEABILITY_VPMREFERENCE = "attribute[XP_VPMReference_Ext.LCD_PartInterchangeability]";
	public static final String ATTR_HASCONFIGCONTEXT_VPMREFERENCE = "attribute[PLMReference.V_hasConfigContext]";
	public static final String ATTR_UNITOFMEASURE_VPMREFERENCE = "attribute[XP_VPMReference_Ext.AtievaUnitofMeasure]";
	public static final String ATTR_TREEORDER_PLMINSTANCE = "attribute[PLMInstance.V_TreeOrder]";

	public static final String DESCRETE = "Make";
	public static final String PHANTOM = "Phantom";
	public static final String SUBCONTRACT = "Sub-Contract";
	public static final String BUYSUBC = "Buy-SubC";

	public static final String EACH = "Each";

	/* 3DX-SAP Integration JSON header Information */
	public static final String TAG_TITLE = "Title";
	public static final String TAG_OID = "OID";
	public static final String TAG_NAME = "name";
	public static final String TAG_DESCRIPTION = "Description";
	public static final String TAG_DATEFROM = "DateFrom";
	public static final String TAG_DATETO = "DateTo";
	public static final String TAG_REALIZED_DATA = "Realized_Data";
	public static final String TAG_REL_ID = "Rel_OID";
	public static final String TAG_CATEGORY_OF_CHANGE = "Category_of_Change";
	public static final String TAG_CHANGEDOMAIN = "ChangeDomain";
	public static final String TAG_CHANGETYPE = "ChangeType";
	public static final String TAG_REASONFORCHANGE = "ReasonForChange";
	public static final String TAG_PLATFORM = "Platform";

	public static final String TAG_PROCUREMENTINTENT = "ProcurementIntent";
	public static final String TAG_SERVICEABLEITEM = "ServiceableItem";
	public static final String TAG_PARTINTERCHANGEABILITY = "PartInterchangeability";
	public static final String TAG_UNIT_OF_MEASURE = "UoM";
	public static final String TAG_VARIAENT_EFFECTIVITY = "Effectivity_Option_Code";
	public static final String TAG_PLANTCODE = "PlantCode";
	public static final String TAG_ERROR_MESSAGE = "ERROR_MESSAGE";
	public static final String HEADER_CHILDREN = "children";
	public static final String TAG_TYPE = "Type";
	public static final String HEADER_PART = "HeaderPart";
	public static final String TAG_SAPUNIQUE_ID = "SAPUniqueID";

	public static final String FAIL = "FAILURE";
	public static final String SUCCESS = "SUCCESS";

	public static final String RELEASED = "RELEASED";

	public static final String NOT_APPLICABLE = "NA";

	public static final String FALSE = "FALSE";

	public static final String EFFECTIVITY_CURRENT_EVOLUTION = "Effectivity_Current_Evolution";
	public static final String EFFECTIVITY_PROJECTED_EVOLUTION = "Effectivity_Projected_Evolution";
	public static final String EFFECTIVITY_VARIANT = "Effectivity_Variant";
	public static final String EFFECTIVITY_FROM_DATE = "Effectivity_From_Date";
	public static final String EFFECTIVITY_TO_DATE = "Effectivity_To_Date";

	public static final String LCD_3DX_SAP_INTEGRATION_KEY = "LCD_3DXSAPStringResource_en";
	public static final String TYPE_LCD_BOM_ANCHOR_OBJECT = "LCD_BOMAnchorObject";
	public static final String NAME_LCD_ANCHOR_OBJECT = "LCD_AnchorObject";
	public static final String REV_LCD_ANCHOR_OBJECT = "A";
//	public static final String VAULT_ESERVICE_PRODUCTION = "eService Production";
	public static final String ATTR_LCD_PROCESS_STATUS_FLAG = "LCD_ProcessStatusFlag";
	public static final String ATTR_LCD_REASON_FOR_FAILURE = "LCD_ReasonforFailure";
	public static final String ATTR_LCD_CAID = "LCD_CAID";
	public static final String VALUE_STATUS_FAILED = "Failed";
	public static final String VALUE_STATUS_COMPLETE = "Complete";
	public static final String REL_LCD_SAP_BOM_INTERFACE = "LCD_SAPBOMInterface";
	public static final String STATUS_COMPLETE = "Complete";
	public static final String STATUS_IN_WORK = "In Work";

	public static final String KEY_BOMCOMPONENT_TYPE = "BOMComponentType";
	public static final String KEY_BOM_COMPONENT_ID = "BOMComponentID";
	public static final String KEY_CA_ID = "CAID";

	public static final String HEADER_LEVEL = "Level";
	public static final String NEW_LINE = "\n";
	public static final String COMMA_SEP = ",";

	public static String caReleasedDate;
//	private static String subContractRelId;

	@POST
	@Produces("text/csv")
	@Path("/exportCSV")
	public Response exportCSV(@Context HttpServletRequest request, String paramString) throws Exception {
		Response res = null;

		try {
			boolean isSCMandatory = false;
			matrix.db.Context context = getAuthenticatedContext(request, isSCMandatory);
	
			JsonReader jsonReader = Json.createReader(new StringReader(paramString));
			JsonObject joRequest = jsonReader.readObject();
			
			String strBomId = ((JsonValue) joRequest.get(KEY_BOM_COMPONENT_ID)).toString().replace("\"", "");
			String strcaId = ((JsonValue) joRequest.get(KEY_CA_ID)).toString().replace("\"", "");
			jsonReader.close();

			String strWorkSpace = context.createWorkspace(); 
			String csvFile = strWorkSpace + "\\" + CSV_FILE;

			Writer writerExportCSV = new OutputStreamWriter(new FileOutputStream(csvFile), UTF_8);
			StringBuilder strBuildExportCSV = new StringBuilder();
			
			JsonObject jsonObject = sendToSAP(context, strBomId, strcaId);
			

			Set<String> caKeys = jsonObject.keySet();
			LinkedList<HashMap> childPartLinkedList = new LinkedList<>();
			LinkedHashSet<String> setPartKeys = new LinkedHashSet<>();

			HashMap<String, Object> headerKeyValueMap = new HashMap<>();
			strBuildExportCSV.append(CA_DETAILS).append(NEW_LINE);
			for (String caKey : caKeys) {

				if (caKey.equals(VALUE_HEADER_PART)) {
					JsonObject jobHeaderPart = jsonObject.getJsonObject(caKey);
					Set<String> headerPartKeys = jobHeaderPart.keySet();

					for (String key : headerPartKeys) {
						if (key.equals(VALUE_CHILDERN_PART)) {

							JsonArray jsonArrChildren = jobHeaderPart.getJsonArray(key);

							for (int i = 0; i < jsonArrChildren.size(); i++) {
								JsonObject jchildPart = jsonArrChildren.getJsonObject(i);
								Set<String> childPartKeys = jchildPart.keySet();
								HashMap<String, String> childKeyValueMap = new HashMap<>();
								for (String childKey : childPartKeys) {
									setPartKeys.add(childKey);
									childKeyValueMap.put(childKey, jchildPart.get(childKey).toString());
								}
								childPartLinkedList.add(childKeyValueMap);
							}

						} else {
							setPartKeys.add(key);
							headerKeyValueMap.put(key, jobHeaderPart.get(key).toString());
						}

					}
				} else {
					strBuildExportCSV.append(caKey + COMMA_SEP + jsonObject.get(caKey) + NEW_LINE);
				}
			}
			strBuildExportCSV.append(NEW_LINE);
			strBuildExportCSV.append(REALIZED_ITEM_DETAILS).append(NEW_LINE);
			writePartKeys(strBuildExportCSV, setPartKeys);
			writeHeaderPartToCSV(strBuildExportCSV, setPartKeys, headerKeyValueMap);
			writeChildPartToCSV(strBuildExportCSV, setPartKeys, childPartLinkedList);
			System.out.println("done");
			
			res = Response.ok(strBuildExportCSV.toString()).type(MediaType.TEXT_PLAIN).header("Content-Disposition", "attachment; filename=\"" + CSV_FILE + "\"" ).build();
			
		} catch (Exception e) {
			e.printStackTrace();
			res = Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
		}
		return res;
	}

	private static void writePartKeys(StringBuilder strBuildExportCSV, LinkedHashSet<String> setPartKeys) {
		strBuildExportCSV.append(HEADER_LEVEL).append(COMMA_SEP);
		for (String keySet : setPartKeys) {

			strBuildExportCSV.append(keySet).append(COMMA_SEP);
		}
		strBuildExportCSV.append(NEW_LINE);

	}

	private static void writeHeaderPartToCSV(StringBuilder strBuildExportCSV, LinkedHashSet<String> setPartKeys,
			HashMap<String, Object> headerKeyValueMap) {
		strBuildExportCSV.append("0").append(COMMA_SEP);
		for (String keySet : setPartKeys) {

			if (headerKeyValueMap.get(keySet) == null) {
				strBuildExportCSV.append("").append(COMMA_SEP);
			} else
				strBuildExportCSV.append(headerKeyValueMap.get(keySet)).append(COMMA_SEP);
		}
		strBuildExportCSV.append(NEW_LINE);

	}

	private static void writeChildPartToCSV(StringBuilder strBuildExportCSV, LinkedHashSet<String> setPartKeys,
			LinkedList<HashMap> childPartLinkedList) {

		for (int i = 0; i < childPartLinkedList.size(); i++) {
			HashMap<?, ?> hm = childPartLinkedList.get(i);
			strBuildExportCSV.append("1").append(COMMA_SEP);
			for (String keySet : setPartKeys) {

				if (hm.get(keySet) == null) {
					strBuildExportCSV.append("").append(COMMA_SEP);
				} else
					strBuildExportCSV.append(hm.get(keySet)).append(COMMA_SEP);
			}
			strBuildExportCSV.append(NEW_LINE);
		}
	}

	private JsonObject sendToSAP(matrix.db.Context context, String strBOMComponentId,
			String caId) throws Exception {

		JsonObject jEachPayloadObj = null;
		try {
			System.out.println("sendToSAP calledd <<<<<<<<< ");

			// STEP :Get input Arguments
			ChangeAction changeAction = ChangeActionServices.getChangeAction(context, caId);

			// STEP : Using Service API getting JSON object for Change Action
			ChangeActionFacets changeActionFacets = new ChangeActionFacets();
			changeActionFacets.attributes = true;
			changeActionFacets.realized = true;
			String changeActionJsonDetails = ChangeActionJsonUtilities.changeActionToJson(context, changeAction,
					changeActionFacets);

			 JsonReader jsonReader = Json.createReader(new StringReader(changeActionJsonDetails));
//			JsonObject joChangeAction = jsonReader.readObject();

			JsonObjectBuilder jEachPayloadBuilder = Json.createObjectBuilder();

			jEachPayloadBuilder = ChangeActionJSONPayload(context, caId);

			// STEP : Traversing Change Action to get the realized items
			
			
			DomainObject domRealizedItemObj;
			Map<?, ?> mobjectDetails;
			JsonObjectBuilder jHeaderPartObjectBuilder;

			// STEP : Retrieving realized item's object details
			domRealizedItemObj = DomainObject.newInstance(context, strBOMComponentId);
			StringList objectSelects = new StringList();
			objectSelects.add(DomainConstants.SELECT_ID);
			objectSelects.add(DomainConstants.SELECT_NAME);
			objectSelects.add(DomainConstants.SELECT_TYPE);
			objectSelects.add(ATTR_V_NAME);
			objectSelects.add(ATTR_V_DESCRIPTION);
			objectSelects.add(ATTR_PROCUREMENTINTENT_MANUFACTURINGASSEMBLY);
			objectSelects.add(ATTR_PLANTCODE_MANUFACTURINGASSEMBLY);
			objectSelects.add(ATTR_SERVICEABLEITEM_MANUFACTURINGASSEMBLY);
			objectSelects.add(ATTR_PARTINTERCHANGEABILITY_MANUFACTURINGASSEMBLY);
			objectSelects.add(ATTR_HASCONFIGCONTEXT_VPMREFERENCE);
			objectSelects.add(ATTR_SAPMBOMUPDATEDON_MANUFACTURINGASSEMBLY);
			objectSelects.add(ATTR_SAPUNIQUEID_MANUFACTURINGASSEMBLY);

			mobjectDetails = domRealizedItemObj.getInfo(context, objectSelects);

			// STEP : Adding Manufacturing Assembly Header in JSON

			jHeaderPartObjectBuilder = addRealizedItemHeader(context, mobjectDetails);

			jEachPayloadBuilder.add(HEADER_PART, jHeaderPartObjectBuilder.build());

			// STEP : Invoking SAP WebService by attaching JSON
			jEachPayloadObj = jEachPayloadBuilder.build();
			jsonReader.close();
		} catch (

		Exception e) {
			e.printStackTrace();
		}

		return jEachPayloadObj;
	}

	private JsonObjectBuilder ChangeActionJSONPayload(matrix.db.Context context, String caId) throws Exception {

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
			String caDate = (String) changeActionAttrDetails.get(RELEASEDDATE_CHANGEACTION);
			if (UIUtil.isNotNullAndNotEmpty(caDate)) {
				caDate = caDate.substring(0, caDate.length() - 11);
				if (UIUtil.isNotNullAndNotEmpty(caReleasedDate)) {
					SimpleDateFormat format1 = new SimpleDateFormat("MM/dd/yyyy");
					SimpleDateFormat format2 = new SimpleDateFormat("MM-dd-yyyy");
					Date date = format1.parse(caDate);
					String formatDate = format2.format(date);
					caReleasedDate = formatDate;
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

		} catch (Exception e) {
			e.printStackTrace();
		}
		return jEachPayloadBuilder;
	}

	private JsonObjectBuilder addRealizedItemHeader(matrix.db.Context context, Map mobjectDetails) throws Exception {

		// STEP : Creating JSON object Builder to add the realized item details
		JsonObjectBuilder jHeaderPartObjectBuilder = Json.createObjectBuilder();
		try {
			// STEP : Retrieving realized item Details
			String strObjProcurementIntent = (String) mobjectDetails.get(ATTR_PROCUREMENTINTENT_MANUFACTURINGASSEMBLY);
			String strObjTitle = (String) mobjectDetails.get(ATTR_V_NAME);
			String strObjId = (String) mobjectDetails.get(DomainConstants.SELECT_ID);
			String strObjdescription = (String) mobjectDetails.get(ATTR_V_DESCRIPTION);
			String strName = (String) mobjectDetails.get(DomainConstants.SELECT_NAME);
			String strObjPlantCode = (String) mobjectDetails.get(ATTR_PLANTCODE_MANUFACTURINGASSEMBLY);
			String strObjServiceable = (String) mobjectDetails.get(ATTR_SERVICEABLEITEM_MANUFACTURINGASSEMBLY);
			String strObjPartInterchangeability = (String) mobjectDetails
					.get(ATTR_PARTINTERCHANGEABILITY_MANUFACTURINGASSEMBLY);

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

			jHeaderPartObjectBuilder.add(TAG_VARIAENT_EFFECTIVITY, NOT_APPLICABLE);
			jHeaderPartObjectBuilder.add(TAG_DATEFROM, NOT_APPLICABLE);
			jHeaderPartObjectBuilder.add(TAG_DATETO, NOT_APPLICABLE);
			jHeaderPartObjectBuilder.add(TAG_REALIZED_DATA, true);

			// STEP : Adding child of Manufacturing Assembly Header in JSON
			if (UIUtil.isNotNullAndNotEmpty(strObjId))
				addRealizedItemChildren(context, strObjId, jHeaderPartObjectBuilder);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return jHeaderPartObjectBuilder;
	}

	private void addRealizedItemChildren(matrix.db.Context context, String strObjId,
			JsonObjectBuilder jHeaderPartObjectBuilder) throws Exception {
		try {
			// STEP : Retrieving realized item child details
			MapList mlMBOMDetails = getMBOMDetails(context, strObjId);
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

					jsonMBOMPartArrayBuilder.add(jMBOMPartBuilder.build());
				}
			}
			// STEP : adding child parts JSON Array in Header Part JSON object
			if (null != jsonMBOMPartArrayBuilder)
				jHeaderPartObjectBuilder.add(HEADER_CHILDREN, jsonMBOMPartArrayBuilder.build());

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private MapList getMBOMDetails(matrix.db.Context context, String strObjectId) throws Exception {
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
			objectSelects.add(ATTR_PROCUREMENTINTENT_MANUFACTURINGASSEMBLY);
			objectSelects.add(ATTR_PLANTCODE_MANUFACTURINGASSEMBLY);
			objectSelects.add(ATTR_SERVICEABLEITEM_MANUFACTURINGASSEMBLY);
			objectSelects.add(ATTR_PARTINTERCHANGEABILITY_MANUFACTURINGASSEMBLY);
			objectSelects.add(ATTR_HASCONFIGCONTEXT_VPMREFERENCE);

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

			mlMBOMList.sort(ATTR_TREEORDER_PLMINSTANCE, "ascending", "real");
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return mlMBOMList;
	}

	private void addChildPartHeader(matrix.db.Context context, Map<?, ?> mPartDetails,
			JsonObjectBuilder jMBOMPartBuilder, boolean isAssembly) throws Exception {
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
				strProcurementIntent = (String) mPartDetails.get(ATTR_PROCUREMENTINTENT_MANUFACTURINGASSEMBLY);
				strServiceable = (String) mPartDetails.get(ATTR_SERVICEABLEITEM_MANUFACTURINGASSEMBLY);
				strPartInterchangeability = (String) mPartDetails
						.get(ATTR_PARTINTERCHANGEABILITY_MANUFACTURINGASSEMBLY);
			} else {
				if (null != strObjectID && !strObjectID.isEmpty()) {
					// STEP : If Child is other than Manufacturing Assembly ,Retrieving Procurement
					// Intent , serviceable ,PartInterchangeability from corresponding Linked CAD
					// PART
					// STEP : Retrieving Child Part Details from corresponding Linked CAD PART
					mLinkedCadPart = getLinkedCADPartFromMBOMPart(context, strObjectID, REL_PROVIDE);
					if (null != mLinkedCadPart) {
						strProcurementIntent = (String) mLinkedCadPart.get(ATTR_PROCUREMENTINTENT_VPMREFERENCE);
						strServiceable = (String) mLinkedCadPart.get(ATTR_SERVICEABLEITEM_VPMREFERENCE);
						strPartInterchangeability = (String) mLinkedCadPart
								.get(ATTR_PARTINTERCHANGEABILITY_VPMREFERENCE);
						strUnitOfMeasure = (String) mLinkedCadPart.get(ATTR_UNITOFMEASURE_VPMREFERENCE);
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
				jMBOMPartBuilder.add(TAG_VARIAENT_EFFECTIVITY, strVariantEffectivity);
			else
				jMBOMPartBuilder.add(TAG_VARIAENT_EFFECTIVITY, " ");
			if (UIUtil.isNotNullAndNotEmpty(caReleasedDate))
				jMBOMPartBuilder.add(TAG_DATEFROM, caReleasedDate);
			else
				jMBOMPartBuilder.add(TAG_DATEFROM, "");

			jMBOMPartBuilder.add(TAG_DATETO, "12-31-9999");
			jMBOMPartBuilder.add(TAG_REALIZED_DATA, true);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private Map getLinkedCADPartFromMBOMPart(matrix.db.Context context, String strOBJId, String strRel)
			throws Exception {
		Map<String, String> objInfoMap = null;
		try {
			if (null != strOBJId && !strOBJId.isEmpty()) {
				// Object Selectables
				StringList objectSelects = new StringList();
				objectSelects.add(DomainConstants.SELECT_ID);
				objectSelects.add(DomainConstants.SELECT_NAME);
				objectSelects.add(ATTR_V_NAME);
				objectSelects.add(ATTR_V_DESCRIPTION);
				objectSelects.add(ATTR_SERVICEABLEITEM_VPMREFERENCE); // "Serviceable Item";
				objectSelects.add(ATTR_PROCUREMENTINTENT_VPMREFERENCE); // "Procurement Intent"
				objectSelects.add(ATTR_PARTINTERCHANGEABILITY_VPMREFERENCE); // "Part Interchangeability ";
				objectSelects.add(ATTR_UNITOFMEASURE_VPMREFERENCE); // "Unit of Measure";

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
									if (UIUtil.isNotNullAndNotEmpty(strRelId)) {
//										subContractRelId = strRelId;
									}
										
									DomainObject boCADObj = DomainObject.newInstance(context, ostrVPMReferenceId);
									objInfoMap = boCADObj.getInfo(context, objectSelects);

								}

							} catch (Exception e) {
							}
						}
					}
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			throw ex;
		}
		return objInfoMap;
	}

	private Map<?, ?> getEffectivity(matrix.db.Context context, String strRelId) throws Exception {
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
		return mEffectivity;
	}

	

}
