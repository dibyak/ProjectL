package com.lcd.partwhereused;

import javax.ws.rs.ApplicationPath;

import com.dassault_systemes.platform.restServices.ModelerBase;

@ApplicationPath("/PartWhereUsedModeler")
public class PartWhereUsedModeler extends ModelerBase {

	public Class<?>[] getServices() {
		return new Class[] { PartWhereUsedService.class };
	}
}