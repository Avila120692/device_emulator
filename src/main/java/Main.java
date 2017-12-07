import com.google.gson.Gson;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Main {

	// CSV headers
	final static String[] record_headers = new String[]{
		"E","B","C","D","F","G","E","E","E","E","E","E","E","E","E","E","E"};

	// Test endpoints
	final static String endpoint1 = "https://requestb.in/1fiesf61";
	//final static String endpoint2 = "http://127.0.0.1:8000";

	// TODO: Get relative path to /resources/*.csv
	final static String csv_filepath = "/Users/salvador.avila/javacode/device_emulator/src/main/resources/data.csv";

	public static void main(String[] args) throws InterruptedException, UnsupportedEncodingException {
		// Stream file
		List<HashMap> records = processFile(csv_filepath);
		for(HashMap record : records) {
			// Make request
			doPOST(endpoint1, record);

			// Wait
			TimeUnit.SECONDS.sleep(5);
		}
	}

	public static List<HashMap> processFile(String file_path) {
		List<HashMap> record_list = new ArrayList<>();
		try {
			File file = new File(file_path);
			InputStream file_stream = new FileInputStream(file);
			BufferedReader br = new BufferedReader(new InputStreamReader(file_stream));

			// Function : Map csv line to dictionary
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
		System.err.println("\nRecord: " + record.toString());
		String json = new Gson().toJson(record);
		System.out.println("JSON: " + json);

		try{
			URL url = new URL(request_url);
			HttpURLConnection con = (HttpURLConnection) url.openConnection();

			// Prepare request
			con.setRequestMethod("POST");
			con.setRequestProperty("Content-Type", "\"Content-Type\", \"application/x-www-form-urlencoded");
			con.setRequestProperty("Content-Length", String.valueOf(json));
			con.setDoOutput(true);

			// Write
			OutputStreamWriter os = new OutputStreamWriter(con.getOutputStream());
			os.write(json);
			os.flush();
			os.close();

			// Response
			int responseCode = con.getResponseCode();
			System.out.println("Sending 'POST' request to URL : " + url);
			System.out.println("Response Code : " + responseCode);

		}catch(MalformedURLException e){
			e.printStackTrace();
		} catch (ProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
