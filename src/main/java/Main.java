import com.google.gson.Gson;
import java.io.*;
import java.net.*;
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
	//final static String endpoint1 = "https://requestb.in/1fiesf61";
	private final static String endpoint3 = "https://n20bcm7mi0.execute-api.us-east-1.amazonaws.com/dev/molspec-hackathon-publish-device-data-record";

	// Logging
	private final static Logger LOGGER = Logger.getLogger(Main.class.getName());
	private static ClassLoader classloader;

	public static void main(String[] args) throws UnsupportedEncodingException, InterruptedException {
		LOGGER.setLevel(Level.INFO);
		classloader = Main.class.getClassLoader();

		// Voltage
		while (true)
			streamDataToEndpoint(voltage_filename);
	}

	public static void streamDataToEndpoint(String data_records_source) throws InterruptedException, UnsupportedEncodingException {
		// Load csv file
		InputStream is = classloader.getResourceAsStream((data_records_source));

		// Map csv lines into dictionaries
		List<HashMap> records = processFile(is);
		for(HashMap record : records) {
			// Make request
			publishMessage(endpoint3, record);

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

	public static void publishMessage(String request_url, HashMap record) throws UnsupportedEncodingException {
		String message = new Gson().toJson(record);
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
			os.write(message);
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
