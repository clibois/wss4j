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
package org.apache.wss4j.policy.stax.test;

import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.policy.stax.PolicyEnforcer;
import org.apache.wss4j.stax.ext.WSSConstants;
import org.apache.wss4j.stax.securityToken.WSSecurityTokenConstants;
import org.apache.wss4j.stax.impl.securityToken.KerberosServiceSecurityTokenImpl;
import org.apache.wss4j.stax.securityEvent.KerberosTokenSecurityEvent;
import org.apache.wss4j.stax.securityEvent.OperationSecurityEvent;
import org.apache.wss4j.stax.securityEvent.SignedPartSecurityEvent;
import org.apache.xml.security.stax.ext.XMLSecurityConstants;
import org.apache.xml.security.stax.impl.util.IDGenerator;
import org.apache.xml.security.stax.securityEvent.ContentEncryptedElementSecurityEvent;
import org.apache.xml.security.stax.securityToken.InboundSecurityToken;
import org.junit.Assert;
import org.junit.Test;

import javax.xml.namespace.QName;

import java.util.LinkedList;
import java.util.List;

public class KerberosTokenTest extends AbstractPolicyTestBase {

    @Test
    public void testPolicy() throws Exception {
        String policyString =
                "<sp:SymmetricBinding xmlns:sp=\"http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200702\" xmlns:sp3=\"http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200802\">\n" +
                        "<wsp:Policy xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2004/09/policy\">\n" +
                        "<sp:EncryptionToken>\n" +
                        "   <wsp:Policy>\n" +
                        "       <sp:KerberosToken>\n" +
                        "           <sp:IssuerName>xs:anyURI</sp:IssuerName>\n" +
                        "           <wsp:Policy xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2004/09/policy\">\n" +
                        "               <sp:RequireKeyIdentifierReference/>" +
                        "               <sp:WssKerberosV5ApReqToken11/>\n" +
                        "           </wsp:Policy>\n" +
                        "       </sp:KerberosToken>\n" +
                        "   </wsp:Policy>\n" +
                        "</sp:EncryptionToken>\n" +
                        "<sp:SignatureToken>\n" +
                        "   <wsp:Policy>\n" +
                        "       <sp:KerberosToken>\n" +
                        "           <sp:IssuerName>xs:anyURI</sp:IssuerName>\n" +
                        "           <wsp:Policy xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2004/09/policy\">\n" +
                        "               <sp:RequireKeyIdentifierReference/>" +
                        "               <sp:WssKerberosV5ApReqToken11/>\n" +
                        "           </wsp:Policy>\n" +
                        "       </sp:KerberosToken>\n" +
                        "   </wsp:Policy>\n" +
                        "</sp:SignatureToken>\n" +
                        "   <sp:AlgorithmSuite>\n" +
                        "       <wsp:Policy>\n" +
                        "           <sp:Basic256/>\n" +
                        "       </wsp:Policy>\n" +
                        "   </sp:AlgorithmSuite>\n" +
                        "</wsp:Policy>\n" +
                        "</sp:SymmetricBinding>";

        PolicyEnforcer policyEnforcer = buildAndStartPolicyEngine(policyString);
        KerberosTokenSecurityEvent initiatorTokenSecurityEvent = new KerberosTokenSecurityEvent();
        initiatorTokenSecurityEvent.setIssuerName("xs:anyURI");

        KerberosServiceSecurityTokenImpl kerberosServiceSecurityToken =
                new KerberosServiceSecurityTokenImpl(null, null, null, WSSConstants.NS_Kerberos5_AP_REQ, IDGenerator.generateID(null),
                        WSSecurityTokenConstants.KeyIdentifier_EmbeddedKeyIdentifierRef);
        kerberosServiceSecurityToken.addTokenUsage(WSSecurityTokenConstants.TokenUsage_MainSignature);
        initiatorTokenSecurityEvent.setSecurityToken(kerberosServiceSecurityToken);
        policyEnforcer.registerSecurityEvent(initiatorTokenSecurityEvent);

        KerberosTokenSecurityEvent recipientTokenSecurityEvent = new KerberosTokenSecurityEvent();
        recipientTokenSecurityEvent.setIssuerName("xs:anyURI");

        kerberosServiceSecurityToken =
                new KerberosServiceSecurityTokenImpl(null, null, null, WSSConstants.NS_Kerberos5_AP_REQ, IDGenerator.generateID(null),
                        WSSecurityTokenConstants.KeyIdentifier_EmbeddedKeyIdentifierRef);
        kerberosServiceSecurityToken.addTokenUsage(WSSecurityTokenConstants.TokenUsage_MainEncryption);
        recipientTokenSecurityEvent.setSecurityToken(kerberosServiceSecurityToken);
        policyEnforcer.registerSecurityEvent(recipientTokenSecurityEvent);

        List<XMLSecurityConstants.ContentType> protectionOrder = new LinkedList<>();
        protectionOrder.add(XMLSecurityConstants.ContentType.SIGNATURE);
        protectionOrder.add(XMLSecurityConstants.ContentType.ENCRYPTION);
        SignedPartSecurityEvent signedPartSecurityEvent =
                new SignedPartSecurityEvent(
                        (InboundSecurityToken)recipientTokenSecurityEvent.getSecurityToken(), true, protectionOrder);
        signedPartSecurityEvent.setElementPath(WSSConstants.SOAP_11_BODY_PATH);
        policyEnforcer.registerSecurityEvent(signedPartSecurityEvent);

        ContentEncryptedElementSecurityEvent contentEncryptedElementSecurityEvent =
                new ContentEncryptedElementSecurityEvent(
                        (InboundSecurityToken)recipientTokenSecurityEvent.getSecurityToken(), true, protectionOrder);
        contentEncryptedElementSecurityEvent.setElementPath(WSSConstants.SOAP_11_BODY_PATH);
        policyEnforcer.registerSecurityEvent(contentEncryptedElementSecurityEvent);

        OperationSecurityEvent operationSecurityEvent = new OperationSecurityEvent();
        operationSecurityEvent.setOperation(new QName("definitions"));
        policyEnforcer.registerSecurityEvent(operationSecurityEvent);

        policyEnforcer.doFinal();
    }

    @Test
    public void testPolicyNegative() throws Exception {
        String policyString =
                "<sp:SymmetricBinding xmlns:sp=\"http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200702\" xmlns:sp3=\"http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200802\">\n" +
                        "<wsp:Policy xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2004/09/policy\">\n" +
                        "<sp:EncryptionToken>\n" +
                        "   <wsp:Policy>\n" +
                        "       <sp:KerberosToken>\n" +
                        "           <sp:IssuerName>xs:anyURI</sp:IssuerName>\n" +
                        "           <wsp:Policy xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2004/09/policy\">\n" +
                        "               <sp:WssKerberosV5ApReqToken11/>\n" +
                        "           </wsp:Policy>\n" +
                        "       </sp:KerberosToken>\n" +
                        "   </wsp:Policy>\n" +
                        "</sp:EncryptionToken>\n" +
                        "<sp:SignatureToken>\n" +
                        "   <wsp:Policy>\n" +
                        "       <sp:KerberosToken>\n" +
                        "           <sp:IssuerName>xs:anyURI</sp:IssuerName>\n" +
                        "           <wsp:Policy xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2004/09/policy\">\n" +
                        "               <sp:WssKerberosV5ApReqToken11/>\n" +
                        "           </wsp:Policy>\n" +
                        "       </sp:KerberosToken>\n" +
                        "   </wsp:Policy>\n" +
                        "</sp:SignatureToken>\n" +
                        "   <sp:AlgorithmSuite>\n" +
                        "       <wsp:Policy>\n" +
                        "           <sp:Basic256/>\n" +
                        "       </wsp:Policy>\n" +
                        "   </sp:AlgorithmSuite>\n" +
                        "</wsp:Policy>\n" +
                        "</sp:SymmetricBinding>";

        PolicyEnforcer policyEnforcer = buildAndStartPolicyEngine(policyString);
        KerberosTokenSecurityEvent initiatorTokenSecurityEvent = new KerberosTokenSecurityEvent();
        initiatorTokenSecurityEvent.setIssuerName("xs:anyURI");

        KerberosServiceSecurityTokenImpl kerberosServiceSecurityToken =
                new KerberosServiceSecurityTokenImpl(null, null, null, WSSConstants.NS_GSS_Kerberos5_AP_REQ, IDGenerator.generateID(null),
                        WSSecurityTokenConstants.KeyIdentifier_ThumbprintIdentifier);
        kerberosServiceSecurityToken.addTokenUsage(WSSecurityTokenConstants.TokenUsage_MainSignature);
        initiatorTokenSecurityEvent.setSecurityToken(kerberosServiceSecurityToken);
        policyEnforcer.registerSecurityEvent(initiatorTokenSecurityEvent);

        KerberosTokenSecurityEvent recipientTokenSecurityEvent = new KerberosTokenSecurityEvent();
        recipientTokenSecurityEvent.setIssuerName("xs:anyURI");

        kerberosServiceSecurityToken =
                new KerberosServiceSecurityTokenImpl(null, null, null, WSSConstants.NS_Kerberos5_AP_REQ, IDGenerator.generateID(null),
                        WSSecurityTokenConstants.KeyIdentifier_ThumbprintIdentifier);
        kerberosServiceSecurityToken.addTokenUsage(WSSecurityTokenConstants.TokenUsage_MainEncryption);
        recipientTokenSecurityEvent.setSecurityToken(kerberosServiceSecurityToken);
        policyEnforcer.registerSecurityEvent(recipientTokenSecurityEvent);

        List<XMLSecurityConstants.ContentType> protectionOrder = new LinkedList<>();
        protectionOrder.add(XMLSecurityConstants.ContentType.SIGNATURE);
        protectionOrder.add(XMLSecurityConstants.ContentType.ENCRYPTION);
        SignedPartSecurityEvent signedPartSecurityEvent =
                new SignedPartSecurityEvent(
                        (InboundSecurityToken)recipientTokenSecurityEvent.getSecurityToken(), true, protectionOrder);
        signedPartSecurityEvent.setElementPath(WSSConstants.SOAP_11_BODY_PATH);
        policyEnforcer.registerSecurityEvent(signedPartSecurityEvent);

        ContentEncryptedElementSecurityEvent contentEncryptedElementSecurityEvent =
                new ContentEncryptedElementSecurityEvent(
                        (InboundSecurityToken)recipientTokenSecurityEvent.getSecurityToken(), true, protectionOrder);
        contentEncryptedElementSecurityEvent.setElementPath(WSSConstants.SOAP_11_BODY_PATH);
        policyEnforcer.registerSecurityEvent(contentEncryptedElementSecurityEvent);

        OperationSecurityEvent operationSecurityEvent = new OperationSecurityEvent();
        operationSecurityEvent.setOperation(new QName("definitions"));

        try {
            policyEnforcer.registerSecurityEvent(operationSecurityEvent);
            Assert.fail("Exception expected");
        } catch (WSSecurityException e) {
            Assert.assertEquals(e.getMessage(), "Policy enforces WssKerberosV5ApReqToken11");
        }
    }
}
