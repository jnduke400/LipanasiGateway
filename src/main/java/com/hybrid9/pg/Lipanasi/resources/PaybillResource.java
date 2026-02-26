package com.hybrid9.pg.Lipanasi.resources;

import com.hybrid9.pg.Lipanasi.dto.airtelmoneypaybill.PayBillRequestDTO;
import com.hybrid9.pg.Lipanasi.dto.customer.CustomerDto;
import com.hybrid9.pg.Lipanasi.dto.mixxpaybill.BillPayRequest;
import com.hybrid9.pg.Lipanasi.dto.mpesapaybill.MpesaBrokerRequest;
import com.hybrid9.pg.Lipanasi.dto.mpesapaybill.Transaction;
import com.hybrid9.pg.Lipanasi.dto.order.OrderRequestDto;
import com.hybrid9.pg.Lipanasi.dto.order.VendorInfo;
import com.hybrid9.pg.Lipanasi.dto.orderpayment.PaymentRequest;
import com.hybrid9.pg.Lipanasi.entities.operators.MnoPrefix;
import com.hybrid9.pg.Lipanasi.entities.order.Order;
import com.hybrid9.pg.Lipanasi.entities.payments.commission.CommissionTier;
import com.hybrid9.pg.Lipanasi.entities.payments.pymtmethods.PaymentMethod;
import com.hybrid9.pg.Lipanasi.entities.vendorx.VendorDetails;
import com.hybrid9.pg.Lipanasi.enums.CollectionStatus;
import com.hybrid9.pg.Lipanasi.entities.payments.paybill.PayBillPayment;
import com.hybrid9.pg.Lipanasi.enums.MobileNetworkType;
import com.hybrid9.pg.Lipanasi.models.pgmodels.commissions.CommissionConfig;
import com.hybrid9.pg.Lipanasi.models.pgmodels.mobilenetworks.MobileNetworkConfig;
import com.hybrid9.pg.Lipanasi.models.pgmodels.mobilenetworks.OperatorMapping;
import com.hybrid9.pg.Lipanasi.models.session.UserSession;
import com.hybrid9.pg.Lipanasi.resources.excpts.CustomExcpts;
import com.hybrid9.pg.Lipanasi.serviceImpl.operators.MnoServiceImpl;
import com.hybrid9.pg.Lipanasi.services.order.OrderService;
import com.hybrid9.pg.Lipanasi.services.payments.gw.MobileNetworkConfigService;
import com.hybrid9.pg.Lipanasi.services.payments.gw.OperatorManagementService;
import com.hybrid9.pg.Lipanasi.services.payments.gw.SessionManagementService;
import com.hybrid9.pg.Lipanasi.services.payments.vendorx.VendorManagementService;
import com.hybrid9.pg.Lipanasi.services.vendorx.VendorService;
import com.hybrid9.pg.Lipanasi.services.vendorx.MainAccountService;
import com.hybrid9.pg.Lipanasi.services.payments.PayBillPaymentService;
import com.hybrid9.pg.Lipanasi.utilities.PaymentUtilities;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.json.XML;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringReader;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

@Component
@Slf4j
public class PaybillResource {

    @Value("${order.session.expiry.default:30}")
    private int DEFAULT_SESSION_EXPIRY;

    private final ExternalResources externalResources;
    private final MnoServiceImpl mnoService;
    private final PaymentUtilities paymentUtilities;
    private final SessionManagementService sessionManagementService;
    private final OperatorManagementService operatorManagementService;
    private final NetworkConfResource networkConfResource;
    private final VendorService vendorService;
    private final OrderService orderService;
    private final VendorManagementService vendorManagementService;
    private final MobileNetworkConfigService networkConfigService;

    public PaybillResource(ExternalResources externalResources, MnoServiceImpl mnoService, PaymentUtilities paymentUtilities, SessionManagementService sessionManagementService, OperatorManagementService operatorManagementService, NetworkConfResource networkConfResource, VendorService vendorService, OrderService orderService, VendorManagementService vendorManagementService, MobileNetworkConfigService networkConfigService) {
        this.externalResources = externalResources;
        this.mnoService = mnoService;
        this.paymentUtilities = paymentUtilities;
        this.sessionManagementService = sessionManagementService;
        this.operatorManagementService = operatorManagementService;
        this.networkConfResource = networkConfResource;
        this.vendorService = vendorService;
        this.orderService = orderService;
        this.vendorManagementService = vendorManagementService;
        this.networkConfigService = networkConfigService;
    }

    //Mpesa Paybill

    public PayBillPayment processPayment(MpesaBrokerRequest request, PayBillPaymentService payBillPaymentService, MnoServiceImpl mnoService, PaymentUtilities paymentUtilities, MainAccountService mainAccountService, VendorService vendorService) {
        validateRequest(request);

        Transaction transaction = request.getRequest().getTransaction();

        // Get order
        Order order = orderService.findByReceipt(transaction.getAccountReference())
                .orElseThrow(() -> new RuntimeException("Order not found"));

        // Compose payment request
        PaymentRequest paymentRequest = PaymentRequest.builder()
                .orderNumber(order.getOrderNumber())
                .paymentChannel("PAY_BILL")
                .paymentMethod("mobile")
                .msisdn(transaction.getInitiator())
                .build();

        // start validating session
        this.manageSession(paymentRequest, order);

        PayBillPayment payment = PayBillPayment.builder()
                .vendorDetails(order.getCustomer().getVendorDetails())
                .payBillId(transaction.getTransactionID())
                .paymentReference(paymentUtilities.generateRefNumber())
                .originalReference(transaction.getAccountReference())
                .amount(Float.parseFloat(transaction.getAmount()))
                .currency("TZS") // Assuming Tanzanian Shillings
                .msisdn(paymentUtilities.formatPhoneNumber("255", transaction.getInitiator()))
                .accountId(String.valueOf(mainAccountService.findMainAccountByAccountNumber("100019802002").getId()))
                .operator(mnoService.searchMno(paymentUtilities.formatPhoneNumber("255", transaction.getInitiator())))
                .receiptNumber(transaction.getMpesaReceipt())
                .collectionStatus(CollectionStatus.COLLECTED)
                .transactionDate(transaction.getTransactionDate())
                .build();

        return payBillPaymentService.createPayBill(payment);
    }

    private void validateRequest(MpesaBrokerRequest request) {
        if (request == null || request.getRequest() == null
                || request.getRequest().getServiceProvider() == null
                || request.getRequest().getTransaction() == null) {
            throw new InvalidPaymentRequestException("Invalid request: Payload cannot be null");
        }

        String spId = request.getRequest().getServiceProvider().getSpId();
        if (!"300300".equals(spId)) {
            throw new InvalidPaymentRequestException("Invalid spId: Expected 300300 but got " + spId);
        }
    }

    // InvalidRequestException.java
    public class InvalidPaymentRequestException extends RuntimeException {
        public InvalidPaymentRequestException(String message) {
            super(message);
        }
    }

    //Mixx By Yas

    public PayBillPayment processPayment(BillPayRequest request, PayBillPaymentService payBillPaymentService, MnoServiceImpl mnoService, PaymentUtilities paymentUtilities, VendorService vendorService, MainAccountService mainAccountService) {
        VendorDetails vendorDetails = vendorService.findVendorDetailsByCode("SC001")
                .orElseThrow(() -> new RuntimeException("Institution not found"));
        log.info("Processing payment for transaction: {}", request.getTransactionId());
        PayBillPayment payment = PayBillPayment.builder()
                .vendorDetails(vendorDetails)
                .payBillId(request.getTransactionId())
                .paymentReference(paymentUtilities.generateRefNumber())
                .originalReference(request.getCustomerReferenceId())
                .amount(request.getAmount())
                .currency("TZS") // Assuming Tanzanian Shillings based on MSISDN
                .msisdn(paymentUtilities.formatPhoneNumber("255", request.getMsisdn()))
                .accountId(String.valueOf(mainAccountService.findMainAccountByAccountNumber("100019802002").getId()))
                .operator(mnoService.searchMno(paymentUtilities.formatPhoneNumber("255", request.getMsisdn()))) // Can be modified based on your requirements
                .receiptNumber(generateReceiptNumber())
                .collectionStatus(CollectionStatus.COLLECTED)
                .transactionDate(LocalDateTime.now().toString())

                .build();

        return payBillPaymentService.createPayBill(payment);
    }

    private String generateReceiptNumber() {
        return "RCPT" + System.currentTimeMillis();
    }

    public void validateMixxRequest(BillPayRequest request) throws InvalidRequestException {
        if (request == null) {
            throw new InvalidRequestException("Request cannot be null");
        }

        // Validate individual fields
        if (request.getType() == null || request.getType().trim().isEmpty()) {
            throw new InvalidRequestException("TYPE is required");
        }
        if (request.getTransactionId() == null || request.getTransactionId().trim().isEmpty()) {
            throw new InvalidRequestException("TXNID is required");
        }
        if (request.getMsisdn() == null || request.getMsisdn().trim().isEmpty()) {
            throw new InvalidRequestException("MSISDN is required");
        }
        if (request.getAmount() == null) {
            throw new InvalidRequestException("AMOUNT is required");
        }
        if (request.getCompanyName() == null || !request.getCompanyName().equals("300300")) {
            throw new InvalidRequestException("Invalid company name. Expected: 300300");
        }
        if (request.getCustomerReferenceId() == null || request.getCustomerReferenceId().trim().isEmpty()) {
            throw new InvalidRequestException("CUSTOMERREFERENCEID is required");
        }
    }

    // InvalidRequestException.java
    public class InvalidRequestException extends RuntimeException {
        public InvalidRequestException(String message) {
            super(message);
        }
    }

    /*public static String convertXmlToJson(String xml) throws Exception {
        // Create XML mapper with configuration
        XmlMapper xmlMapper = new XmlMapper();
        xmlMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // Create JSON mapper with pretty printing
        ObjectMapper jsonMapper = new ObjectMapper();
        jsonMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // Remove XML declaration if present
        xml = xml.replaceFirst("<\\?xml.*\\?>", "").trim();

        try {
            // First convert XML to POJO
            BillPayRequest request = xmlMapper.readValue(xml, BillPayRequest.class);

            // Then convert POJO to JSON
            return jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(request);
        } catch (Exception e) {
            // Fallback to org.json method if POJO mapping fails
            JSONObject jsonObj = XML.toJSONObject(xml);
            JSONObject command = jsonObj.getJSONObject("COMMAND");

            // Map to our POJO structure
            BillPayRequest request = new BillPayRequest();
            request.setType(command.getString("TYPE"));
            request.setTransactionId(command.getString("TXNID"));
            request.setMsisdn(command.getString("MSISDN"));
            request.setAmount(command.getFloat("AMOUNT"));
            request.setCompanyName(command.getString("COMPANYNAME"));
            request.setCustomerReferenceId(command.getString("CUSTOMERREFERENCEID"));

            return jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(request);
        }
    }*/


    public static String convertXmlToJson(String xml) throws Exception {
        try {
            xml.replace("<?xml version=\"1.0\" encoding=\"UTF-8\"?>", "").trim();
            // Convert XML to JSONObject using org.json
            JSONObject jsonObj = XML.toJSONObject(xml);

            // Get the COMMAND object
            JSONObject command = jsonObj.getJSONObject("COMMAND");

            // Create a new JSONObject with our desired structure
            JSONObject result = new JSONObject();
            result.put("type", command.getString("TYPE"));
            result.put("transactionId", command.get("TXNID").toString());
            result.put("msisdn", command.get("MSISDN").toString());
            result.put("amount", command.get("AMOUNT").toString());
            result.put("companyName", command.get("COMPANYNAME").toString());
            result.put("customerReferenceId", command.get("CUSTOMERREFERENCEID").toString());

            // Convert to pretty-printed string
            return result.toString(2);
        } catch (Exception e) {
            log.error("Error converting XML to JSON: " + e.getMessage(), e);
            throw e;
        }
    }

    //Airtel Money

   /* public PayBillRequestDTO extractPayloadFromSoap(String soapRequest) {
        try {
            // Remove SOAP envelope
            String payload = soapRequest.replaceAll("<soap:Envelope[^>]*>|</soap:Envelope>|<soap:Body>|</soap:Body>", "").trim();

            // Use JAXB to unmarshal the XML
            JAXBContext jaxbContext = JAXBContext.newInstance(PayBillRequestDTO.class);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            StringReader reader = new StringReader(payload);
            return (PayBillRequestDTO) unmarshaller.unmarshal(reader);
        } catch (Exception e) {
            throw new ValidationException("Invalid SOAP payload");
        }
    }*/

    public PayBillRequestDTO extractPayloadFromSoap(String soapRequest) {
        try {
            // Remove SOAP envelope but preserve namespace information
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(new InputSource(new StringReader(soapRequest)));

            // Get the requestToken element
            NodeList nodes = doc.getElementsByTagNameNS("http://www.airtel.com/", "requestToken");
            if (nodes.getLength() == 0) {
                throw new ValidationException("Missing requestToken element");
            }

            // Convert node to string for JAXB
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(nodes.item(0)), new StreamResult(writer));
            String xmlString = writer.toString();

            // Use JAXB to unmarshal
            JAXBContext jaxbContext = JAXBContext.newInstance(PayBillRequestDTO.class);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            StringReader reader = new StringReader(xmlString);
            return (PayBillRequestDTO) unmarshaller.unmarshal(reader);
        } catch (Exception e) {
            log.error("Error parsing SOAP request", e);
            throw new ValidationException("Invalid SOAP payload: " + e.getMessage());
        }
    }

    public PayBillPayment processPayment(PayBillRequestDTO request, PayBillPaymentService payBillPaymentService, MnoServiceImpl mnoService, PaymentUtilities paymentUtilities, MainAccountService mainAccountService, VendorService vendorService) {
        // Validate request
        validateRequest(request);
        VendorDetails vendorDetails = vendorService.findVendorDetailsByCode("SC001")
                .orElseThrow(() -> new RuntimeException("Vendor not found"));
        // Create payment entity
        PayBillPayment payment = PayBillPayment.builder()
                .payBillId(request.getTransID())
                .paymentReference(paymentUtilities.generateRefNumber())
                .originalReference(request.getReferenceField())
                .amount(request.getAmount())
                .currency("TZS") // Assuming Tanzania Shillings
                .msisdn(paymentUtilities.formatPhoneNumber("255", request.getMSISDN()))
                .operator(mnoService.searchMno(paymentUtilities.formatPhoneNumber("255", request.getMSISDN())))
                .transactionDate(LocalDateTime.now().toString())
                .collectionStatus(CollectionStatus.COLLECTED)
                .receiptNumber(request.getTransID())
                .vendorDetails(vendorDetails)
                .accountId(String.valueOf(mainAccountService.findMainAccountByAccountNumber("100019802002").getId()))
                .build();

        // Save and return
        return payBillPaymentService.createPayBill(payment);
    }

    private void validateRequest(PayBillRequestDTO request) {
        if (request == null) {
            throw new ValidationException("Request payload cannot be null");
        }

        if (!"Airtel".equals(request.getAPIusername())) {
            throw new ValidationException("Invalid API username");
        }

        if (!"20M3et1604".equals(request.getAPIPassword())) {
            throw new ValidationException("Invalid API password");
        }
    }

    // Custom exception for validation
    public class ValidationException extends RuntimeException {
        public ValidationException(String message) {
            super(message);
        }
    }


    public OperatorMapping manageSession(PaymentRequest request, Order order) {
        AtomicReference<Optional<OperatorMapping>> operatorMapping = new AtomicReference<>();
        AtomicReference<MnoPrefix> mnoPrefixAtomic = new AtomicReference<>();
        // Get mobile network operator
        String operatorPrefix = this.paymentUtilities.getOperatorPrefix(request.getMsisdn());
        //Retrieve MNO from Redis
        Optional<OperatorMapping> operator = this.operatorManagementService.getOperator(operatorPrefix);
        if (operator.isEmpty()) {
            // If not found in Redis, call database service to get MNO
            MnoPrefix prefix = this.mnoService.getMno(paymentUtilities.formatPhoneNumber("255", request.getMsisdn()));
            //validate result
            if (prefix == null) {
                throw new CustomExcpts.OperatorNotFoundException("Operator not found for prefix: " + operatorPrefix);
            }
            // Get Mno and map the value to record in Redis
            operator = Optional.of(new OperatorMapping(String.valueOf(prefix.getMnoMapping().getId()), prefix.getMnoMapping().getMno(), prefix.getPrefix(), prefix.getMnoMapping(), "TZ", LocalDateTime.now(), LocalDateTime.now()));
            this.operatorManagementService.createOperator(operator.get());
            mnoPrefixAtomic.set(prefix);
        }
        operatorMapping.set(operator);

        OperatorMapping mapping = this.extractOperatorDetails(operatorMapping.get(), mnoPrefixAtomic.get());
        // check for network configuration from Redis
        log.debug("Session value is:- " + String.valueOf(order.getPaymentSessionId()));
        Optional<UserSession> userSession = this.sessionManagementService.getSession(order.getPaymentSessionId());
        log.debug("Session is present:- " + String.valueOf(userSession.isPresent()));
        Optional<OperatorMapping> finalOperator = operator;
        userSession.ifPresentOrElse(userSession1 -> {

            // check if the session is still valid
            if (userSession1.getLastAccessedAt().isBefore(LocalDateTime.now().minusMinutes(DEFAULT_SESSION_EXPIRY))) {
                log.debug("Session is expired 01:- " + String.valueOf(userSession1.getLastAccessedAt().isBefore(LocalDateTime.now().minusMinutes(DEFAULT_SESSION_EXPIRY))));

                // start checking for operator configurations from external service
                MobileNetworkType networkType = networkConfResource.getNetworkType(mapping.getOperatorName(), userSession1);

                // Get network configuration
                MobileNetworkConfig networkConfig = this.networkConfResource.getOperatorConf(mapping.getOperatorName(), userSession1);

                // Set network configuration in user session
                userSession1.setSelectedNetworkType(networkType);
                userSession1.setNetworkConfig(networkConfig);

                this.sessionManagementService.updateSession(order.getPaymentSessionId(), userSession1);

            } else {
                // log.debug(">>>>>>>>>>>>>>>Vendor External Id"+this.vendorManagementService.getVendor(userSession1.getMerchantId()).get().getVendorExternalId());
                // TODO: Validate vendor - (to be removed if not needed)
                // validate vendor
                VendorInfo vendorInfo = externalResources.validateVendor(order, this.vendorManagementService, mapping.getOperatorName(), this.vendorManagementService.getVendor(userSession1.getMerchantId()));

                // create Payment Method on and Payment Channel on Database

                externalResources.createPaymentMethod(vendorInfo);
                externalResources.createPaymentChannel(getOrderRequestDto(order), vendorInfo);

                PaymentMethod paymentMethod = this.externalResources.getPaymentMethod(request.getPaymentMethod());
                String paymentChannel = this.externalResources.getPaymentChannel(request.getPaymentChannel()).name();

                // create commissionTire on Database

                CommissionTier commissionTireFuture = externalResources.createCommissionTire(vendorInfo, getOrderRequestDto(order), request.getPaymentMethod(), paymentChannel, mapping.getMnoMapping()).join();


                // set commission configurations for Redis
                CommissionConfig commissionConfig = this.externalResources.getCommissionConfig(vendorInfo, commissionTireFuture, paymentMethod, paymentChannel, finalOperator);


                // Store CommissionConfig in Redis
                userSession1.setCommissionConfig(commissionConfig);

                if (userSession1.getNetworkConfig() != null) {
                    // check if the network configuration has changed
                    MobileNetworkType networkType = networkConfResource.getNetworkType(mapping.getOperatorName(), userSession1);
                    log.debug("Network Type:- " + String.valueOf(networkType));
                    if (!userSession1.getSelectedNetworkType().equals(networkType)) {
                        // update network configuration
                        MobileNetworkConfig networkConfig = networkConfigService.getConfigByNetworkType(networkType);
                        userSession1.setSelectedNetworkType(networkType);
                        userSession1.setNetworkConfig(networkConfig);
                        //AirtelMoneyConfig airtelMoneyConfig = (AirtelMoneyConfig) userSession1.getNetworkConfig();

                        //log.debug("Airtel Money Token URL:- " + String.valueOf(airtelMoneyConfig.getTokenUrl()));
                        this.sessionManagementService.updateSession(order.getPaymentSessionId(), userSession1);
                    }
                }
                // start checking for operator configurations from external service
                MobileNetworkType networkType = networkConfResource.getNetworkType(mapping.getOperatorName(), userSession1);

                // Get network configuration
                MobileNetworkConfig networkConfig = this.networkConfResource.getOperatorConf(mapping.getOperatorName(), userSession1);


                // Set network configuration in user session
                userSession1.setSelectedNetworkType(networkType);
                userSession1.setNetworkConfig(networkConfig);
                // update last accessed time
                userSession1.setLastAccessedAt(LocalDateTime.now());

                //AirtelMoneyConfig airtelMoneyConfig = (AirtelMoneyConfig) userSession1.getNetworkConfig();
                //log.debug("Airtel Money Token URL:- " + String.valueOf(airtelMoneyConfig.getTokenUrl()));
                this.sessionManagementService.updateSession(order.getPaymentSessionId(), userSession1);
            }

        }, () -> {

            log.debug("Session is expired 02:- ");

            // validate vendor
            VendorInfo vendorInfo = externalResources.validateVendor(order, this.vendorManagementService, mapping.getOperatorName(), this.vendorManagementService.getVendor(order.getPartnerId()));

            // create Payment Method on and Payment Channel on Database

            externalResources.createPaymentMethod(vendorInfo);
            externalResources.createPaymentChannel(getOrderRequestDto(order), vendorInfo);


            PaymentMethod paymentMethod = this.externalResources.getPaymentMethod(request.getPaymentMethod());
            String paymentChannel = this.externalResources.getPaymentChannel(request.getPaymentChannel()).name();

            // create commissionTire on Database

            CommissionTier commissionTireFuture = externalResources.createCommissionTire(vendorInfo, getOrderRequestDto(order), request.getPaymentMethod(), paymentChannel, mapping.getMnoMapping()).join();

            // set commission configurations for Redis
            CommissionConfig commissionConfig = this.externalResources.getCommissionConfig(vendorInfo, commissionTireFuture, paymentMethod, paymentChannel, finalOperator);

            // session expired, create a new session
            UserSession sessionForUser = this.initializePaymentSession(String.valueOf(order.getCustomer().getId()), order.getPartnerId());

            // Store CommissionConfig in Redis
            sessionForUser.setCommissionConfig(commissionConfig);

            // Store IP and device info
            sessionForUser.setIpAddress(order.getIpAddress());
            sessionForUser.setDeviceInfo(order.getDeviceInfo());

            // start checking for operator configurations from external service
            MobileNetworkType networkType = networkConfResource.getNetworkType(mapping.getOperatorName(), sessionForUser);

            // Get network configuration
            MobileNetworkConfig networkConfig = this.networkConfResource.getOperatorConf(mapping.getOperatorName(), sessionForUser);

            // Set network configuration in user session
            sessionForUser.setSelectedNetworkType(networkType);
            sessionForUser.setNetworkConfig(networkConfig);

            // Store Order Details
            sessionForUser.setOrderNumber(order.getOrderNumber());
            sessionForUser.setReceiptNumber(order.getReceipt());


            // Create session in Redis
            String sessionId = this.sessionManagementService.createSession(sessionForUser);

            //update order with new session ID
            order.setPaymentSessionId(sessionId);
            this.orderService.updateOrder(order);
        });
        return mapping;
    }

    /**
     * Initialize a payment session with 3DS capabilities
     */
    public UserSession initializePaymentSession(String userId, String merchantId) {

        // Add 3DS capabilities flag to session
        // session.addAttribute("3dsEnabled", properties.isEnable3ds());

        return UserSession.builder()
                .userId(userId)
                .merchantId(merchantId)
                .createdAt(LocalDateTime.now())
                .lastAccessedAt(LocalDateTime.now())
                .authenticated(true)
                .mfaVerified(false)
                .transactionStatus(UserSession.TransactionStatus.PENDING)
                .roles(Collections.singletonList("PAYMENT_USER"))
                .attributes(new HashMap<>())
                .build();
    }

    private static OrderRequestDto getOrderRequestDto(Order order) {
        return OrderRequestDto.builder()
                .amount((int) order.getAmount())
                .currency(order.getCurrency())
                .receipt(order.getReceipt())
                .metadata(order.getMetadata())
                .customers(CustomerDto.builder()
                        .firstName(order.getCustomer().getFirstName())
                        .lastName(order.getCustomer().getLastName())
                        .email(order.getCustomer().getEmail())
                        .phoneNumber(order.getCustomer().getPhoneNumber() != null ? order.getCustomer().getPhoneNumber() : null)
                        .build())
                .build();
    }

    private OperatorMapping extractOperatorDetails(Optional<OperatorMapping> operatorMapping, MnoPrefix mnoPrefix) {
        if (mnoPrefix != null) {
            return OperatorMapping.builder()
                    .operatorId(String.valueOf(mnoPrefix.getId()))
                    .createdAt(LocalDateTime.now())
                    .lastAccessedAt(LocalDateTime.now())
                    .operatorCountryCode(mnoPrefix.getCountryName())
                    .operatorName(mnoPrefix.getMnoMapping().getMno())
                    .operatorPrefix(mnoPrefix.getPrefix())
                    .build();

        }
        if (operatorMapping.isPresent()) {
            return operatorMapping.get();
        } else {
            throw new CustomExcpts.OperatorNotFoundException("Operator not found for prefix: ");
        }

    }



}
