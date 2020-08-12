/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2013 XStream Committers.
 * All rights reserved.
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 *
 * Created on 30. March 2007 by Joerg Schaible
 */

/*
 * (BSD Style License)
 *
 * Copyright (c) 2003-2006, Joe Walnes
 * Copyright (c) 2006-2011, XStream Committers
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this list of
 * conditions and the following disclaimer. Redistributions in binary form must reproduce
 * the above copyright notice, this list of conditions and the following disclaimer in
 * the documentation and/or other materials provided with the distribution.
 *
 * Neither the name of XStream nor the names of its contributors may be used to endorse
 * or promote products derived from this software without specific prior written
 * permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT
 * SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */
package io.jenkins.plugins.postgresql.jettison;

import com.thoughtworks.xstream.io.naming.NameCoder;
import com.thoughtworks.xstream.io.xml.QNameMap;
import com.thoughtworks.xstream.io.xml.StaxWriter;
import com.thoughtworks.xstream.io.xml.XmlFriendlyReplacer;

import org.codehaus.jettison.AbstractXMLStreamWriter;
import org.codehaus.jettison.mapped.MappedNamespaceConvention;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import java.util.Collection;
import java.util.Map;


/**
 * A specialized {@link StaxWriter} that makes usage of internal functionality of Jettison.
 *
 * @author J&ouml;rg Schaible
 * @since 1.3.1
 */
@Restricted(NoExternalUse.class)
public class JettisonStaxWriter extends StaxWriter {

    private final MappedNamespaceConvention convention;

    /**
     * @since 1.4
     */
    public JettisonStaxWriter(
            QNameMap qnameMap, XMLStreamWriter out, boolean writeEnclosingDocument,
            boolean namespaceRepairingMode, NameCoder nameCoder,
            MappedNamespaceConvention convention) throws XMLStreamException {
        super(qnameMap, out, writeEnclosingDocument, namespaceRepairingMode, nameCoder);
        this.convention = convention;
    }

    /**
     * @deprecated As of 1.4 use
     *             {@link JettisonStaxWriter#JettisonStaxWriter(QNameMap, XMLStreamWriter, boolean, boolean, NameCoder, MappedNamespaceConvention)}
     *             instead
     */
    public JettisonStaxWriter(
            QNameMap qnameMap, XMLStreamWriter out, boolean writeEnclosingDocument,
            boolean namespaceRepairingMode, XmlFriendlyReplacer replacer,
            MappedNamespaceConvention convention) throws XMLStreamException {
        this(qnameMap, out, writeEnclosingDocument, namespaceRepairingMode, (NameCoder) replacer, convention);
    }

    public JettisonStaxWriter(
            QNameMap qnameMap, XMLStreamWriter out, boolean writeEnclosingDocument,
            boolean namespaceRepairingMode, MappedNamespaceConvention convention)
            throws XMLStreamException {
        super(qnameMap, out, writeEnclosingDocument, namespaceRepairingMode);
        this.convention = convention;
    }

    public JettisonStaxWriter(
            QNameMap qnameMap, XMLStreamWriter out, MappedNamespaceConvention convention)
            throws XMLStreamException {
        super(qnameMap, out);
        this.convention = convention;
    }

    /**
     * @since 1.4
     */
    public JettisonStaxWriter(
            QNameMap qnameMap, XMLStreamWriter out, NameCoder nameCoder, MappedNamespaceConvention convention)
            throws XMLStreamException {
        super(qnameMap, out, nameCoder);
        this.convention = convention;
    }

    public void startNode(String name, Class clazz) {
        XMLStreamWriter out = getXMLStreamWriter();
        if (clazz != null && out instanceof AbstractXMLStreamWriter) {
            if (Collection.class.isAssignableFrom(clazz)
                    || Map.class.isAssignableFrom(clazz)
                    || clazz.isArray()) {
                QName qname = getQNameMap().getQName(encodeNode(name));
                String prefix = qname.getPrefix();
                String uri = qname.getNamespaceURI();
                String key = convention.createKey(prefix, uri, qname.getLocalPart());
                if (!((AbstractXMLStreamWriter)out).getSerializedAsArrays().contains(key)) {
                    // Typo is in the API of Jettison ...
                    ((AbstractXMLStreamWriter)out).seriliazeAsArray(key);
                }
            }
        }
        startNode(name);
    }
}