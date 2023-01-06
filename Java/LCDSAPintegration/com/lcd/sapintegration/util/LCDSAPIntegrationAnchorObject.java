package com.lcd.sapintegration.util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;

import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.DomainRelationship;
import com.matrixone.apps.domain.util.FrameworkException;
import com.matrixone.apps.domain.util.MapList;

import matrix.db.BusinessObject;
import matrix.db.Context;
import matrix.util.MatrixException;
import matrix.util.StringList;

public class LCDSAPIntegrationAnchorObject {
	private final Map<String, Map<?,?>> multipleCAInfoMap = new HashMap<>();
	
	/**
	 * TODO
	 * @param context
	 * @return
	 * @throws MatrixException
	 */
	public static String getConnectedAssemblyDetailsAsJsonString (Context context) throws MatrixException {
		LCDSAPIntegration3DExpConstants lcdSAPInteg3DExpConstants = new LCDSAPIntegration3DExpConstants(context);
		BusinessObject busObjAchor = new BusinessObject(lcdSAPInteg3DExpConstants.TYPE_LCD_BOM_ANCHOR_OBJECT, lcdSAPInteg3DExpConstants.NAME_LCD_BOM_ANCHOR_OBJECT,
				lcdSAPInteg3DExpConstants.REVISION_LCD_BOM_ANCHOR_OBJECT, lcdSAPInteg3DExpConstants.VAULT_ESERVICE_PRODUCTION);
		DomainObject domObjSAPAnchorObj = DomainObject.newInstance(context, busObjAchor);
		StringList slObjectSelect = new StringList();
		slObjectSelect.add(DomainConstants.SELECT_ID);
		slObjectSelect.add(DomainConstants.SELECT_NAME);
		slObjectSelect.add(DomainConstants.SELECT_REVISION);
		slObjectSelect.add(lcdSAPInteg3DExpConstants.SELECT_ATTRIBUTE_PLMENTITY_V_NAME);
		// TODO
		slObjectSelect.add("attribute[PLMEntity.V_description]");
		slObjectSelect.add(DomainConstants.SELECT_CURRENT);
		// TODO
		slObjectSelect.add("attribute[LCDMF_ManufacturingAssembly.LCDMF_SAPMBOMUpdatedOn]");
		StringList slRelSelect = new StringList();
		slRelSelect.add(DomainRelationship.SELECT_ID);
		// TODO : Add comments for each argument
		MapList mlAssmblyConnectedToAnchorObject = 
				domObjSAPAnchorObj.getRelatedObjects(
						context,
						"LCD_SAPBOMInterface", //TODO
						DomainConstants.QUERY_WILDCARD,
						slObjectSelect,
						slRelSelect,
						false,
						true,
						(short) 1,
						"",
						"",
						0);
		
		return new LCDSAPIntegrationAnchorObject().createAssmblyDataJsonString(context, mlAssmblyConnectedToAnchorObject, lcdSAPInteg3DExpConstants);
	}
	
	/**
	 * TODO
	 * @param context
	 * @param mlAssmblyConnectedToAnchorObject
	 * @param lcdSAPInteg3DExpConstants
	 * @return
	 * @throws FrameworkException
	 */
	private String createAssmblyDataJsonString(Context context, MapList mlAssmblyConnectedToAnchorObject, LCDSAPIntegration3DExpConstants lcdSAPInteg3DExpConstants)
			throws FrameworkException {
		Iterator<?> iterMLAssmblyConnectedToAnchorObject = mlAssmblyConnectedToAnchorObject.iterator();
		JsonArrayBuilder jabMAs = Json.createArrayBuilder();
		StringList slCAobjectSelects = new StringList();
		slCAobjectSelects.add(DomainConstants.SELECT_NAME);
		slCAobjectSelects.add(lcdSAPInteg3DExpConstants.SELECT_ATTRIBUTE_ACTUAL_COMPLETION_DATE);
		while (iterMLAssmblyConnectedToAnchorObject.hasNext()) {
			JsonObjectBuilder jobMA = Json.createObjectBuilder();
			Map<?, ?> tempAssmblyDataMap = (Map<?, ?>) iterMLAssmblyConnectedToAnchorObject.next();
			String strLCDSAPInterfaceConnId = (String) tempAssmblyDataMap.get(DomainConstants.SELECT_ID);
			Map<?,?> tempLCDSAPInterfaceConnDataMap = DomainRelationship.newInstance(context, strLCDSAPInterfaceConnId).getAttributeMap(context);
			//TODO
			String strCAID = (String) tempLCDSAPInterfaceConnDataMap.get("LCD_CAID");
			Map<?,?> changeActionAttrDetailsMap = getChangeActionDetails(context, strCAID, slCAobjectSelects);
			jobMA.add(LCDSAPIntegrationDataConstants.PROPERTY_CONNECTION_ID, strLCDSAPInterfaceConnId);
			jobMA.add(LCDSAPIntegrationDataConstants.PROPERTY_CA_ID, strCAID);
			jobMA.add(LCDSAPIntegrationDataConstants.PROPERTY_BOM_COMPONENT_ID, (String) tempAssmblyDataMap.get(DomainConstants.SELECT_ID));
			jobMA.add(LCDSAPIntegrationDataConstants.PROPERTY_BOM_COMPONENT_NAME, (String) tempAssmblyDataMap.get(DomainConstants.SELECT_NAME));
			//TODO
			jobMA.add(LCDSAPIntegrationDataConstants.PROPERTY_STATUS, (String)tempLCDSAPInterfaceConnDataMap.get("LCD_ProcessStatusFlag"));
			jobMA.add(LCDSAPIntegrationDataConstants.PROPERTY_REVISION, (String) tempAssmblyDataMap.get(DomainConstants.SELECT_REVISION));
			jobMA.add(LCDSAPIntegrationDataConstants.PROPERTY_TITLE, (String) tempAssmblyDataMap.get(lcdSAPInteg3DExpConstants.SELECT_ATTRIBUTE_PLMENTITY_V_NAME));
			jobMA.add(LCDSAPIntegrationDataConstants.PROPERTY_MATURITY, (String) tempAssmblyDataMap.get(DomainConstants.SELECT_CURRENT));
			jobMA.add(LCDSAPIntegrationDataConstants.DESCRIPTION, (String) tempAssmblyDataMap.get(lcdSAPInteg3DExpConstants.ATTRIBUTE_PLMENTITY_V_DESCRIPTION));
			jobMA.add(LCDSAPIntegrationDataConstants.CA_COMPLETED_TIME, (String) changeActionAttrDetailsMap.get(lcdSAPInteg3DExpConstants.SELECT_ATTRIBUTE_ACTUAL_COMPLETION_DATE));
			jobMA.add(LCDSAPIntegrationDataConstants.CA_NAME, (String) changeActionAttrDetailsMap.get(DomainConstants.SELECT_NAME));
			//TODO
			jobMA.add(LCDSAPIntegrationDataConstants.SAP_FEEDBACK_TIMESTAMP, (String) tempAssmblyDataMap.get("attribute[LCDMF_ManufacturingAssembly.LCDMF_SAPMBOMUpdatedOn]"));
			//TODO
			jobMA.add(LCDSAPIntegrationDataConstants.SAP_FEEDBACK_MESSAGE, (String)tempLCDSAPInterfaceConnDataMap.get("LCD_ReasonforFailure"));
			jabMAs.add(jobMA);
		}
		return jabMAs.build().toString();
	}
	
	/**
	 * TODO
	 * @param context
	 * @param strCAId
	 * @param slObjSelects
	 * @return
	 * @throws FrameworkException
	 */
	private Map<?,?> getChangeActionDetails(Context context, String strCAId, StringList slObjSelects) throws FrameworkException{
		if(!multipleCAInfoMap.containsKey(strCAId)) {
			DomainObject domObjCA = DomainObject.newInstance(context, strCAId);
			multipleCAInfoMap.put(strCAId, domObjCA.getInfo(context, slObjSelects));
		}
		return multipleCAInfoMap.get(strCAId);
	}
	
}
