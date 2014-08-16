package jp.mamesoft.mailsocketchat;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.util.Timer;
import java.util.TimerTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import io.socket.IOAcknowledge;
import io.socket.IOCallback;
import io.socket.SocketIO;
import io.socket.SocketIOException;


public class Mailsocketchat {
	public static String chat_url;
	public static String chat_name;
	public static String mail_user;
	public static String mail_pass;
	public static SocketIO socket;
	public static List<HashMap<String, String>> logs = new ArrayList<HashMap<String, String>> ();
	public static HashMap<Integer, HashMap<String, String>> roms = new HashMap<Integer, HashMap<String, String>> ();
	public static HashMap<Integer, HashMap<String, String>> users = new HashMap<Integer, HashMap<String, String>> ();
	public static Boolean in;
	public static String version = "3.02";
	public static Boolean newver = false;
	public static Boolean push = false;
	public static Boolean repeat = false;
	public static String address;
	public static String logformat;
	public static Boolean subjectname;
	static String osName = System.getProperty("os.name");
	static String osVer = System.getProperty("os.version");
	static String javaver = System.getProperty("java.version");
	public static void main(String argv[]) {
		System.out.println("MailSocketChat Ver." + version);
		if(argv.length != 6){
			System.out.println("ERROR! 引数に <チャットURL> <名前> <GMailユーザー名> <GMailパスワード> <ログフォーマット(SimpleまたはNormalまたはAll)> <プッシュ時の発言者名の表示場所(SubjectまたはText)> を指定してください");
			System.exit(0);
		}
		
		TimerTask newversion = new NewVersion();
		Timer timer = new Timer();
		timer.schedule(newversion, 0, 6 * 60 * 60 * 1000);  //6時間おきに新バージョンを確認
		
		chat_url = argv[0];
		chat_name = argv[1];
		mail_user = argv[2];
		mail_pass = argv[3];

		if(argv[4].equals("Simple")){
			logformat = "simple";
		}else if(argv[4].equals("Normal")){
			logformat = "normal";
		}else if(argv[4].equals("All")){
			logformat = "all";
		}else{
			System.out.println("ERROR! ログフォーマット(第5引数)が間違っています。SimpleまたはNormalまたはAllを指定してください。");
			System.exit(0);
		}
		
		if(argv[5].equals("Subject")){
			subjectname = true;
		}else if(argv[5].equals("Text")){
			subjectname = false;
		}else{
			System.out.println("ERROR! プッシュ時の発言者名の表示場所(第6引数)が間違っています。SubjectまたはTextを指定してください。");
			System.exit(0);
		}
		
		try {
			Properties headers = new Properties();
			headers.setProperty("user-agent", "MailSocketChat/" + version + " (" + osName + " " + osVer + ") Java/" + javaver + " (Mamesoft Web)");
			socket = new SocketIO(chat_url, headers);
	        socket.connect(new IOCallback() {
	            @Override
	            public void onMessage(JSONObject json, IOAcknowledge ack) {
	                try {
	                } catch (JSONException e) {
	                    e.printStackTrace();
	                }
	            }
	
	            @Override
	            public void onMessage(String data, IOAcknowledge ack) {
	            }
	
	            @Override
	            public void onError(SocketIOException socketIOException) {
	    	        System.out.println("エラーが発生しました！");
	    	        System.err.println(socketIOException);
					System.exit(0);
	            }
	
	            @Override
	            public void onDisconnect() {
	    	        System.out.println("サーバーから切断されました！");
					System.exit(0);
	            }
	
	            @Override
	            public void onConnect() {
	    	        socket.emit("register", new JSONObject().put("mode", "client").put("lastid", 1));
	    	        System.out.println("SocketChatに接続しました。");
	    			Thread mail = new Mail();
	    			mail.start();
	            }

	            @Override
	            public void on(String event, IOAcknowledge ack, Object... args) {
	            	if (event.equals("log")){
		            	JSONObject jsondata = (JSONObject)args[0];
		            	Logperse(jsondata);
	            	}
	            	if (event.equals("init")){
		            	JSONObject jsondata = (JSONObject)args[0];
		            	JSONArray logs = jsondata.getJSONArray("logs");
		            	for (int i = logs.length() - 1; i >= 0; i--){
		            		JSONObject log = logs.getJSONObject(i);
		            		Logperse(log);
		            	}
						socket.emit("inout", new JSONObject().put("name", chat_name));
	            	}
	            	if (event.equals("result")){
		            	JSONObject jsondata = (JSONObject)args[0];
	            		System.out.println(jsondata);
	            	}
	            	if (event.equals("users")){
		            	JSONObject jsondata = (JSONObject)args[0];
		            	JSONArray users = jsondata.getJSONArray("users");
		            	for (int i = 0; i < users.length(); i++){
		            		JSONObject user = users.getJSONObject(i);
		            		userchange(user);
		            	}
	            	}
	            	if (event.equals("newuser")){
		            	JSONObject jsondata = (JSONObject)args[0];
	            		userchange(jsondata);
	            	}
	            	if (event.equals("inout")){
		            	JSONObject jsondata = (JSONObject)args[0];
	            		userchange(jsondata);
	            	}
	            	if (event.equals("deluser")){
		            	Integer id = (Integer)args[0];
		    			if(users.containsKey(id)){
		    				users.remove(id);
		    			}
		    			if(roms.containsKey(id)){
		    				roms.remove(id);
		    			}
		            	
	            	}
	            	if (event.equals("userinfo")){
		            	JSONObject jsondata = (JSONObject)args[0];
		            	if(jsondata.getBoolean("rom")){
		            		in = false;
		            	}else{
		            		in = true;
		            	}
	            	}
	            }
	        });
			
		} catch (MalformedURLException e1) {
			e1.printStackTrace();
			System.out.println("URLがおかしいです！");
			return;
		}
	}
	
	static void Logperse(JSONObject jsondata){
		if (!jsondata.isNull("comment")){
    		String name = jsondata.getString("name");
    		String comment = jsondata.getString("comment");
    		String ip = jsondata.getString("ip");
    		String time_js = jsondata.getString("time");
    		Pattern time_p = Pattern.compile("([0-9]{4}).([0-9]{2}).([0-9]{2})T([0-9]{2}):([0-9]{2}):([0-9]{2})");
			Matcher time_m = time_p.matcher(time_js);
			String time = "";
			String simpletime = "";
			if(time_m.find()){
				int year = Integer.parseInt(time_m.group(1));
				int month = Integer.parseInt(time_m.group(2));
				int day = Integer.parseInt(time_m.group(3));
				int hour = Integer.parseInt(time_m.group(4));
				int min = Integer.parseInt(time_m.group(5));
				int sec = Integer.parseInt(time_m.group(6));
				hour = hour + 9;
				if(hour >= 24){
					hour = hour - 24;
					day = day + 1;
				}
				time = String.format("%1$04d", year) + "-" + String.format("%1$02d", month) + "-" + String.format("%1$02d", day) + " " + String.format("%1$02d", hour) + ":" + String.format("%1$02d", min) + ":" + String.format("%1$02d", sec);
				simpletime = String.format("%1$02d", hour) + ":" + String.format("%1$02d", min) + ":" + String.format("%1$02d", sec);
			}
    		String channel = "";
    		
    		HashMap<String, String> log = new HashMap<String, String> ();
    		log.put("name", name);
    		log.put("_id", jsondata.getString("_id"));
    		log.put("comment", comment);
    		log.put("ip", ip);
    		log.put("time", time);
    		log.put("simpletime", simpletime);
    		if(!jsondata.isNull("response")){
    			log.put("res", jsondata.getString("response"));
    		}else{
    			log.put("res", "");
    		}
        	if (!jsondata.isNull("channel")){
        		HashSet<String> channels_hash = new HashSet<String>();
        		for (int i = 0; i < jsondata.getJSONArray("channel").length(); i++){
        			channels_hash.add(jsondata.getJSONArray("channel").getString(i));
        		}
        		String channels[] = (String[])channels_hash.toArray(new String[0]);
        		for (int i = 0; i < channels.length ; i++){
        			channel = channel + " #" + channels[i];
        		}
        		log.put("channel", channel);
        	}
        	if(push){
        		logs.add(log);
        		Mail.Send(address, 1);
        	}else{
        		logs.add(log);
        	}
        }
    	
	}
	

	static void userchange(JSONObject jsondata){
		int id = jsondata.getInt("id");
		Boolean rom = jsondata.getBoolean("rom");
		String ip = jsondata.getString("ip");
		String ua = jsondata.getString("ua");
		HashMap<String, String> userinfo = new HashMap<String, String> ();
		userinfo.put("ip", ip);
		userinfo.put("ua", ua);
		if(rom){
			roms.put(id, userinfo);
			if(users.containsKey(id)){
				users.remove(id);
			}
		}else{
			String name = jsondata.getString("name");
			userinfo.put("name", name);
			users.put(id, userinfo);
			if(roms.containsKey(id)){
				roms.remove(id);
			}
		}
	}
}