package com.navmad.zappos;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Scanner;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.navmad.zappos.model.Product;

public class Main {
	//to maintain the list of items we found to sum upto the given amount
	static HashSet<ArrayList<Product>> hs = new HashSet<ArrayList<Product>>();

	static double amt;
	static int num;
	static String url;

	static String excludes = "[\"brandName\"," + "\"styleId\","
			+ "\"productUrl\"," + "\"percentOff\"," + "\"isHighRes\","
			+ "\"thumbnailImageUrl\"," + "\"originalPrice\"]";

	public static ArrayList<Product> jsonParse(JSONArray jo)
			throws JSONException {
		int count = jo.length();
		ArrayList<Product> products = new ArrayList<Product>();
		for (int i = 0; i < count; i++) {
			Product p = new Product();
			String s = (jo.getJSONObject(i).getString("price"));
			p.setPrice(Double.parseDouble(s.replace("$", "").replace(",", "")));
			p.setId(Integer
					.parseInt(jo.getJSONObject(i).getString("productId")));
			p.setName(jo.getJSONObject(i).getString("productName"));
			p.setColorId(jo.getJSONObject(i).getString("colorId"));
			products.add(p);
		}
		return products;
	}

	public static HttpResponse getResponse() throws ClientProtocolException,
			IOException {

		HttpClient client = HttpClientBuilder.create().build();
		HttpGet request = new HttpGet(url);

		HttpResponse response = client.execute(request);
		int responseCode = response.getStatusLine().getStatusCode();

		System.out.println("Response Code : " + responseCode);

		if (responseCode != 200) {
			System.out.println("Looks like API is throttled.! Try Again!");
			System.exit(responseCode);
		}

		return response;
	}
	

	public static void main(String[] args) {

		Scanner sc = new Scanner(System.in);
		System.out.print("Enter Amount : ");
		amt = Double.parseDouble(sc.next());
		System.out.print("Enter Number of Items : ");
		num = Integer.parseInt(sc.next());

		try {

			url = Const.API_URL
					+ Const.API_SEARCH
					+ Const.EXCLUDES
					+ URLEncoder.encode(excludes.toString(), "UTF-8")
					+ Const.SORT
					+ URLEncoder.encode("{\"productPopularity\":\"desc\"}",
							"UTF-8") + "&filters="
					+ "&key=" + Const.KEY
					+ "&limit=" + Const.LIMIT;

			System.out.println(url);

			HttpResponse response = getResponse();
			BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity()
					.getContent()));
			
			StringBuilder result = new StringBuilder();
			String line = null;
			while ((line = rd.readLine()) != null) {
				result.append(line);
			}
			
			
			JSONObject js = new JSONObject(result.toString());
			JSONArray jo = js.getJSONArray("results");

			ArrayList<Product> products = jsonParse(jo);

			// Divides the Task among the available processors.
			int processors = Runtime.getRuntime().availableProcessors();
			int portion = products.size() / processors;
			ArrayList<MyTask> threads = new ArrayList<MyTask>();
			for (int i = 0; i < processors; ++i) {
				int from = i * portion;
				int to = i < processors - 1 ? (i + 1) * portion : products.size();
				MyTask task = new MyTask((ArrayList<Product>) products, from, to,
						amt);
				threads.add(task);
			}
			
			//Starting the jobs on different threads
			for (MyTask t : threads) {
				t.start();
			}

			//Joining all threads once job is done
			for (MyTask t : threads) {
				t.join();
			}

			System.out.println("Total :" + hs.size());
			
		} catch (ClientProtocolException e) {

			System.out.println("Malformed URL");
		} catch (IOException e) {

			System.out.println("IO exception");
		} catch (JSONException e) {
			
			System.out.println("JSON exception. Check the string");
		} catch (InterruptedException e) {

			System.out.println("Thread interrupted.");
		}
	
	}

	static class MyTask extends Thread implements Runnable{
		int from;
		int to;
		double amt;
		static int limit;
		ArrayList<Product> products;
		ArrayList<Product> result = new ArrayList<Product>();

		public MyTask(ArrayList<Product> p, int from, int to, double amt) {
			this.from = from;
			this.to = to;
			this.amt = amt;
			this.products = p;
			limit =  Integer.parseInt(Const.LIMIT);
		}
		
		//run method for thread
		public void run() {

			for (int i = from; i < to; i++) {
				Product tmp = products.get(i);
				if ((amt - tmp.getPrice() >= 0)) {
					result.add(tmp);
					getGifts(products, num, result, i + 1, amt - tmp.getPrice());
					result.remove(result.size() - 1);
				}
			}
		}

		private static void getGifts(ArrayList<Product> al, int k,
				ArrayList<Product> result, int startIndex, double amt) {

			if (result.size() == k) {
				if (amt >= 0 && amt <= Main.amt / 100) {
					//Sychronization for threads.
					synchronized (hs) {
						hs.add(result);
					}
					//Printing results here. We can choose not to.
					//The results will be stored in the global HashSet
					printResult(result);
				}
				return;
			}
			for (int i = startIndex; i < limit ; i++) {
				Product tmp = al.get(i);
				if ((amt - tmp.getPrice() >= 0)) {
					result.add(tmp);
					getGifts(al, k, result, i + 1, amt - tmp.getPrice());
					result.remove(result.size() - 1);
				}
			}
		}
	}
	
	private static void printResult(ArrayList<Product> rs) {

		System.out.println("Result Set");
		for (Product p : rs)
			System.out.println(p.toString());
		System.out.println();
	}
}
