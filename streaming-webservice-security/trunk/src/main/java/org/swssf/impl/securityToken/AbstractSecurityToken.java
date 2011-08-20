/**
 * Copyright 2010, 2011 Marc Giger
 *
 * This file is part of the streaming-webservice-security-framework (swssf).
 *
 * The streaming-webservice-security-framework is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The streaming-webservice-security-framework is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the streaming-webservice-security-framework.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.swssf.impl.securityToken;

import org.swssf.crypto.Crypto;
import org.swssf.ext.SecurityToken;
import org.swssf.ext.WSSecurityException;

import javax.security.auth.callback.CallbackHandler;
import java.security.cert.X509Certificate;

/**
 * @author $Author$
 * @version $Revision$ $Date$
 */
public abstract class AbstractSecurityToken implements SecurityToken {

    private Crypto crypto;
    private CallbackHandler callbackHandler;
    private String id;
    private Object processor;

    AbstractSecurityToken(Crypto crypto, CallbackHandler callbackHandler, String id, Object processor) {
        this.crypto = crypto;
        this.callbackHandler = callbackHandler;
        this.id = id;
        this.processor = processor;
    }

    AbstractSecurityToken(String id) {
        this.id = id;
    }

    public String getId() {
        return this.id;
    }

    public Object getProcessor() {
        return processor;
    }

    public Crypto getCrypto() {
        return crypto;
    }

    public CallbackHandler getCallbackHandler() {
        return callbackHandler;
    }

    public X509Certificate[] getX509Certificates() throws WSSecurityException {
        return null;
    }

    public void verify() throws WSSecurityException {
    }
}