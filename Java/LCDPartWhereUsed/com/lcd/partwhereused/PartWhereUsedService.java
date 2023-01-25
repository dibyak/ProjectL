package com.lcd.partwhereused;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dassault_systemes.platform.restServices.RestService;

@Path("/PartWhereUsedService")
public class PartWhereUsedService extends RestService {
	private static final Logger logger = LoggerFactory.getLogger("ERI_PRM_Logger");

	@POST
	@Path("/getPartWhereUsedData")
	@Produces({ "application/json" })
	public Response getPartWhereUsedData(@Context HttpServletRequest httpReq, String jsonObjBOM) throws Exception {
		Response res = null;
		matrix.db.Context context = getAuthenticatedContext(httpReq, false);
		try {
			PartWhereUsedUtil oUtil = new PartWhereUsedUtil();
			res = oUtil.getPartWhereUsedData(context,jsonObjBOM);
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
		finally {
			logger.debug("Closing session");
			context.shutdown();
		}
		return res;
	}
	
	@POST
	@Path("/getPartTitleAndRev")
	@Produces({ "application/json" })
	public Response getPartTitleAndRev(@Context HttpServletRequest httpReq, String sObjectID) throws Exception {
		Response res = null;
		matrix.db.Context context = getAuthenticatedContext(httpReq, false);
		try {
			PartWhereUsedUtil oUtil = new PartWhereUsedUtil();
			res = oUtil.getPartTitleAndRev(context,sObjectID);
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
		finally {
			context.shutdown();
		}
		return res;
	}
}