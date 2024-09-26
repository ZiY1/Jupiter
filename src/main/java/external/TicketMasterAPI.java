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

import entity.Constants;
import entity.Item;
import entity.Item.ItemBuilder;

public class TicketMasterAPI {
	
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
		
		if (event.isNull(Constants.EMBEDDED)) {
			return emptyAddress;
		}
		
		JSONObject embedded = event.getJSONObject(Constants.EMBEDDED);
		
		if (embedded.isNull(Constants.VENUES)) {
			return emptyAddress;
		}
		
		JSONArray venues = embedded.getJSONArray(Constants.VENUES);
		
		for (int i = 0; i < venues.length(); i++) {
			JSONObject venue = venues.getJSONObject(i);
					
			StringBuilder addressBuilder = new StringBuilder();
			
			if (!venue.isNull(Constants.ADDRESS)) {
				JSONObject address = venue.getJSONObject(Constants.ADDRESS);
				
				if (!address.isNull(Constants.LINE1)) {
					addressBuilder.append(address.getString(Constants.LINE1));
				}
				if (!address.isNull(Constants.LINE2)) {
					addressBuilder.append(" ");
					addressBuilder.append(address.getString(Constants.LINE2));
				}
				if (!address.isNull(Constants.LINE3)) {
					addressBuilder.append(" ");
					addressBuilder.append(address.getString(Constants.LINE3));
				}
			}
			
			if (!venue.isNull(Constants.CITY)) {
				JSONObject city = venue.getJSONObject(Constants.CITY);
				
				if (!city.isNull(Constants.NAME)) {
					addressBuilder.append(" ");
					addressBuilder.append(city.getString(Constants.NAME));
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
		
		if (event.isNull(Constants.IMAGES)) {
			return emptyImageUrl;
		}
		
		JSONArray images = event.getJSONArray(Constants.IMAGES);
		
		for (int i = 0; i < images.length(); i++) {
			JSONObject image = images.getJSONObject(i);
			
			if (!image.isNull(Constants.URL)) {
				return image.getString(Constants.URL);
			}
		}
		
		return emptyImageUrl;
	}

	// {"classifications" : [{"segment": {"name": "music"}}, ...]}
	private static Set<String> getCategories(JSONObject event) throws JSONException {
		Set<String> categories = new HashSet<>();
		
		if (event.isNull(Constants.CLASSIFICATIONS)) {
			return categories;
		}
		
		JSONArray classifications = event.getJSONArray(Constants.CLASSIFICATIONS);
		
		for (int i = 0; i < classifications.length(); i++) {
			JSONObject classification = classifications.getJSONObject(i);
			
			if (classification.isNull(Constants.SEGMENT)) {
				continue;
			}
			
			JSONObject segment = classification.getJSONObject(Constants.SEGMENT);
			
			if (!segment.isNull(Constants.NAME)) {
				categories.add(segment.getString(Constants.NAME));
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
			
			if (!event.isNull(Constants.NAME)) {
				builder.setName(event.getString(Constants.NAME));
			}
			
			if (!event.isNull(Constants.ID)) {
				builder.setItemId(event.getString(Constants.ID));
			}
			
			if (!event.isNull(Constants.URL)) {
				builder.setUrl(event.getString(Constants.URL));
			}
			
			if (!event.isNull(Constants.RATING)) {
				builder.setRating(event.getDouble(Constants.RATING));
			}
			
			if (!event.isNull(Constants.DISTANCE)) {
				builder.setDistance(event.getDouble(Constants.DISTANCE));
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
        	keyword = Constants.DEFAULT_KEYWORD;
        }
        
        try {
        	keyword = java.net.URLEncoder.encode(keyword, "UTF-8");
        } catch (Exception e) {
        	e.printStackTrace();
        }
        
        String geoHash = GeoHash.encodeGeohash(lat, lon, 8);
        
        String query = String.format(Constants.DEFAULT_QUERY, 
        		Constants.API_KEY, geoHash, keyword, Constants.DEFAULT_QUERY_RADIUS);
        
        HttpURLConnection connection = null;
        try {
        	connection = (HttpURLConnection) new URL(Constants.TICKET_MASTER_URL + "?" + query).openConnection();
        	connection.setRequestMethod("GET");
        	int responseCode = connection.getResponseCode();
        	
        	System.out.println("\nSending 'GET' request to URL: " + Constants.TICKET_MASTER_URL + "?" + query);
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
            	if (obj.isNull(Constants.EMBEDDED)) {
            		throw new Exception();
            	}
            	
            	JSONObject embedded = obj.getJSONObject(Constants.EMBEDDED);
            	JSONArray events = embedded.getJSONArray(Constants.EVENTS);
            	
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
