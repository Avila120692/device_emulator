import com.google.gson.Gson;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class Main {
	// CSV headers
	private final static String[] record_headers = new String[]{
		"mod_sn","group","test","value","date","sn","comment","modsn","Tbd","Tmod"};

	// Data files
	private final static String voltage_filename = "ifg_height.csv";
	private final static String noise_filename = "noise.csv";

	// Test endpoints
	final static String instrument_server_endpoint = "http://10.161.36.181:9090/instrument/diagnostics";
	final static String data_publish_endpoint = "https://v6xj2yycva.execute-api.us-east-1.amazonaws.com/prod/hackathon-instrument-post";


	// Logging
	private final static Logger LOGGER = Logger.getLogger(Main.class.getName());
	private static ClassLoader classloader;

	public static void main(String[] args) throws UnsupportedEncodingException, InterruptedException {
		LOGGER.setLevel(Level.INFO);
		classloader = Main.class.getClassLoader();

		// Instrument server (Pete's endpoint)
		while(true){
			JSONArray response_array = readDataFromEndpoint(instrument_server_endpoint);
			sendMessage(data_publish_endpoint, response_array);

			// Wait
			TimeUnit.SECONDS.sleep(5);
		}


		// Voltage
//		publishDataToEndpoint(voltage_filename);
//		publishDataToEndpoint(noise_filename);
	}


	public static JSONArray readDataFromEndpoint(String request_url){
		try {
			URL url = new URL(request_url);
			HttpURLConnection con = (HttpURLConnection) url.openConnection();

			con.setRequestMethod("GET");
			int responseCode = con.getResponseCode();

			LOGGER.info("HTTP - GET Response Code : " + responseCode);

			BufferedReader in = new BufferedReader(
				new InputStreamReader(con.getInputStream()));
			String inputLine;
			StringBuffer response = new StringBuffer();

			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			in.close();

			// Capture timestamp
			Instant instant = Instant.now();
			long timeStampMillis = instant.toEpochMilli();

			// Response
			JSONArray json_array = new JSONArray(response.toString());

			for (int i = 0; i < json_array.length(); i++) {
				JSONObject jsonObj = json_array.getJSONObject(i);

				jsonObj.put("dateTime", timeStampMillis);
				jsonObj.put("instrumentName", "fake_instrument");
				jsonObj.put("group", jsonObj.get("Group"));
				jsonObj.put("label", jsonObj.get("Label"));
				jsonObj.put("value", jsonObj.get("CurrentValue"));

				jsonObj.remove("Group");
				jsonObj.remove("Label");
				jsonObj.remove("Max");
				jsonObj.remove("Min");
				jsonObj.remove("CurrentValue");
				jsonObj.remove("Type");
			}
			return json_array;

		}catch(MalformedURLException e){
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static void sendMessage(String request_url, JSONArray payload) throws UnsupportedEncodingException {
		try{
			URL url = new URL(request_url);
			HttpURLConnection con = (HttpURLConnection) url.openConnection();

			// Prepare request
			con.setRequestMethod("POST");
			con.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
			con.setRequestProperty("Content-Length", String.valueOf(payload));
			con.setDoOutput(true);

			// Write
			OutputStreamWriter os = new OutputStreamWriter(con.getOutputStream());

			for (int i = 0; i < payload.length(); i++) {
				JSONObject jsonObj = payload.getJSONObject(i);

				LOGGER.info("JSON OBJ: " + jsonObj.toString());
				os.write(jsonObj.toString());

				// Response
				int responseCode = con.getResponseCode();
				LOGGER.info("Sending 'POST' request to URL : " + url);
				LOGGER.info("HTTP - POST Response Code : " + responseCode);
			}
			os.flush();
			os.close();

		}catch(MalformedURLException e){
			e.printStackTrace();
		} catch (ProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void publishDataToEndpoint(String target_endpoint, JSONArray array) throws InterruptedException, UnsupportedEncodingException {
		// POST
		sendMessage(target_endpoint, array);
	}


	public static void publishDataToEndpoint(String target_endpoint, String data_sourcefile) throws InterruptedException, UnsupportedEncodingException {
		// Load csv file
		InputStream is = classloader.getResourceAsStream((data_sourcefile));

		// Map csv lines into dictionaries
		List<HashMap> records = processFile(is);
		for(HashMap record : records) {
			// Make request
			JSONArray json_array = new JSONArray();
			JSONObject jsonObject = new JSONObject(record);
			json_array.put(jsonObject);

			sendMessage(target_endpoint, json_array);

			// Wait
			TimeUnit.SECONDS.sleep(5);
		}
	}

	public static List<HashMap> processFile(InputStream is) {
		List<HashMap> record_list = new ArrayList<>();
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(is));

			// Function : Map line to dictionary
			record_list = br.lines().skip(1).map(mapToRecordDictionary).collect(Collectors.toList());
			br.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return record_list;
	}

	public static Function<String, HashMap> mapToRecordDictionary = (line) -> {
		String[] values = line.split(",");
		HashMap record = new HashMap();
		for (int i=0; i < values.length; i++){
			record.put(record_headers[i], values[i]);
		}

		return record;
	};

	public static void sendMessage1(String request_url, HashMap record) throws UnsupportedEncodingException {
		JSONObject message = new JSONObject(record);
		System.out.println();
		LOGGER.info("JSON: " + message);

		try{
			URL url = new URL(request_url);
			HttpURLConnection con = (HttpURLConnection) url.openConnection();

			// Prepare request
			con.setRequestMethod("POST");
			con.setRequestProperty("Content-Type", "\"Content-Type\", \"application/x-www-form-urlencoded");
			con.setRequestProperty("Content-Length", String.valueOf(message));
			con.setDoOutput(true);

			// Write
			OutputStreamWriter os = new OutputStreamWriter(con.getOutputStream());
			os.write(message.toString());
			os.flush();
			os.close();

			// Response
			int responseCode = con.getResponseCode();
			LOGGER.info("Sending 'POST' request to URL : " + url);
			LOGGER.info("Response Code : " + responseCode);

		}catch(MalformedURLException e){
			e.printStackTrace();
		} catch (ProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
