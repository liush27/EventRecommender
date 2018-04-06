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
import external.ExternalAPI;
import external.ExternalAPIFactory;

public class MySQLConnection implements DBConnection {
	private Connection conn;

	public MySQLConnection() {
		try {
			Class.forName("com.mysql.jdbc.Driver").newInstance();// 比较老的写法
			conn = DriverManager.getConnection(MySQLDBUtil.URL);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void close() {
		if (conn != null) {
			try {
				conn.close();
			} catch (Exception e) {
				System.out.println(e.getMessage());
			}
		}
	}

	@Override
	public void setFavoriteItems(String userId, List<String> itemIds) {
		if (conn == null) {
			return;
		}
		String query = "INSERT INTO history (user_id, item_id) VALUES (?, ?)";
		try {
			PreparedStatement statement = conn.prepareStatement(query);
			for (String itemId : itemIds) {
				statement.setString(1, userId);
				statement.setString(2, itemId);
				statement.execute();
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
		String query = "DELETE FROM history WHERE user_id = ? and item_id = ?";
		try {
			PreparedStatement statement = conn.prepareStatement(query);
			for (String itemId : itemIds) {
				statement.setString(1, userId);
				statement.setString(2, itemId);
				statement.execute();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

	}

	@Override
	public Set<String> getFavoriteItemIds(String userId) {
		if (conn == null) {
			return null;
		}
		Set<String> itemIds = new HashSet<>();
		try {
			String sql = "SELECT item_id from history WHERE user_id = ?";
			PreparedStatement statement = conn.prepareStatement(sql);
			statement.setString(1, userId);
			ResultSet rs = statement.executeQuery();
			while (rs.next()) {
				String itemId = rs.getString("item_id");
				itemIds.add(itemId);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return itemIds;
	}

	@Override
	public Set<Item> getFavoriteItems(String userId) {
		if (conn == null) {
			return null;
		}
		Set<String> itemIds = getFavoriteItemIds(userId);
		Set<Item> favoriteItems = new HashSet<>();
		try {

			for (String itemId : itemIds) {
				String sql = "SELECT * from items WHERE item_id = ? ";
				PreparedStatement statement = conn.prepareStatement(sql);
				statement.setString(1, itemId);
				ResultSet rs = statement.executeQuery();
				ItemBuilder builder = new ItemBuilder();

				// Because itemId is unique and given one item id there should
				// have
				// only one result returned.
				if (rs.next()) {
					builder.setItemId(rs.getString("item_id"));
					builder.setName(rs.getString("name"));
					builder.setCity(rs.getString("city"));
					builder.setState(rs.getString("state"));
					builder.setCountry(rs.getString("country"));
					builder.setZipcode(rs.getString("zipcode"));
					builder.setDate(rs.getString("date"));
					builder.setPriceRange(rs.getString("priceRange"));
					builder.setRating(rs.getDouble("rating"));
					builder.setAddress(rs.getString("address"));
					builder.setLatitude(rs.getDouble("latitude"));
					builder.setLongitude(rs.getDouble("longitude"));
					builder.setDescription(rs.getString("description"));
					builder.setSnippet(rs.getString("snippet"));
					builder.setSnippetUrl(rs.getString("snippet_url"));
					builder.setImageUrl(rs.getString("image_url"));
					builder.setUrl(rs.getString("url"));
				}

				Set<String> categories = getCategories(itemId);
				builder.setCategories(categories);
				favoriteItems.add(builder.build());
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return favoriteItems;

	}

	@Override
	public Set<String> getCategories(String itemId) {
		Set<String> categories = new HashSet<>();
		try {
			String sql = "SELECT category from categories WHERE item_id = ? ";
			PreparedStatement statement = conn.prepareStatement(sql);
			statement.setString(1, itemId);
			ResultSet rs = statement.executeQuery();
			while (rs.next()) {
				categories.add(rs.getString("category"));
			}
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		return categories;

	}

	@Override
	public List<Item> searchItems(String userId, double lat, double lon, String term) {
		// Connect to external API
		ExternalAPI api = ExternalAPIFactory.getExternalAPI(); // moved here
		List<Item> items = api.search(lat, lon, term);
		for (Item item : items) {
			// Save the item into our own db.
			saveItem(item);
		}
		return items;
	}

	@Override
	public void saveItem(Item item) {
		if (conn == null) {
			return;
		}
		try {
			// First, insert into items table
			// Ignore : 如果有重复的id，则忽略之后的record
			String sql = "INSERT IGNORE INTO items VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

			PreparedStatement statement = conn.prepareStatement(sql);
			statement.setString(1, item.getItemId());
			statement.setString(2, item.getName());
			statement.setString(3, item.getCity());
			statement.setString(4, item.getState());
			statement.setString(5, item.getCountry());
			statement.setString(6, item.getZipcode());
			statement.setString(7, item.getDate());
			statement.setString(8, item.getPriceRange());
			statement.setDouble(9, item.getRating());
			statement.setString(10, item.getAddress());
			statement.setDouble(11, item.getLatitude());
			statement.setDouble(12, item.getLongitude());
			statement.setString(13, item.getDescription());
			statement.setString(14, item.getSnippet());
			statement.setString(15, item.getSnippetUrl());
			statement.setString(16, item.getImageUrl());
			statement.setString(17, item.getUrl());
			statement.execute();

			// Second, update categories table for each category.
			sql = "INSERT IGNORE INTO categories VALUES (?,?)";
			for (String category : item.getCategories()) {
				statement = conn.prepareStatement(sql);
				statement.setString(1, item.getItemId());
				statement.setString(2, category);
				statement.execute();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

	}
}
