package com.example.wal;

import org.json.JSONArray;
import org.json.JSONException;
import org.json. JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.CookieStore;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.http.AndroidHttpClient;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.widget.ProgressBar;

public class WALConnection extends Service {

	private String password = "MYEMAIL";
	private String email = "DEFINE_THIS_IN_WALINTERFACE";
	private String courseFolder = "RLL";
	private final IBinder mBinder = new ConnectionBinder();
	private ProgressUpdateListener listener = null;
	HttpClient client;
	WalState state = new WalState();
	
	
	private String getBody(HttpEntity h)
	{
		
		int len = (int) h.getContentLength();
		
		char cbuf[] = new char[(int) len];
		String body = "";
		String cur;
		try {
			InputStreamReader r = new InputStreamReader(h.getContent());
			BufferedReader reader = new BufferedReader(r);
			while ((cur = reader.readLine()) != null)
			{
				body = body + cur;
			}
			return body;
		   // r.read(cbuf, 0, len);
		   // r.close();
		  } catch (Exception e) {
		       e.printStackTrace();
		       System.exit(-1);
		  }
		return new String(cbuf);
	}
	
	public class ConnectionBinder extends Binder {
		WALConnection getService() {
			return WALConnection.this;
		}
		
		
	}
	
	public void registerProgressListener(ProgressUpdateListener p)
	{
		listener = p;
	}
	
	public void login()
	{
		new LoginAsyncTask().execute(password, email);
	}
	

	private class LoginAsyncTask extends AsyncTask<String, Integer, Integer> {
		
		
		protected void onProgressUpdate(Integer... progress)
		{
			if (listener != null)
				listener.onProgress(progress[0]);
		}

		@Override
		protected Integer doInBackground(String... arg0) {
			client = new DefaultHttpClient();
			client.getParams().setParameter(
					CoreProtocolPNames.USER_AGENT,
				    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_9_0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/31.0.1650.57 Safari/537.36"
				);
			String password = (String) arg0[0];
			BasicCookieStore cookieStore = new BasicCookieStore();

		    // Create local HTTP context
		    HttpContext localContext = new BasicHttpContext();
		    // Bind custom cookie store to the local context
		    localContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);



			String email = (String) arg0[1];
			HttpPost httppost = new HttpPost("http://wal.lrc.cornell.edu/login.cfm");
			try {
				// Add your data
				List<NameValuePair> startPair = new ArrayList<NameValuePair>(2);
				startPair.add(new BasicNameValuePair("start", "Start"));
				httppost.setEntity(new UrlEncodedFormEntity(startPair));
				// Execute HTTP Post Request
				HttpEntity stream = client.execute(httppost, localContext).getEntity();
				publishProgress(10);
				
				String body = getBody(stream);
				body = body.substring(body.indexOf("<fieldset id=\"inputs\""), body.indexOf("</fieldset>") + 11);
				body = "<html><head><body>" + body + "<html><head><body>";
				publishProgress(20);
				Document startPage = Jsoup.parse(body);
				String mathIndex = startPage.getElementById("MathIndex").val().trim();
				String fields = startPage.getElementById("inputs").text().toString();
				String numberToType = "0";

				publishProgress(30);
				int numberIdx = fields.indexOf("Type this number below:");
				numberToType = fields.substring(numberIdx + 24, numberIdx + 28).trim().replaceAll("[^0-9]","");
				httppost = new HttpPost("http://wal.lrc.cornell.edu/login.cfm");
				publishProgress(40);
				List<NameValuePair> loginPairs = new ArrayList<NameValuePair>(2);
				loginPairs.add(new BasicNameValuePair("username", email));
				loginPairs.add(new BasicNameValuePair("MathValue", numberToType));
				loginPairs.add(new BasicNameValuePair("MathIndex", mathIndex));
				loginPairs.add(new BasicNameValuePair("CourseFolder", ""));
				loginPairs.add(new BasicNameValuePair("LessonFolder", ""));
				loginPairs.add(new BasicNameValuePair("ExerciseFolder", ""));
				loginPairs.add(new BasicNameValuePair("password", password));
				loginPairs.add(new BasicNameValuePair("submit", "Log in"));
				httppost.setEntity(new UrlEncodedFormEntity(loginPairs));
				stream = client.execute(httppost, localContext).getEntity();
				publishProgress(50);
				body = getBody(stream);
				body = body.substring(body.indexOf("<div id=\"Switchboard\""), body.indexOf("<div id=\"Enroll\""));
				body = "<html><head><body>" + body + "<html><head><body>";
				Document loggedinPage = Jsoup.parse(body);
				publishProgress(60);
				String paramString = loggedinPage.getElementsByAttributeValue("title", "Click to start using this WAL course.").first().attr("href");
				String link = "http://wal.lrc.cornell.edu/" + paramString;
				URL url = new URL(link);
				String path = url.getPath();
				String query = url.getQuery();
				String params[] = query.split("&");
				for (int i = 0; i < params.length; i++)
				{
					String cur[] = params[i].split("=");
					if (cur.length < 2)
						loginPairs.add(new BasicNameValuePair(cur[0], "0"));
					else
						loginPairs.add(new BasicNameValuePair(cur[0], cur[1]));
				}
				HttpGet httpget = new HttpGet("http://wal.lrc.cornell.edu" + path + "?" + query);
				HttpResponse httpResponse = client.execute(httpget, localContext);
				publishProgress(80);
				stream= httpResponse.getEntity();
				int r = httpResponse.getStatusLine().getStatusCode();
				if (r != 200)
				{
					return -1;
				}
				BufferedReader javascript = new BufferedReader(new InputStreamReader(stream.getContent()));
				parseJavascript(javascript);
				publishProgress(100);
			} catch (Exception e) {
				e.printStackTrace();
				return -1;
			}
			return 0;
		}
	}
	
	private class StringOrJson
	{
		public JSONObject jsonObject;
		public JSONArray jsonArray;
		
		public String stringValue = "";
		public boolean isJsonObject = false;
		public boolean isJsonArray = false;
		
		StringOrJson(String s)
		{
			stringValue = s;
			isJsonArray = false;
			isJsonObject = false;
		}
		
		StringOrJson(JSONObject j)
		{
			jsonObject = j;
			isJsonObject = true;
			isJsonArray = false;
		}
		
		StringOrJson(JSONArray a)
		{
			jsonArray = a;
			isJsonObject = false;
			isJsonArray = true;
		}
	}
	
	private boolean parseJavascript(BufferedReader javascript) {
		String htmlLine = "";
		boolean inJavascript = false;
		HashMap<String, StringOrJson> config = new HashMap<String, StringOrJson>();
		try
		{
			config.put("LessonArr", new StringOrJson("a"));
			config.put("ExerciseArr", new StringOrJson("a"));
			config.put("UserID", new StringOrJson("s"));
			config.put("CurrStudentUName", new StringOrJson("s"));
			config.put("CurrStudentPsw", new StringOrJson("s"));
			config.put("CourseFolder", new StringOrJson("s"));
			config.put("LessonFolder", new StringOrJson("s"));
			config.put("ExerciseFolder", new StringOrJson("s"));
			config.put("ExerciseData", new StringOrJson("o"));
			config.put("ExerciseHTMLFiles", new StringOrJson("o"));
		}
		catch (Exception e)
		{
			return false;
		}
		try {
			StringOrJson newVar;
			JSONObject jo;
			JSONArray ja;
			String name;
			while ((htmlLine = javascript.readLine()) != null)
			{
				if (inJavascript)
				{
					if (htmlLine.contains("</script>"))
					{
						inJavascript = false;
					}
					else
					{
						String vars[] = htmlLine.split("=", 2);
						name = vars[0].replaceAll("var", "").trim();
						if (config.containsKey(name))
						{
							String type = config.get(name).stringValue;
							if (type == "a")
							{
								ja = new JSONArray(vars[1].trim());
								config.put(name, new StringOrJson(ja));
							}
							else if (type == "o")
							{
								String obj = vars[1].trim();
								jo = new JSONObject(obj);
								config.put(name, new StringOrJson(jo));
							}
							else
							{
								config.put(name, new StringOrJson(vars[1].trim()));
							}
						}	
					}
				}
				else 
				{
					if (htmlLine.contains("<script type="))
					{
						if (!htmlLine.contains("</script>"))
							inJavascript = true;
					}
				}
			}
			loadLessons(config.get("LessonArr").jsonArray);
			loadExercises(config.get("ExerciseArr").jsonArray);
			loadExerciseData();
			loadExerciseData();
			state.password = config.get("CurrStudentPsw").stringValue;
			state.uName = config.get("CurrStudentUName").stringValue;
			state.userId = config.get("UserID").stringValue;
			state.exerciseFolder = config.get("ExerciseFolder").stringValue;
			state.lessonFolder = config.get("LessonFolder").stringValue;
			state.courseFolder = config.get("CourseFolder").stringValue;
			return true;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return false;
		}
		
	}
	
	

	private void loadExerciseData() {
		// TODO Auto-generated method stub
		
	}

	private void loadExercises(JSONArray jsonArray) {
		// TODO Auto-generated method stub
		
	}

	private void loadLessons(JSONArray jsonArray) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public IBinder onBind(Intent arg0) {
		password = arg0.getStringExtra("password");
		email = arg0.getStringExtra("username");
		return mBinder;
	}
		

}
