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
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.DomainRelationship;
import com.matrixone.apps.domain.util.PropertyUtil;

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
	private static final String ATTR_PROCUREMENT_INTENT_MFG_ASMBLY = "attribute[LCDMF_ManufacturingAssembly.LCDMF_ProcurementIntent]";
	private static final String ATTR_PROCUREMENT_INTENT_VPMREFERENCE = "attribute[XP_VPMReference_Ext.AtievaProcurementIntent]";
	private static final String ATTR_HAS_CONFIG_CONTEXT_VPMREFERENCE = "attribute[PLMReference.V_hasConfigContext]";
	private static final String ATTR_LCD_PROCESS_STATUS_FLAG = "LCD_ProcessStatusFlag";
	private static final String ATTR_LCD_CAID = "LCD_CAID";

	private static final String TYPE_MANUFACTURINGASSEMBLY = "CreateAssembly";
	private static final String TYPE_VPMREFERENCE = "VPMReference";
	private static final String TYPE_ELECTRICALBRANCHGEOMETRY = "ElectricalBranchGeometry";
	private static final String TYPE_ELECTRICALGEOMETRY = "ElectricalGeometry";
	private static final String TYPE_PIPING_RIGID_PIPE = "Piping_Rigid_Pipe";
	private static final String TYPE_SPOTFASTENERSINGLE = "SpotFastenerSingle";
	private static final String TYPE_LCD_BOM_ANCHOR_OBJECT = "LCD_BOMAnchorObject";

	private static final String DESCRETE = "Make";
	private static final String SUBCONTRACT = "Sub-Contract";
	private static final String FALSE = "FALSE";
	private static final String VALUE_STATUS_WAITING = "Waiting";

	private static final String NAME_LCD_ANCHOR_OBJECT = "LCD_AnchorObject";
	private static final String REV_LCD_ANCHOR_OBJECT = "A";
	private static final String VAULT_ESERVICE_PRODUCTION = "eService Production";
	private static final String REL_LCD_SAP_BOM_INTERFACE = "LCD_SAPBOMInterface";

	private static final String KEY_CA = "changeaction";
	private static final String KEY_REALIZED = "realized";
	private static final String KEY_CA_WHERE = "where";
	private static final String KEY_CA_INFO = "info";
	private static final String KEY_CA_ID = "id";
	private static final String KEY_CA_TYPE = "type";
	private static final String POLICY_VPLM_SMB_DEFINITION_STATE_RELEASED = "RELEASED";
	

	/**
	 * Method to connect Parts to Anchor object. Invoked from CA-Promote Action
	 * trigger when promoted to Release state
	 * 
	 * @param context The enovia context object
	 * @param args    Map of arguments packed in enovia-format of string array. *
	 * @throws Exception when operation fails
	 */
	public void connectManAssemblyToAnchorObject(Context context, String[] args) throws Exception {

		String strCAId = args[0];

		ChangeAction changeActionObj = ChangeActionServices.getChangeAction(context, strCAId);

		BusinessObject busObjAchor = new BusinessObject(TYPE_LCD_BOM_ANCHOR_OBJECT, // String Type
				NAME_LCD_ANCHOR_OBJECT, // String Name
				REV_LCD_ANCHOR_OBJECT, // String Revision
				VAULT_ESERVICE_PRODUCTION); // String Vault
		
		if(busObjAchor.exists(context)) {

		DomainObject domObjAnchor = DomainObject.newInstance(context, busObjAchor);

		ChangeActionFacets changeActionFacetsSelectables = new ChangeActionFacets();
		changeActionFacetsSelectables.attributes = true;
		changeActionFacetsSelectables.realized = true;

		String strCAJsonDetails = ChangeActionJsonUtilities.changeActionToJson(context, changeActionObj,
				changeActionFacetsSelectables);

		try (JsonReader jsonReadCADetails = Json.createReader(new StringReader(strCAJsonDetails));) {
			JsonObject jsonObjChangeAction = jsonReadCADetails.readObject();
			JsonArray jsonArrRealizedItems = jsonObjChangeAction.getJsonObject(KEY_CA).getJsonArray(KEY_REALIZED);

			if (!jsonArrRealizedItems.isEmpty()) {
				for (int i = 0; i < jsonArrRealizedItems.size(); i++) {
					JsonObject jsonObjRealizedItem = jsonArrRealizedItems.getJsonObject(i);
					String strRealizedItemType = jsonObjRealizedItem.getJsonObject(KEY_CA_WHERE)
							.getJsonObject(KEY_CA_INFO).getString(KEY_CA_TYPE);
					String realizedItemId = jsonObjRealizedItem.getJsonObject(KEY_CA_WHERE).get(KEY_CA_ID).toString()
							.split(":")[1].replace("\"", "");

					DomainObject domObjRealizedItem = DomainObject.newInstance(context, realizedItemId);
					StringList slObjectSelects = new StringList();
					slObjectSelects.add(DomainConstants.SELECT_ID);
					slObjectSelects.add(ATTR_PROCUREMENT_INTENT_MFG_ASMBLY);
					slObjectSelects.add(ATTR_HAS_CONFIG_CONTEXT_VPMREFERENCE);

					Map<?, ?> realizedItemMap = domObjRealizedItem.getInfo(context, slObjectSelects);
					String strRealizedID = (String) realizedItemMap.get(DomainConstants.SELECT_ID);

					if (TYPE_MANUFACTURINGASSEMBLY.equalsIgnoreCase(strRealizedItemType)) {
						DomainRelationship relEbom = domObjAnchor.addRelatedObject(context,
								new RelationshipType(REL_LCD_SAP_BOM_INTERFACE), false, strRealizedID);
						relEbom.setAttributeValue(context, ATTR_LCD_PROCESS_STATUS_FLAG, VALUE_STATUS_WAITING);
						relEbom.setAttributeValue(context, ATTR_LCD_CAID, strCAId);
					}

				}
			}
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

		String strObjectId = args[0];
		String strNextState = args[1];

		if(strNextState.equalsIgnoreCase(POLICY_VPLM_SMB_DEFINITION_STATE_RELEASED)) {
			DomainObject domObjCadPart = DomainObject.newInstance(context, strObjectId);

			BusinessObject busObjAchor = new BusinessObject(TYPE_LCD_BOM_ANCHOR_OBJECT, // String Type
					NAME_LCD_ANCHOR_OBJECT, // String Name
					REV_LCD_ANCHOR_OBJECT, // String Revision
					VAULT_ESERVICE_PRODUCTION); // String Vault
			
			if(busObjAchor.exists(context)) {

			DomainObject domObjAnchor = DomainObject.newInstance(context, busObjAchor);

			StringList slObjectSelects = new StringList();
			slObjectSelects.add(DomainConstants.SELECT_ID);
			slObjectSelects.add(DomainConstants.SELECT_TYPE);
			slObjectSelects.add(ATTR_PROCUREMENT_INTENT_VPMREFERENCE);

			Map<?, ?> realizedItemMap = domObjCadPart.getInfo(context, slObjectSelects);
			String strCadPartId = (String) realizedItemMap.get(DomainConstants.SELECT_ID);
			String strCadPartType = (String) realizedItemMap.get(DomainConstants.SELECT_TYPE);
			String strObjProcIntent = (String) realizedItemMap.get(ATTR_PROCUREMENT_INTENT_VPMREFERENCE);

			if (SUBCONTRACT.equalsIgnoreCase(strObjProcIntent) && (TYPE_VPMREFERENCE.equalsIgnoreCase(strCadPartType)
					|| TYPE_ELECTRICALBRANCHGEOMETRY.equalsIgnoreCase(strCadPartType)
					|| TYPE_ELECTRICALGEOMETRY.equalsIgnoreCase(strCadPartType)
					|| TYPE_PIPING_RIGID_PIPE.equalsIgnoreCase(strCadPartType)
					|| TYPE_SPOTFASTENERSINGLE.equalsIgnoreCase(strCadPartType))) {

				DomainRelationship relObjAnchor = domObjAnchor.addRelatedObject(context,
						new RelationshipType(REL_LCD_SAP_BOM_INTERFACE), false, strCadPartId);
				relObjAnchor.setAttributeValue(context, ATTR_LCD_PROCESS_STATUS_FLAG, VALUE_STATUS_WAITING);

			}
		}
		}
	}
}
