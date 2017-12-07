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
	final static String[] record_headers = new String[]{
		"E","B","C","D","F","G","E","E","E","E","E","E","E","E","E","E","E"};

	// Test endpoints
	//final static String endpoint1 = "https://requestb.in/1fiesf61";
	final static String endpoint3 = "https://n20bcm7mi0.execute-api.us-east-1.amazonaws.com/dev/molspec-hackathon-publish-device-data-record";

	// Logging
	private final static Logger LOGGER = Logger.getLogger(Main.class.getName());

	public static void main(String[] args) throws InterruptedException, UnsupportedEncodingException {
		LOGGER.setLevel(Level.INFO);

		// Load csv file
		ClassLoader classloader = Main.class.getClassLoader();
		InputStream is = classloader.getResourceAsStream(("data.csv"));

		// Map csv lines into dictionaries
		List<HashMap> records = processFile(is);
		for(HashMap record : records) {
			// Make request
			doPOST(endpoint3, record);

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

	public static void doPOST(String request_url, HashMap record) throws UnsupportedEncodingException {
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
