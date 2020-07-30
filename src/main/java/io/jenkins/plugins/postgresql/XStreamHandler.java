package io.jenkins.plugins.postgresql;

import com.thoughtworks.xstream.converters.collections.CollectionConverter;
import com.thoughtworks.xstream.io.json.JettisonMappedXmlDriver;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.model.Fingerprint;
import hudson.util.HexBinaryConverter;
import hudson.util.XStream2;

import java.util.ArrayList;

public class XStreamHandler {

    private static XStream2 XSTREAM;

    @NonNull
    static XStream2 getXStream() {
        return XSTREAM;
    }

    static {
        XSTREAM = new XStream2(new JettisonMappedXmlDriver());
        XSTREAM.setMode(XStream2.NO_REFERENCES);
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
