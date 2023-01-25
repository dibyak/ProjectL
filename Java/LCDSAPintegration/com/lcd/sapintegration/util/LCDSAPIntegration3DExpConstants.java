
package com.lcd.sapintegration.util;

import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.util.PropertyUtil;

import matrix.db.Context;

public class LCDSAPIntegration3DExpConstants {
	private Context context;
	public final String ATTRIBUTE_PLMENTITY_V_DESCRIPTION = PropertyUtil.getSchemaProperty(context, "attribute_PLMEntity.V_description");
	public final String ATTRIBUTE_PLMENTITY_V_NAME = PropertyUtil.getSchemaProperty(context, "attribute_PLMEntity.V_Name");
	public final String NAME_LCD_BOM_ANCHOR_OBJECT = "LCD_AnchorObject";
	public final String REVISION_LCD_BOM_ANCHOR_OBJECT = "A";
	public final String SELECT_ATTRIBUTE_ACTUAL_COMPLETION_DATE = DomainObject.getAttributeSelect(DomainConstants.ATTRIBUTE_ACTUAL_COMPLETION_DATE);
	public final String SELECT_ATTRIBUTE_PLMENTITY_V_DESCRIPTION = DomainObject.getAttributeSelect(ATTRIBUTE_PLMENTITY_V_DESCRIPTION);
	public final String SELECT_ATTRIBUTE_PLMENTITY_V_NAME = DomainObject.getAttributeSelect(ATTRIBUTE_PLMENTITY_V_NAME);
	public final String TYPE_LCD_BOM_ANCHOR_OBJECT = PropertyUtil.getSchemaProperty(context, "type_LCD_BOMAnchorObject");
	public final String VAULT_ESERVICE_PRODUCTION = PropertyUtil.getSchemaProperty(context, "vault_eServiceProduction");
	public final String ATTRIBUTE_LCD_PROCESS_STATUS_FLAG = PropertyUtil.getSchemaProperty(context, "attribute_LCD_ProcessStatusFlag");
	public final String ATTRIBUTE_LCD_REASON_FOR_FAILURE = PropertyUtil.getSchemaProperty(context, "attribute_LCD_ReasonforFailure");
	public final String SELECT_ATTRIBUTE_LCD_PROCESS_STATUS_FLAG = DomainObject.getAttributeSelect(ATTRIBUTE_LCD_PROCESS_STATUS_FLAG);
	public final String SELECT_ATTRIBUTE_LCD_REASON_FOR_FAILURE = DomainObject.getAttributeSelect(ATTRIBUTE_LCD_REASON_FOR_FAILURE);
	public final String ATTRIBUTE_LCD_CAID = PropertyUtil.getSchemaProperty(context, "attribute_LCD_CAID");
	public final String SELECT_ATTRIBUTE_LCD_CAID = DomainObject.getAttributeSelect(ATTRIBUTE_LCD_CAID);
	public final String ATTRIBUTE_LCD_MF_SAPMBOMUpdatedOn = PropertyUtil.getSchemaProperty(context, "attribute_LCDMF_ManufacturingAssembly.LCDMF_SAPMBOMUpdatedOn");
	public final String ATTRIBUTE_LCD_MF_SAP_UNIQUEID = PropertyUtil.getSchemaProperty(context, "attribute_LCDMF_ManufacturingAssembly.LCDMF_SAPUniqueID");
	public final String SELECT_ATTRIBUTE_LCD_MF_SAP_UNIQUEID = DomainObject.getAttributeSelect(ATTRIBUTE_LCD_MF_SAP_UNIQUEID);
	public final String SELECT_ATTRIBUTE_LCD_MF_SAPMBOMUpdatedOn = DomainObject.getAttributeSelect(ATTRIBUTE_LCD_MF_SAPMBOMUpdatedOn);
	public final String RELATIONSHIP_LCD_SAP_BOM_INTERFACE = PropertyUtil.getSchemaProperty(context, "relationship_LCD_SAPBOMInterface");
	public final String RELATIONSHIP_DELFMI_FUNCTION_IDENTIFIED_INSTANCE = "DELFmiFunctionIdentifiedInstance";
	public final String RELATIONSHIP_PROCESS_INSTANCE_CONTINUOUS = "ProcessInstanceContinuous";
	public final String ATTRIBUTE_PLM_EXTERNALID = PropertyUtil.getSchemaProperty(context, "attribute_PLMEntity.PLM_ExternalID");
	public final String SELECT_ATTRIBUTE_PLM_EXTERNALID = DomainObject.getAttributeSelect(ATTRIBUTE_PLM_EXTERNALID);
	public final String ATTRIBUTE_CHANGE_ACTION_TITLE = "attribute_Synopsis";
	public final String ATTRIBUTE_CHANGE_ACTION_CHANGETYPE = "attribute_LCD_3ChangeType";
	public final String ATTRIBUTE_CHANGE_ACTION_CHANGEDOMAIN = "attribute_LCD_1ChangeDomain";
	public final String ATTRIBUTE_CHANGE_ACTION_PLATFORM = "attribute_LCD_2Platform";
	public final String ATTRIBUTE_CHANGE_ACTION_REASONFORCHANGE = "attribute_LCD_4ReasonForChange";
	public final String ATTRIBUTE_CHANGE_ACTION_CATEGORY_OF_CHANGE = "attribute_CategoryofChange";
	public final String ATTRIBUTE_CHANGE_ACTION_RELEASED_DATE = "attribute_ActualCompletionDate";
	public final String ATTRIBUTE_PROPOSED_APPLICABILITY_START_DATE = "attribute_LCD_5ProposedApplicabilityStartDate";
	public final String ATTRIBUTE_PROPOSED_APPLICABILITY_END_DATE = "attribute_LCD_6ProposedApplicabilityEndDate";
	public final String ATTRIBUTE_PROCUREMENT_INTENT_VPMREFERENCE = PropertyUtil.getSchemaProperty(context, "attribute_XP_VPMReference_Ext.AtievaProcurementIntent");
	public final String ATTRIBUTE_SERVICEABL_EITEM_VPMREFERENCE = PropertyUtil.getSchemaProperty(context, "attribute_XP_VPMReference_Ext.AtievaSpareProduct");
	public final String ATTRIBUTE_PART_INTERCHANGE_ABILITY_VPMREFERENCE = PropertyUtil.getSchemaProperty(context, "attribute_XP_VPMReference_Ext.LCD_PartInterchangeability");
	public final String ATTRIBUTE_HAS_CONFIG_CONTEXT_VPMREFERENCE = PropertyUtil.getSchemaProperty(context, "attribute_PLMReference.V_hasConfigContext");
	public final String ATTRIBUTE_UNIT_OF_MEASURE_VPMREFERENCE = PropertyUtil.getSchemaProperty(context, "attribute_XP_VPMReference_Ext.AtievaUnitofMeasure");
	public final String ATTRIBUTE_TREE_ORDER_PLMINSTANCE = PropertyUtil.getSchemaProperty(context, "attribute_PLMInstance.V_TreeOrder");
	public final String SELECT_ATTRIBUTE_PROCUREMENTINTENT_VPMREFERENCE = DomainObject.getAttributeSelect(ATTRIBUTE_PROCUREMENT_INTENT_VPMREFERENCE);
	public final String SELECT_ATTRIBUTE_SERVICEABLEITEM_VPMREFERENCE = DomainObject.getAttributeSelect(ATTRIBUTE_SERVICEABL_EITEM_VPMREFERENCE);
	public final String SELECT_ATTRIBUTE_PARTINTERCHANGEABILITY_VPMREFERENCE = DomainObject.getAttributeSelect(ATTRIBUTE_PART_INTERCHANGE_ABILITY_VPMREFERENCE);
	public final String SELECT_ATTRIBUTE_HASCONFIGCONTEXT_VPMREFERENCE = DomainObject.getAttributeSelect(ATTRIBUTE_HAS_CONFIG_CONTEXT_VPMREFERENCE);
	public final String SELECT_ATTRIBUTE_UNITOFMEASURE_VPMREFERENCE = DomainObject.getAttributeSelect(ATTRIBUTE_UNIT_OF_MEASURE_VPMREFERENCE);
	public final String SELECT_ATTRIBUTE_TREEORDER_PLMINSTANCE = DomainObject.getAttributeSelect(ATTRIBUTE_TREE_ORDER_PLMINSTANCE);
	public final String ATTRIBUTE_HAS_CONFIG_EFFECTIVITY = PropertyUtil.getSchemaProperty(context, "attribute_PLMInstance.V_hasConfigEffectivity");
	public final String ATTRIBUTE_PROCUREMENT_INTENT_MANUFACTURING_ASSEMBLY = PropertyUtil.getSchemaProperty(context, "attribute_LCDMF_ManufacturingAssembly.LCDMF_ProcurementIntent");
	public final String ATTRIBUTE_PLANT_CODE_MANUFACTURING_ASSEMBLY = PropertyUtil.getSchemaProperty(context, "attribute_XP_CreateAssembly_Ext.LCDMF_PlantCode");
	public final String ATTRIBUTE_SERVICEABLE_ITEM_MANUFACTURING_ASSEMBLY = PropertyUtil.getSchemaProperty(context, "attribute_XP_CreateAssembly_Ext.LCD_ServiceableItem");
	public final String ATTRIBUTE_PART_INTERCHANGE_ABILITY_MANUFACTURING_ASSEMBLY = PropertyUtil.getSchemaProperty(context, "attribute_XP_CreateAssembly_Ext.LCD_PartInterchangeability");
	public final String ATTRIBUTE_HAS_CONFIG_CONTEXT_MANUFACTURING_ASSEMBLY = PropertyUtil.getSchemaProperty(context, "attribute_PLMReference.V_hasConfigContext");
	public final String ATTRIBUTE_SAP_MBOM_UPDATED_ON_MANUFACTURING_ASSEMBLY = PropertyUtil.getSchemaProperty(context, "attribute_LCDMF_ManufacturingAssembly.LCDMF_SAPMBOMUpdatedOn");
	public final String ATTRIBUTE_SAP_UNIQUE_ID_MANUFACTURING_ASSEMBLY = PropertyUtil.getSchemaProperty(context, "attribute_LCDMF_ManufacturingAssembly.LCDMF_SAPUniqueID");
	public final String ATTRIBUTE_SAP_BOM_UPDATED_ON_MANUFACTURING_ASSEMBLY = PropertyUtil.getSchemaProperty(context, "attribute_XP_CreateAssembly_Ext.LCD_SAPBOMUpdateOn");
	public final String SELECT_ATTRIBUTE_HAS_CONFIG_EFFECTIVITY = DomainObject.getAttributeSelect(ATTRIBUTE_HAS_CONFIG_EFFECTIVITY);
	public final String SELECT_ATTRIBUTE_PROCUREMENT_INTENT_MANUFACTURING_ASSEMBLY = DomainObject.getAttributeSelect(ATTRIBUTE_PROCUREMENT_INTENT_MANUFACTURING_ASSEMBLY);
	public final String SELECT_ATTRIBUTE_PLANT_CODE_MANUFACTURING_ASSEMBLY = DomainObject.getAttributeSelect(ATTRIBUTE_PLANT_CODE_MANUFACTURING_ASSEMBLY);
	public final String SELECT_ATTRIBUTE_SERVICEABLE_ITEM_MANUFACTURING_ASSEMBLY = DomainObject.getAttributeSelect(ATTRIBUTE_SERVICEABLE_ITEM_MANUFACTURING_ASSEMBLY);
	public final String SELECT_ATTRIBUTE_PART_INTERCHANGE_ABILITY_MANUFACTURING_ASSEMBLY = DomainObject.getAttributeSelect(ATTRIBUTE_PART_INTERCHANGE_ABILITY_MANUFACTURING_ASSEMBLY);
	public final String SELECT_ATTRIBUTE_HAS_CONFIG_CONTEXT_MANUFACTURING_ASSEMBLY = DomainObject.getAttributeSelect(ATTRIBUTE_HAS_CONFIG_CONTEXT_MANUFACTURING_ASSEMBLY);
	public final String SELECT_ATTRIBUTE_SAP_MBOM_UPDATED_ON_MANUFACTURING_ASSEMBLY = DomainObject.getAttributeSelect(ATTRIBUTE_SAP_MBOM_UPDATED_ON_MANUFACTURING_ASSEMBLY);
	public final String SELECT_ATTRIBUTE_SAP_UNIQUE_ID_MANUFACTURING_ASSEMBLY = DomainObject.getAttributeSelect(ATTRIBUTE_SAP_UNIQUE_ID_MANUFACTURING_ASSEMBLY);
	public final String SELECT_ATTRIBUTE_SAP_BOM_UPDATED_ON_MANUFACTURING_ASSEMBLY = DomainObject.getAttributeSelect(ATTRIBUTE_SAP_BOM_UPDATED_ON_MANUFACTURING_ASSEMBLY);
	public final String ATTRIBUTE_SAP_INSATNCE_UPDATED_ON = PropertyUtil.getSchemaProperty(context, "attribute_LCD_MBOMInstance.LCD_SAPMBOMUpdatedOn");
	public final String ATTRIBUTE_SAP_CAD_INSATNCE_UPDATED_ON = PropertyUtil.getSchemaProperty(context, "attribute_LCD_CADInstanceExt.LCD_SAPBOMUpdatedOn");
	public final String TYPE_PLM_ENTITY = PropertyUtil.getSchemaProperty(context, "type_PLMEntity");
	public final String TYPE_PLM_REFERENCE = PropertyUtil.getSchemaProperty(context, "type_PLMReference");
	public final String TYPE_PLM_CORE_REFERENCE = PropertyUtil.getSchemaProperty(context, "type_PLMCoreReference");
	public final String TYPE_PLM_CORE_REP_REFERENCE = PropertyUtil.getSchemaProperty(context, "type_PLMCoreRepReference");
	public final String TYPE_LP_ABSTRACT_REP_REF = PropertyUtil.getSchemaProperty(context, "type_LPAbstractRepReference");
	public final String TYPE_VPM_INSTANCE = PropertyUtil.getSchemaProperty(context, "type_VPMInstance");
	public final String TYPE_VPM_REFERENCE = PropertyUtil.getSchemaProperty(context, "type_VPMReference");
	public final String TYPE_VPM_REPREFERENCE = PropertyUtil.getSchemaProperty(context, "type_VPMRepReference");
	public final String TYPE_3DSHAPE = PropertyUtil.getSchemaProperty(context, "type_3DShape");
	public final String TYPE_VPM_REP_INSTANCE = PropertyUtil.getSchemaProperty(context, "type_VPMRepInstance");
	public final String TYPE_XCAD_ASSEMBLY_REP_REFERENCE = PropertyUtil.getSchemaProperty(context, "type_XCADAssemblyRepReference");
	public final String TYPE_MANUFACTURING_ASSEMBLY = PropertyUtil.getSchemaProperty(context, "type_CreateAssembly");
	public final String TYPE_PROVIDE = PropertyUtil.getSchemaProperty(context, "type_Provide");
	public final String TYPE_PROCESS_INSTANCE_CONTINUOUS = PropertyUtil.getSchemaProperty(context, "type_ProcessContinuousProvide");
	public final String TYPE_FASTEN = PropertyUtil.getSchemaProperty(context, "type_Fasten");
	public final String EFFECTIVITY_CURRENT_EVOLUTION = "Effectivity_Current_Evolution";
	public final String EFFECTIVITY_PROJECTED_EVOLUTION = "Effectivity_Projected_Evolution";
	public final String EFFECTIVITY_VARIANT = "Effectivity_Variant";
	public final String EFFECTIVITY_FROM_DATE = "Effectivity_From_Date";
	public final String EFFECTIVITY_TO_DATE = "Effectivity_To_Date";
	public final String ROLE_ADMIN = "admin_platform";
	public final String LCD_3DX_SAP_INTEGRATION_KEY = "LCD_3DXSAPStringResource_en";
	
	public LCDSAPIntegration3DExpConstants(Context context) {
		this.context = context;
	}
}
