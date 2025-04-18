package rs.teslaris.core.util.xmlutil;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import java.io.StringWriter;

public class XMLUtil {

    public static <T> String convertToXml(T object) throws JAXBException {
        var context = JAXBContext.newInstance(object.getClass());
        var marshaller = context.createMarshaller();

        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE); // Pretty print XML

        var writer = new StringWriter();
        marshaller.marshal(object, writer);
        return writer.toString();
    }
}
