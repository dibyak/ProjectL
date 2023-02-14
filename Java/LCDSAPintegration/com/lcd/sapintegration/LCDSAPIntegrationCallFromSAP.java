package com.lcd.sapintegration;

import java.io.StringReader;
import java.util.logging.Logger;

import javax.json.Json;
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
import com.lcd.sapintegration.util.LCDSAPIntegrationProceesAfterEbomAndMbomCreation;
import com.matrixone.apps.domain.util.ContextUtil;

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
	public Response processRequestFromSAP(@javax.ws.rs.core.Context HttpServletRequest request, String paramString)
			throws Exception {
		
//		Logger loggerDebug = Logger.getLogger("LCDSAPIntegrationCallFromSAP");
//		loggerDebug.info(">>>>>LCDSAPIntegrationCallFromSAP---processRequestFromSAP()-----STARTED");
		
		Response res = null;
		boolean bContextPushed = false;
		
		boolean isSCMandatory = false;
		

		matrix.db.Context context = getAuthenticatedContext(request, isSCMandatory);
		
		LCDSAPIntegration3DExpConstants lcdSAPInteg3DExpConstants = new LCDSAPIntegration3DExpConstants(context);

		ContextUtil.pushContext(context);
		bContextPushed = true;
		
		try{
			JsonReader jsonReader = Json.createReader(new StringReader(paramString));
			JsonObject jWebServiceResponse = jsonReader.readObject();
			jsonReader.close();

			if (null != jWebServiceResponse) {
				String resultType = jWebServiceResponse.getString(LCDSAPIntegrationDataConstants.PROPERTY_TYPE);
				String strConnectionId = jWebServiceResponse.getString(LCDSAPIntegrationDataConstants.PROPERTY_OID);
				
				if (jWebServiceResponse.containsKey(LCDSAPIntegrationDataConstants.PROPERTY_HEADER_PART)) {
					
					LCDSAPIntegrationProceesAfterEbomAndMbomCreation.processAfterEBOMCreation(context, jWebServiceResponse, resultType, strConnectionId, lcdSAPInteg3DExpConstants);
					
				} else {
					LCDSAPIntegrationProceesAfterEbomAndMbomCreation.processAfterMBOMCreation(context, jWebServiceResponse, resultType, strConnectionId, lcdSAPInteg3DExpConstants);
				}
			}
//			loggerDebug.info(">>>>>LCDSAPIntegrationCallFromSAP---processRequestFromSAP()-----ENDED");
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

	

}
