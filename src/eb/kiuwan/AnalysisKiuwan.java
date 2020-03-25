package eb.kiuwan;

import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.apache.commons.codec.binary.Base64;
import org.json.JSONArray;
import org.json.JSONObject;


public class AnalysisKiuwan {

	// API endpoints
	private final static String KIUWAN_BASE_URL = "/saas/rest/v1";
	private final static String APPLICATIONS_URL = "/applications";
	private final static String ANALYSIS_URL = "/applications/analyses";
	private final static String DELIVERIES_URL = "/applications/deliveries";
	private final static String ANALYSISRESULTS_URL = "/apps/analysis/";				
	
	// Excel column names
	private static String[] columns = { "App Name", "Analysis Type", "LoC", "Date (UTC)", "Time (UTC)", "Analysis Code" };


	private static String EncodeUserPass (String user, String pass) {
		String authString = user + ":" + pass;
		byte[] authEncBytes = Base64.encodeBase64(authString.getBytes());
		String authStringEnc = new String(authEncBytes);
		return authStringEnc;
	}


	private static String RestApiCall(String userNameKiuwan, String passwordKiuwan, String url, ArrayList <String[]> parameters) {
		// Generic call to Kiuwan Rest-Api
		HttpClient httpclient = HttpClientBuilder.create().build();
		RequestConfig params = RequestConfig.custom().setCookieSpec(CookieSpecs.IGNORE_COOKIES).build();

		URIBuilder builder = null;
		try {
			builder = new URIBuilder(url);
		} catch (URISyntaxException e1) {
			System.out.println("URISyntaxException:");
			e1.printStackTrace();
		}
		for (String[] parameter : parameters) {
			builder.setParameter(parameter[0], parameter[1]);
		}

		HttpGet getCall = null;
		try {
			getCall = new HttpGet(builder.build());
		} catch (URISyntaxException e1) {
			System.out.println("URISyntaxException:");
			e1.printStackTrace();
		}
		getCall.setConfig(params);
		String authStringEnc = EncodeUserPass(userNameKiuwan, passwordKiuwan);
		getCall.addHeader("Authorization", "Basic " + authStringEnc);
		getCall.addHeader("Content-Type", "application/json");

		HttpResponse response = null;
		try {
			response = httpclient.execute(getCall);
		} catch (ClientProtocolException e) {
			System.out.println("ClientProtocolException:");
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("IOException:");
			e.printStackTrace();
		}

		StatusLine status = response.getStatusLine();
		if (status.getStatusCode() == 200) {
			//System.out.println("Login and get OK");
		} else {
			System.out.println("Login and get NOK: Exiting");
			System.out.println(status);
			return null;
		}	

		String json_response = "";
		try {
			json_response = EntityUtils.toString(response.getEntity());
		} catch (ParseException e) {
			System.out.println("ParseException:");
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("IOException:");
			e.printStackTrace();
		}

		return json_response;
	}

	private static ArrayList<String> GetDateCodes(String json_response, int maxDays) {
		ArrayList<String> dateCodes = new ArrayList<String>();
		JSONArray jsonArr = new JSONArray(json_response);
		Date today = new Date();

		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
		df.setTimeZone(TimeZone.getTimeZone("UTC"));

		for (int i = 0; i < jsonArr.length(); i++) {
			JSONObject jsonObj = jsonArr.getJSONObject(i);
			String code = jsonObj.getString("code");
			String creationDate = jsonObj.getString("creationDate");

			if (maxDays > 0) {
				Date cDate = new Date();
				try {
					cDate = df.parse(creationDate);
				} catch (java.text.ParseException e) {
					System.out.println("ParseException:");
					e.printStackTrace();
				}
				long todayTime = today.getTime();
				long cTime = cDate.getTime();
				float diffTime = todayTime - cTime;
				float diffDays = diffTime / (1000 * 60 * 60 * 24);
				if (diffDays <= maxDays) {
					dateCodes.add(creationDate + "_" +code);
				}
			} else {
				dateCodes.add(creationDate + "_" +code);
			}
		}
		return dateCodes;
	}
	

	public static void main(String[] args) throws ClientProtocolException, IOException {
		
		if (args.length != 5) {
			System.out.println("Program must have 5 arguments: <user> <password> <Kiuwan server url> <maxDays> <xlsx path>");
			return;
		}
		
		XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet("Analysis History");
		
        Row headerRow = sheet.createRow(0);

        for (int i = 0; i < columns.length; i++) {
          headerRow.createCell(i).setCellValue(columns[i]);
        }
        
		int rowNum = 1;

		ArrayList <String[]> parametersCall = new ArrayList <String[]>();
		String url = "";
		String json_apps_response = "";

		String usernameKiuwan = args[0];
		String passwordKiuwan = args[1];
		String baseUrl = args[2] + KIUWAN_BASE_URL;
		int maxDays = Integer.parseInt(args[3]);
		String path = args[4];
		
		// Get all application names
		parametersCall.clear();
		url = baseUrl + APPLICATIONS_URL;
		json_apps_response = RestApiCall(usernameKiuwan, passwordKiuwan, url, parametersCall);
		
		JSONArray jsonArrApp = new JSONArray(json_apps_response);
		
		for (int i = 0; i < jsonArrApp.length(); i++) {
			JSONObject jsonObj = jsonArrApp.getJSONObject(i);
			String appName = jsonObj.getString("name");
			System.out.println("\n--------------------------------");
			System.out.println("--------- "+appName+"-----------");
			System.out.println("--------------------------------");

			// Retrieve analysis codes
			System.out.println("BASELINES:");
			System.out.println("---------\n");

			url = baseUrl + ANALYSIS_URL;
			parametersCall.clear();
			parametersCall.add(new String[]{"application", appName});
			String json_response = RestApiCall(usernameKiuwan, passwordKiuwan, url, parametersCall);
			ArrayList<String> analysisDate_Codes = GetDateCodes(json_response, maxDays);
			
			for (String analysisDate_Code : analysisDate_Codes) {

				System.out.println(analysisDate_Code);
				
				String code = analysisDate_Code.substring(analysisDate_Code.indexOf("_") + 1);				

				// Get results for an analysis code
				url = baseUrl + ANALYSISRESULTS_URL + code;
				parametersCall.clear();
				parametersCall.add(new String[]{"code", code});
				String res = RestApiCall(usernameKiuwan, passwordKiuwan, url, parametersCall);
				JSONObject jsonobj = new JSONObject(res);
				
				Row row = sheet.createRow(rowNum++);

				try {
					String date = jsonobj.getString("date");
					long lines = jsonobj.getJSONArray("Main metrics").getJSONObject(5).getLong("value");
					System.out.println(lines+" Lines of Code on " + date +"\n");
					row.createCell(0).setCellValue(appName);
				    row.createCell(1).setCellValue("Baseline");
				    row.createCell(2).setCellValue(lines);
				    row.createCell(3).setCellValue(date.substring(0,10));
				    row.createCell(4).setCellValue(date.substring(11,19));
				    row.createCell(5).setCellValue(code);
				} catch(Exception e) {
					System.out.println("No analysis\n");
					rowNum--;
				}
				
			}

			// Retrieve deliveries codes
			System.out.println("DELIVERIES:");
			System.out.println("----------\n");

			url = baseUrl + DELIVERIES_URL;
			parametersCall.clear();
			parametersCall.add(new String[]{"application", appName});
			json_response = RestApiCall(usernameKiuwan, passwordKiuwan, url, parametersCall);
			ArrayList<String> deliveriesDate_Codes = GetDateCodes(json_response, maxDays);
			for (String deliveryDate_Code : deliveriesDate_Codes) {
				System.out.println(deliveryDate_Code);

				String code = deliveryDate_Code.substring(deliveryDate_Code.indexOf("_") + 1);

				// Get results for a delivery code
				url = baseUrl + ANALYSISRESULTS_URL + code;
				parametersCall.clear();
				parametersCall.add(new String[]{"code", code});
				String result = RestApiCall(usernameKiuwan, passwordKiuwan, url, parametersCall);
				JSONObject jsonobj = new JSONObject(result);
				
				Row row = sheet.createRow(rowNum++);
				
				try {
					String date = jsonobj.getString("date");
					long lines = jsonobj.getJSONArray("Main metrics").getJSONObject(5).getLong("value");
					System.out.println(lines+" Lines of Code on " + date +"\n");
					row.createCell(0).setCellValue(appName);
				    row.createCell(1).setCellValue("Delivery");
				    row.createCell(2).setCellValue(lines);
				    row.createCell(3).setCellValue(date.substring(0,10));
				    row.createCell(4).setCellValue(date.substring(11,19));
				    row.createCell(5).setCellValue(code);
				} catch(Exception e) {
					System.out.println("No analysis\n");
					rowNum--;
				}
			}
		}
		
		// Resize all columns to fit the content size
	    for (int i = 0; i < columns.length; i++) {
	      sheet.autoSizeColumn(i);
	    }

	    // Write the output to a xlsx file
	    FileOutputStream fileOut = new FileOutputStream(path);
	    workbook.write(fileOut);
	    fileOut.close();
	    workbook.close();

		System.out.println("===============END===============");
	}
}
