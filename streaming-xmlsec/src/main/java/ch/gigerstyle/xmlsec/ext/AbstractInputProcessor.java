package ch.gigerstyle.xmlsec.ext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import java.util.HashSet;
import java.util.Set;

/**
 * User: giger
 * Date: May 13, 2010
 * Time: 1:32:12 PM
 * Copyright 2010 Marc Giger gigerstyle@gmx.ch
 * <p/>
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation; either version 2, or (at your option) any
 * later version.
 * <p/>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
public abstract class AbstractInputProcessor implements InputProcessor {

    protected final transient Log logger = LogFactory.getLog(this.getClass());

    private SecurityProperties securityProperties;

    private Constants.Phase phase = Constants.Phase.PROCESSING;
    private Set<String> beforeProcessors = new HashSet<String>();
    private Set<String> afterProcessors = new HashSet<String>();

    public AbstractInputProcessor(SecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    public Constants.Phase getPhase() {
        return phase;
    }

    public void setPhase(Constants.Phase phase) {
        this.phase = phase;
    }

    public Set<String> getBeforeProcessors() {
        return beforeProcessors;
    }

    public Set<String> getAfterProcessors() {
        return afterProcessors;
    }

    public void processSecurityHeaderEvent(XMLEvent xmlEvent, InputProcessorChain inputProcessorChain) throws XMLStreamException, XMLSecurityException {
        inputProcessorChain.processSecurityHeaderEvent(xmlEvent);
    }

    public void processNextSecurityHeaderEvent(XMLEvent xmlEvent, InputProcessorChain inputProcessorChain) throws XMLStreamException, XMLSecurityException {
        processSecurityHeaderEvent(xmlEvent, inputProcessorChain);
    }

    public abstract void processEvent(XMLEvent xmlEvent, InputProcessorChain inputProcessorChain) throws XMLStreamException, XMLSecurityException;

    public void processNextEvent(XMLEvent xmlEvent, InputProcessorChain inputProcessorChain) throws XMLStreamException, XMLSecurityException {
        processEvent(xmlEvent, inputProcessorChain);
    }

    public void doFinal(InputProcessorChain inputProcessorChain) throws XMLStreamException, XMLSecurityException {
        inputProcessorChain.doFinal();
    }

    public SecurityProperties getSecurityProperties() {
        return securityProperties;
    }
}