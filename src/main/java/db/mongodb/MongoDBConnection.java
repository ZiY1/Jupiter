package db.mongodb;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bson.Document;

import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;

import db.DBConnection;
import entity.Item;
import entity.Item.ItemBuilder;
import external.TicketMasterAPI;

public class MongoDBConnection implements DBConnection {
	private MongoClient mongoClient; 
	private MongoDatabase db;
	
	

	public MongoDBConnection() {
		// Connects to local mongodb server.
		mongoClient = new MongoClient();
		db = mongoClient.getDatabase(MongoDBUtil.DB_NAME);
	}

	@Override
	public void close() {
		if (mongoClient != null) {
			mongoClient.close();
		}
	}

	@Override
	public void setFavoriteItems(String userId, List<String> itemIds) {
		db.getCollection("users").updateOne(new Document("user_id", userId), 
										    new Document("$push", new Document("favorite", new Document("$each", itemIds))));
	}

	@Override
	public void unsetFavoriteItems(String userId, List<String> itemIds) {
		db.getCollection("users").updateOne(new Document("user_id", userId), 
			    							new Document("$pullAll", new Document("favorite", itemIds)));
	}

	@Override
	public Set<String> getFavoriteItemIds(String userId) {
		Set<String> favoriteItemIds = new HashSet<>();
		
		FindIterable<Document> iterable = db.getCollection("users").find(Filters.eq("user_id", userId));
		if (iterable != null && iterable.first().containsKey("favorite")) {
			@SuppressWarnings("unchecked")
			List<String> idslist = (List<String>) iterable.first().get("favorite");
			favoriteItemIds.addAll(idslist);
		}
		
		return favoriteItemIds;
	}

	@Override
	public Set<Item> getFavoriteItems(String userId) {
		Set<Item> favoriteItems = new HashSet<>();
		
		Set<String> itemIds = getFavoriteItemIds(userId);
		for (String itemId : itemIds) {
			FindIterable<Document> iterable = db.getCollection("items").find(Filters.eq("item_id", itemId));
			if (iterable == null) {
				continue;
			}
			
			Document doc = iterable.first();
			
			ItemBuilder itemBuilder = new ItemBuilder();
			itemBuilder.setItemId(doc.getString("item_id"));
			itemBuilder.setName(doc.getString("name"));
			itemBuilder.setRating(doc.getDouble("rating"));
			itemBuilder.setAddress(doc.getString("address"));
			itemBuilder.setImageUrl(doc.getString("image_url"));
			itemBuilder.setUrl(doc.getString("url"));
			itemBuilder.setCategories(getCategories(itemId));
			itemBuilder.setDistance(doc.getDouble("distance"));
			
			favoriteItems.add(itemBuilder.build());
		}
		
		return favoriteItems;
	}

	@Override
	public Set<String> getCategories(String itemId) {
		Set<String> categories = new HashSet<>();
		
		FindIterable<Document> iterable = db.getCollection("items").find(Filters.eq("item_id", itemId));
		if (iterable.first() != null && iterable.first().containsKey("categories")) {
			@SuppressWarnings("unchecked")
			List<String> categoriesList = (List<String>) iterable.first().get("categories");
			categories.addAll(categoriesList);
		}
		
		return categories;
	}

	@Override
	public List<Item> searchItems(double lat, double lon, String term) {
		List<Item> items = TicketMasterAPI.search(lat, lon, term);
		for (Item item : items) {
			saveItem(item);
		}
		return items;
	}

	@Override
	public void saveItem(Item item) {
		FindIterable<Document> iterable = db.getCollection("items").find(Filters.eq("item_id", item.getItemId()));
		
		if (iterable.first() != null) {
			return;
		}
		
		db.getCollection("items").insertOne(new Document().append("item_id", item.getItemId())
														  .append("name", item.getName())
														  .append("rating", item.getRating())
														  .append("address", item.getAddress())
														  .append("image_url", item.getImageUrl())
														  .append("url", item.getUrl())
														  .append("distance", item.getDistance())
														  .append("categories", item.getCategories()));

	}

	@Override
	public String getFullname(String userId) {
		FindIterable<Document> iterable = db.getCollection("users").find(Filters.eq("user_id", userId));
		if (iterable.first()!= null) {
			Document doc = iterable.first();
			return doc.getString("first_name") + " " + doc.getString("last_name");
		}
		
		return "";
	}

	@Override
	public boolean verifyLogin(String userId, String password) {
		FindIterable<Document> iterable = db.getCollection("users").find(Filters.eq("user_id", userId)); 
		if (iterable.first() != null) {
			Document doc = iterable.first();
			return doc.getString("password").equals(password);
		}
		return false;
	}

}
