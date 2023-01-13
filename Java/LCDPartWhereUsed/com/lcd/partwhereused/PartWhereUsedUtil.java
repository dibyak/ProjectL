package com.lcd.partwhereused;

import java.io.StringReader;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;

import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.util.MqlUtil;
import com.matrixone.apps.domain.util.PropertyUtil;
import com.matrixone.apps.framework.ui.UIUtil;

import matrix.db.Context;
import matrix.db.ExpansionIterator;
import matrix.db.RelationshipWithSelect;
import matrix.util.StringList;

public class PartWhereUsedUtil {

	private int iPrevLevel;
	private String sInputPartTitle;
	private String sRootTitle;
	private String sRootVDescription;
	private String sRootId;
	private static Deque<Map<String, String>> dq = new ArrayDeque<Map<String, String>>();

	private static final String ATTRIBUTE_PLMENTITY_V_NAME = PropertyUtil
			.getSchemaProperty("attribute_PLMEntity.V_Name");
	private static final String ATTRIBUTE_PLMENTITY_V_DESCRIPTION = PropertyUtil
			.getSchemaProperty("attribute_PLMEntity.V_description");
	private static final String REL_VPMINSTANCE = PropertyUtil.getSchemaProperty("relationship_VPMInstance");
	private static final String TYPE_VPMREFERENCE = PropertyUtil.getSchemaProperty("type_VPMReference");
	private static final String FIELD_TOP_NODE_NAME = "topNodeName";
	private static final String FIELD_TOP_NODE_TITLE = "topNodeTitle";
	private static final String FIELD_NODE_MAP_ARR = "nodeMapArr";
	private static final String FIELD_PHYSICALID = "physicalid";

	/**
	 * 
	 * @param context
	 * @param jsonObjBOM
	 * @return
	 * @throws Exception
	 */
	public Response getPartWhereUsedData(Context context, String jsonObjBOM) throws Exception {
		JsonObjectBuilder output = Json.createObjectBuilder();
		JsonObjectBuilder jsonObjectInput = Json.createObjectBuilder();
		JsonArrayBuilder jsonArrayResult = Json.createArrayBuilder();
		JsonReader jsonReader = null;

		StringList busSelectList = new StringList();
		busSelectList.add(DomainConstants.SELECT_LEVEL);
		busSelectList.add(DomainConstants.SELECT_TYPE);
		busSelectList.add(DomainConstants.SELECT_NAME);
		busSelectList.add("attribute[" + ATTRIBUTE_PLMENTITY_V_NAME + "]");
		busSelectList.add("attribute[" + ATTRIBUTE_PLMENTITY_V_DESCRIPTION + "]");
		busSelectList.add(DomainConstants.SELECT_PHYSICAL_ID);

		StringList relSelectList = new StringList();
		relSelectList.add(DomainConstants.SELECT_RELATIONSHIP_ID);

		dq.clear();
		System.out.println("Start() :: Stack is empty ?? - " + dq.isEmpty());
		try {
			if (!jsonObjBOM.isEmpty()) {
				jsonReader = Json.createReader(new StringReader(jsonObjBOM));
				JsonObject jsonObjInput = jsonReader.readObject();
				sInputPartTitle = jsonObjInput.getString("partTitle");
				JsonArray jaBOMData = jsonObjInput.getJsonArray("bomData");

				if (UIUtil.isNullOrEmpty(sInputPartTitle)) {
					jsonObjectInput.add("message", "");
					return Response.status(HttpServletResponse.SC_BAD_REQUEST)
							.entity(jsonObjectInput.build().toString()).build();
				}
				if (jaBOMData.isEmpty()) {
					jsonObjectInput.add("message", "BOM data is missing!");
					return Response.status(HttpServletResponse.SC_BAD_REQUEST)
							.entity(jsonObjectInput.build().toString()).build();
				}

				for (int i = 0; i < jaBOMData.size(); i++) {
					boolean bHasMatch = false;
					iPrevLevel = 0;
					String sPartTitle, sPartId;
					Integer iCurrLevel;

					JsonObject jsonObjectResp = jaBOMData.getJsonObject(i);
					String sRootType = jsonObjectResp.getString(DomainConstants.SELECT_TYPE);
					String sRootName = jsonObjectResp.getString(DomainConstants.SELECT_NAME);
					String sRootRevision = jsonObjectResp.getString(DomainConstants.SELECT_REVISION);

					String sObjectId = MqlUtil.mqlCommand(context, "print bus $1 $2 $3 select $4 dump", sRootType,
							sRootName, sRootRevision, DomainConstants.SELECT_ID);

					DomainObject domBOM = new DomainObject(sObjectId);
					Map<String, String> mRootInfo = domBOM.getInfo(context, busSelectList);
					sRootTitle = mRootInfo.get("attribute[" + ATTRIBUTE_PLMENTITY_V_NAME + "]");
					sRootVDescription = mRootInfo.get("attribute[" + ATTRIBUTE_PLMENTITY_V_DESCRIPTION + "]");
					sRootId = mRootInfo.get(DomainConstants.SELECT_PHYSICAL_ID);
					
					System.out.println("TF:  getPartWhereUsedData :: sRootTitle - "+sRootTitle);
					System.out.println("TF:  getPartWhereUsedData :: sRootVDescription - "+sRootVDescription);

					ExpansionIterator infoBOM = domBOM.getExpansionIterator(context, // context
							REL_VPMINSTANCE, // rel
							TYPE_VPMREFERENCE, // type
							busSelectList, // object select
							relSelectList, // rel select
							false, // get To relationships
							true, // get from relationships
							(short) 0, // recurseToLevel
							"", // objectWhereClause
							"", // relationshipWhereClause
							(short) 0, // limit
							false, // checkHidden
							false, // preventDuplicagtes
							(short) 500);// pageSize

					while (infoBOM.hasNext()) {
						RelationshipWithSelect relSelect = infoBOM.next();
						sPartTitle = relSelect.getTargetSelectData("attribute[" + ATTRIBUTE_PLMENTITY_V_NAME + "]");
						sPartId = relSelect.getTargetSelectData(DomainConstants.SELECT_PHYSICAL_ID);
						iCurrLevel = Integer.valueOf(relSelect.getLevel());
						System.out.println("TF:  getPartWhereUsedData :: BOMSTR - " + iCurrLevel + " | " + sPartTitle);
						JsonObject joFinalObject = computeBomSTR(sPartTitle, iCurrLevel, sPartId);
						if (!joFinalObject.isEmpty()) {
							bHasMatch = true;
							jsonArrayResult.add(joFinalObject);
						}
						iPrevLevel = iCurrLevel;
					}
					if (!bHasMatch) {
						JsonObjectBuilder jsonBuilderObj = Json.createObjectBuilder();
						jsonBuilderObj.add(DomainConstants.SELECT_LEVEL, "-");
						jsonBuilderObj.add(FIELD_TOP_NODE_TITLE, sRootTitle);
						jsonBuilderObj.add(FIELD_TOP_NODE_NAME, sRootVDescription);
						jsonBuilderObj.add(FIELD_NODE_MAP_ARR, Json.createArrayBuilder());
						jsonArrayResult.add(jsonBuilderObj.build());
					}
				}
				System.out.println("getPartWhereUsedData :: Final Result END() - ");
				output.add("data", jsonArrayResult.build());
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
		return Response.status(HttpServletResponse.SC_OK).entity(output.build().toString()).build();
	}

	private JsonObject computeBomSTR(String sPartTitle, int iCurrLevel, String sPartId) throws Exception {
		int diff = 0;
		JsonObjectBuilder jsonBuilderObj = Json.createObjectBuilder();
		Map<String, String> nodeMap = new HashMap<String, String>();
		try {
			if (iCurrLevel <= iPrevLevel) {
				diff = iPrevLevel - iCurrLevel;
				for (int i = 0; i < diff + 1; i++) {
					dq.pollLast();
				}
			}
			nodeMap.put("PartTitle", sPartTitle);
			nodeMap.put("PartId", sPartId);
			dq.add(nodeMap);
			if (sPartTitle.equals(sInputPartTitle)) {
				JsonArray jsonArrPath = prepareNavigation();
				jsonBuilderObj.add(DomainConstants.SELECT_LEVEL, Integer.toString(iCurrLevel));
				jsonBuilderObj.add(FIELD_TOP_NODE_TITLE, sRootTitle);
				jsonBuilderObj.add(FIELD_TOP_NODE_NAME, sRootVDescription);
				jsonBuilderObj.add(FIELD_NODE_MAP_ARR, jsonArrPath);
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
		return jsonBuilderObj.build();
	}

	private JsonArray prepareNavigation() throws Exception {
		JsonArrayBuilder jsonArrPath = Json.createArrayBuilder();
		try {
			JsonObjectBuilder jsonNodeMap = Json.createObjectBuilder();
			jsonNodeMap.add("PartTitle", sRootTitle);
			jsonNodeMap.add("PartId", sRootId);
			jsonArrPath.add(jsonNodeMap.build());
			Iterator<Map<String, String>> itrDq = dq.iterator();
			while (itrDq.hasNext()) {
				jsonNodeMap = Json.createObjectBuilder();
				Map<String, String> nm = (Map<String, String>) itrDq.next();
				jsonNodeMap.add("PartTitle", nm.get("PartTitle"));
				jsonNodeMap.add("PartId", nm.get("PartId"));
				jsonArrPath.add(jsonNodeMap.build());
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
		return jsonArrPath.build();
	}
}
