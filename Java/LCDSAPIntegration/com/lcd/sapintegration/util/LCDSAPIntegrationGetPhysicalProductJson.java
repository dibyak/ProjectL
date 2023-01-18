package com.lcd.sapintegration.util;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import com.dassault_systemes.plm.config.exposed.factory.ConfigurationExposedFilterablesFactory;
import com.dassault_systemes.plm.config.exposed.interfaces.IConfigurationExposedFilterables;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.framework.ui.UIUtil;

import matrix.util.StringList;

public class LCDSAPIntegrationGetPhysicalProductJson {

	/**
	 * This Method is to add the Change Action object details in JSON Header
	 *
	 * @param context
	 * @param JsonObject : Input Change Action JSON
	 * @return JsonObjectBuilder : After adding Change Action information returning
	 *         JsonObjectBuilder
	 * @throws Exception
	 */
	private static JsonObjectBuilder changeActionJSONPayload() throws NullPointerException {

		// STEP : Creating JSON object Builder to add the change action object details
		JsonObjectBuilder jEachPayloadBuilder = Json.createObjectBuilder();
		try {
			// STEP : Adding Change Action object details in JSONObjectBuilder

			jEachPayloadBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_TITLE,
					LCDSAPIntegrationDataConstants.VALUE_NOT_APPLICABLE);

			jEachPayloadBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_OID,
					LCDSAPIntegrationDataConstants.VALUE_NOT_APPLICABLE);

			jEachPayloadBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_NAME,
					LCDSAPIntegrationDataConstants.VALUE_NOT_APPLICABLE);

			jEachPayloadBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_DESCRIPTION,
					LCDSAPIntegrationDataConstants.VALUE_NOT_APPLICABLE);

			jEachPayloadBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_CATEGORY_OF_CHANGE,
					LCDSAPIntegrationDataConstants.VALUE_NOT_APPLICABLE);

			jEachPayloadBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_CHANGEDOMAIN,
					LCDSAPIntegrationDataConstants.VALUE_NOT_APPLICABLE);

			jEachPayloadBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_CHANGETYPE,
					LCDSAPIntegrationDataConstants.VALUE_NOT_APPLICABLE);

			jEachPayloadBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_REASONFORCHANGE,
					LCDSAPIntegrationDataConstants.VALUE_NOT_APPLICABLE);

			jEachPayloadBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_PLATFORM,
					LCDSAPIntegrationDataConstants.VALUE_NOT_APPLICABLE);

			jEachPayloadBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_CA_APPLICABILITY_START_DATE,
					LCDSAPIntegrationDataConstants.VALUE_NOT_APPLICABLE);

			jEachPayloadBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_CA_APPLICABILITY_END_DATE,
					LCDSAPIntegrationDataConstants.VALUE_NOT_APPLICABLE);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return jEachPayloadBuilder;
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
	public static JsonObject getPhysicalProductJSON(matrix.db.Context context, Map<?, ?> mCadPartDetails,
			String strConnectionId, LCDSAPIntegration3DExpConstants lcdSAPInteg3DExpConstants) throws Exception {
		JsonObject jSubContractPayloadObj = null;
		try {
			if (mCadPartDetails != null)
				jSubContractPayloadObj = sendSubContractPartPayload(context, mCadPartDetails, strConnectionId,
						lcdSAPInteg3DExpConstants);
		} catch (Exception e) {
			e.printStackTrace();
		}
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

	private static JsonObject sendSubContractPartPayload(matrix.db.Context context, Map<?, ?> mSubContractPartDetails,
			String strConnectionId, LCDSAPIntegration3DExpConstants lcdSAPInteg3DExpConstants) throws Exception {
		JsonObject jSubContractPayloadObj = null;

		try {

			// STEP : Get Change Action Header
			JsonObjectBuilder jEachSubContractPayloadBuilder = changeActionJSONPayload();
			if (null != jEachSubContractPayloadBuilder) {
				// STEP : Get SubContract part Header

				JsonObjectBuilder jSubContractBuilder = getSubContractPartHeader(context, mSubContractPartDetails,
						lcdSAPInteg3DExpConstants);
				if (null != jSubContractBuilder) {

					// STEP : Retrieving all level BuySUBC Cad part , single level SubContract Cad
					// part and adding in JSON array
					JsonArrayBuilder jsubContractChildPartArrayBuilder = Json.createArrayBuilder();
					String strObjectID = (String) mSubContractPartDetails.get(DomainConstants.SELECT_ID);
					String strTitle = (String) mSubContractPartDetails
							.get(lcdSAPInteg3DExpConstants.SELECT_ATTRIBUTE_PLMENTITY_V_NAME);
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
							strProcIntent = (String) mchildPartDetails
									.get(lcdSAPInteg3DExpConstants.SELECT_ATTRIBUTE_PROCUREMENTINTENT_VPMREFERENCE);
							strObjID = (String) mchildPartDetails.get(DomainConstants.SELECT_ID);

							// STEP : adding SUBCONTRACT part in array of Child JSON
							if (LCDSAPIntegrationDataConstants.SUBCONTRACT.equalsIgnoreCase(strProcIntent)) {
								jChildBuilder = addCADChildPartHeader(context, mchildPartDetails,
										lcdSAPInteg3DExpConstants);
								if (null != jChildBuilder)
									jsubContractChildPartArrayBuilder.add(jChildBuilder.build());
							} else {
								// STEP : adding all level BUYSUBC part in array of Child JSON
								if (LCDSAPIntegrationDataConstants.BUYSUBC.equalsIgnoreCase(strProcIntent)) {
									jChildBuilder = addCADChildPartHeader(context, mchildPartDetails,
											lcdSAPInteg3DExpConstants);
									if (null != jChildBuilder)
										jsubContractChildPartArrayBuilder.add(jChildBuilder.build());
								}

								mlBuySubCObjects = getAllLevelBuySubC(context, strObjID, lcdSAPInteg3DExpConstants);
								if (mlBuySubCObjects != null) {
									for (int idx = 0; idx < mlBuySubCObjects.size(); idx++) {
										mchildPartDetails = (Map<?, ?>) mlBuySubCObjects.get(idx);
										strProcIntent = (String) mchildPartDetails.get(
												lcdSAPInteg3DExpConstants.SELECT_ATTRIBUTE_PROCUREMENTINTENT_VPMREFERENCE);
										if (LCDSAPIntegrationDataConstants.BUYSUBC.equalsIgnoreCase(strProcIntent)) {
											jChildBuilder = addCADChildPartHeader(context, mchildPartDetails,
													lcdSAPInteg3DExpConstants);
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
							jSubContractBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_CHILDREN,
									jsubContractChildPartArrayBuilder.build());
						}
						if (null != jSubContractBuilder)
							jEachSubContractPayloadBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_HEADER_PART,
									jSubContractBuilder.build());

						jSubContractPayloadObj = jEachSubContractPayloadBuilder.build();
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return jSubContractPayloadObj;
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
	private static MapList getAllLevelBuySubC(matrix.db.Context context, String strObjectId,
			LCDSAPIntegration3DExpConstants lcdSAPInteg3DExpConstants) throws Exception {
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
				objectSelects.add(lcdSAPInteg3DExpConstants.SELECT_ATTRIBUTE_SERVICEABLEITEM_VPMREFERENCE); // "Serviceable
																											// Item";
				objectSelects.add(lcdSAPInteg3DExpConstants.SELECT_ATTRIBUTE_PROCUREMENTINTENT_VPMREFERENCE); // "Procurement
																												// Intent"
				objectSelects.add(lcdSAPInteg3DExpConstants.SELECT_ATTRIBUTE_PARTINTERCHANGEABILITY_VPMREFERENCE); // "Part
																													// Interchangeability
																													// ";
				objectSelects.add(lcdSAPInteg3DExpConstants.SELECT_ATTRIBUTE_UNITOFMEASURE_VPMREFERENCE); // "Unit of
																											// Measure";

				// RelationShip Selectables
				StringList relSelects = new StringList();
				relSelects.add(DomainConstants.SELECT_RELATIONSHIP_ID);
				relSelects.add(lcdSAPInteg3DExpConstants.SELECT_ATTRIBUTE_HAS_CONFIG_EFFECTIVITY);

				// STEP : Expanding the Input Linked CAD Part to get all children of it
				DomainObject partDomObj = DomainObject.newInstance(context, strObjectId);
				MapList ebomList = partDomObj.getRelatedObjects(context, lcdSAPInteg3DExpConstants.TYPE_VPM_INSTANCE, // relationship
																														// pattern
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
						strProcurementIntent = (String) tempMap
								.get(lcdSAPInteg3DExpConstants.SELECT_ATTRIBUTE_PROCUREMENTINTENT_VPMREFERENCE);
						if (LCDSAPIntegrationDataConstants.BUYSUBC.equalsIgnoreCase(strProcurementIntent)) {
							mlObjects.add(tempMap);
						}
					}
				}
			}
		} catch (Exception expMsg) {
		}
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
	private static JsonObjectBuilder addCADChildPartHeader(matrix.db.Context context, Map<?, ?> mchildPartDetails,
			LCDSAPIntegration3DExpConstants lcdSAPInteg3DExpConstants) throws Exception {
		JsonObjectBuilder jCadPartBuilder = Json.createObjectBuilder();
		try {
			// STEP : Retrieving necessary information of BuySubC Part
			String strVName = (String) mchildPartDetails
					.get(lcdSAPInteg3DExpConstants.SELECT_ATTRIBUTE_PLMENTITY_V_NAME);
			String strName = (String) mchildPartDetails.get(DomainConstants.SELECT_NAME);
			String strObjectID = (String) mchildPartDetails.get(DomainObject.SELECT_ID);
			String strRId = (String) mchildPartDetails.get(DomainConstants.SELECT_RELATIONSHIP_ID);
			String strdesc = (String) mchildPartDetails
					.get(lcdSAPInteg3DExpConstants.SELECT_ATTRIBUTE_PLMENTITY_V_DESCRIPTION);
			String strUnitOfMeasure = (String) mchildPartDetails
					.get(lcdSAPInteg3DExpConstants.SELECT_ATTRIBUTE_UNITOFMEASURE_VPMREFERENCE);
			String strProcIntent = (String) mchildPartDetails
					.get(lcdSAPInteg3DExpConstants.SELECT_ATTRIBUTE_PROCUREMENTINTENT_VPMREFERENCE);
			String strServiceItem = (String) mchildPartDetails
					.get(lcdSAPInteg3DExpConstants.SELECT_ATTRIBUTE_SERVICEABLEITEM_VPMREFERENCE);
			String strPartability = (String) mchildPartDetails
					.get(lcdSAPInteg3DExpConstants.SELECT_ATTRIBUTE_PARTINTERCHANGEABILITY_VPMREFERENCE);
			String strhasConfigEffectivity = (String) mchildPartDetails
					.get(lcdSAPInteg3DExpConstants.SELECT_ATTRIBUTE_HAS_CONFIG_EFFECTIVITY);

			String strVariantEffectivity = null;
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
			}
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

			jCadPartBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_EFFECTIVITY_OPTION_CODE,
					LCDSAPIntegrationDataConstants.VALUE_NOT_APPLICABLE);

			SimpleDateFormat format1 = new SimpleDateFormat("yyyy-MM-dd");
			SimpleDateFormat format2 = new SimpleDateFormat("MM-dd-yyyy");
			if (UIUtil.isNotNullAndNotEmpty(strCADReleaseDateIn)) {
				Date date = format1.parse(strCADReleaseDateIn);
				String formatDate = format2.format(date);
				jCadPartBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_DATEFROM, formatDate);
			} else
				jCadPartBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_DATEFROM, " ");

			if (UIUtil.isNotNullAndNotEmpty(strCADReleaseDateOut)) {
				if (strCADReleaseDateOut.equals("INF")) {
					strCADReleaseDateOut = "12-31-9999";
					jCadPartBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_DATETO, strCADReleaseDateOut);
				} else {
					Date date = format1.parse(strCADReleaseDateOut);
					String formatDate = format2.format(date);
					jCadPartBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_DATETO, formatDate);
				}
			} else
				jCadPartBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_DATETO, " ");

			jCadPartBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_REALIZED_DATA, true);

		} catch (Exception e) {
			e.printStackTrace();
		}
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
	private static MapList getFirstLevelChildren(matrix.db.Context context, String strObjectId,
			LCDSAPIntegration3DExpConstants lcdSAPInteg3DExpConstants) throws NullPointerException {
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
				objectSelects.add(lcdSAPInteg3DExpConstants.SELECT_ATTRIBUTE_SERVICEABLEITEM_VPMREFERENCE); // "Serviceable
																											// Item";
				objectSelects.add(lcdSAPInteg3DExpConstants.SELECT_ATTRIBUTE_PROCUREMENTINTENT_VPMREFERENCE); // "Procurement
																												// Intent"
				objectSelects.add(lcdSAPInteg3DExpConstants.SELECT_ATTRIBUTE_PARTINTERCHANGEABILITY_VPMREFERENCE); // "Part
																													// Interchangeability
																													// ";
				objectSelects.add(lcdSAPInteg3DExpConstants.SELECT_ATTRIBUTE_UNITOFMEASURE_VPMREFERENCE); // "Unit of
																											// Measure";

				// RelationShip Selectables
				StringList relSelects = new StringList();
				relSelects.add(DomainConstants.SELECT_RELATIONSHIP_ID);
				relSelects.add(lcdSAPInteg3DExpConstants.SELECT_ATTRIBUTE_HAS_CONFIG_EFFECTIVITY);

				// STEP : Expanding the Input Linked CAD Part to get all children of it
				DomainObject partDomObj = DomainObject.newInstance(context, strObjectId);
				mlObjects = partDomObj.getRelatedObjects(context, lcdSAPInteg3DExpConstants.TYPE_VPM_INSTANCE, // relationship
																												// pattern
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
			expMsg.printStackTrace();
		}
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

	private static JsonObjectBuilder getSubContractPartHeader(matrix.db.Context context,
			Map<?, ?> mSubContractPartDetails, LCDSAPIntegration3DExpConstants lcdSAPInteg3DExpConstants)
			throws Exception {
		JsonObjectBuilder jSubContractBuilder = Json.createObjectBuilder();
		try {
			// STEP : Retrieving necessary information of SubContract Part
			String strObjectID = (String) mSubContractPartDetails.get(DomainConstants.SELECT_ID);
			String sName = (String) mSubContractPartDetails.get(DomainConstants.SELECT_NAME);
			String strTitle = (String) mSubContractPartDetails
					.get(lcdSAPInteg3DExpConstants.SELECT_ATTRIBUTE_PLMENTITY_V_NAME);
			String strdescription = (String) mSubContractPartDetails
					.get(lcdSAPInteg3DExpConstants.SELECT_ATTRIBUTE_PLMENTITY_V_DESCRIPTION);
			String strProcurementIntent = (String) mSubContractPartDetails
					.get(lcdSAPInteg3DExpConstants.SELECT_ATTRIBUTE_PROCUREMENTINTENT_VPMREFERENCE);
			String strPartInterchangeability = (String) mSubContractPartDetails
					.get(lcdSAPInteg3DExpConstants.SELECT_ATTRIBUTE_PARTINTERCHANGEABILITY_VPMREFERENCE);
			String strServiceable = (String) mSubContractPartDetails
					.get(lcdSAPInteg3DExpConstants.SELECT_ATTRIBUTE_SERVICEABLEITEM_VPMREFERENCE);

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

			jSubContractBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_REL_ID,
					LCDSAPIntegrationDataConstants.VALUE_NOT_APPLICABLE);

			if (UIUtil.isNotNullAndNotEmpty(strdescription))
				jSubContractBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_DESCRIPTION, strdescription);
			else
				jSubContractBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_DESCRIPTION, " ");

			if (UIUtil.isNotNullAndNotEmpty(strProcurementIntent))
				jSubContractBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_PROCUREMENTINTENT,
						strProcurementIntent);
			else
				jSubContractBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_PROCUREMENTINTENT, " ");

			if (UIUtil.isNotNullAndNotEmpty(strServiceable))
				jSubContractBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_SERVICEABLEITEM, strServiceable);
			else
				jSubContractBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_SERVICEABLEITEM, " ");

			jSubContractBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_PLANTCODE,
					LCDSAPIntegrationDataConstants.VALUE_NOT_APPLICABLE);

			if (UIUtil.isNotNullAndNotEmpty(strPartInterchangeability))
				jSubContractBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_PARTINTERCHANGEABILITY,
						strPartInterchangeability);
			else
				jSubContractBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_PARTINTERCHANGEABILITY, " ");

			jSubContractBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_EFFECTIVITY_OPTION_CODE,
					LCDSAPIntegrationDataConstants.VALUE_NOT_APPLICABLE);
			jSubContractBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_DATEFROM,
					LCDSAPIntegrationDataConstants.VALUE_NOT_APPLICABLE);
			jSubContractBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_DATETO,
					LCDSAPIntegrationDataConstants.VALUE_NOT_APPLICABLE);
			jSubContractBuilder.add(LCDSAPIntegrationDataConstants.PROPERTY_REALIZED_DATA, true);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return jSubContractBuilder;
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
	public static Map<?, ?> getCADPartDetails(matrix.db.Context context, String strOBJId,
			LCDSAPIntegration3DExpConstants lcdSAPInteg3DExpConstants) throws Exception {
		Map<?, ?> objInfoMap = new HashMap<>();
		try {
			if (null != strOBJId && !strOBJId.isEmpty()) {
				// Object Selectables
				StringList objectSelects = new StringList();
				objectSelects.add(DomainConstants.SELECT_ID);
				objectSelects.add(DomainConstants.SELECT_NAME);
				objectSelects.add(lcdSAPInteg3DExpConstants.SELECT_ATTRIBUTE_PLMENTITY_V_NAME);
				objectSelects.add(lcdSAPInteg3DExpConstants.SELECT_ATTRIBUTE_PLMENTITY_V_DESCRIPTION);
				objectSelects.add(lcdSAPInteg3DExpConstants.SELECT_ATTRIBUTE_SERVICEABLEITEM_VPMREFERENCE); // "Serviceable
																											// Item";
				objectSelects.add(lcdSAPInteg3DExpConstants.SELECT_ATTRIBUTE_PROCUREMENTINTENT_VPMREFERENCE); // "Procurement
																												// Intent"
				objectSelects.add(lcdSAPInteg3DExpConstants.SELECT_ATTRIBUTE_PARTINTERCHANGEABILITY_VPMREFERENCE); // "Part
																													// Interchangeability
																													// ";

				DomainObject boCADObj = DomainObject.newInstance(context, strOBJId);
				objInfoMap = boCADObj.getInfo(context, objectSelects);
				return objInfoMap;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return objInfoMap;
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
	private static Map<?, ?> getEffectivity(matrix.db.Context context, String strRelId) throws Exception {
		Map<?, ?> mEffectivity;
		try {

			// ContextUtil.pushContext(context,ROLE_ADMIN,DomainConstants.EMPTY_STRING,DomainConstants.EMPTY_STRING);
			// ContextUtil.pushContext(context);
			ConfigurationExposedFilterablesFactory configurationExposedFilterablesactory = new ConfigurationExposedFilterablesFactory();
			IConfigurationExposedFilterables iConfigurationExposedFilterables = configurationExposedFilterablesactory
					.getIPublicConfigurationFilterablesServices();
			List<String> objects = new ArrayList<String>();
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
		return mEffectivity;
	}

	/***
	 * This method is to sort the Option code
	 * 
	 * @param strOptionCode
	 * @return
	 * @throws Exception
	 */
	public static String sortOptionCode(String strOptionCode) throws Exception {
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
	private static java.util.List<String> sortAlphaNumeric(String strValue) throws Exception {
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

}
