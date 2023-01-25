package com.lcd.sapintegration.util;


import com.matrixone.apps.domain.DomainRelationship;
import com.matrixone.apps.domain.util.ContextUtil;

import matrix.db.Context;

public class LCDSAPIntegrationProcessWebServiceResponse {

	/**
	 * This Method is to process the response from POST call of SAP WebService
	 *
	 * @param context
	 * @param String  responseString : Response from SAP webService
	 * @throws Exception
	 */
	public static void processWebServiceResponseForSubContract(Context context, int intResponseCode, String strConnectionId)
			throws NullPointerException {
		System.out.println("ProcessWebServiceResponseForSubContract START");

		LCDSAPIntegration3DExpConstants lcdSAPInteg3DExpConstants = new LCDSAPIntegration3DExpConstants(context);
		try {
			ContextUtil.pushContext(context);

			DomainRelationship domRelBomConnectedToAnchorObj = DomainRelationship.newInstance(context, strConnectionId);

			if (intResponseCode == 200) {
				domRelBomConnectedToAnchorObj.setAttributeValue(context, lcdSAPInteg3DExpConstants.ATTRIBUTE_LCD_PROCESS_STATUS_FLAG, LCDSAPIntegrationDataConstants.VALUE_PROCESS_STATUS_FLAG_IN_WORK);
				domRelBomConnectedToAnchorObj.setAttributeValue(context, lcdSAPInteg3DExpConstants.ATTRIBUTE_LCD_REASON_FOR_FAILURE,
						LCDSAPIntegrationDataConstants.MSG_JSON_FORMAT_VALIDATION_COMPLETED);
			} else if (intResponseCode == 417) {
				domRelBomConnectedToAnchorObj.setAttributeValue(context, lcdSAPInteg3DExpConstants.ATTRIBUTE_LCD_PROCESS_STATUS_FLAG, LCDSAPIntegrationDataConstants.VALUE_PROCESS_STATUS_FLAG_FAILED);
				domRelBomConnectedToAnchorObj.setAttributeValue(context, lcdSAPInteg3DExpConstants.ATTRIBUTE_LCD_REASON_FOR_FAILURE,
						LCDSAPIntegrationDataConstants.MSG_JSON_FORMAT_VALIDATION_FAILED);
			} else {
				domRelBomConnectedToAnchorObj.setAttributeValue(context, lcdSAPInteg3DExpConstants.ATTRIBUTE_LCD_PROCESS_STATUS_FLAG, LCDSAPIntegrationDataConstants.VALUE_PROCESS_STATUS_FLAG_FAILED);
				domRelBomConnectedToAnchorObj.setAttributeValue(context, lcdSAPInteg3DExpConstants.ATTRIBUTE_LCD_REASON_FOR_FAILURE,
						LCDSAPIntegrationDataConstants.MSG_NETWORK_FAILURE);
			}
			ContextUtil.popContext(context);
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("ProcessWebServiceResponseForSubContract END");
	}

	/**
	 * This Method is to process the response from POST call of SAP WebService
	 *
	 * @param context
	 * @param String  responseString : Response from SAP webService
	 * @return
	 * @throws Exception
	 */
	public static void processWebServiceResponseForManufacturingAssembly(Context context, int intResponseCode,
			String strConnectionId) throws NullPointerException {
		System.out.println("ProcessWebServiceResponseForManufacturingAssembly START");
		LCDSAPIntegration3DExpConstants lcdSAPInteg3DExpConstants = new LCDSAPIntegration3DExpConstants(context);
		try {
			ContextUtil.pushContext(context);
			DomainRelationship domRelBomConnectedToAnchorObj = DomainRelationship.newInstance(context, strConnectionId);

			if (intResponseCode == 200) {
				domRelBomConnectedToAnchorObj.setAttributeValue(context, lcdSAPInteg3DExpConstants.ATTRIBUTE_LCD_PROCESS_STATUS_FLAG, LCDSAPIntegrationDataConstants.VALUE_PROCESS_STATUS_FLAG_IN_WORK);
				domRelBomConnectedToAnchorObj.setAttributeValue(context, lcdSAPInteg3DExpConstants.ATTRIBUTE_LCD_REASON_FOR_FAILURE,
						LCDSAPIntegrationDataConstants.MSG_JSON_FORMAT_VALIDATION_COMPLETED);
			} else if (intResponseCode == 417) {
				domRelBomConnectedToAnchorObj.setAttributeValue(context, lcdSAPInteg3DExpConstants.ATTRIBUTE_LCD_PROCESS_STATUS_FLAG, LCDSAPIntegrationDataConstants.VALUE_PROCESS_STATUS_FLAG_FAILED);
				domRelBomConnectedToAnchorObj.setAttributeValue(context, lcdSAPInteg3DExpConstants.ATTRIBUTE_LCD_REASON_FOR_FAILURE,
						LCDSAPIntegrationDataConstants.MSG_JSON_FORMAT_VALIDATION_FAILED);
			} else {
				domRelBomConnectedToAnchorObj.setAttributeValue(context, lcdSAPInteg3DExpConstants.ATTRIBUTE_LCD_PROCESS_STATUS_FLAG, LCDSAPIntegrationDataConstants.VALUE_PROCESS_STATUS_FLAG_FAILED);
				domRelBomConnectedToAnchorObj.setAttributeValue(context, lcdSAPInteg3DExpConstants.ATTRIBUTE_LCD_REASON_FOR_FAILURE,
						LCDSAPIntegrationDataConstants.MSG_NETWORK_FAILURE);
			}
			ContextUtil.popContext(context);
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("ProcessWebServiceResponseForManufacturingAssembly END");
	}
}
