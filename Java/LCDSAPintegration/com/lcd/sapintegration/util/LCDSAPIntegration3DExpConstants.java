
package com.lcd.sapintegration.util;

import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.util.PropertyUtil;

import matrix.db.Context;

public class LCDSAPIntegration3DExpConstants {
	private Context context;
	public final String ATTRIBUTE_PLMENTITY_V_DESCRIPTION = PropertyUtil.getSchemaProperty(context, "attribute_PLMEntity.V_description");
	public final String ATTRIBUTE_PLMENTITY_V_NAME = PropertyUtil.getSchemaProperty(context, "attribute_PLMEntity.V_Name");
	public final String NAME_LCD_BOM_ANCHOR_OBJECT = "LCD_BOMAnchorObject";
	public final String REVISION_LCD_BOM_ANCHOR_OBJECT = "A";
	public final String SELECT_ATTRIBUTE_ACTUAL_COMPLETION_DATE = DomainObject.getAttributeSelect(DomainConstants.ATTRIBUTE_ACTUAL_COMPLETION_DATE);
	public final String SELECT_ATTRIBUTE_PLMENTITY_V_DESCRIPTION = DomainObject.getAttributeSelect(ATTRIBUTE_PLMENTITY_V_DESCRIPTION);
	public final String SELECT_ATTRIBUTE_PLMENTITY_V_NAME = DomainObject.getAttributeSelect(ATTRIBUTE_PLMENTITY_V_NAME);
	public final String TYPE_LCD_BOM_ANCHOR_OBJECT = PropertyUtil.getSchemaProperty(context, "type_LCD_BOMAnchorObject");
	public final String VAULT_ESERVICE_PRODUCTION = PropertyUtil.getSchemaProperty(context, "vault_eServiceProduction");
	
	public LCDSAPIntegration3DExpConstants(Context context) {
		this.context = context;
	}
}
