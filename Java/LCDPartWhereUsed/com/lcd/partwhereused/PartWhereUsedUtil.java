package com.lcd.partwhereused;

import java.io.StringReader;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
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
import com.matrixone.apps.domain.util.MqlUtil;
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
	private static final String FIELD_TOP_NODE_NAME = "topNodeName";
	private static final String FIELD_TOP_NODE_TITLE = "topNodeTitle";
	private static final String FIELD_NAVIGATION_DATA = "navigationData";

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

		JsonObjectBuilder joInputError = Json.createObjectBuilder();
		JsonArrayBuilder jsonArrayResult = Json.createArrayBuilder();
		JsonReader jsonReader = null;
		ArrayDeque<Map<String, String>> arrayDEQueue = new ArrayDeque<Map<String, String>>();
		
		String sInputPartTitle = DomainConstants.EMPTY_STRING;
		JsonObject jaBOMData = Json.createObjectBuilder().build();

		boolean hasMatch = false;
		int iPrevLevel = 0,iCurrLevel = 0;
		String sPartTitle = DomainConstants.EMPTY_STRING;
		String sPartPId = DomainConstants.EMPTY_STRING;

		String sRootTitle = DomainConstants.EMPTY_STRING;
		String sRootVDescription = DomainConstants.EMPTY_STRING;
		String sRootPId = DomainConstants.EMPTY_STRING;

		StringList busSelectList = new StringList();
		busSelectList.add(DomainConstants.SELECT_LEVEL);
		busSelectList.add(SELECT_ATTRIBUTE_PLMENTITY_V_NAME);
		busSelectList.add(SELECT_ATTRIBUTE_PLMENTITY_V_DESCRIPTION);
		busSelectList.add(DomainConstants.SELECT_PHYSICAL_ID);

		StringList relSelectList = new StringList();
		relSelectList.add(DomainConstants.SELECT_RELATIONSHIP_ID);

		HashMap<String, Object> mapFinalData = new HashMap<String, Object>();

		ArrayList<HashMap<String, Object>> alFinalData = new ArrayList<HashMap<String, Object>>();

//		System.out.println("Start() :: DQueue is empty ?? - " + dq.isEmpty());
		try {
			if (!jsonObjBOM.isEmpty()) {
				jsonReader = Json.createReader(new StringReader(jsonObjBOM));
				JsonObject jsonObjInput = jsonReader.readObject();
				sInputPartTitle = jsonObjInput.getString("partTitle");
				jaBOMData = jsonObjInput.getJsonObject("bomData");

				if (UIUtil.isNullOrEmpty(sInputPartTitle)) {
					joInputError.add("message", "");
					return Response.status(HttpServletResponse.SC_BAD_REQUEST).entity(joInputError.build().toString())
							.build();
				}
				if (jaBOMData.isEmpty()) {
					joInputError.add("message", "BOM data is missing!");
					return Response.status(HttpServletResponse.SC_BAD_REQUEST).entity(joInputError.build().toString())
							.build();
				}

				String sRootType = jaBOMData.getString(DomainConstants.SELECT_TYPE);
				String sRootName = jaBOMData.getString(DomainConstants.SELECT_NAME);
				String sRootRevision = jaBOMData.getString(DomainConstants.SELECT_REVISION);

//				String sObjectId = MqlUtil.mqlCommand(context, "print bus $1 $2 $3 select $4 dump", sRootType,
//						sRootName, sRootRevision, DomainConstants.SELECT_ID);
				
				DomainObject domObjRootPart = DomainObject.newInstance(context, new BusinessObject(sRootType, sRootName, sRootRevision, "vplm"));

				Map<?,?> mRootInfo = domObjRootPart.getInfo(context, busSelectList);
				sRootTitle = (String) mRootInfo.get(SELECT_ATTRIBUTE_PLMENTITY_V_NAME);
				sRootVDescription = (String) mRootInfo.get(SELECT_ATTRIBUTE_PLMENTITY_V_DESCRIPTION);
				sRootPId = (String) mRootInfo.get(DomainConstants.SELECT_PHYSICAL_ID);

//				ExpansionIterator infoBOM = domBOM.getExpansionIterator(context, // context
//						REL_VPMINSTANCE, // rel
//						TYPE_VPMREFERENCE, // type
//						busSelectList, // object select
//						relSelectList, // rel select
//						false, // get To relationships
//						true, // get from relationships
//						(short) 0, // recurseToLevel
//						"", // objectWhereClause
//						"", // relationshipWhereClause
//						(short) 0, // limit
//						false, // checkHidden
//						false, // preventDuplicagtes
//						(short) 500);// pageSize

				long lStartExpansion2 = System.currentTimeMillis();
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

				long diffExpansion2 = System.currentTimeMillis() - lStartExpansion2;
				System.out.println("getPartWhereUsedData :: END Expansion 02 - " + diffExpansion2 + " in ms.");

//					while (infoBOM2.hasNext()) {
//						RelationshipWithSelect relSelect = infoBOM2.next();
//						sPartTitle = relSelect.getTargetSelectData(SELECT_ATTRIBUTE_PLMENTITY_V_NAME);
//						iCurrLevel = Integer.valueOf(relSelect.getLevel());
//						System.out
//								.println("TF:  getPartWhereUsedData :: BOMSTR 02 - " + iCurrLevel + " | " + sPartTitle);
//					}

				RelationshipWithSelect relSelect = null;
				
				
				
				ArrayList<ArrayDeque<Map<String, String>>> alPaths = new ArrayList<>();
				while (infoBOM2.hasNext()) {
					relSelect = infoBOM2.next();
					sPartTitle = relSelect.getTargetSelectData(SELECT_ATTRIBUTE_PLMENTITY_V_NAME);
					sPartPId = relSelect.getTargetSelectData(DomainConstants.SELECT_PHYSICAL_ID);
					iCurrLevel = Integer.valueOf(relSelect.getLevel());

					//System.out.println("TF:  getPartWhereUsedData :: BOMSTR - " + iCurrLevel + " | " + sPartTitle);

					int diff = 0 ;
					
					if (iCurrLevel <= iPrevLevel) {
						diff = iPrevLevel - iCurrLevel;
						for (int i = 0; i < diff + 1; i++) {
							arrayDEQueue.pop();
						}
					}
					HashMap<String, String> nodeMap = new HashMap<>();
					nodeMap.put("PartTitle", sPartTitle);
					nodeMap.put("PartId", sPartPId);
					arrayDEQueue.push(nodeMap);
					if (sPartTitle.equals(sInputPartTitle)) {
						alPaths.add(arrayDEQueue.clone());
						hasMatch = true;
						//aPath = prepareNavigation(arrayDEQueue, sRootPId);
					}
					
					
					
					//ArrayList<Map<String, String>> navigationPath = computeBomSTR(arrayDEQueue, sInputPartTitle, sPartTitle,
						//	sPartPId, iCurrLevel, iPrevLevel, sRootPId);

					//System.out.println("TF:  getPartWhereUsedData :: navigationPath - " + navigationPath);

//					if (!navigationPath.isEmpty()) {
//						bHasMatch = true;
//						mapFinalData = new HashMap<>();
//						mapFinalData.put(DomainConstants.SELECT_LEVEL, Integer.toString(iCurrLevel));
//						mapFinalData.put(FIELD_TOP_NODE_TITLE, sRootTitle);
//						mapFinalData.put(FIELD_TOP_NODE_NAME, sRootVDescription);
//						mapFinalData.put(FIELD_NAVIGATION_DATA, navigationPath);
//						alFinalData.add(mapFinalData);
//
//					}
					iPrevLevel = iCurrLevel;
				}
				
				
				System.out.println("all paths = " + alPaths);
				
//				if (!bHasMatch) {
//					mapFinalData = new HashMap<>();
//					mapFinalData.put(DomainConstants.SELECT_LEVEL, "-");
//					mapFinalData.put(FIELD_TOP_NODE_TITLE, sRootTitle);
//					mapFinalData.put(FIELD_TOP_NODE_NAME, sRootVDescription);
//					mapFinalData.put(FIELD_NAVIGATION_DATA, new ArrayList<>());
//					alFinalData.add(mapFinalData);
//				}
//				jsonArrayResult = buildResponse(alFinalData);
				
				jsonArrayResult = buildResponse(alPaths, sRootTitle, sRootVDescription, hasMatch);

			}
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
		long diff = System.currentTimeMillis() - start;
		System.out.println("getPartWhereUsedData :: START Method Total time - " + diff + " in ms.");
		return Response.status(HttpServletResponse.SC_OK).entity(jsonArrayResult.build().toString()).build();
	}

	
	
	private JsonArrayBuilder buildResponse(ArrayList<ArrayDeque<Map<String, String>>> alPaths, String strRootVName, String strRootDesc, boolean hasMatch) throws Exception {
		JsonArrayBuilder jArrFinal = Json.createArrayBuilder();
		try {
			
			if(!hasMatch) {
				JsonObjectBuilder jObj = Json.createObjectBuilder();
				jObj.add("topNodeName", strRootDesc);
				jObj.add("topNodeTitle", strRootVName);
				jObj.add("level", "-");
				jObj.add("nodeMapArr", Json.createArrayBuilder().build());
				jArrFinal.add(jObj);
				return jArrFinal;
			}
			
			for (ArrayDeque<Map<String, String>> arrayDeque : alPaths) {
				Iterator<Map<String, String>> itr = arrayDeque.descendingIterator();
				JsonObjectBuilder jObj = Json.createObjectBuilder();
				jObj.add("topNodeName", strRootDesc);
				jObj.add("topNodeTitle", strRootVName);
				jObj.add("level", Integer.toString(arrayDeque.size()));
				JsonArrayBuilder jArrPaths = Json.createArrayBuilder();
				while (itr.hasNext()) {
					Map<java.lang.String, java.lang.String> map = (Map<java.lang.String, java.lang.String>) itr.next();
					JsonObjectBuilder jObjPartDetail = Json.createObjectBuilder();

					for (Map.Entry<String, String> entry : map.entrySet()) {
						jObjPartDetail.add(entry.getKey(), entry.getValue());
					}
					
					jArrPaths.add(jObjPartDetail);
				}
				
				jObj.add("nodeMapArr", jArrPaths);
				jArrFinal.add(jObj);
			}
			
		
			
			
//			[
//				{
//				  level: "6",
//				  topNodeName: "GRAVITY PRODUCTION P-TREE",
//				  topNodeTitle: "P21-A00001-00 1.1",
//				  nodeMapArr: [
//						{"PartTitle": "P21-A00001-00 1.1", "PartId": "2506FF85000016246113E8BB001031B0"},
//						{"PartTitle": "V21-B00000-00 1.1", "PartId": "2506FF85000016246113E8BB001031B1"},
//					]
//				}
//			]
			
			
			
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
		return jArrFinal;
	}
	
	
//	private JsonArrayBuilder buildResponse(ArrayList<HashMap<String, Object>> mlFinalData) throws Exception {
//		JsonArrayBuilder jArr = Json.createArrayBuilder();
//		JsonObjectBuilder jObj = Json.createObjectBuilder();
//		try {
//			for (HashMap<String, Object> map : mlFinalData) {
//
//				map.forEach((key, value) -> {
//					if (value instanceof String) {
//						jObj.add(key, (String) value);
//					} else if (value instanceof Object) {
//						JsonArrayBuilder jsonArray = Json.createArrayBuilder();
//
//						for (Map<String, String> map1 : (ArrayList<Map<String, String>>) value) {
//							JsonObjectBuilder jsonObj = Json.createObjectBuilder();
//							for (Map.Entry<String, String> entry : map1.entrySet()) {
//								jsonObj.add(entry.getKey(), entry.getValue());
//							}
//							jsonArray.add(jsonObj.build());
//						}
//						jObj.add(key, jsonArray.build());
//					}
//				});
//
//				jArr.add(jObj);
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//			throw e;
//		}
//		return jArr;
//	}

	private ArrayList<ArrayDeque<Map<String, String>>> computeBomSTR(ArrayDeque<Map<String, String>> arrayDEQueue, String sInputPartTitle,
			String sPartTitle, String sPartId, int iCurrLevel, int iPrevLevel, String sRootPId) throws Exception {
		int diff = 0;
		ArrayList<ArrayDeque<Map<String, String>>> aPath = new ArrayList<ArrayDeque<Map<String, String>>>();
		Map<String, String> nodeMap = new HashMap<String, String>();
		try {
			if (iCurrLevel <= iPrevLevel) {
				diff = iPrevLevel - iCurrLevel;
				for (int i = 0; i < diff + 1; i++) {
					arrayDEQueue.pop();
				}
			}
			nodeMap.put("PartTitle", sPartTitle);
			nodeMap.put("PartId", sPartId);
			arrayDEQueue.push(nodeMap);
			if (sPartTitle.equals(sInputPartTitle)) {
				aPath.add(arrayDEQueue.clone());
				//aPath = prepareNavigation(arrayDEQueue, sRootPId);
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
		return aPath;
	}

	private ArrayList<Map<String, String>> prepareNavigation(Deque<Map<String, String>> dq, String sRootPId)
			throws Exception {
		ArrayList<Map<String, String>> arrayPath = new ArrayList<Map<String, String>>();
		try {
			Map<String, String> mapPath = new HashMap<String, String>();
			mapPath.put("sPartId", sRootPId);
			mapPath.put("sPartTitle", "");
			arrayPath.add(mapPath);
			Iterator<Map<String, String>> itrDq = dq.iterator();
			while (itrDq.hasNext()) {
				mapPath = new HashMap<String, String>();
				Map<String, String> value = itrDq.next();
				mapPath.put("sPartId", (String) value.get("PartId"));
				mapPath.put("sPartTitle", (String) value.get("PartTitle"));
				arrayPath.add(mapPath);
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
		return arrayPath;
	}
}
