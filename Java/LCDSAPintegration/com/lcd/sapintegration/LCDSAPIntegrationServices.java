
package com.lcd.sapintegration;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.dassault_systemes.platform.restServices.RestService;
import com.lcd.sapintegration.util.LCDSAPIntegrationAnchorObject;
import com.lcd.sapintegration.util.LCDSAPIntegrationPushToSAPUtil;

@Path("/lcdSAPIntegrationServices")
public class LCDSAPIntegrationServices extends RestService {
	@GET
	@Path("/getSAPTransferDetailsOfAnchoredAssemblies")
	public Response getSAPTransferDetailsOfAnchoredAssemblies(@javax.ws.rs.core.Context HttpServletRequest request) throws Exception {
		Response res;
		try {
			matrix.db.Context context = getAuthenticatedContext(request, false);
			res = Response.ok(LCDSAPIntegrationAnchorObject.getConnectedAssemblyDetailsAsJsonString(context)).type(MediaType.APPLICATION_JSON).build();
		} catch (Exception e) {
			e.printStackTrace();
			res = Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
		}
		return res;
	}

	

	/**
	 * @param request
	 * @param paramString
	 * @return
	 * @throws NullPointerException
	 */
	@POST
	@Path("/pushAssemblyToSAP")
	@Consumes({ "application/json", "application/ds-json" })
	public Response sendFailedDataToSap(@javax.ws.rs.core.Context HttpServletRequest request, String paramString) throws NullPointerException {
		matrix.db.Context context = getAuthenticatedContext(request, false);
		return LCDSAPIntegrationPushToSAPUtil.sendFailedDataToSap(context, paramString);
	}
}
