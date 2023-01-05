package com.lcd.sapintegration;

import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.json.JsonValue;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.dassault_systemes.platform.restServices.RestService;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.DomainRelationship;
import com.matrixone.apps.domain.util.ContextUtil;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.PropertyUtil;
import com.matrixone.apps.framework.ui.UIUtil;

import matrix.db.BusinessObject;
import matrix.db.Context;
import matrix.db.JPO;
import matrix.util.StringList;

@Path("/LCDGetFromSAPServices")
public class LCDSAPIntegrationCallFromSAP extends RestService {

	private static final String TYPE_APPLICATION_FORMAT = "application/json";

	private static final String ATTR_LCD_PROCESS_STATUS_FLAG = "LCD_ProcessStatusFlag";
	private static final String ATTR_LCD_REASON_FOR_FAILURE = "LCD_ReasonforFailure";

	private static final String STATUS_FAILED = "Failed";

	public static final String TAG_PROCUREMENTINTENT = "ProcurementIntent";
	public static final String TAG_SERVICEABLEITEM = "ServiceableItem";
	public static final String TAG_PARTINTERCHANGEABILITY = "PartInterchangeability";
	public static final String TAG_UNIT_OF_MEASURE = "UoM";
	public static final String TAG_VARIAENT_EFFECTIVITY = "Effectivity_Option_Code";
	public static final String TAG_PLANTCODE = "PlantCode";
	public static final String TAG_ERROR_MESSAGE = "ERROR_MESSAGE";
	public static final String HEADER_CHILDREN = "children";
	public static final String TAG_TYPE = "Type";
	public static final String HEADER_PART = "HeaderPart";
	public static final String TAG_SAPUNIQUE_ID = "SAPUniqueID";

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

	public static final String ATTR_SAP_UNIQUEID = "LCDMF_ManufacturingAssembly.LCDMF_SAPUniqueID";
	public static final String ATTR_SAPMBOM_UPDATED_ON = "LCDMF_ManufacturingAssembly.LCDMF_SAPMBOMUpdatedOn";

	public static final String FAIL = "FAILURE";
	public static final String SUCCESS = "SUCCESS";

	public static final String RELEASED = "RELEASED";

	public static final String NOT_APPLICABLE = "NA";

	public static final String FALSE = "FALSE";
	
	public static final String NEW_LINE = "\n";
	public static final String COLON_SEP = ":";

	public static final String STATUS_COMPLETE = "Complete";
	public static final String KEY_STATUS = "status";
	public static final String KEY_ERROR_MESSAGE = "ErrorMessage";
	
	private static final String MSG_SUCCESS = "Success";

	/**
	 * Method
	 * 
	 * @param request     : HttpServletRequest request param
	 * @param type        :
	 * @param name        :
	 * @param revision    :
	 * @param showAllCols :
	 * @return : Response of Json object with object info
	 * @throws Exception
	 */
	@POST
	@Path("/callFromSAP")
	@Consumes({ "application/json", "application/ds-json" })
	public Response sendFailedDataToSap(@javax.ws.rs.core.Context HttpServletRequest request, String paramString)
			throws NullPointerException {
		
		System.out.println(">>>>>LCDSAPIntegrationCallFromSAP---sendFailedDataToSap()-----STARTED");
		Response res = null;
		String strConnectionId = "";

		try {
			boolean isSCMandatory = false;
			matrix.db.Context context = getAuthenticatedContext(request, isSCMandatory);
			
			StringBuffer sbfErrMes = new StringBuffer();
			SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy hh.mm.ss aa");
			
			JsonReader jsonReader = Json.createReader(new StringReader(paramString));
			JsonObject jWebServiceResponse = jsonReader.readObject();
				
			if (null != jWebServiceResponse) {
				if (jWebServiceResponse.containsKey(HEADER_PART)) {
					JsonObject joHeaderPart = jWebServiceResponse.getJsonObject(HEADER_PART);
					if (null != joHeaderPart) {
						String resultType = jWebServiceResponse.getString(TAG_TYPE);

						if (UIUtil.isNotNullAndNotEmpty(resultType) && resultType.equalsIgnoreCase(FAIL)) {
							String strErrorMessage = joHeaderPart.getString(TAG_ERROR_MESSAGE);
							String strObjID = joHeaderPart.getString(TAG_OID);
							String strObjTitle = joHeaderPart.getString(TAG_TITLE);
							String strBusWhere = "id==\"" + strObjID + "\"";
							BusinessObject busObjAchor = new BusinessObject("LCD_BOMAnchorObject", 
							          "LCD_AnchorObject", 
							          "A", 
							          "eService Administration");
							      DomainObject domObj = DomainObject.newInstance(context, busObjAchor);
							      StringList slObjectSelect = new StringList();
							      slObjectSelect.add("id");
							      StringList slRelSelect = new StringList();
							      slRelSelect.add(DomainRelationship.SELECT_ID);
							      MapList manAssMapList = domObj.getRelatedObjects(context, 
							          "LCD_SAPBOMInterface", 
							          "*", 
							          slObjectSelect, 
							          slRelSelect, 
							          false, 
							          true, 
							          (short)1,
							          strBusWhere, 
							          "", 
							          0);
							      Iterator<?> iterMAsMaplist = manAssMapList.iterator();
							      while (iterMAsMaplist.hasNext()) {
							        Map<?, ?> item = (Map<?, ?>)iterMAsMaplist.next();
//							        strConnectionId = (String)item.get("id[connection]");
							        strConnectionId = (String)item.get(DomainRelationship.SELECT_ID);
							      }
							

							if (UIUtil.isNotNullAndNotEmpty(strObjID) && UIUtil.isNotNullAndNotEmpty(strErrorMessage)) {
								sbfErrMes.append(strObjTitle + COLON_SEP + strErrorMessage + NEW_LINE);
							}

							JsonArray jArrayOfChildParts = joHeaderPart.getJsonArray(HEADER_CHILDREN);
							if (null != jArrayOfChildParts) {
								JsonObject jchildPart;
								String strPartTitle;
								String sErrorMessage;
								for (int i = 0; i < jArrayOfChildParts.size(); i++) {
									jchildPart = jArrayOfChildParts.getJsonObject(i);
									strPartTitle = jchildPart.getString(TAG_TITLE);
									sErrorMessage = jchildPart.getString(TAG_ERROR_MESSAGE);

									if (UIUtil.isNotNullAndNotEmpty(sErrorMessage)) {
										sbfErrMes.append(strPartTitle).append(COLON_SEP).append(sErrorMessage).append(NEW_LINE);
									}
								}
							}
							DomainRelationship domRelMA = DomainRelationship.newInstance(context, strConnectionId);
							
							domRelMA.setAttributeValue(context, ATTR_LCD_PROCESS_STATUS_FLAG, STATUS_FAILED);
							domRelMA.setAttributeValue(context, ATTR_LCD_REASON_FOR_FAILURE, sbfErrMes.toString());

						} else if (UIUtil.isNotNullAndNotEmpty(resultType) && resultType.equalsIgnoreCase(SUCCESS)) {
							String strObjID = joHeaderPart.getString(TAG_OID);
							
							if (UIUtil.isNotNullAndNotEmpty(strObjID)) {
								DomainObject domObjBomComponent = new DomainObject(strObjID);
								
								domObjBomComponent.getExpansionIterator(context, null);
								String strSAPUniqueID = joHeaderPart.getString(TAG_SAPUNIQUE_ID);
								// Push context as no Manufacturing Assembly access to 3DXLeader in release
								// state in respective policy.
								try{
										Date date = new Date();
										ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "person_UserAgent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
										
										domObjBomComponent.setAttributeValue(context, ATTR_SAP_UNIQUEID, strSAPUniqueID);
										domObjBomComponent.setAttributeValue(context, ATTR_SAPMBOM_UPDATED_ON, dateFormat.format(date));
										
										DomainRelationship domRel = DomainRelationship.newInstance(context, strConnectionId);
										
										domRel.setAttributeValue(context, ATTR_LCD_PROCESS_STATUS_FLAG, STATUS_COMPLETE);
										domRel.setAttributeValue(context, ATTR_LCD_REASON_FOR_FAILURE, sbfErrMes.toString());
										
										ContextUtil.popContext(context);
								} catch (Exception e) {
									e.printStackTrace();
									throw new Exception(e.getMessage());
								}
							}
						}
					} 
				}else {
					throw new NullPointerException("Inavlid Response from SAP.");
				}
			}else {
				throw new NullPointerException("Empty from Response from SAP.");
			}
			System.out.println(">>>>>LCDSAPIntegrationCallFromSAP---sendFailedDataToSap()-----ENDED");
			res = Response.ok(MSG_SUCCESS).type(MediaType.TEXT_PLAIN).build();		
			} catch (Exception e) {
				throw new NullPointerException(e.getMessage());
		} 
		return res;
	}
}
