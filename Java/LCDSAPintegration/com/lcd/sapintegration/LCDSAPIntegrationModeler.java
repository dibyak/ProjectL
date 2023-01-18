package com.lcd.sapintegration;


import com.dassault_systemes.platform.restServices.ModelerBase;
import javax.ws.rs.ApplicationPath;

@ApplicationPath("/LCDSAPIntegrationModeler")
public class LCDSAPIntegrationModeler extends ModelerBase {
	public Class<?>[] getServices() {
		return new Class[] { LCDSAPIntegrationServices.class , LCDSAPIntegrationExportCSVServices.class, LCDSAPIntegrationCallFromSAP.class};
	}
}
