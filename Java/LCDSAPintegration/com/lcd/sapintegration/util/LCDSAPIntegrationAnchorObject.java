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
import com.matrixone.apps.framework.ui.UIUtil;

import matrix.db.BusinessObject;
import matrix.db.Context;
import matrix.util.StringList;

public class LCDSAPIntegrationAnchorObject {
	private final Map<String, Map<?,?>> multipleCAInfoMap = new HashMap<>();
	
	/**
	 * @param context
	 * @return
	 * @throws Exception 
	 */
	public static String getConnectedAssemblyDetailsAsJsonString (Context context) throws Exception {
		LCDSAPIntegration3DExpConstants lcdSAPInteg3DExpConstants = new LCDSAPIntegration3DExpConstants(context);
		BusinessObject busObjAchor = new BusinessObject(lcdSAPInteg3DExpConstants.TYPE_LCD_BOM_ANCHOR_OBJECT, lcdSAPInteg3DExpConstants.NAME_LCD_BOM_ANCHOR_OBJECT,
				lcdSAPInteg3DExpConstants.REVISION_LCD_BOM_ANCHOR_OBJECT, lcdSAPInteg3DExpConstants.VAULT_ESERVICE_PRODUCTION);
		DomainObject domObjSAPAnchorObj = DomainObject.newInstance(context, busObjAchor);
		StringList slObjectSelect = new StringList();
		slObjectSelect.add(DomainConstants.SELECT_ID);
		slObjectSelect.add(DomainConstants.SELECT_NAME);
		slObjectSelect.add(DomainConstants.SELECT_TYPE);
		slObjectSelect.add(DomainConstants.SELECT_REVISION);
		slObjectSelect.add(lcdSAPInteg3DExpConstants.SELECT_ATTRIBUTE_PLMENTITY_V_NAME);
		slObjectSelect.add(lcdSAPInteg3DExpConstants.SELECT_ATTRIBUTE_PLMENTITY_V_DESCRIPTION);
		slObjectSelect.add(DomainConstants.SELECT_CURRENT);
		slObjectSelect.add(lcdSAPInteg3DExpConstants.SELECT_ATTRIBUTE_LCD_MF_SAPMBOMUpdatedOn);
		slObjectSelect.add(lcdSAPInteg3DExpConstants.SELECT_ATTRIBUTE_LCD_VPMREFERENCE_SAP_MBOM_UPDATED_ON);
		StringList slRelSelect = new StringList();
		slRelSelect.add(DomainRelationship.SELECT_ID);
		MapList mlAssmblyConnectedToAnchorObject = 
				domObjSAPAnchorObj.getRelatedObjects(
						context, // context
						lcdSAPInteg3DExpConstants.RELATIONSHIP_LCD_SAP_BOM_INTERFACE, // Relationship String
						DomainConstants.QUERY_WILDCARD, // Type String
						slObjectSelect, // Object Select StringList
						slRelSelect, //// Relationship Select StringList
						false, // To
						true, // from
						(short) 1, // Recursion Level
						"", // Object Where clause String
						"", // Relationship Where clause String
						0); // limit
		
		return new LCDSAPIntegrationAnchorObject().createAssmblyDataJsonString(context, mlAssmblyConnectedToAnchorObject, lcdSAPInteg3DExpConstants);
	}
	
	/**
	 * @param context
	 * @param mlAssmblyConnectedToAnchorObject
	 * @param lcdSAPInteg3DExpConstants
	 * @return
	 * @throws FrameworkException
	 */
	private String createAssmblyDataJsonString(Context context, MapList mlAssmblyConnectedToAnchorObject, LCDSAPIntegration3DExpConstants lcdSAPInteg3DExpConstants)
			throws FrameworkException {
//		Map<?,?> changeActionAttrDetailsMap= new HashMap<>();
		Iterator<?> iterMLAssmblyConnectedToAnchorObject = mlAssmblyConnectedToAnchorObject.iterator();
		JsonArrayBuilder jabMAs = Json.createArrayBuilder();
		StringList slCAobjectSelects = new StringList();
		slCAobjectSelects.add(DomainConstants.SELECT_NAME);
		slCAobjectSelects.add(lcdSAPInteg3DExpConstants.SELECT_ATTRIBUTE_ACTUAL_COMPLETION_DATE);
		while (iterMLAssmblyConnectedToAnchorObject.hasNext()) {
			JsonObjectBuilder jobMA = Json.createObjectBuilder();
			Map<?, ?> tempAssmblyDataMap = (Map<?, ?>) iterMLAssmblyConnectedToAnchorObject.next();
			String strLCDSAPInterfaceConnId = (String) tempAssmblyDataMap.get(DomainRelationship.SELECT_ID);
			Map<?,?> tempLCDSAPInterfaceConnDataMap = DomainRelationship.newInstance(context, strLCDSAPInterfaceConnId).getAttributeMap(context);
			String strCAID = (String) tempLCDSAPInterfaceConnDataMap.get(lcdSAPInteg3DExpConstants.ATTRIBUTE_LCD_CAID);
			String strBomType = (String) tempAssmblyDataMap.get(DomainConstants.SELECT_TYPE);
			if(UIUtil.isNotNullAndNotEmpty(strCAID)) {
			Map<?, ?> changeActionAttrDetailsMap = getChangeActionDetails(context, strCAID, slCAobjectSelects);
			jobMA.add(LCDSAPIntegrationDataConstants.CA_COMPLETED_TIME, (String) changeActionAttrDetailsMap.get(lcdSAPInteg3DExpConstants.SELECT_ATTRIBUTE_ACTUAL_COMPLETION_DATE));
			jobMA.add(LCDSAPIntegrationDataConstants.CA_NAME, (String) changeActionAttrDetailsMap.get(DomainConstants.SELECT_NAME));
			} else {
				jobMA.add(LCDSAPIntegrationDataConstants.CA_COMPLETED_TIME, "");
				jobMA.add(LCDSAPIntegrationDataConstants.CA_NAME, "");
			}
			jobMA.add(LCDSAPIntegrationDataConstants.PROPERTY_CONNECTION_ID, strLCDSAPInterfaceConnId);
			jobMA.add(LCDSAPIntegrationDataConstants.PROPERTY_CA_ID, strCAID);
			jobMA.add(LCDSAPIntegrationDataConstants.PROPERTY_BOM_COMPONENT_ID, (String) tempAssmblyDataMap.get(DomainConstants.SELECT_ID));
			jobMA.add(LCDSAPIntegrationDataConstants.PROPERTY_BOM_COMPONENT_NAME, (String) tempAssmblyDataMap.get(DomainConstants.SELECT_NAME));
			jobMA.add(LCDSAPIntegrationDataConstants.PROPERTY_STATUS, (String)tempLCDSAPInterfaceConnDataMap.get(lcdSAPInteg3DExpConstants.ATTRIBUTE_LCD_PROCESS_STATUS_FLAG));
			jobMA.add(LCDSAPIntegrationDataConstants.PROPERTY_REVISION, (String) tempAssmblyDataMap.get(DomainConstants.SELECT_REVISION));
			jobMA.add(LCDSAPIntegrationDataConstants.PROPERTY_TITLE, (String) tempAssmblyDataMap.get(lcdSAPInteg3DExpConstants.SELECT_ATTRIBUTE_PLMENTITY_V_NAME));
			jobMA.add(LCDSAPIntegrationDataConstants.PROPERTY_MATURITY, (String) tempAssmblyDataMap.get(DomainConstants.SELECT_CURRENT));
			jobMA.add(LCDSAPIntegrationDataConstants.DESCRIPTION, (String) tempAssmblyDataMap.get(lcdSAPInteg3DExpConstants.SELECT_ATTRIBUTE_PLMENTITY_V_DESCRIPTION));
			

			if(strBomType.equalsIgnoreCase(lcdSAPInteg3DExpConstants.TYPE_MANUFACTURING_ASSEMBLY)) {
				jobMA.add(LCDSAPIntegrationDataConstants.SAP_FEEDBACK_TIMESTAMP, (String) tempAssmblyDataMap.get(lcdSAPInteg3DExpConstants.SELECT_ATTRIBUTE_LCD_MF_SAPMBOMUpdatedOn));
			} else {
				jobMA.add(LCDSAPIntegrationDataConstants.SAP_FEEDBACK_TIMESTAMP, (String) tempAssmblyDataMap.get(lcdSAPInteg3DExpConstants.SELECT_ATTRIBUTE_LCD_VPMREFERENCE_SAP_MBOM_UPDATED_ON));
			}
			
			jobMA.add(LCDSAPIntegrationDataConstants.SAP_FEEDBACK_MESSAGE, (String) tempLCDSAPInterfaceConnDataMap.get(lcdSAPInteg3DExpConstants.ATTRIBUTE_LCD_SAP_FEEDBACK_MESSAGE));
			jabMAs.add(jobMA);
		}
		return jabMAs.build().toString();
	}
	
	/**
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
