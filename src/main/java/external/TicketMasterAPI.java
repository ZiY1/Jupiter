package external;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import entity.Item;
import entity.Item.ItemBuilder;

public class TicketMasterAPI {
	private static final String URL = "https://app.ticketmaster.com/discovery/v2/events.json";
	private static final String DEFAULT_KEYWORD = ""; // no restriction
	private static final String DEFAULT_QUERY_RADIUS = "50";
	private static final String API_KEY = "A76y8ioHzsApuCvysyQQS6ZYzmft3Z6o";
	
	/**
	 * Helper methods
	 */

	//  {
	//    "name": "event_name",
              //    "id": "12345",
              //    "url": "www.event.com",
	//    ...
	//    "_embedded": {
	//	    "venues": [
	//	        {
	//		        "address": {
	//		           "line1": "103 First St,",
	//		           "line2": "Suite 101",
	//		           "line3": "...",
	//		        },
	//		        "city": {
	//		        	"name": "New York"
	//		        }
	//		        ...
	//	        },
	//	        ...
	//	    ]
	//    }
	//    ...
	//  }
	private static String getAddress(JSONObject event) throws JSONException {
		String emptyAddress = "";
		
		if (event.isNull("_embedded")) {
			return emptyAddress;
		}
		
		JSONObject embedded = event.getJSONObject("_embedded");
		
		if (embedded.isNull("venues")) {
			return emptyAddress;
		}
		
		JSONArray venues = embedded.getJSONArray("venues");
		
		for (int i = 0; i < venues.length(); i++) {
			JSONObject venue = venues.getJSONObject(i);
					
			StringBuilder addressBuilder = new StringBuilder();
			
			if (!venue.isNull("address")) {
				JSONObject address = venue.getJSONObject("address");
				
				if (!address.isNull("line1")) {
					addressBuilder.append(address.getString("line1"));
				}
				if (!address.isNull("line2")) {
					addressBuilder.append(" ");
					addressBuilder.append(address.getString("line2"));
				}
				if (!address.isNull("line3")) {
					addressBuilder.append(" ");
					addressBuilder.append(address.getString("line3"));
				}
			}
			
			if (!venue.isNull("city")) {
				JSONObject city = venue.getJSONObject("city");
				
				if (!city.isNull("name")) {
					addressBuilder.append(" ");
					addressBuilder.append(city.getString("name"));
				}
			}
			
			if (!addressBuilder.toString().equals("")) {
				return addressBuilder.toString();
			}
		}
		
		return emptyAddress;
	}


	// {"images": [{"url": "www.example.com/my_image.jpg"}, ...]}
	private static String getImageUrl(JSONObject event) throws JSONException {
		String emptyImageUrl = "";
		
		if (event.isNull("images")) {
			return emptyImageUrl;
		}
		
		JSONArray images = event.getJSONArray("images");
		
		for (int i = 0; i < images.length(); i++) {
			JSONObject image = images.getJSONObject(i);
			
			if (!image.isNull("url")) {
				return image.getString("url");
			}
		}
		
		return emptyImageUrl;
	}

	// {"classifications" : [{"segment": {"name": "music"}}, ...]}
	private static Set<String> getCategories(JSONObject event) throws JSONException {
		Set<String> categories = new HashSet<>();
		
		if (event.isNull("classifications")) {
			return categories;
		}
		
		JSONArray classifications = event.getJSONArray("classifications");
		
		for (int i = 0; i < classifications.length(); i++) {
			JSONObject classification = classifications.getJSONObject(i);
			
			if (classification.isNull("segment")) {
				continue;
			}
			
			JSONObject segment = classification.getJSONObject("segment");
			
			if (!segment.isNull("name")) {
				categories.add(segment.getString("name"));
			}
		}
		
		return categories;
	}

	// Convert JSONArray to a list of item objects.
	private static List<Item> getItemList(JSONArray events) throws JSONException {
		List<Item> itemList = new ArrayList<>();
		for (int i = 0; i < events.length(); i++) {
			JSONObject event = events.getJSONObject(i);
			
			ItemBuilder builder = new ItemBuilder();
			
			if (!event.isNull("name")) {
				builder.setName(event.getString("name"));
			}
			
			if (!event.isNull("id")) {
				builder.setItemId(event.getString("id"));
			}
			
			if (!event.isNull("url")) {
				builder.setUrl(event.getString("url"));
			}
			
			if (!event.isNull("rating")) {
				builder.setRating(event.getDouble("rating"));
			}
			
			if (!event.isNull("distance")) {
				builder.setDistance(event.getDouble("distance"));
			}
			
			builder.setCategories(getCategories(event));
			builder.setAddress(getAddress(event));
			builder.setImageUrl(getImageUrl(event));
			
			itemList.add(builder.build());
		}
		
		return itemList;
	}

    public static List<Item> search(double lat, double lon, String keyword) {
    	List<Item> searchResult = new ArrayList<>();
    	
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
            	
            	searchResult = getItemList(events);
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
    private static void queryAPI(double lat, double lon) {
		List<Item> itemList = search(lat, lon, "Soccer");
		try {
		    for (Item item : itemList) {
		        System.out.println(item.toJSONObject());
		    }
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
    
    // Main entry for sample TicketMaster API requests
	public static void main(String[] args) {
		// New York, NY
		TicketMasterAPI.queryAPI(40.730610, -73.935242);
	}
}
