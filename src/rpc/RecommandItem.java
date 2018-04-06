package rpc;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import algorithm.GeoRecommendation;
import entity.Item;

/**
 * Servlet implementation class RecommandItem
 */
@WebServlet("/recommendation")
public class RecommandItem extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public RecommandItem() {
        super();
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String userId = request.getParameter("user_id");
		double lat = Double.parseDouble(request.getParameter("lat"));
		double lon = Double.parseDouble(request.getParameter("lon"));
		GeoRecommendation recommendation = new GeoRecommendation();
		List<Item> items = recommendation.recommendItems(userId, lat, lon);

		JSONArray result = new JSONArray();
		try {
			for (Item item : items) {
				result.put(item.toJSONObject());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		RpcHelper.writeJsonArray(response, result);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
	}

}
