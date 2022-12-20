
/* 
	Author 		: Akash THAKUR
	Version		: 1.0
	Description	: This program is used for Lucid Motors custom constants 
 */

public class LCD_Constants_mxJPO 
{
    public LCD_Constants_mxJPO(){		

	}	

  public static final String ROLE_ADMIN =  "admin_platform";
  public static final String TYPE_PLMENTITY = "PLMEntity";
  public static final String TYPE_PLMREFERENCE = "PLMReference";
  public static final String TYPE_PLMCOREREFERENCE = "PLMCoreReference";
  public static final String TYPE_PLMCOREREPREFERENCE = "PLMCoreRepReference";
  public static final String TYPE_LPABSTRACTREPREF = "LPAbstractRepReference";
  public static final String TYPE_VPMINSTANCE = "VPMInstance";
  public static final String TYPE_VPMREFERENCE = "VPMReference";
  public static final String TYPE_VPMREPREFERENCE = "VPMRepReference";
  public static final String TYPE_3DSHAPE = "3DShape";
  public static final String TYPE_VPMREPINSTANCE = "VPMRepInstance";
  public static final String TYPE_XCADASSEMBLYREPREFERENCE = "XCADAssemblyRepReference";
  public static final String TYPE_MANUFACTURINGASSEMBLY = "CreateAssembly";
  public static final String TYPE_PROVIDE  = "Provide";
  public static final String TYPE_PROCESSINSTANCECONTINUOUS  = "ProcessContinuousProvide";
  public static final String TYPE_FASTEN  = "Fasten";
  
  public static final String REL_PROVIDE  = "DELFmiFunctionIdentifiedInstance";
  public static final String REL_PROCESSINSTANCECONTINUOUS  = "ProcessInstanceContinuous";
 
  public static final String ATTR_V_NAME = "attribute[PLMEntity.V_Name]";
  public static final String ATTR_V_DESCRIPTION = "attribute[PLMEntity.V_description]";
  public static final String ATTR_PLM_EXTERNALID = "attribute[PLMEntity.PLM_ExternalID]";
  
  public static final String ATTR__PROCUREMENTINTENT_MANUFACTURINGASSEMBLY= "attribute[LCDMF_ManufacturingAssembly.LCDMF_ProcurementIntent]";
  public static final String ATTR__PLANTCODE_MANUFACTURINGASSEMBLY= "attribute[XP_CreateAssembly_Ext.LCDMF_PlantCode]";
  public static final String ATTR__SERVICEABLEITEM_MANUFACTURINGASSEMBLY= "attribute[XP_CreateAssembly_Ext.LCD_ServiceableItem]";
  public static final String ATTR__PARTINTERCHANGEABILITY_MANUFACTURINGASSEMBLY= "attribute[XP_CreateAssembly_Ext.LCD_PartInterchangeability]";
  public static final String ATTR__HASCONFIGCONTEXT_MANUFACTURINGASSEMBLY= "attribute[PLMReference.V_hasConfigContext]";
  public static final String ATTR__SAPMBOMUPDATEDON_MANUFACTURINGASSEMBLY= "attribute[LCDMF_ManufacturingAssembly.LCDMF_SAPMBOMUpdatedOn]";
  public static final String ATTR__SAPUNIQUEID_MANUFACTURINGASSEMBLY= "attribute[LCDMF_ManufacturingAssembly.LCDMF_SAPUniqueID]";
  public static final String ATTR__SAPBOMUPDATEDON_MANUFACTURINGASSEMBLY =  "attribute[XP_CreateAssembly_Ext.LCD_SAPBOMUpdateOn]";
  
  
  public static final String ATTR__SAP_UNIQUEID= "LCDMF_ManufacturingAssembly.LCDMF_SAPUniqueID";
  public static final String ATTR__SAPMBOM_UPDATED_ON="LCDMF_ManufacturingAssembly.LCDMF_SAPMBOMUpdatedOn"; 
  public static final String ATTR__SAP_INSATNCE_UPDATED_ON="LCD_MBOMInstance.LCD_SAPMBOMUpdatedOn";
  
  public static final String ATTR__SAP_CAD_INSATNCE_UPDATED_ON="LCD_CADInstanceExt.LCD_SAPBOMUpdatedOn";
  
  public static final String TITLE_CHANGEACTION = "attribute_Synopsis";
  public static final String CHANGETYPE_CHANGEACTION= "attribute_LCD_3ChangeType";
  public static final String CHANGEDOMAIN_CHANGEACTION= "attribute_LCD_1ChangeDomain";
  public static final String PLATFORM_CHANGEACTION= "attribute_LCD_2Platform";
  public static final String REASONFORCHANGE_CHANGEACTION= "attribute_LCD_4ReasonForChange";
  public static final String CATEGORYOFCHANGE_CHANGEACTION= "attribute_CategoryofChange";
  public static final String RELEASEDDATE_CHANGEACTION = "attribute_ActualCompletionDate";

  public static final String ATTR__V_NAME_VPMREFERENCE= "attribute[V_Name]";
  public static final String ATTR__PROCUREMENTINTENT_VPMREFERENCE= "attribute[XP_VPMReference_Ext.AtievaProcurementIntent]";
  public static final String ATTR__SERVICEABLEITEM_VPMREFERENCE = "attribute[XP_VPMReference_Ext.AtievaSpareProduct]";
  public static final String ATTR__PARTINTERCHANGEABILITY_VPMREFERENCE= "attribute[XP_VPMReference_Ext.LCD_PartInterchangeability]";
  public static final String ATTR__HASCONFIGCONTEXT_VPMREFERENCE= "attribute[PLMReference.V_hasConfigContext]";
  public static final String ATTR__UNITOFMEASURE_VPMREFERENCE= "attribute[XP_VPMReference_Ext.AtievaUnitofMeasure]";
  public static final String ATTR__TREEORDER_PLMINSTANCE= "attribute[PLMInstance.V_TreeOrder]";
  
  public static final String DESCRETE= "Make";
  public static final String PHANTOM= "Phantom";
  public static final String SUBCONTRACT = "Sub-Contract";
  public static final String BUYSUBC = "Buy-SubC";
  
  public static final String EACH = "Each";
  
  /* 3DX-SAP Integration JSON header  Information */
  public static final String TAG_TITLE = "Title";
  public static final String TAG_OID = "OID";
  public static final String TAG_NAME = "name";
  public static final String TAG_DESCRIPTION = "Description";
  public static final String TAG_DATEFROM = "DateFrom";
  public static final String TAG_DATETO = "DateTo";
  public static final String TAG_REALIZED_DATA = "Realized_Data";
  public static final String TAG_REL_ID = "Rel_OID";
  public static final String TAG_CATEGORY_OF_CHANGE = "Category_of_Change";
  public static final String TAG_CHANGEDOMAIN = "ChangeDomain";
  public static final String TAG_CHANGETYPE = "ChangeType";
  public static final String TAG_REASONFORCHANGE = "ReasonForChange";
  public static final String TAG_PLATFORM = "Platform";
  
  public static final String TAG_PROCUREMENTINTENT =  "ProcurementIntent";
  public static final String TAG_SERVICEABLEITEM = "ServiceableItem";
  public static final String TAG_PARTINTERCHANGEABILITY = "PartInterchangeability";
  public static final String TAG_UNIT_OF_MEASURE =    "UoM";
  public static final String TAG_VARIAENT_Effectivity =   "Effectivity_Option_Code";
  public static final String TAG_PLANTCODE = "PlantCode";
  public static final String TAG_ERROR_MESSAGE =  "ERROR_MESSAGE";
  public static final String HEADER_CHILDREN = "children";
  public static final String TAG_TYPE = "Type";
  public static final String HEADER_PART =  "HeaderPart";
  public static final String TAG_SAPUNIQUE_ID= "SAPUniqueID";
 
  public static final String FAIL =  "FAILURE";
  public static final String SUCCESS =  "SUCCESS";
  
  public static final String RELEASED = "RELEASED";
  
  public static final String NOT_APPLICABLE ="NA";
  
  public static final String FALSE = "FALSE";
  
  protected static final String EFFECTIVITY_CURRENT_EVOLUTION = "Effectivity_Current_Evolution";
  protected static final String EFFECTIVITY_PROJECTED_EVOLUTION = "Effectivity_Projected_Evolution";
  protected static final String EFFECTIVITY_VARIANT = "Effectivity_Variant";
  protected static final String EFFECTIVITY_FROM_DATE = "Effectivity_From_Date";
  protected static final String EFFECTIVITY_TO_DATE = "Effectivity_To_Date";
  

}