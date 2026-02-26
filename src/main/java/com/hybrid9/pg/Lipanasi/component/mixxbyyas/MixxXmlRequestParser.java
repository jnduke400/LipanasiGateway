package com.hybrid9.pg.Lipanasi.component.mixxbyyas;

import com.hybrid9.pg.Lipanasi.dto.airtelmoneypaybill.v2.PayBillValidationRequest;
import com.hybrid9.pg.Lipanasi.dto.mixxpaybill.v2.SyncBillPayRequest;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.StringReader;
@Component
@Slf4j
public class MixxXmlRequestParser {
    private final JAXBContext jaxbContext;

    public MixxXmlRequestParser() throws JAXBException {
        this.jaxbContext = JAXBContext.newInstance(SyncBillPayRequest.class);
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
