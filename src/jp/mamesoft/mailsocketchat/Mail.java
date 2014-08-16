package jp.mamesoft.mailsocketchat;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.Folder;
import javax.mail.FolderClosedException;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.Transport;
import javax.mail.event.MessageCountAdapter;
import javax.mail.event.MessageCountEvent;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.json.JSONObject;

import com.sun.mail.imap.IMAPFolder;

public class Mail extends Thread {
	String hashtag;
	TimerTask repeatthread = new Repeat();
	Timer repeat = new Timer();
	public void run(){
		while(true){
			try {
				System.out.println("メールサーバーに接続中です");
			    Properties props = System.getProperties();
	
			    // Get a Session object
			    Session session = Session.getInstance(props, null);
			    // session.setDebug(true);
	
			    // Get a Store object
			    Store store = session.getStore("imaps");
	
			    // Connect
			    store.connect("imap.gmail.com", 993, Mailsocketchat.mail_user, Mailsocketchat.mail_pass);
				System.out.println("メールサーバーに接続しました");
	
			    // Open a Folder
			    Folder folder = store.getFolder("INBOX");
			    if (folder == null || !folder.exists()) {
					System.out.println("IMAPフォルダーが見つかりません！");
					System.exit(1);
			    }
	
			    folder.open(Folder.READ_WRITE);
	
			    // Add messageCountListener to listen for new messages
			    folder.addMessageCountListener(new MessageCountAdapter() {
				public void messagesAdded(MessageCountEvent ev) {
				    Message[] msgs = ev.getMessages();
				    
				    // Just dump out the new messages
				    for (int i = 0; i < msgs.length; i++) {
						try {
							System.out.println("メールを受信しました。");
						    InternetAddress addrfrom = (InternetAddress)msgs[i].getFrom()[0];
							String subject = msgs[i].getSubject();
							if(subject == null){
								subject = "";
							}

							Pattern hashtag_p = Pattern.compile("#(.+)");
							Matcher hashtag_m = hashtag_p.matcher(subject);
							
							if(subject.equals("#")){
								hashtag = null;
							}else if(hashtag_m.find()){
								hashtag = hashtag_m.group(1);
							}
							
						    String comment = msgs[i].getContent().toString().replaceAll("\r\n"," ");
						    comment = comment.replaceAll("\n"," ");
						    comment = comment.replaceAll("\r"," ");
						    JSONObject data = new JSONObject();
						    data.put("comment", comment);
						    if(hashtag != null){
						    	data.put("channel", hashtag);
						    }
						    if(!comment.equals("") && !comment.equals(" ") && !comment.equals("  ")){
						    	Mailsocketchat.socket.emit("say", data);
								System.out.println("発言しました");
						    }
						    
						    //コマンド判定
						    if(subject.equals("push") || subject.equals("Push") || subject.equals("プッシュ")){
								Send(addrfrom.getAddress(), 2);
								Mailsocketchat.push = true;
								Mailsocketchat.repeat = false;
								Mailsocketchat.address = addrfrom.getAddress();
								repeatthread.cancel();
								repeatthread = null;
								System.out.println("プッシュモードに切り替えました。");
							}else if(subject.equals("fetch") || subject.equals("Fetch") || subject.equals("フェッチ")){
								Send(addrfrom.getAddress(), 3);
								Mailsocketchat.push = false;
								Mailsocketchat.repeat = false;
								repeatthread.cancel();
								repeatthread = null;
								System.out.println("フェッチモードに切り替えました。");
							}else if(subject.equals("repeat") || subject.equals("Repeat") || subject.equals("リピート")){
								Send(addrfrom.getAddress(), 7);
								Mailsocketchat.push = false;
								Mailsocketchat.repeat = true;
								Mailsocketchat.address = addrfrom.getAddress();
								if (repeatthread == null) {
									repeatthread = new Repeat();
									repeat = new Timer();
						        }
								repeat.schedule(repeatthread, 0, 30 * 1000);
								System.out.println("リピートモードに切り替えました。");
							}else if(subject.equals("list") || subject.equals("List") || subject.equals("リスト")){
						    	Send(addrfrom.getAddress(), 4);
						    }else if(subject.equals("help") || subject.equals("Help") || subject.equals("ヘルプ")){
						    	Send(addrfrom.getAddress(), 5);
						    }else{
							    if(!Mailsocketchat.push && !Mailsocketchat.repeat){
							    	Send(addrfrom.getAddress(), 0);
							    }else if(comment.equals("") || comment.equals(" ") || comment.equals("  ")){
							    	Send(addrfrom.getAddress(), 5);
							    }
						    }
						} catch (IOException ioex) {
						    System.out.println("メールサーバーから切断されました。再接続します。");
						} catch (MessagingException mex) {
						    System.out.println("メールサーバーから切断されました。再接続します。");
						}
				    }
				}
			    });
					
			    // Check mail once in "freq" MILLIseconds
			    int freq = 1000;	//確認間隔?
			    boolean supportsIdle = false;
			    try {
				if (folder instanceof IMAPFolder) {
				    IMAPFolder f = (IMAPFolder)folder;
				    f.idle();
				    supportsIdle = true;
				}
			    } catch (FolderClosedException fex) {
				throw fex;
			    } catch (MessagingException mex) {
				supportsIdle = false;
			    }
			    for (;;) {
				if (supportsIdle && folder instanceof IMAPFolder) {
				    IMAPFolder f = (IMAPFolder)folder;
				    f.idle();
				} else {
				    Thread.sleep(freq); // sleep for freq milliseconds
	
				    // This is to force the IMAP server to send us
				    // EXISTS notifications. 
				    folder.getMessageCount();
				}
			    }
	
			} catch (Exception ex) {
			    System.out.println("メールサーバーから切断されました。再接続します。");
			}
		}
	}
	public static void Send(String to, int mode){
		//mode 0 = フェッチモードログ返信, 1 = プッシュモード送信, 2 = プッシュモードに切替, 3 = フェッチモードに切替, 4 = ユーザーリスト返信, 5 = ヘルプ返信
		System.out.println("メールを送信中です");
		String from = Mailsocketchat.mail_user;
		String host = "smtp.gmail.com";
		String port = "465";
		String text = "";
		String subject = "";
		if(mode == 0){
			text = logprint(text);
			subject = "最新発言一覧 - MailSocketChat";
			if(Mailsocketchat.newver){
				text = text + "\n\n新しいバージョンが公開されています！\n";
			}
		}
		if(mode == 1){
			if(Mailsocketchat.subjectname){
				subject = Mailsocketchat.logs.get(0).get("name");
			}else{
				subject = "新しい発言 - MailSocketChat";
			}
			text = logprint(text);
		}
		if(mode == 8){
			text = logprint(text);
			subject = "新しい発言 - MailSocketChat";
		}
		if(mode == 2){

			if(!Mailsocketchat.push){
				subject = "プッシュモードに切り替えました - MailSocketChat";
				text = "プッシュモードに切り替えました。新しい発言があると自動的にこのアドレスにメールが送信されます。\n\n【いままでの最新発言一覧】\n";
				text = logprint(text);
			}else{
				subject = "既にプッシュモードです - MailSocketChat";
				text = "既にプッシュモードです。新しい発言があると自動的にこのアドレスにメールが送信されます。\n(アドレスが違う場合このアドレスに切り替えました)\n\n";
			}
			if(Mailsocketchat.newver){
				text = text + "\n\n新しいバージョンが公開されています！\n";
			}
		}
		if(mode == 3){

			if(!Mailsocketchat.push && !Mailsocketchat.repeat){
				subject = "既にフェッチモードです - MailSocketChat";
				text = "既にフェッチモードです\n\n【いままでの最新発言一覧】\n";
				text = logprint(text);
			}else{
				subject = "フェッチモードに切り替えました - MailSocketChat";
				text = "フェッチモードに切り替えました。メールを受信したときに最新発言の一覧を返信します。\n\n";
				if(Mailsocketchat.repeat){
					text = text + "【いままでの最新発言一覧】\n";
					text = logprint(text);
				}
			}
			if(Mailsocketchat.newver){
				text = text + "\n\n新しいバージョンが公開されています！\n";
			}
		}
		
		if(mode == 7){

			if(!Mailsocketchat.repeat){
				subject = "リピートモードに切り替えました - MailSocketChat";
				text = "リピートモードに切り替えました。30秒ごとに最新発言がある場合は自動的にメールを送信します。\n\n";
				if(!Mailsocketchat.push){
					text = text + "【いままでの最新発言一覧】\n";
					text = logprint(text);
				}
			}else{
				subject = "既にリピートモードです - MailSocketChat";
				text = "既にリピートモードです\n\n";
			}
			if(Mailsocketchat.newver){
				text = text + "\n\n新しいバージョンが公開されています！\n";
			}
		}
		
		if(mode == 4){
			subject = "ユーザーリスト - MailSocketChat";

			int userint = Mailsocketchat.users.size();
			int romint = Mailsocketchat.roms.size();
			text = "入室者: " + userint + " ROM: " + romint + "\n\n＜入室者＞\n";
			
			for( Integer id : Mailsocketchat.users.keySet()){
				HashMap<String, String> data = Mailsocketchat.users.get(id);
				text = text + data.get("name") + "\n";
				text = text + " (" + data.get("ip") + ")\n";
			}
			
			text = text + "\n\n＜ROM＞\n";
			for( Integer id : Mailsocketchat.roms.keySet()){
				HashMap<String, String> data = Mailsocketchat.roms.get(id);
				text = text + data.get("ip") + "\n";
			}
			if(Mailsocketchat.newver){
				text = text + "\n\n新しいバージョンが公開されています！\n";
			}
		}
		if(mode == 5){
			subject = "ヘルプ - MailSocketChat";
			if(Mailsocketchat.push){
				text = "現在のモード: プッシュモード\n\n";
			}else if(Mailsocketchat.repeat){
				text = "現在のモード: リピートモード\n\n";
			}else{
				text = "現在のモード: フェッチモード\n\n";
			}
			text = text + "【コマンド一覧】\n";
			text = text + "フェッチ(fetch): フェッチモードに切り替えます\n";
			text = text + "プッシュ(push): プッシュモードに切り替えます\n";
			text = text + "リピート(repeat): リピートモードに切り替えます\n";
			text = text + "リスト(list): ユーザーリストを返信します\n";
			text = text + "#: チャネル設定を解除します\n";
			text = text + "#hoge: チャネル「hoge」を設定します\n";
			text = text + "ヘルプ(help): このメッセージを返信します\n\n";
			
			text = text + "\nMailSocketChat Ver." + Mailsocketchat.version + "\n";
			if(Mailsocketchat.newver){
				text = text + "新しいバージョンが公開されています！\n";
			}
		}
		if(mode == 6){
			subject = "新しいバージョンが公開されています - MailSocketChat";
			text = text + "MailSocketChatの新しいバージョンが公開されています。ご利用のコンピューターで更新をお勧めします。\n";
		}
		
		// create some properties and get the default Session
		Properties props = new Properties();
		props.put("mail.smtp.auth", "true");
		props.put("mail.transport.protocol", "smtps");
		props.put("mail.smtp.starttls.enable", "true");
		props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
		props.put("mail.smtp.socketFactory.fallback", "false");
		props.put("mail.smtp.host", host);
		props.put("mail.smtp.port", port);

		Session sendsession = Session.getInstance(props, new PlainAuthenticator(Mailsocketchat.mail_user, Mailsocketchat.mail_pass));

		try {
		    // create a message
		    MimeMessage msg = new MimeMessage(sendsession);
		    msg.setFrom(new InternetAddress(from));
		    InternetAddress[] sendaddress = {new InternetAddress(to)};
		    msg.setRecipients(Message.RecipientType.TO, sendaddress);
		    msg.setSubject(subject);
		    msg.setSentDate(new Date());
		    // If the desired charset is known, you can use
		    // setText(text, charset)
		    msg.setText(text);
		    
		    Transport.send(msg);
			System.out.println("メールを送信しました。");
		} catch (MessagingException mex) {
		    System.out.println("メール送信に失敗しました");
		}
	}
	static String logprint(String str){
		for(int i = Mailsocketchat.logs.size() - 1; i >= 0 ; i--){
			HashMap<String, String> log = Mailsocketchat.logs.get(i);
			if(Mailsocketchat.subjectname || !Mailsocketchat.push){
				str = str + log.get("name") + "> ";
			}
			str = str + log.get("comment");
			if(log.containsKey("channel")){
				str = str + log.get("channel");
			}
			if(Mailsocketchat.logformat.equals("all")){
				str = str + " (" + log.get("ip") + "; " + log.get("time") + ")";
			}
			if(Mailsocketchat.logformat.equals("normal")){
				str = str + " (" + log.get("ip");
				if(!Mailsocketchat.push){
					 str = str + "; " + log.get("simpletime");
				}
				str = str + ")";
			}
			if(Mailsocketchat.logformat.equals("simple") && !Mailsocketchat.push){
				str = str + " (" + log.get("simpletime") + ")";
			}
			str = str + "\n";
			if(!Mailsocketchat.push){
				str = str + "\n";
			}
		}
		Mailsocketchat.logs.clear();
		return str;
	}
}
