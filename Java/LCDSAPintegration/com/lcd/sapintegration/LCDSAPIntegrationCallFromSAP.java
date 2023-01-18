package com.lcd.sapintegration;

import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.dassault_systemes.platform.restServices.RestService;
import com.lcd.sapintegration.util.LCDSAPIntegration3DExpConstants;
import com.lcd.sapintegration.util.LCDSAPIntegrationDataConstants;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.DomainRelationship;
import com.matrixone.apps.domain.util.ContextUtil;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.framework.ui.UIUtil;

import matrix.db.BusinessObject;
import matrix.db.Context;
import matrix.util.MatrixException;
import matrix.util.StringList;

@Path("/LCDGetFromSAPServices")
public class LCDSAPIntegrationCallFromSAP extends RestService {

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
	public Response processWebServiceResponse(@javax.ws.rs.core.Context HttpServletRequest request, String paramString)
			throws Exception {

		System.out.println(">>>>>LCDSAPIntegrationCallFromSAP---sendFailedDataToSap()-----STARTED");
		Response res = null;
		String strConnectionId = "";
		boolean bContextPushed = false;

		boolean isSCMandatory = false;
		matrix.db.Context context = getAuthenticatedContext(request, isSCMandatory);
		LCDSAPIntegration3DExpConstants lcdSAPInteg3DExpConstants = new LCDSAPIntegration3DExpConstants(context);

		
			ContextUtil.pushContext(context);
			bContextPushed = true;

			StringBuffer sbfErrMes = new StringBuffer();
			SimpleDateFormat dateFormat = new SimpleDateFormat(LCDSAPIntegrationDataConstants.DATE_FORMAT);

			try(JsonReader jsonReader = Json.createReader(new StringReader(paramString))) {
			JsonObject jWebServiceResponse = jsonReader.readObject();

			if (null != jWebServiceResponse) {
				if (jWebServiceResponse.containsKey(LCDSAPIntegrationDataConstants.PROPERTY_HEADER_PART)) {
					JsonObject joHeaderPart = jWebServiceResponse
							.getJsonObject(LCDSAPIntegrationDataConstants.PROPERTY_HEADER_PART);
					if (null != joHeaderPart) {
						String resultType = jWebServiceResponse.getString(LCDSAPIntegrationDataConstants.PROPERTY_TYPE);
						if (UIUtil.isNotNullAndNotEmpty(resultType)
								&& resultType.equalsIgnoreCase(LCDSAPIntegrationDataConstants.VALUE_FAILURE)) {
							String strErrorMessage = joHeaderPart.getString(LCDSAPIntegrationDataConstants.PROPERTY_SAP_RESPONSE_ERROR_MESSAGE);
							String strObjID = joHeaderPart.getString(LCDSAPIntegrationDataConstants.PROPERTY_OID);
//							String strObjTitle = joHeaderPart.getString(LCDSAPIntegrationDataConstants.PROPERTY_SAP_RESPONSE_TITLE);
							
							strConnectionId = getConnectionId(context, strObjID, lcdSAPInteg3DExpConstants);

							if (UIUtil.isNotNullAndNotEmpty(strObjID) && UIUtil.isNotNullAndNotEmpty(strErrorMessage)) {
//								sbfErrMes.append(strObjTitle + LCDSAPIntegrationDataConstants.COLON_SEP
//										+ strErrorMessage + LCDSAPIntegrationDataConstants.NEW_LINE);
							}

							JsonArray jArrayOfChildParts = joHeaderPart
									.getJsonArray(LCDSAPIntegrationDataConstants.PROPERTY_CHILDREN);
							if (null != jArrayOfChildParts) {
								JsonObject jchildPart;
								String strPartTitle;
								String sErrorMessage;
								for (int i = 0; i < jArrayOfChildParts.size(); i++) {
									jchildPart = jArrayOfChildParts.getJsonObject(i);
//									strPartTitle = jchildPart.getString(LCDSAPIntegrationDataConstants.PROPERTY_SAP_RESPONSE_TITLE);
									sErrorMessage = jchildPart.getString(LCDSAPIntegrationDataConstants.PROPERTY_SAP_RESPONSE_ERROR_MESSAGE);

									if (UIUtil.isNotNullAndNotEmpty(sErrorMessage)) {
//										sbfErrMes.append(strPartTitle).append(LCDSAPIntegrationDataConstants.COLON_SEP)
//												.append(sErrorMessage).append(LCDSAPIntegrationDataConstants.NEW_LINE);
									}
								}
							}
							DomainRelationship domRelMA = DomainRelationship.newInstance(context, strConnectionId);

							domRelMA.setAttributeValue(context,
									lcdSAPInteg3DExpConstants.ATTRIBUTE_LCD_PROCESS_STATUS_FLAG,
									LCDSAPIntegrationDataConstants.VALUE_PROCESS_STATUS_FLAG_FAILED);
							domRelMA.setAttributeValue(context,
									lcdSAPInteg3DExpConstants.ATTRIBUTE_LCD_REASON_FOR_FAILURE, sbfErrMes.toString());

						} else if (UIUtil.isNotNullAndNotEmpty(resultType)
								&& resultType.equalsIgnoreCase(LCDSAPIntegrationDataConstants.VALUE_SUCCESS)) {
							String strObjID = joHeaderPart.getString(LCDSAPIntegrationDataConstants.PROPERTY_OID);

							if (UIUtil.isNotNullAndNotEmpty(strObjID)) {
								DomainObject domObjBomComponent = new DomainObject(strObjID);

								strConnectionId = getConnectionId(context, strObjID, lcdSAPInteg3DExpConstants);

								String strSAPUniqueID = joHeaderPart
										.getString(LCDSAPIntegrationDataConstants.PROPERTY_SAPUNIQUE_ID);
								// Push context as no Manufacturing Assembly access to 3DXLeader in release
								// state in respective policy.
								try {
									Date date = new Date();
									domObjBomComponent.setAttributeValue(context,
											lcdSAPInteg3DExpConstants.ATTRIBUTE_LCD_MF_SAP_UNIQUEID, strSAPUniqueID);
									domObjBomComponent.setAttributeValue(context,
											lcdSAPInteg3DExpConstants.ATTRIBUTE_LCD_MF_SAPMBOMUpdatedOn,
											dateFormat.format(date));

									DomainRelationship domRel = DomainRelationship.newInstance(context,
											strConnectionId);

									domRel.setAttributeValue(context,
											lcdSAPInteg3DExpConstants.ATTRIBUTE_LCD_PROCESS_STATUS_FLAG,
											LCDSAPIntegrationDataConstants.VALUE_PROCESS_STATUS_FLAG_COMPLETE);
									domRel.setAttributeValue(context,
											lcdSAPInteg3DExpConstants.ATTRIBUTE_LCD_REASON_FOR_FAILURE,
											sbfErrMes.toString());

								} catch (Exception e) {
									e.printStackTrace();
								}
							}
						}
					}
				} else {
					throw new NullPointerException();
				}
			} else {
				throw new NullPointerException();
			}
			System.out.println(">>>>>LCDSAPIntegrationCallFromSAP---sendFailedDataToSap()-----ENDED");
			res = Response.ok(LCDSAPIntegrationDataConstants.VALUE_SUCCESS).type(MediaType.TEXT_PLAIN).build();
		} catch (Exception e) {
			e.printStackTrace();
			res = Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
		} finally {
			if (bContextPushed) {
				ContextUtil.popContext(context);
			}
		}
		return res;
	}

	private String getConnectionId(Context context, String strObjID,LCDSAPIntegration3DExpConstants lcdSAPInteg3DExpConstants) throws MatrixException {
		String strConnectionId = "";
		String strBusWhere = "id==\"" + strObjID + "\"";
		BusinessObject busObjAchor = new BusinessObject(lcdSAPInteg3DExpConstants.TYPE_LCD_BOM_ANCHOR_OBJECT, lcdSAPInteg3DExpConstants.NAME_LCD_BOM_ANCHOR_OBJECT,
				lcdSAPInteg3DExpConstants.REVISION_LCD_BOM_ANCHOR_OBJECT, lcdSAPInteg3DExpConstants.VAULT_ESERVICE_PRODUCTION);
		DomainObject domObj = DomainObject.newInstance(context, busObjAchor);
		StringList slObjectSelect = new StringList();
		slObjectSelect.add(DomainConstants.SELECT_ID);
		StringList slRelSelect = new StringList();
		slRelSelect.add(DomainRelationship.SELECT_ID);
		MapList manAssMapList = domObj.getRelatedObjects(context, lcdSAPInteg3DExpConstants.RELATIONSHIP_LCD_SAP_BOM_INTERFACE, "*", slObjectSelect,
				slRelSelect, false, true, (short) 1, strBusWhere, "", 0);
		Iterator<?> iterMAsMaplist = manAssMapList.iterator();
		while (iterMAsMaplist.hasNext()) {
			Map<?, ?> item = (Map<?, ?>) iterMAsMaplist.next();
			strConnectionId = (String) item.get(DomainRelationship.SELECT_ID);
		}
		return strConnectionId;
	}

}
