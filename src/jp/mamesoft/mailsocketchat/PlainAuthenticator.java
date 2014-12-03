//
// Copyright (c) 2014 Mamesoft
// Released under the MIT license
// http://opensource.org/licenses/mit-license.php
//

package jp.mamesoft.mailsocketchat;

import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;

public class PlainAuthenticator extends Authenticator {
	private final String user;
    private final String password;

    public PlainAuthenticator(final String user, final String password) {
        this.user = user;
        this.password = password;
    }

    @Override
    protected PasswordAuthentication getPasswordAuthentication() {
        return new PasswordAuthentication(this.user, this.password);
    }
}
