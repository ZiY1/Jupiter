package db.mysql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import db.DBConnection;
import entity.Item;
import entity.Item.ItemBuilder;
import external.TicketMasterAPI;

public class MySQLConnection implements DBConnection {
	private Connection conn;
	
	public MySQLConnection() {
		try {
			Class.forName("com.mysql.jdbc.Driver").newInstance();
			conn = DriverManager.getConnection(MySQLDBUtil.URL);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void close() {
		if (conn == null) {
			return;
		}
		
		try {
			conn.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void setFavoriteItems(String userId, List<String> itemIds) {
		if (conn == null) {
			return;
		}
		
		try {
			String sql = "INSERT IGNORE INTO history (user_id, item_id) VALUES (?, ?)";
			PreparedStatement stmt = conn.prepareStatement(sql);
			for (String itemId : itemIds) {
				stmt.setString(1, userId);
				stmt.setString(2, itemId);
				stmt.execute();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void unsetFavoriteItems(String userId, List<String> itemIds) {
		if (conn == null) {
			return;
		}
		
		try {
			String sql = "DELETE FROM history WHERE user_id = ? AND item_id = ?";
			PreparedStatement stmt = conn.prepareStatement(sql);
			for (String itemId : itemIds) {
				stmt.setString(1, userId);
				stmt.setString(2, itemId);
				stmt.execute();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

	}

	@Override
	public Set<String> getFavoriteItemIds(String userId) {
		if (conn == null) {
			return new HashSet<>();
		}
		
		Set<String> favoriteItemIds = new HashSet<>();
		
		try {
			String sql = "SELECT item_id FROM history WHERE user_id = ?";
			PreparedStatement stmt = conn.prepareStatement(sql);
			stmt.setString(1, userId);
			
			ResultSet rs = stmt.executeQuery();
			
			while (rs.next()) {
				favoriteItemIds.add(rs.getString("item_id"));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return favoriteItemIds;
	}

	@Override
	public Set<Item> getFavoriteItems(String userId) {
		if (conn == null) {
			return new HashSet<>();
		}
		
		Set<Item> favoriteItems = new HashSet<>();
		Set<String> itemIds = getFavoriteItemIds(userId);
		
		try {
			String sql = "SELECT * FROM items WHERE item_id = ?";
			PreparedStatement stmt = conn.prepareStatement(sql);
			for (String itemId : itemIds) {
				stmt.setString(1, itemId);
				
				ResultSet rs = stmt.executeQuery();
				
				ItemBuilder itemBuilder = new ItemBuilder();
				
				while (rs.next()) {
					itemBuilder.setItemId(rs.getString("item_id"));
					itemBuilder.setName(rs.getString("name"));
					itemBuilder.setRating(rs.getDouble("rating"));
					itemBuilder.setAddress(rs.getString("address"));
					itemBuilder.setImageUrl(rs.getString("image_url"));
					itemBuilder.setUrl(rs.getString("url"));
					itemBuilder.setCategories(getCategories(itemId));
					itemBuilder.setDistance(rs.getDouble("distance"));
					
					favoriteItems.add(itemBuilder.build());
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return favoriteItems;
	}

	@Override
	public Set<String> getCategories(String itemId) {
		if (conn == null) {
			return new HashSet<>();
		}
		
		Set<String> categories = new HashSet<>();
		
		try {
			String sql = "SELECT category FROM categories WHERE item_id = ?";
			PreparedStatement stmt = conn.prepareStatement(sql);
			stmt.setString(1, itemId);
			
			ResultSet rs = stmt.executeQuery();
			
			while (rs.next()) {
				categories.add(rs.getString("category"));
			}
		} catch (SQLException e) {
			e.printStackTrace();
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
		if (conn == null) {
			// TODO: Tell front end save failed
			return;
		}
		
		try {
			String sql = "INSERT IGNORE INTO items VALUES (?, ?, ?, ?, ?, ?, ?)";
			// Use PreparedStatement to prevent SQL injection
			PreparedStatement stmt = conn.prepareStatement(sql);
			stmt.setString(1, item.getItemId());
			stmt.setString(2, item.getName());
			stmt.setDouble(3, item.getRating());
			stmt.setString(4, item.getAddress());
			stmt.setString(5, item.getImageUrl());
			stmt.setString(6, item.getUrl());
			stmt.setDouble(7, item.getDistance());
			stmt.execute();
			
			sql = "INSERT IGNORE INTO categories VALUES(?, ?)";
			stmt = conn.prepareStatement(sql);
			for (String category : item.getCategories()) {
				stmt.setString(1, item.getItemId());
				stmt.setString(2, category);
				stmt.execute();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

	}

	@Override
	public String getFullname(String userId) {
		String fullName = "";
		if (conn == null) {
			return fullName;
		}
		
		try {
			String sql = "SELECT first_name, last_name FROM users WHERE user_id = ?";
			PreparedStatement stmt = conn.prepareStatement(sql);
			stmt.setString(1, userId);
			
			ResultSet rs = stmt.executeQuery();
			
			if (rs.next()) {
				String firstName = rs.getString("first_name");
				String lastName = rs.getString("last_name");
				fullName = firstName + " " + lastName;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return fullName;
	}

	@Override
	public boolean verifyLogin(String userId, String password) {
//		if (conn == null) {
//			return false;
//		}
//		
//		try {
//			String sql = "SELECT password FROM users WHERE user_id = ?";
//			PreparedStatement stmt = conn.prepareStatement(sql);
//			stmt.setString(1, userId);
//			
//			ResultSet rs = stmt.executeQuery();
//			
//			if (rs.next()) {
//				String storedHashedPassword = rs.getString("password");
//				return password.equals(storedHashedPassword);
//			}
//		} catch (SQLException e) {
//			e.printStackTrace();
//		}
		
		return false;
	}
}
