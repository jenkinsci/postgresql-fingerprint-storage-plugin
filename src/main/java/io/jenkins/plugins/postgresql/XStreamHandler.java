/*
 * The MIT License
 *
 * Copyright (c) 2020, Jenkins project contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package io.jenkins.plugins.postgresql;

import com.thoughtworks.xstream.converters.collections.CollectionConverter;
import com.thoughtworks.xstream.io.json.JettisonMappedXmlDriver;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.model.Fingerprint;
import hudson.util.HexBinaryConverter;
import hudson.util.XStream2;
import com.thoughtworks.xstream.XStream;

import java.util.ArrayList;

/**
 * Supports ORM to and from JSON using XStream's {@link JettisonMappedXmlDriver} driver.
 */
public class XStreamHandler {

    private static XStream2 XSTREAM = new XStream2(new JettisonMappedXmlDriver());

    /**
     * Returns {@link XStream2} instance.
     */
    static @NonNull XStream2 getXStream() {
        return XSTREAM;
    }

    static {
        XSTREAM.setMode(XStream.NO_REFERENCES);
        XSTREAM.alias("fingerprint", Fingerprint.class);
        XSTREAM.alias("range", Fingerprint.Range.class);
        XSTREAM.alias("ranges", Fingerprint.RangeSet.class);
        XSTREAM.registerConverter(new HexBinaryConverter(), 10);
        XSTREAM.registerConverter(new Fingerprint.RangeSet.ConverterImpl(
                new CollectionConverter(XSTREAM.getMapper()) {
                    @Override
                    protected Object createCollection(Class type) {
                        return new ArrayList();
                    }
                }
        ), 10);
    }

}
