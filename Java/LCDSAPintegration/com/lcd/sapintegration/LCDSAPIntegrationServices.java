package com.lcd.sapintegration;

import com.dassault_systemes.platform.restServices.RestService;
import com.lcd.sapintegration.LCDSAPIntegrationServices;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.DomainRelationship;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.framework.ui.UIUtil;

import java.util.Iterator;
import java.util.Map;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import matrix.db.BusinessObject;
import matrix.util.StringList;

@Path("/LCDSAPIntegrationService")
public class LCDSAPIntegrationServices extends RestService {
  static final String TYPE_APPLICATION_FORMAT = "application/json";
  
  private static final String TYPE_LCD_BOM_ANCHOR_OBJECT = "LCD_BOMAnchorObject";
  private static final String NAME_LCD_ANCHOR_OBJECT = "LCD_AnchorObject";
  private static final String REV_LCD_ANCHOR_OBJECT = "A";
  private static final String VAULT_ESERVICE_PRODUCTION = "eService Production";
  
  @GET
  @Path("/getMA")
  public Response getReportTemplateData(@Context HttpServletRequest request) throws Exception {
    Response res = null;
    String SELECT_ATTRIBUTE_ACTUAL_COMPLETION_DATE = "attribute[" + DomainConstants.ATTRIBUTE_ACTUAL_COMPLETION_DATE + "]";
    try {
      boolean isSCMandatory = false;
      matrix.db.Context context = getAuthenticatedContext(request, isSCMandatory);
      BusinessObject busObjAchor = new BusinessObject("LCD_BOMAnchorObject", 
          "LCD_AnchorObject", 
          "A", 
          "eService Production");
      DomainObject domObj = DomainObject.newInstance(context, busObjAchor);
      StringList slObjectSelect = new StringList();
      slObjectSelect.add("id");
      slObjectSelect.add("name");
      slObjectSelect.add("revision");
      slObjectSelect.add("attribute[PLMEntity.V_Name]");
      slObjectSelect.add("attribute[PLMEntity.V_description]");
      slObjectSelect.add("current");
      slObjectSelect.add("attribute[LCDMF_ManufacturingAssembly.LCDMF_SAPMBOMUpdatedOn]");
      StringList slRelSelect = new StringList();
      slRelSelect.add("id[connection]");
      MapList manAssMapList = domObj.getRelatedObjects(context, 
          "LCD_SAPBOMInterface", 
          "*", 
          slObjectSelect, 
          slRelSelect, 
          false, 
          true, (short)1, 
          
          "", 
          "", 
          0);
      Iterator<?> iterMAsMaplist = manAssMapList.iterator();
      JsonArrayBuilder jabMAs = Json.createArrayBuilder();
      while (iterMAsMaplist.hasNext()) {
        JsonObjectBuilder jobMA = Json.createObjectBuilder();
        Map<?, ?> item = (Map<?, ?>)iterMAsMaplist.next();
        String sConnectionId = (String)item.get("id[connection]");
        DomainRelationship relEbom = DomainRelationship.newInstance(context, sConnectionId);
        String Status = relEbom.getAttributeValue(context, "LCD_ProcessStatusFlag");
        String ReasonForFailure = relEbom.getAttributeValue(context, "LCD_ReasonforFailure");
        String CAID = relEbom.getAttributeValue(context, "LCD_CAID");
        
        if(UIUtil.isNullOrEmpty(CAID)) {
        	jobMA.add("ConnectionID", sConnectionId);
            jobMA.add("caID", CAID);
            jobMA.add("BOMComponentID", (String)item.get("id"));
            jobMA.add("BOMComponentName", (String)item.get("name"));
            jobMA.add("status", Status);
            jobMA.add("revision", (String)item.get("revision"));
            jobMA.add("title", (String)item.get("attribute[PLMEntity.V_Name]"));
            jobMA.add("maturity", (String)item.get("current"));
            jobMA.add("description", (String)item.get("attribute[PLMEntity.V_description]"));
            jobMA.add("caCompletedTime", "");
            jobMA.add("caName", "");
            jobMA.add("SapFeedbackTimeStamp", (String)item.get("attribute[LCDMF_ManufacturingAssembly.LCDMF_SAPMBOMUpdatedOn]"));
            jobMA.add("SapFeedbackMessage", ReasonForFailure);
            jabMAs.add(jobMA);
        	 
        } else {
        	StringList CAobjectSelects = new StringList();
            CAobjectSelects.add("name");
            CAobjectSelects.add(SELECT_ATTRIBUTE_ACTUAL_COMPLETION_DATE);
            DomainObject domCAObj = DomainObject.newInstance(context, CAID);
            Map<?, ?> CaAttrDetails = domCAObj.getInfo(context, CAobjectSelects);
            jobMA.add("ConnectionID", sConnectionId);
            jobMA.add("caID", CAID);
            jobMA.add("BOMComponentID", (String)item.get("id"));
            jobMA.add("BOMComponentName", (String)item.get("name"));
            jobMA.add("status", Status);
            jobMA.add("revision", (String)item.get("revision"));
            jobMA.add("title", (String)item.get("attribute[PLMEntity.V_Name]"));
            jobMA.add("maturity", (String)item.get("current"));
            jobMA.add("description", (String)item.get("attribute[PLMEntity.V_description]"));
            jobMA.add("caCompletedTime", (String)CaAttrDetails.get(SELECT_ATTRIBUTE_ACTUAL_COMPLETION_DATE));
            jobMA.add("caName", (String)CaAttrDetails.get("name"));
            jobMA.add("SapFeedbackTimeStamp", (String)item.get("attribute[LCDMF_ManufacturingAssembly.LCDMF_SAPMBOMUpdatedOn]"));
            jobMA.add("SapFeedbackMessage", ReasonForFailure);
            jabMAs.add(jobMA);
        }
        
       
      } 
      String strOutput = jabMAs.build().toString();
      res = Response.ok(strOutput).type("application/json").build();
    } catch (Exception e) {
      e.printStackTrace();
      res = Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
    } 
    return res;
  }
}
