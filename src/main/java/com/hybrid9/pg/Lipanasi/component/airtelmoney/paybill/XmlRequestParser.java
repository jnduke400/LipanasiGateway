package com.hybrid9.pg.Lipanasi.component.airtelmoney.paybill;

import com.hybrid9.pg.Lipanasi.dto.airtelmoneypaybill.v2.PayBillValidationRequest;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.StringReader;
@Component
@Slf4j
public class XmlRequestParser {
    private final JAXBContext jaxbContext;

    public XmlRequestParser() throws JAXBException {
        this.jaxbContext = JAXBContext.newInstance(PayBillValidationRequest.class);
    }

    public <T> T parseXmlRequest(String xmlRequest, Class<T> clazz) throws JAXBException {
        try {
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            return clazz.cast(unmarshaller.unmarshal(new StringReader(xmlRequest)));
        } catch (JAXBException e) {
            log.error("Error parsing XML request: {}", e.getMessage());
            throw e;
        }
    }
}
