//
// Copyright (c) 2014 Mamesoft
// Released under the MIT license
// http://opensource.org/licenses/mit-license.php
//

package jp.mamesoft.mailsocketchat;

import java.util.TimerTask;

public class Repeat extends TimerTask {

	@Override
	public void run() {
		if(!Mailsocketchat.logs.isEmpty()){
			Mail.Send(Mailsocketchat.address, 8);
		}
	}

}
