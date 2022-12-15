
package com.lcd.sapintegration;

import com.dassault_systemes.platform.restServices.ModelerBase;
/**
 * Modeler class for report generator widget
 * @since 2018x.5
 */
import javax.ws.rs.ApplicationPath;

@ApplicationPath("/LCDSAPIntegrationModeler")
public class LCDSAPIntegrationModeler extends ModelerBase {
	public Class<?>[] getServices() {
		return new Class[] { LCDSAPIntegrationServices.class , LCDSAPIntegrationPushToSAPServices.class};
	}
}
