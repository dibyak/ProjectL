package com.lcd.sapintegration.util;

import java.util.Base64;

import javax.json.JsonObject;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import com.matrixone.apps.domain.util.EnoviaResourceBundle;
import com.matrixone.apps.domain.util.FrameworkException;
import com.matrixone.apps.framework.ui.UIUtil;

import matrix.db.Context;

public class LCDSAPIntegrationSendJsonToSAP {
	
	public static final String LCD_3DX_SAP_INTEGRATION_KEY = "LCD_3DXSAPStringResource_en";
	private static final Base64.Encoder BASE64_ENCODER = Base64.getEncoder();

	private static String language;
	private static String url;
	private static String xcsrfToken;
	private static String cookie;
	private static CloseableHttpClient httpClient;
	
	/**
	 * Invoking SAP Web service with GET Method to get the x-csrf-token and cookies
	 * error or send empty string.
	 *
	 * @param CloseableHttpClient
	 * @return String Array in arguments ( x-csrf-token and cookies )
	 * @throws FrameworkException 
	 * @throws Exception
	 */
	public static int callGETService(Context context) throws FrameworkException {
		System.out.println("callGETService()..... START");

		language = context.getSession().getLanguage();
		
		String strStatusLine = null;
		int responseCode = 0;

		String userName = EnoviaResourceBundle.getProperty(context, LCD_3DX_SAP_INTEGRATION_KEY,
				"LCD_3DXSAPStringResource_en.SAP.UserName", language);

		String password = EnoviaResourceBundle.getProperty(context, LCD_3DX_SAP_INTEGRATION_KEY,
				"LCD_3DXSAPStringResource_en.SAP.Password", language);

		url = EnoviaResourceBundle.getProperty(context, LCD_3DX_SAP_INTEGRATION_KEY,
				"LCD_3DXSAPStringResource_en.SAP.WebServiceURL.DEV", language);

		httpClient = HttpClients.createDefault();

		try {
			// STEP : Creating Authorization Header for calling SAP webService using GET
			// method
			String authString = userName + ":" + password;
			String authStringEnc = BASE64_ENCODER.encodeToString(authString.getBytes());

			// STEP : Creating HttpGet Object with SAP webService URL
			HttpGet getURL = new HttpGet(url);

			String authorization = EnoviaResourceBundle.getProperty(context, LCD_3DX_SAP_INTEGRATION_KEY,
					"LCD_3DXSAPStringResource_en.SAP.Authorization", language);
			xcsrfToken = EnoviaResourceBundle.getProperty(context, LCD_3DX_SAP_INTEGRATION_KEY,
					"LCD_3DXSAPStringResource_en.SAP.XCSRFTOKEN", language);

			// STEP : Creating request header for SAP WebService GET Method call
			getURL.setHeader(authorization, "Basic " + authStringEnc);
			getURL.setHeader(xcsrfToken, "fetch");

			// STEP : Invoking SAP webService with GET Method
			HttpResponse response = httpClient.execute(getURL);

			// STEP : Collecting the acknowledgement from SAP webService
			responseCode = response.getStatusLine().getStatusCode();
			strStatusLine = response.getStatusLine().toString();
			if (responseCode == HttpStatus.SC_OK) { // success

				// STEP : Collecting the x-csrf-token and cookies from SAP webService Response
				Header[] resHeaders = response.getAllHeaders();
				int index = -1;
				index = resHeaders[1].getValue().indexOf(":");
				String strCookie = resHeaders[1].getValue().substring(index + 1);
				String xcsrftoken = null;
				for (Header header : resHeaders) {
					if (xcsrfToken.equalsIgnoreCase(header.getName())) {
						xcsrftoken = header.getValue();
					}
				}
				if (UIUtil.isNotNullAndNotEmpty(xcsrftoken)) {
					xcsrfToken = xcsrftoken;
					cookie = strCookie;
				}
			} else {
				System.out.println("GET request Failed , Response code ==  " + response.getStatusLine().getStatusCode());
				System.out.println("GET request Failed , Response Status Line ==  " + strStatusLine);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("callGETService()..... END");
		return responseCode;
	}
	
	
	/**
	 * This Method Accepts xcsrfToken and cookie retrieved from GET SAP webService
	 * Call calls Custom created Post web service in SAP
	 *
	 * @param Cookie              From SAP
	 * @param xcsrftoken          From SAP
	 * @param Input               JsonObject to SAP WebService
	 * @param CloseableHttpClient for Active session to call the SAP web service
	 * @return web service response
	 * @throws Exception
	 */
	public static int callPostService(Context context, JsonObject jEachPayloadObj) throws NullPointerException {
		System.out.println("callPostService()..... START");
		// STEP : Creating HttpPost Object with SAP webService URL
		HttpPost postURL = new HttpPost(url);
		int intResponseCode = 0;
		try {
			// STEP : Creating request header for SAP WebService POST Method call
			String xcsrftoken = EnoviaResourceBundle.getProperty(context, LCD_3DX_SAP_INTEGRATION_KEY,
					"LCD_3DXSAPStringResource_en.SAP.XCSRFTOKEN", language);
//			String Cookie = EnoviaResourceBundle.getProperty(context, LCD_3DX_SAP_INTEGRATION_KEY,
//					"LCD_3DXSAPStringResource_en.SAP.COOKIE", language);
			String contentType = EnoviaResourceBundle.getProperty(context, LCD_3DX_SAP_INTEGRATION_KEY,
					"LCD_3DXSAPStringResource_en.SAP.CONTENTTYPE", language);

			StringEntity params = new StringEntity(jEachPayloadObj.toString());
			postURL.setHeader(cookie, cookie);
			postURL.setHeader(contentType, "application/json");
			postURL.setHeader(xcsrftoken, xcsrfToken);
			postURL.setEntity(params);

			// STEP : Invoking SAP webService with Post Method
			HttpResponse response = httpClient.execute(postURL);
			System.out.println("postURL -- " + postURL);
			intResponseCode = response.getStatusLine().getStatusCode();
			if (intResponseCode == HttpStatus.SC_OK) { // success
				// STEP : Collecting the acknowledgement from SAP webService
			} else {
				System.out.println("POST request failed , response code ==  " + response.getStatusLine().getStatusCode());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("callPostService()..... END");
		return intResponseCode;
	}
}
