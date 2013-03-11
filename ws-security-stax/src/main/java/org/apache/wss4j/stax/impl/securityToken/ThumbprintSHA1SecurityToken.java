/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.wss4j.stax.impl.securityToken;

import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoType;
import org.apache.wss4j.stax.ext.WSSConstants;
import org.apache.wss4j.stax.ext.WSSSecurityProperties;
import org.apache.wss4j.stax.ext.WSSecurityContext;
import org.apache.xml.security.exceptions.XMLSecurityException;

import javax.security.auth.callback.CallbackHandler;
import java.security.cert.X509Certificate;

public class ThumbprintSHA1SecurityToken extends X509SecurityToken {

    private String alias = null;
    private final byte[] binaryContent;

    ThumbprintSHA1SecurityToken(WSSecurityContext wsSecurityContext, Crypto crypto, CallbackHandler callbackHandler,
                                byte[] binaryContent, String id, WSSConstants.KeyIdentifierType keyIdentifierType,
                                WSSSecurityProperties securityProperties) {
        super(WSSConstants.X509V3Token, wsSecurityContext, crypto, callbackHandler, id, keyIdentifierType, securityProperties);
        this.binaryContent = binaryContent;
    }

    @Override
    protected String getAlias() throws XMLSecurityException {
        if (this.alias == null) {
            CryptoType cryptoType = new CryptoType(CryptoType.TYPE.THUMBPRINT_SHA1);
            cryptoType.setBytes(binaryContent);
            X509Certificate[] certs = getCrypto().getX509Certificates(cryptoType);

            this.alias = getCrypto().getX509Identifier(certs[0]);
        }
        return this.alias;
    }
}
