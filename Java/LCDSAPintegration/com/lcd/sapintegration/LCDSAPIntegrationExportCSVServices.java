package com.lcd.sapintegration;

import java.io.File;
import java.io.FileWriter;
import java.io.StringReader;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.dassault_systemes.platform.restServices.RestService;
import com.lcd.sapintegration.util.LCDSAPIntegration3DExpConstants;
import com.lcd.sapintegration.util.LCDSAPIntegrationDataConstants;
import com.lcd.sapintegration.util.LCDSAPIntegrationGetManAssemblyJson;
import com.lcd.sapintegration.util.LCDSAPIntegrationGetPhysicalProductJson;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;

import matrix.util.StringList;

@Path("/LCDSAPIntegrationExportCSVService")
public class LCDSAPIntegrationExportCSVServices extends RestService {
	
	
	
	@POST
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	@Path("/exportCSV")
	public Response exportCSV(@Context HttpServletRequest request, String paramString) throws Exception {
		Response res = null;

		try {
			boolean isSCMandatory = false;
			matrix.db.Context context = getAuthenticatedContext(request, isSCMandatory);

			LCDSAPIntegration3DExpConstants lcdSAPInteg3DExpConstants = new LCDSAPIntegration3DExpConstants(context);

			JsonObject jsonObject;
			JsonReader jsonReader = Json.createReader(new StringReader(paramString));
			JsonObject joRequest = jsonReader.readObject();

			String strBomId = ((JsonValue) joRequest.get(LCDSAPIntegrationDataConstants.PROPERTY_BOM_COMPONENT_ID))
					.toString().replace("\"", "");
			String strcaId = ((JsonValue) joRequest.get(LCDSAPIntegrationDataConstants.PROPERTY_CA_ID)).toString()
					.replace("\"", "");
			String strConnectionId = ((JsonValue) joRequest.get(LCDSAPIntegrationDataConstants.PROPERTY_CONNECTION_ID))
					.toString().replace("\"", "");
			String strBomName = ((JsonValue) joRequest.get(LCDSAPIntegrationDataConstants.PROPERTY_BOM_COMPONENT_NAME))
					.toString().replace("\"", "").replace("-", "_");
			jsonReader.close();

			StringBuilder strBuildExportCSV = new StringBuilder();

			String strWorkSpace = context.createWorkspace();
			String csvFile = strWorkSpace + "\\" + strBomName + LCDSAPIntegrationDataConstants.CSV_EXTENSION;

			DomainObject domObjBomConponent = DomainObject.newInstance(context, strBomId);
			StringList slObjectSelect = new StringList();
			slObjectSelect.add(DomainConstants.SELECT_TYPE);

			Map<?, ?> bomDetailsMap = domObjBomConponent.getInfo(context, slObjectSelect);
			String strBomType = (String) bomDetailsMap.get(DomainConstants.SELECT_TYPE);

			if (lcdSAPInteg3DExpConstants.TYPE_MANUFACTURING_ASSEMBLY.equalsIgnoreCase(strBomType)) {
				jsonObject = LCDSAPIntegrationGetManAssemblyJson.getManAssemblyJSON(context, strBomId, strBomType,
						strcaId);
			} else {
				Map<?, ?> mCadPartDetails = LCDSAPIntegrationGetPhysicalProductJson.getCADPartDetails(context, strBomId,
						lcdSAPInteg3DExpConstants);
				jsonObject = LCDSAPIntegrationGetPhysicalProductJson.getPhysicalProductJSON(context, mCadPartDetails,
						strConnectionId, lcdSAPInteg3DExpConstants);
			}

			if (jsonObject != null) {
				Set<String> caKeys = jsonObject.keySet();
				LinkedList<HashMap> childPartLinkedList = new LinkedList<>();
				LinkedHashSet<String> setPartKeys = new LinkedHashSet<>();

				HashMap<String, Object> headerKeyValueMap = new HashMap<>();
				strBuildExportCSV.append(LCDSAPIntegrationDataConstants.CA_DETAILS)
						.append(LCDSAPIntegrationDataConstants.NEW_LINE);
				for (String caKey : caKeys) {

					if (caKey.equals(LCDSAPIntegrationDataConstants.PROPERTY_HEADER_PART)) {
						JsonObject jobHeaderPart = jsonObject.getJsonObject(caKey);
						Set<String> headerPartKeys = jobHeaderPart.keySet();

						for (String key : headerPartKeys) {
							if (key.equals(LCDSAPIntegrationDataConstants.PROPERTY_CHILDREN)) {

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

						if (caKey.equalsIgnoreCase(LCDSAPIntegrationDataConstants.PROPERTY_DESCRIPTION)) {
							strBuildExportCSV.append(LCDSAPIntegrationDataConstants.PROPERTY_DESCRIPTION
									+ LCDSAPIntegrationDataConstants.COMMA_SEP + jsonObject.get(caKey).toString()
									+ LCDSAPIntegrationDataConstants.NEW_LINE);
						} else if (caKey.equalsIgnoreCase(
								LCDSAPIntegrationDataConstants.PROPERTY_CA_APPLICABILITY_START_DATE)) {
							strBuildExportCSV
									.append(LCDSAPIntegrationDataConstants.PROPERTY_CA_APPLICABILITY_START_DATE + LCDSAPIntegrationDataConstants.COMMA_SEP
											+ jsonObject.get(caKey).toString() + LCDSAPIntegrationDataConstants.NEW_LINE);
						} else if (caKey
								.equalsIgnoreCase(LCDSAPIntegrationDataConstants.PROPERTY_CA_APPLICABILITY_END_DATE)) {
							strBuildExportCSV
									.append(LCDSAPIntegrationDataConstants.PROPERTY_CA_APPLICABILITY_END_DATE + LCDSAPIntegrationDataConstants.COMMA_SEP
											+ jsonObject.get(caKey).toString() + LCDSAPIntegrationDataConstants.NEW_LINE);
						} else
							strBuildExportCSV.append(caKey + LCDSAPIntegrationDataConstants.COMMA_SEP
									+ jsonObject.get(caKey).toString() + LCDSAPIntegrationDataConstants.NEW_LINE);
					}
				}
				strBuildExportCSV.append(LCDSAPIntegrationDataConstants.NEW_LINE);
				strBuildExportCSV.append(LCDSAPIntegrationDataConstants.REALIZED_ITEM_DETAILS)
						.append(LCDSAPIntegrationDataConstants.NEW_LINE);
				writePartKeys(strBuildExportCSV, setPartKeys);
				writeHeaderPartToCSV(strBuildExportCSV, setPartKeys, headerKeyValueMap);
				writeChildPartToCSV(strBuildExportCSV, setPartKeys, childPartLinkedList);

			}
			System.out.println("done");
			File file = new File(csvFile);
			if (file.createNewFile()) {
				FileWriter myWriter = new FileWriter(file);
				myWriter.write(strBuildExportCSV.toString());
				myWriter.flush();
				myWriter.close();
			} else {
				FileWriter myWriter = new FileWriter(file);
				myWriter.write(strBuildExportCSV.toString());
				myWriter.flush();
				myWriter.close();
			}

			res = Response.ok(file, MediaType.APPLICATION_OCTET_STREAM)
					.header("Content-Disposition", "attachment; filename=\"" + file.getName() + "\"").build();
		} catch (Exception e) {
			e.printStackTrace();
			res = Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
		}
		return res;
	}

	private static void writePartKeys(StringBuilder strBuildExportCSV, LinkedHashSet<String> setPartKeys) {
		strBuildExportCSV.append(LCDSAPIntegrationDataConstants.HEADER_LEVEL)
				.append(LCDSAPIntegrationDataConstants.COMMA_SEP);
		for (String keySet : setPartKeys) {
			if (keySet.equalsIgnoreCase(LCDSAPIntegrationDataConstants.PROPERTY_OID))
				strBuildExportCSV.append(LCDSAPIntegrationDataConstants.PROPERTY_OBJECT_UNIQUE_ID).append(LCDSAPIntegrationDataConstants.COMMA_SEP);
			else if (keySet.equalsIgnoreCase(LCDSAPIntegrationDataConstants.PROPERTY_REL_ID))
				strBuildExportCSV.append(LCDSAPIntegrationDataConstants.PROPERTY_INSTANCE_UNIQUE_ID).append(LCDSAPIntegrationDataConstants.COMMA_SEP);
			else if (keySet.equalsIgnoreCase(LCDSAPIntegrationDataConstants.PROPERTY_SAP_MBOM_UPDATED_ON))
				strBuildExportCSV.append(LCDSAPIntegrationDataConstants.PROPERTY_SAP_FEEDBACK_TIMESTAMP).append(LCDSAPIntegrationDataConstants.COMMA_SEP);
			else
				strBuildExportCSV.append(keySet).append(LCDSAPIntegrationDataConstants.COMMA_SEP);
		}
		strBuildExportCSV.append(LCDSAPIntegrationDataConstants.NEW_LINE);

	}

	private static void writeHeaderPartToCSV(StringBuilder strBuildExportCSV, LinkedHashSet<String> setPartKeys,
			HashMap<String, Object> headerKeyValueMap) {
		strBuildExportCSV.append("0").append(LCDSAPIntegrationDataConstants.COMMA_SEP);
		for (String keySet : setPartKeys) {
			if (headerKeyValueMap.get(keySet) == null) {
				strBuildExportCSV.append("").append(LCDSAPIntegrationDataConstants.COMMA_SEP);
			} else
				strBuildExportCSV.append(headerKeyValueMap.get(keySet))
						.append(LCDSAPIntegrationDataConstants.COMMA_SEP);
		}
		strBuildExportCSV.append(LCDSAPIntegrationDataConstants.NEW_LINE);
	}

	private static void writeChildPartToCSV(StringBuilder strBuildExportCSV, LinkedHashSet<String> setPartKeys,
			LinkedList<HashMap> childPartLinkedList) {

		for (int i = 0; i < childPartLinkedList.size(); i++) {
			HashMap<?, ?> hm = childPartLinkedList.get(i);
			strBuildExportCSV.append("1").append(LCDSAPIntegrationDataConstants.COMMA_SEP);
			for (String keySet : setPartKeys) {

				if (hm.get(keySet) == null) {
					strBuildExportCSV.append("").append(LCDSAPIntegrationDataConstants.COMMA_SEP);
				} else
					strBuildExportCSV.append(hm.get(keySet)).append(LCDSAPIntegrationDataConstants.COMMA_SEP);
			}
			strBuildExportCSV.append(LCDSAPIntegrationDataConstants.NEW_LINE);
		}
	}
}
