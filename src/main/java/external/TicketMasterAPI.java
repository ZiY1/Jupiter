package external;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.json.JSONArray;
import org.json.JSONObject;

public class TicketMasterAPI {
	private static final String URL = "https://app.ticketmaster.com/discovery/v2/events.json";
	private static final String DEFAULT_KEYWORD = ""; // no restriction
	private static final String DEFAULT_QUERY_RADIUS = "50";
	private static final String API_KEY = "A76y8ioHzsApuCvysyQQS6ZYzmft3Z6o";
	
	
    public JSONArray search(double lat, double lon, String keyword) {
    	JSONArray searchResult = new JSONArray();
    	
        if (keyword == null) {
        	keyword = DEFAULT_KEYWORD;
        }
        
        try {
        	keyword = java.net.URLEncoder.encode(keyword, "UTF-8");
        } catch (Exception e) {
        	e.printStackTrace();
        }
        
        String geoHash = GeoHash.encodeGeohash(lat, lon, 8);
        
        String query = String.format("apikey=%s&geoPoint=%s&keyword=%s&radius=%s", 
        		API_KEY, geoHash, keyword, DEFAULT_QUERY_RADIUS);
        
        HttpURLConnection connection = null;
        try {
        	connection = (HttpURLConnection) new URL(URL + "?" + query).openConnection();
        	connection.setRequestMethod("GET");
        	int responseCode = connection.getResponseCode();
        	
        	System.out.println("\nSending 'GET' request to URL: " + URL + "?" + query);
        	System.out.println("Response code: " + responseCode);
        	
        	if (responseCode != 200) {
        		// TODO: handle it
        	}
        	
        	// Try-with-resources for BufferedReader to ensure it gets closed
        	try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
        		String inputline;
            	StringBuilder response = new StringBuilder();
            	while ((inputline = in.readLine()) != null) {
            		response.append(inputline);
            	}
            	
            	JSONObject obj = new JSONObject(response.toString());
            	if (obj.isNull("_embedded")) {
            		throw new Exception();
            	}
            	
            	JSONObject embedded = obj.getJSONObject("_embedded");
            	JSONArray events = embedded.getJSONArray("events");
            	
            	searchResult = events;
        	}
        	
        } catch (Exception e) {
        	e.printStackTrace();
        } finally {
        	if (connection != null) {
        		connection.disconnect();
        	}
        }
        
        return searchResult;
    }
    
    // queryAPI is used for debugging
    private void queryAPI(double lat, double lon) {
		JSONArray events = search(lat, lon, null);
		try {
		    for (int i = 0; i < events.length(); i++) {
		        JSONObject event = events.getJSONObject(i);
		        System.out.println(event);
		    }
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
    
    // Main entry for sample TicketMaster API requests
	public static void main(String[] args) {
		TicketMasterAPI tmApi = new TicketMasterAPI();
		// New York, NY
		tmApi.queryAPI(40.730610, -73.935242);
	}
}
