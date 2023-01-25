
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
    
    
    public static final String ATTR__SAP_UNIQUEID= "attribute[LCDMF_ManufacturingAssembly.LCDMF_SAPUniqueID]";
    public static final String ATTR__SAPMBOM_UPDATED_ON="attribute[LCDMF_ManufacturingAssembly.LCDMF_SAPMBOMUpdatedOn]"; 
    public static final String ATTR__SAP_INSATNCE_UPDATED_ON="attribute[LCD_MBOMInstance.LCD_SAPMBOMUpdatedOn]";
    
    public static final String ATTR__SAP_CAD_INSATNCE_UPDATED_ON="attribute[LCD_CADInstanceExt.LCD_SAPBOMUpdatedOn]";
    public static final String ATTR__HASCONFIGEFFECTIVITY = "attribute[PLMInstance.V_hasConfigEffectivity]" ;
    
    public static final String TITLE_CHANGEACTION = "attribute[Synopsis]";
    public static final String CHANGETYPE_CHANGEACTION= "attribute[LCD_3ChangeType]";
    public static final String CHANGEDOMAIN_CHANGEACTION= "attribute[LCD_1ChangeDomain]";
    public static final String PLATFORM_CHANGEACTION= "attribute[LCD_2Platform]";
    public static final String REASONFORCHANGE_CHANGEACTION= "attribute[LCD_4ReasonForChange]";
    public static final String CATEGORYOFCHANGE_CHANGEACTION= "attribute[CategoryofChange]";
    public static final String RELEASEDDATE_CHANGEACTION = "attribute[ActualCompletionDate]";
    
    
    public static final String PROPOSED_APPLICABILITY_START_DATE = "attribute_LCD_5ProposedApplicabilityStartDate";
    public static final String PROPOSED_APPLICABILITY_END_DATE = "attribute_LCD_6ProposedApplicabilityEndDate";
    

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
    public static final String CFG_EXPOSED_EXCEPTION  = "dassault_systemes.plm.config.exposed.exception.CfgExposedException";
    public static final String FALSE = "FALSE";
    public static final String TRUE = "TRUE";
    
    protected static final String EFFECTIVITY_CURRENT_EVOLUTION = "Effectivity_Current_Evolution";
    protected static final String EFFECTIVITY_PROJECTED_EVOLUTION = "Effectivity_Projected_Evolution";
    protected static final String EFFECTIVITY_VARIANT = "Effectivity_Variant";
    protected static final String EFFECTIVITY_FROM_DATE = "Effectivity_From_Date";
    protected static final String EFFECTIVITY_TO_DATE = "Effectivity_To_Date";
    
    public static final String LCD_3DX_SAP_INTEGRATION_KEY = "LCD_3DXSAPStringResource_en";
	public static final String TYPE_LCD_BOM_ANCHOR_OBJECT = "LCD_BOMAnchorObject";
	public static final String NAME_LCD_ANCHOR_OBJECT = "LCD_AnchorObject";
	public static final String REV_LCD_ANCHOR_OBJECT = "A";
	public static final String VAULT_ESERVICE_PRODUCTION = "eService Production";
	public static final String ATTR_LCD_PROCESS_STATUS_FLAG = "LCD_ProcessStatusFlag";
	public static final String ATTR_LCD_REASON_FOR_FAILURE = "LCD_ReasonforFailure";
	public static final String ATTR_LCD_CAID = "LCD_CAID";
	public static final String REL_LCD_SAP_BOM_INTERFACE = "LCD_SAPBOMInterface";
	public static final String STATUS_COMPLETE = "Complete";
	public static final String STATUS_IN_WORK = "In Work";
	
	public static final String KEY_CONNECTION_ID = "ConnectionID";
	public static final String KEY_BOM_COMPONENT_ID = "BOMComponentID";
	public static final String KEY_BOM_NAME = "BOMName";
	public static final String KEY_CA_ID = "CAID";
    

  }