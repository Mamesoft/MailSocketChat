package jp.mamesoft.mailsocketchat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.*;
import java.util.TimerTask;

public class NewVersion extends TimerTask {
	public void run(){
		try {
            URL url = new URL("http://mamesoft.jp/soft/mailsocketchat/latestversion.php");
            Object content = url.getContent();
            if (content instanceof InputStream) {
                BufferedReader bf = new BufferedReader(new InputStreamReader( (InputStream)content) );
                String latestversion_str = bf.readLine();
                double version = Double.parseDouble(Mailsocketchat.version);
                double latestversion = Double.parseDouble(latestversion_str);
                if(version < latestversion){
					System.out.println("新しいバージョンが公開されています！");
					System.out.println("http://mamesoft.jp/soft/mailsocketchat/");
					if(!Mailsocketchat.newver && Mailsocketchat.push){
						Mail.Send(Mailsocketchat.address, 6);
					}
					Mailsocketchat.newver = true;
                }
            }
        }
        catch (IOException e) {
        	
        }
	}
}
