package com.lcd.partwhereused;

import java.io.StringReader;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;

import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.util.PropertyUtil;
import com.matrixone.apps.framework.ui.UIUtil;

import matrix.db.BusinessObject;
import matrix.db.Context;
import matrix.db.ExpandParams;
import matrix.db.ExpansionIterator;
import matrix.db.RelationshipWithSelect;
import matrix.util.StringList;

public class PartWhereUsedUtil {

	// attributes
	private static final String ATTRIBUTE_PLMENTITY_V_NAME = PropertyUtil.getSchemaProperty(null,
			"attribute_PLMEntity.V_Name");
	private static final String SELECT_ATTRIBUTE_PLMENTITY_V_NAME = DomainObject
			.getAttributeSelect(ATTRIBUTE_PLMENTITY_V_NAME);
	private static final String ATTRIBUTE_PLMENTITY_V_DESCRIPTION = PropertyUtil.getSchemaProperty(null,
			"attribute_PLMEntity.V_description");
	private static final String SELECT_ATTRIBUTE_PLMENTITY_V_DESCRIPTION = DomainObject
			.getAttributeSelect(ATTRIBUTE_PLMENTITY_V_DESCRIPTION);

	// Relationships
	private static final String REL_VPMINSTANCE = PropertyUtil.getSchemaProperty(null, "relationship_VPMInstance");

	// types
	private static final String TYPE_VPMREFERENCE = PropertyUtil.getSchemaProperty(null, "type_VPMReference");

	// extra
	private static final String FIELD_LEVEL = "level";
	private static final String FIELD_TOP_NODE_NAME = "topNodeName";
	private static final String FIELD_TOP_NODE_TITLE = "topNodeTitle";
	private static final String FIELD_NAVIGATION_DATA =  "nodeMapArr";
	private static final String FIELD_PART_TITLE = "PartTitle";
	private static final String FIELD_PART_ID = "PartId";

	/**
	 * Search User Input in EBom Structure
	 * 
	 * @param context
	 * @param jsonObjBOM
	 * @return Response
	 * @throws Exception
	 */
	public Response getPartWhereUsedData(Context context, String jsonObjBOM) throws Exception {
		System.out.println("getPartWhereUsedData :: START Method !!");
		long start = System.currentTimeMillis();

		Response res = null;
		JsonObjectBuilder joInputError = Json.createObjectBuilder();
		JsonArrayBuilder jsonArrayResult = Json.createArrayBuilder();
		JsonReader jsonReader = null;
		ArrayDeque<Map<String, String>> arrayDEQueue = new ArrayDeque<Map<String, String>>();

		String sInputPartTitle = DomainConstants.EMPTY_STRING;
		String sInputPartRevision = DomainConstants.EMPTY_STRING;
		JsonObject jaBOMData = Json.createObjectBuilder().build();

		boolean hasMatch = false;
		int iPrevLevel = 0, iCurrLevel = 0;
		String sPartTitle = DomainConstants.EMPTY_STRING;
		String sPartPId = DomainConstants.EMPTY_STRING;
		String sPartRevision = DomainConstants.EMPTY_STRING;

		String sRootTitle = DomainConstants.EMPTY_STRING;
		String sRootVDescription = DomainConstants.EMPTY_STRING;
		String sRootPId = DomainConstants.EMPTY_STRING;

		StringList busSelectList = new StringList();
		busSelectList.add(DomainConstants.SELECT_LEVEL);
		busSelectList.add(SELECT_ATTRIBUTE_PLMENTITY_V_NAME);
		busSelectList.add(SELECT_ATTRIBUTE_PLMENTITY_V_DESCRIPTION);
		busSelectList.add(DomainConstants.SELECT_PHYSICAL_ID);
		busSelectList.add(DomainConstants.SELECT_REVISION);

		StringList relSelectList = new StringList();
		relSelectList.add(DomainConstants.SELECT_RELATIONSHIP_ID);

		try {
			if (!jsonObjBOM.isEmpty()) {
				jsonReader = Json.createReader(new StringReader(jsonObjBOM));
				JsonObject jsonObjInput = jsonReader.readObject();
				sInputPartTitle = jsonObjInput.getString("partTitle");
				jaBOMData = jsonObjInput.getJsonObject("bomData");

				if (sInputPartTitle.contains(" ")) {
					String[] inputData = sInputPartTitle.split(" ");
					if (inputData.length > 2) {
						joInputError.add("message", "Please remove extra blank spaces from input string.");
						return Response.status(HttpServletResponse.SC_BAD_REQUEST)
								.entity(joInputError.build().toString()).build();
					}
					sInputPartTitle = inputData[0];
					sInputPartRevision = inputData[1];
				} else {
					sInputPartRevision = "";
				}
				
				if (jaBOMData.isEmpty()) {
					joInputError.add("message", "BOM data is missing!");
					return Response.status(HttpServletResponse.SC_BAD_REQUEST).entity(joInputError.build().toString())
							.build();
				}

				String sRootType = jaBOMData.getString(DomainConstants.SELECT_TYPE);
				String sRootName = jaBOMData.getString(DomainConstants.SELECT_NAME);
				String sRootRevision = jaBOMData.getString(DomainConstants.SELECT_REVISION);

				DomainObject domObjRootPart = DomainObject.newInstance(context,
						new BusinessObject(sRootType, sRootName, sRootRevision, "vplm"));

				Map<?, ?> mRootInfo = domObjRootPart.getInfo(context, busSelectList);
				sRootTitle = (String) mRootInfo.get(SELECT_ATTRIBUTE_PLMENTITY_V_NAME);
				sRootVDescription = (String) mRootInfo.get(SELECT_ATTRIBUTE_PLMENTITY_V_DESCRIPTION);
				sRootPId = (String) mRootInfo.get(DomainConstants.SELECT_PHYSICAL_ID);

				ExpandParams expandParams = ExpandParams.getParams();
				expandParams.setRelPattern(REL_VPMINSTANCE);
				expandParams.setTypePattern(TYPE_VPMREFERENCE);
				expandParams.setBusSelects(busSelectList);
				expandParams.setRelSelects(relSelectList);
				expandParams.setGetTo(false);
				expandParams.setGetFrom(true);
				expandParams.setRecurse((short) 0);
				expandParams.setObjWhere("");
				expandParams.setRelWhere("");
				expandParams.setLimit((short) 0);
				expandParams.setCheckHidden(false);
				expandParams.setPreventDuplicates(false);
				expandParams.setPageSize(500);
				ExpansionIterator infoBOM2 = domObjRootPart.getExpansionIterator(context, expandParams);

				RelationshipWithSelect relSelect = null;
				ArrayList<ArrayDeque<Map<String, String>>> alPaths = new ArrayList<>();

				while (infoBOM2.hasNext()) {
					relSelect = infoBOM2.next();
					sPartTitle = relSelect.getTargetSelectData(SELECT_ATTRIBUTE_PLMENTITY_V_NAME);
					sPartPId = relSelect.getTargetSelectData(DomainConstants.SELECT_PHYSICAL_ID);
					sPartRevision = relSelect.getTargetSelectData(DomainConstants.SELECT_REVISION);
					iCurrLevel = Integer.valueOf(relSelect.getLevel());

					int diff = 0;
					if (iCurrLevel <= iPrevLevel) {
						diff = iPrevLevel - iCurrLevel;
						for (int i = 0; i < diff + 1; i++) {
							arrayDEQueue.pop();
						}
					}

					HashMap<String, String> nodeMap = new HashMap<>();
					nodeMap.put(FIELD_PART_TITLE, sPartTitle + " " + sPartRevision);
					nodeMap.put(FIELD_PART_ID, sPartPId);
					arrayDEQueue.push(nodeMap);

					if (sPartTitle.equals(sInputPartTitle) && (UIUtil.isNullOrEmpty(sInputPartRevision)
							|| (UIUtil.isNotNullAndNotEmpty(sInputPartRevision)
									&& sPartRevision.equals(sInputPartRevision)))) {
						alPaths.add(arrayDEQueue.clone());
						hasMatch = true;
					}
					iPrevLevel = iCurrLevel;
				}
				jsonArrayResult = buildResponse(alPaths, sRootTitle, sRootVDescription, sRootPId, sRootRevision, hasMatch);
				res = Response.status(HttpServletResponse.SC_OK).entity(jsonArrayResult.build().toString()).build();
			}
		} catch (Exception e) {
			e.printStackTrace();
			res = Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
			throw e;
		}
		long diff = System.currentTimeMillis() - start;
		System.out.println("getPartWhereUsedData :: START Method Total time - " + diff + " in ms.");
		return res;
	}

	private JsonArrayBuilder buildResponse(ArrayList<ArrayDeque<Map<String, String>>> alPaths, String strRootVName,
			String strRootDesc, String sRootPId, String sRootRevision, boolean hasMatch) throws Exception {
		JsonArrayBuilder jArrFinal = Json.createArrayBuilder();
		try {

			if (!hasMatch) {
				JsonObjectBuilder jObj = Json.createObjectBuilder();
				jObj.add(FIELD_LEVEL, "-");
				jObj.add(FIELD_TOP_NODE_NAME, strRootDesc);
				jObj.add(FIELD_TOP_NODE_TITLE, strRootVName);
				jObj.add(FIELD_NAVIGATION_DATA, Json.createArrayBuilder().build());
				jArrFinal.add(jObj);
				return jArrFinal;
			}

			for (ArrayDeque<Map<String, String>> arrayDeque : alPaths) {
				Iterator<Map<String, String>> itr = arrayDeque.descendingIterator();
				JsonObjectBuilder jObj = Json.createObjectBuilder();
				jObj.add(FIELD_LEVEL, Integer.toString(arrayDeque.size()));
				jObj.add(FIELD_TOP_NODE_NAME, strRootDesc);
				jObj.add(FIELD_TOP_NODE_TITLE, strRootVName);

				JsonArrayBuilder jArrPaths = Json.createArrayBuilder();

				JsonObjectBuilder jObjPartDetail = Json.createObjectBuilder();
				jObjPartDetail.add(FIELD_PART_TITLE, strRootVName + " " + sRootRevision);
				jObjPartDetail.add(FIELD_PART_ID, sRootPId);
				jArrPaths.add(jObjPartDetail);

				while (itr.hasNext()) {
					Map<String, String> map = (Map<String, String>) itr.next();
					jObjPartDetail = Json.createObjectBuilder();

					for (Map.Entry<String, String> entry : map.entrySet()) {
						jObjPartDetail.add(entry.getKey(), entry.getValue());
					}
					jArrPaths.add(jObjPartDetail);
				}
				jObj.add(FIELD_NAVIGATION_DATA, jArrPaths);
				jArrFinal.add(jObj);
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
		return jArrFinal;
	}
}