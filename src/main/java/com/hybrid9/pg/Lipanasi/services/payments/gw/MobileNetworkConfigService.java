package com.hybrid9.pg.Lipanasi.services.payments.gw;

import com.hybrid9.pg.Lipanasi.enums.MobileNetworkType;
import com.hybrid9.pg.Lipanasi.models.pgmodels.mobilenetworks.*;
import com.hybrid9.pg.Lipanasi.models.session.UserSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Service
public class MobileNetworkConfigService {
    // HaloPesa Configuration
    private String halopesaApiUrl;

    private String halopesaSpId;

    private String halopesaUsername;

    private String halopesaPassword;

    private String halopesaBusinessNumber;

    private String halopesaBeneficiaryAccountId;

    private String halopesaSecretKey;

    private String halopesaMerchantCode;

    private String halopesaTqsUrl;

    private String halopesaStatus;

    // M-Pesa Configuration
    private String mpesaApiBaseUrl;

    private String mpesaCountry;

    private String mpesaUsername;

    private String mpesaPassword;

    private String mpesaTokenEventId;

    private String mpesaRequestEventId;

    private String mpesaBusinessName;

    private String mpesaBusinessNumber;

    private String mpesaCallbackUrl;

    private String mpesaPaybillApiBaseUrl;

    private String mpesaPaybillCallbackUrl;

    private String mpesaPaybillSpId;

    private String mpesaPaybillSpPassword;

    private String mpesaTokenApiUrl;

    private String mpesaStatus;

    // MixxByYas Configuration
    private String mixxbyyasApiUrl;

    private String mixxbyyasTokenUrl;

    private String mixxbyyasBillerMsisdn;

    private String mixxbyyasCallbackUrl;

    private String mixxbyyasUsername;

    private String mixxbyyasPassword;

    private String mixxbyyasTqsApiUrl;

    private String mixxbyyasTqsPassword;

    private String mixxbyyasStatus;

    // Airtel Money Configuration
    private String airtelMoneyApiUrl;

    private String airtelMoneyTokenUrl;

    private String airtelMoneyBaseUrl;

    private String airtelMoneyCountry;

    private String airtelMoneyCurrency;

    private String airtelMoneyClientId;

    private String airtelMoneyClientSecret;

    private String airtelMoneyStatus;


    /**
     * Default constructor for Spring dependency injection
     */
    public MobileNetworkConfigService() {
        // Default constructor for Spring to use with @Value annotations
    }

    /**
     * Constructor for HaloPesa configuration
     *
     * @param halopesaApiUrl       API URL for HaloPesa
     * @param halopesaSpId         Service Provider ID for HaloPesa
     * @param halopesaSecretKey    Secret key for HaloPesa
     * @param halopesaMerchantCode Merchant code for HaloPesa
     * @param halopesaTqsUrl       TQS URL for HaloPesa
     * @param halopesaStatus       Status for HaloPesa
     *                              @param halopesaUsername       Username for HaloPesa
     *                              @param halopesaPassword       Password for HaloPesa
     *                              @param halopesaBusinessNumber Business number for HaloPesa
     *                              @param halopesaBeneficiaryAccountId Beneficiary account ID for HaloPesa
     */
    public MobileNetworkConfigService(String halopesaApiUrl, String halopesaSpId,
                                      String halopesaSecretKey, String halopesaMerchantCode,
                                      String halopesaTqsUrl, String halopesaStatus,
                                      String halopesaUsername, String halopesaPassword,
                                      String halopesaBusinessNumber, String halopesaBeneficiaryAccountId) {
        this.halopesaApiUrl = halopesaApiUrl;
        this.halopesaSpId = halopesaSpId;
        this.halopesaSecretKey = halopesaSecretKey;
        this.halopesaMerchantCode = halopesaMerchantCode;
        this.halopesaTqsUrl = halopesaTqsUrl;
        this.halopesaStatus = halopesaStatus;
        this.halopesaUsername = halopesaUsername;
        this.halopesaPassword = halopesaPassword;
        this.halopesaBusinessNumber = halopesaBusinessNumber;
        this.halopesaBeneficiaryAccountId = halopesaBeneficiaryAccountId;
    }

    /**
     * Constructor for M-Pesa configuration
     *
     * @param mpesaApiBaseUrl         Base API URL for M-Pesa
     * @param mpesaCountry            Country code for M-Pesa
     * @param mpesaUsername           Username for M-Pesa API
     * @param mpesaPassword           Password for M-Pesa API
     * @param mpesaTokenEventId       Token event ID for M-Pesa
     * @param mpesaRequestEventId     Request event ID for M-Pesa
     * @param mpesaBusinessName       Business name for M-Pesa
     * @param mpesaBusinessNumber     Business number for M-Pesa
     * @param mpesaCallbackUrl        Callback URL for M-Pesa
     * @param mpesaPaybillApiBaseUrl  Paybill API base URL
     * @param mpesaPaybillCallbackUrl Paybill callback URL
     * @param mpesaPaybillSpId        Paybill service provider ID
     * @param mpesaPaybillSpPassword  Paybill service provider password
     * @param mpesaTokenApiUrl        Token API URL for M-Pesa
     * @param mpesaStatus       Status for M-Pesa
     */
    public MobileNetworkConfigService(String mpesaApiBaseUrl, String mpesaCountry,
                                      String mpesaUsername, String mpesaPassword,
                                      String mpesaTokenEventId, String mpesaRequestEventId,
                                      String mpesaBusinessName, String mpesaBusinessNumber,
                                      String mpesaCallbackUrl, String mpesaPaybillApiBaseUrl,
                                      String mpesaPaybillCallbackUrl, String mpesaPaybillSpId,
                                      String mpesaPaybillSpPassword, String mpesaTokenApiUrl,
                                      String mpesaStatus) {
        this.mpesaApiBaseUrl = mpesaApiBaseUrl;
        this.mpesaCountry = mpesaCountry;
        this.mpesaUsername = mpesaUsername;
        this.mpesaPassword = mpesaPassword;
        this.mpesaTokenEventId = mpesaTokenEventId;
        this.mpesaRequestEventId = mpesaRequestEventId;
        this.mpesaBusinessName = mpesaBusinessName;
        this.mpesaBusinessNumber = mpesaBusinessNumber;
        this.mpesaCallbackUrl = mpesaCallbackUrl;
        this.mpesaPaybillApiBaseUrl = mpesaPaybillApiBaseUrl;
        this.mpesaPaybillCallbackUrl = mpesaPaybillCallbackUrl;
        this.mpesaPaybillSpId = mpesaPaybillSpId;
        this.mpesaPaybillSpPassword = mpesaPaybillSpPassword;
        this.mpesaTokenApiUrl = mpesaTokenApiUrl;
        this.mpesaStatus = mpesaStatus;
    }

    /**
     * Constructor for MixxByYas configuration
     *
     * @param mixxbyyasApiUrl       API URL for MixxByYas
     * @param mixxbyyasTokenUrl     Token URL for MixxByYas
     * @param mixxbyyasBillerMsisdn Biller MSISDN for MixxByYas
     * @param mixxbyyasCallbackUrl  Callback URL for MixxByYas
     * @param mixxbyyasUsername     Username for MixxByYas
     * @param mixxbyyasPassword     Password for MixxByYas
     * @param mixxbyyasTqsApiUrl    TQS API URL for MixxByYas
     * @param mixxbyyasTqsPassword  TQS Password for MixxByYas     *
     * @param mixxbyyasStatus       Status for MixxByYas
     */
    public MobileNetworkConfigService(String mixxbyyasApiUrl, String mixxbyyasTokenUrl,
                                      String mixxbyyasBillerMsisdn, String mixxbyyasCallbackUrl,
                                      String mixxbyyasUsername, String mixxbyyasPassword,
                                      String mixxbyyasTqsApiUrl, String mixxbyyasTqsPassword,
                                      String mixxbyyasStatus) {
        this.mixxbyyasApiUrl = mixxbyyasApiUrl;
        this.mixxbyyasTokenUrl = mixxbyyasTokenUrl;
        this.mixxbyyasBillerMsisdn = mixxbyyasBillerMsisdn;
        this.mixxbyyasCallbackUrl = mixxbyyasCallbackUrl;
        this.mixxbyyasUsername = mixxbyyasUsername;
        this.mixxbyyasPassword = mixxbyyasPassword;
        this.mixxbyyasTqsApiUrl = mixxbyyasTqsApiUrl;
        this.mixxbyyasTqsPassword = mixxbyyasTqsPassword;
        this.mixxbyyasStatus = mixxbyyasStatus;
    }

    /**
     * Constructor for Airtel Money configuration
     *
     * @param airtelMoneyApiUrl       API URL for Airtel Money
     * @param airtelMoneyTokenUrl     Token URL for Airtel Money
     * @param airtelMoneyBaseUrl      Base URL for Airtel Money
     * @param airtelMoneyCountry      Country code for Airtel Money
     * @param airtelMoneyCurrency     Currency code for Airtel Money
     * @param airtelMoneyClientId     Client ID for Airtel Money
     * @param airtelMoneyClientSecret Client Secret for Airtel Money
     * @param airtelMoneyStatus       Status for Airtel Money
     */
    public MobileNetworkConfigService(String airtelMoneyApiUrl, String airtelMoneyTokenUrl,
                                      String airtelMoneyBaseUrl, String airtelMoneyCountry,
                                      String airtelMoneyCurrency, String airtelMoneyClientId,
                                      String airtelMoneyClientSecret, String airtelMoneyStatus) {
        this.airtelMoneyApiUrl = airtelMoneyApiUrl;
        this.airtelMoneyTokenUrl = airtelMoneyTokenUrl;
        this.airtelMoneyBaseUrl = airtelMoneyBaseUrl;
        this.airtelMoneyCountry = airtelMoneyCountry;
        this.airtelMoneyCurrency = airtelMoneyCurrency;
        this.airtelMoneyClientId = airtelMoneyClientId;
        this.airtelMoneyClientSecret = airtelMoneyClientSecret;
        this.airtelMoneyStatus = airtelMoneyStatus;
    }

    /**
     * Complete constructor with all mobile network configurations
     *
     * @param halopesaConfig    HaloPesa configuration parameters
     * @param mpesaConfig       M-Pesa configuration parameters
     * @param mixxByYasConfig   MixxByYas configuration parameters
     * @param airtelMoneyConfig Airtel Money configuration parameters
     *
     */
    public MobileNetworkConfigService(HaloPesaConfig halopesaConfig, MPesaConfig mpesaConfig,
                                      MixxByYasConfig mixxByYasConfig, AirtelMoneyConfig airtelMoneyConfig) {
        // HaloPesa
        this.halopesaApiUrl = halopesaConfig.getApiUrl();
        this.halopesaSpId = halopesaConfig.getSpId();
        this.halopesaSecretKey = halopesaConfig.getSecretKey();
        this.halopesaMerchantCode = halopesaConfig.getMerchantCode();
        this.halopesaTqsUrl = halopesaConfig.getTqsUrl();

        // M-Pesa
        this.mpesaApiBaseUrl = mpesaConfig.getApiUrl();
        this.mpesaCountry = mpesaConfig.getCountry();
        this.mpesaUsername = mpesaConfig.getUsername();
        this.mpesaPassword = mpesaConfig.getPassword();
        this.mpesaTokenEventId = mpesaConfig.getTokenEventId();
        this.mpesaRequestEventId = mpesaConfig.getRequestEventId();
        this.mpesaBusinessName = mpesaConfig.getBusinessName();
        this.mpesaBusinessNumber = mpesaConfig.getBusinessNumber();
        this.mpesaCallbackUrl = mpesaConfig.getCallbackUrl();
        this.mpesaPaybillApiBaseUrl = mpesaConfig.getPaybillApiBaseUrl();
        this.mpesaPaybillCallbackUrl = mpesaConfig.getPaybillCallbackUrl();
        this.mpesaPaybillSpId = mpesaConfig.getPaybillSpId();
        this.mpesaPaybillSpPassword = mpesaConfig.getPaybillSpPassword();
        this.mpesaTokenApiUrl = mpesaConfig.getTokenApiUrl();

        // MixxByYas
        this.mixxbyyasApiUrl = mixxByYasConfig.getApiUrl();
        this.mixxbyyasTokenUrl = mixxByYasConfig.getTokenUrl();
        this.mixxbyyasBillerMsisdn = mixxByYasConfig.getBillerMsisdn();
        this.mixxbyyasCallbackUrl = mixxByYasConfig.getCallbackUrl();
        this.mixxbyyasUsername = mixxByYasConfig.getUsername();
        this.mixxbyyasPassword = mixxByYasConfig.getPassword();
        this.mixxbyyasTqsApiUrl = mixxByYasConfig.getTqsApiUrl();
        this.mixxbyyasTqsPassword = mixxByYasConfig.getTqsPassword();

        // Airtel Money
        this.airtelMoneyApiUrl = airtelMoneyConfig.getApiUrl();
        this.airtelMoneyTokenUrl = airtelMoneyConfig.getTokenUrl();
        this.airtelMoneyBaseUrl = airtelMoneyConfig.getBaseUrl();
        this.airtelMoneyCountry = airtelMoneyConfig.getCountry();
        this.airtelMoneyCurrency = airtelMoneyConfig.getCurrency();
        this.airtelMoneyClientId = airtelMoneyConfig.getClientId();
        this.airtelMoneyClientSecret = airtelMoneyConfig.getClientSecret();
    }


    /**
     * Get HaloPesa configuration
     *
     * @return HaloPesa configuration object
     */
    public HaloPesaConfig getHaloPesaConfig() {
        return HaloPesaConfig.builder()
                .apiUrl(halopesaApiUrl)
                .spId(halopesaSpId)
                .secretKey(halopesaSecretKey)
                .merchantCode(halopesaMerchantCode)
                .tqsUrl(halopesaTqsUrl)
                .status(halopesaStatus)
                .build();
    }

    /**
     * Get M-Pesa configuration
     *
     * @return M-Pesa configuration object
     */
    public MPesaConfig getMPesaConfig() {
        return MPesaConfig.builder()
                .apiUrl(mpesaApiBaseUrl)
                .callbackUrl(mpesaCallbackUrl)
                .country(mpesaCountry)
                .username(mpesaUsername)
                .password(mpesaPassword)
                .tokenEventId(mpesaTokenEventId)
                .requestEventId(mpesaRequestEventId)
                .businessName(mpesaBusinessName)
                .businessNumber(mpesaBusinessNumber)
                .paybillApiBaseUrl(mpesaPaybillApiBaseUrl)
                .paybillCallbackUrl(mpesaPaybillCallbackUrl)
                .paybillSpId(mpesaPaybillSpId)
                .paybillSpPassword(mpesaPaybillSpPassword)
                .tokenApiUrl(mpesaTokenApiUrl)
                .status(mpesaStatus)
                .build();
    }

    /**
     * Get MixxByYas configuration
     *
     * @return MixxByYas configuration object
     */
    public MixxByYasConfig getMixxByYasConfig() {
        return MixxByYasConfig.builder()
                .apiUrl(mixxbyyasApiUrl)
                .tokenUrl(mixxbyyasTokenUrl)
                .billerMsisdn(mixxbyyasBillerMsisdn)
                .callbackUrl(mixxbyyasCallbackUrl)
                .username(mixxbyyasUsername)
                .password(mixxbyyasPassword)
                .tqsApiUrl(mixxbyyasTqsApiUrl)
                .tqsPassword(mixxbyyasTqsPassword)
                .status(mixxbyyasStatus)
                .build();
    }

    /**
     * Get Airtel Money configuration
     *
     * @return Airtel Money configuration object
     */
    public AirtelMoneyConfig getAirtelMoneyConfig() {
        return AirtelMoneyConfig.builder()
                .apiUrl(airtelMoneyApiUrl)
                .tokenUrl(airtelMoneyTokenUrl)
                .baseUrl(airtelMoneyBaseUrl)
                .country(airtelMoneyCountry)
                .currency(airtelMoneyCurrency)
                .clientId(airtelMoneyClientId)
                .clientSecret(airtelMoneyClientSecret)
                .status(airtelMoneyStatus)
                .build();
    }

    /**
     * Get mobile network configuration by type
     *
     * @param networkType Type of mobile network
     * @return Configuration for the specified network type
     */
    public MobileNetworkConfig getConfigByNetworkType(MobileNetworkType networkType) {
        switch (networkType) {
            case HALOPESA:
                return getHaloPesaConfig();
            case MPESA:
                return getMPesaConfig();
            case MIXXBYYAS:
                return getMixxByYasConfig();
            case AIRTEL_MONEY:
                return getAirtelMoneyConfig();
            default:
                throw new IllegalArgumentException("Unsupported network type: " + networkType);
        }
    }

}
