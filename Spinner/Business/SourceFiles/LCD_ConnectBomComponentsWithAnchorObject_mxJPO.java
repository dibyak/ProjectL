import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;

import com.dassault_systemes.enovia.changeaction.impl.ChangeAction;
import com.dassault_systemes.enovia.changeaction.servicesimpl.ChangeActionServices;
import com.dassault_systemes.enovia.changeaction.webservice.services.ChangeActionJsonUtilities;
import com.dassault_systemes.enovia.changeaction.webservice.services.ChangeActionJsonUtilities.ChangeActionFacets;
import com.lcd.sapintegration.util.LCDSAPIntegration3DExpConstants;
import com.lcd.sapintegration.util.LCDSAPIntegrationDataConstants;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.DomainRelationship;
import com.matrixone.apps.domain.util.PropertyUtil;
import com.matrixone.apps.framework.ui.UIUtil;
import com.matrixone.apps.domain.util.ContextUtil;

import matrix.db.BusinessObject;
import matrix.db.Context;
import matrix.db.JPO;
import matrix.db.RelationshipType;
import matrix.util.StringList;

/**
 * 
 * @author
 *
 */
public class LCD_ConnectBomComponentsWithAnchorObject_mxJPO {	

	/**
	 * Method to connect Parts to Anchor object. Invoked from CA-Promote Action
	 * trigger when promoted to Release state
	 * 
	 * @param context The enovia context object
	 * @param args    Map of arguments packed in enovia-format of string array. *
	 * @throws Exception when operation fails
	 */
	public void connectManAssemblyToAnchorObject(Context context, String[] args) throws Exception {
		
		LCDSAPIntegration3DExpConstants lcdSAPInteg3DExpConstants = new LCDSAPIntegration3DExpConstants(context);
		boolean bIsContextPushed = false;
		try {
			
		ContextUtil.pushContext(context);
		bIsContextPushed = true;
		String strCAId = args[0];
		
		for(int i =0;i< args.length ; i++) {
			System.out.println("connectManAssemblyToAnchorObject-----ARG "+i+" - "+args[i]);
		}


		BusinessObject busObjAchor = new BusinessObject(lcdSAPInteg3DExpConstants.TYPE_LCD_BOM_ANCHOR_OBJECT, // String Type
				lcdSAPInteg3DExpConstants.NAME_LCD_BOM_ANCHOR_OBJECT, // String Name
				lcdSAPInteg3DExpConstants.REVISION_LCD_BOM_ANCHOR_OBJECT, // String Revision
				lcdSAPInteg3DExpConstants.VAULT_ESERVICE_PRODUCTION); // String Vault
		
		if(busObjAchor.exists(context)) {

		DomainObject domObjAnchor = DomainObject.newInstance(context, busObjAchor.getObjectId(context));
		
		ChangeAction changeActionObj = ChangeActionServices.getChangeAction(context, strCAId);

		ChangeActionFacets changeActionFacetsSelectables = new ChangeActionFacets();
		changeActionFacetsSelectables.attributes = true;
		changeActionFacetsSelectables.realized = true;

		String strCAJsonDetails = ChangeActionJsonUtilities.changeActionToJson(context, changeActionObj,
				changeActionFacetsSelectables);

			JsonReader jsonReadCADetails = Json.createReader(new StringReader(strCAJsonDetails));
			JsonObject jsonObjChangeAction = jsonReadCADetails.readObject();
			JsonArray jsonArrRealizedItems = jsonObjChangeAction.getJsonObject(LCDSAPIntegrationDataConstants.KEY_CA).getJsonArray(LCDSAPIntegrationDataConstants.KEY_REALIZED);
			jsonReadCADetails.close();

			if (jsonArrRealizedItems != null) {
				for (int i = 0; i < jsonArrRealizedItems.size(); i++) {
					JsonObject jsonObjRealizedItem = jsonArrRealizedItems.getJsonObject(i);
					String strRealizedItemType = jsonObjRealizedItem.getJsonObject(LCDSAPIntegrationDataConstants.KEY_CA_WHERE)
							.getJsonObject(LCDSAPIntegrationDataConstants.KEY_CA_INFO).getString(LCDSAPIntegrationDataConstants.KEY_CA_TYPE);
					String realizedItemId = jsonObjRealizedItem.getJsonObject(LCDSAPIntegrationDataConstants.KEY_CA_WHERE).get(LCDSAPIntegrationDataConstants.KEY_CA_ID).toString()
							.split(":")[1].replace("\"", "");
					if(UIUtil.isNotNullAndNotEmpty(realizedItemId)) {
						DomainObject domObjRealizedItem = DomainObject.newInstance(context, realizedItemId);
						StringList slObjectSelects = new StringList();
						slObjectSelects.add(DomainConstants.SELECT_ID);

						Map<?, ?> realizedItemMap = domObjRealizedItem.getInfo(context, slObjectSelects);
						String strRealizedID = (String) realizedItemMap.get(DomainConstants.SELECT_ID);

						if (lcdSAPInteg3DExpConstants.TYPE_MANUFACTURING_ASSEMBLY.equalsIgnoreCase(strRealizedItemType)) {
							DomainRelationship relEbom = domObjAnchor.addRelatedObject(context,new RelationshipType(lcdSAPInteg3DExpConstants.RELATIONSHIP_LCD_SAP_BOM_INTERFACE), false, strRealizedID);
							relEbom.setAttributeValue(context, lcdSAPInteg3DExpConstants.ATTRIBUTE_LCD_PROCESS_STATUS_FLAG, LCDSAPIntegrationDataConstants.VALUE_PROCESS_STATUS_FLAG_Waiting);
							relEbom.setAttributeValue(context, lcdSAPInteg3DExpConstants.ATTRIBUTE_LCD_CAID, strCAId);
						}
					}
				}
			}
		}
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		} finally {
			if(bIsContextPushed) {
				ContextUtil.popContext(context);
			}
		}
		}

	/**
	 * Method to connect CAD Part to Anchor object. Invoked from CAD Part-Promote
	 * Action trigger when promoted to Release state
	 * 
	 * @param context The enovia context object
	 * @param args    Map of arguments packed in enovia-format of string array. *
	 * @throws Exception when operation fails
	 */
	public void connectPhysicalProductToAnchorObject(Context context, String[] args) throws Exception {
		System.out.println("connectPhysicalProductToAnchorObject-----START");
		LCDSAPIntegration3DExpConstants lcdSAPInteg3DExpConstants = new LCDSAPIntegration3DExpConstants(context);
		boolean bIsContextPushed = false;
		try {
		ContextUtil.pushContext(context);
		bIsContextPushed = true;
		
		for(int i =0;i< args.length ; i++) {
			System.out.println("connectPhysicalProductToAnchorObject---ARG "+i+" - "+args[i]);
		}
		String strObjectId = args[0];
		String strNextState = args[1];

		if(LCDSAPIntegrationDataConstants.VALUE_RELEASED.equalsIgnoreCase(strNextState)) {
			DomainObject domObjCadPart = DomainObject.newInstance(context, strObjectId);

			BusinessObject busObjAchor = new BusinessObject(lcdSAPInteg3DExpConstants.TYPE_LCD_BOM_ANCHOR_OBJECT, // String Type
					lcdSAPInteg3DExpConstants.NAME_LCD_BOM_ANCHOR_OBJECT, // String Name
					lcdSAPInteg3DExpConstants.REVISION_LCD_BOM_ANCHOR_OBJECT, // String Revision
					lcdSAPInteg3DExpConstants.VAULT_ESERVICE_PRODUCTION); // String Vault
			
			if(busObjAchor.exists(context)) {

			DomainObject domObjAnchor = DomainObject.newInstance(context, busObjAchor.getObjectId(context));

			StringList slObjectSelects = new StringList();
			slObjectSelects.add(DomainConstants.SELECT_ID);
			slObjectSelects.add(DomainConstants.SELECT_TYPE);
			slObjectSelects.add(lcdSAPInteg3DExpConstants.SELECT_ATTRIBUTE_PROCUREMENTINTENT_VPMREFERENCE);

			Map<?, ?> realizedItemMap = domObjCadPart.getInfo(context, slObjectSelects);
			String strCadPartId = (String) realizedItemMap.get(DomainConstants.SELECT_ID);
			String strCadPartType = (String) realizedItemMap.get(DomainConstants.SELECT_TYPE);
			String strObjProcIntent = (String) realizedItemMap.get(lcdSAPInteg3DExpConstants.SELECT_ATTRIBUTE_PROCUREMENTINTENT_VPMREFERENCE);

			if (LCDSAPIntegrationDataConstants.SUBCONTRACT.equalsIgnoreCase(strObjProcIntent) && (lcdSAPInteg3DExpConstants.TYPE_VPM_REFERENCE.equalsIgnoreCase(strCadPartType)
					|| lcdSAPInteg3DExpConstants.TYPE_ELECTRICALBRANCHGEOMETRY.equalsIgnoreCase(strCadPartType)
					|| lcdSAPInteg3DExpConstants.TYPE_ELECTRICALGEOMETRY.equalsIgnoreCase(strCadPartType)
					|| lcdSAPInteg3DExpConstants.TYPE_PIPING_RIGID_PIPE.equalsIgnoreCase(strCadPartType)
					|| lcdSAPInteg3DExpConstants.TYPE_SPOTFASTENERSINGLE.equalsIgnoreCase(strCadPartType))) {

				DomainRelationship relObjAnchor = domObjAnchor.addRelatedObject(context,
						new RelationshipType(lcdSAPInteg3DExpConstants.RELATIONSHIP_LCD_SAP_BOM_INTERFACE), false, strCadPartId);
				relObjAnchor.setAttributeValue(context, lcdSAPInteg3DExpConstants.ATTRIBUTE_LCD_PROCESS_STATUS_FLAG, LCDSAPIntegrationDataConstants.VALUE_PROCESS_STATUS_FLAG_Waiting);
			}
		}
			
		}
	}catch (Exception e) {
		e.printStackTrace();
		throw e;
	}finally {
		if(bIsContextPushed) {
			ContextUtil.popContext(context);
		}
	}
		}
	
}
