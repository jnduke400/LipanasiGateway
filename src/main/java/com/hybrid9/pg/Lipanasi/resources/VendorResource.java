package com.hybrid9.pg.Lipanasi.resources;

import com.hybrid9.pg.Lipanasi.dto.*;
import com.hybrid9.pg.Lipanasi.dto.commission.PaymentChannelConfig;
import com.hybrid9.pg.Lipanasi.dto.commission.PaymentMethodConfig;
import com.hybrid9.pg.Lipanasi.dto.order.OrderRequestDto;
import com.hybrid9.pg.Lipanasi.dto.order.VendorInfo;
import com.hybrid9.pg.Lipanasi.entities.payments.commission.CommissionTier;
import com.hybrid9.pg.Lipanasi.entities.payments.pymtmethods.PaymentMethod;
import com.hybrid9.pg.Lipanasi.entities.vendorx.Address;
import com.hybrid9.pg.Lipanasi.entities.vendorx.Business;
import com.hybrid9.pg.Lipanasi.entities.vendorx.MainAccount;
import com.hybrid9.pg.Lipanasi.entities.vendorx.VendorDetails;
import com.hybrid9.pg.Lipanasi.enums.PaymentChannel;
import com.hybrid9.pg.Lipanasi.enums.PaymentMethodType;
import com.hybrid9.pg.Lipanasi.enums.VendorStatus;
import com.hybrid9.pg.Lipanasi.models.pgmodels.commissions.CommissionConfig;
import com.hybrid9.pg.Lipanasi.models.pgmodels.vendorx.VendorManager;
import com.hybrid9.pg.Lipanasi.resources.excpts.CustomExcpts;
import com.hybrid9.pg.Lipanasi.serviceImpl.UserManager;
import com.hybrid9.pg.Lipanasi.serviceImpl.vendorx.BusinessServiceImpl;
import com.hybrid9.pg.Lipanasi.services.RoleService;
import com.hybrid9.pg.Lipanasi.services.commission.CommissionTierService;
import com.hybrid9.pg.Lipanasi.services.payments.PaymentMethodService;
import com.hybrid9.pg.Lipanasi.services.payments.vendorx.VendorManagementService;
import com.hybrid9.pg.Lipanasi.services.vendorx.*;
import com.hybrid9.pg.Lipanasi.utilities.PaymentUtilities;
import com.hybrid9.pg.Lipanasi.utilities.VendorUtilities;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
@AllArgsConstructor
public class VendorResource {
    private PaymentUtilities paymentUtilities;
    private RoleService roleService;
    private MainAccountService mainAccountService;
    private BusinessServiceImpl businessService;
    private AddressService addressService;
    private VendorService vendorService;
    //private UserRepository userRepository;
    private UserManager userManager;
    private final VendorUtilities vendorUtilities;

    public void createVendor(VendorCreatorDto vendorDto, VendorManagementService vendorManagementService, OrderRequestDto orderRequestDto) {
        // Create VendorManager Object
        VendorManager vendorManager = VendorManager.builder()
                .vendorCode(vendorDto.getVendorDto().getVendorCode())
                .vendorAccountNumber(vendorDto.getVendorAccountDto().getAccountNumber())
                .vendorId(UUID.randomUUID().toString())
                .vendorExternalId(vendorDto.getVendorDto().getExternalId())
                .vendorName(vendorDto.getVendorDto().getVendorName())
                .vendorStatus(vendorDto.getVendorDto().getStatus().name())
                .vendorCharges(String.valueOf(vendorDto.getVendorDto().getCharges()))
                .vendorHasCommission(vendorDto.getVendorDto().getHasCommission())
                .vendorKey(vendorDto.getVendorDto().getKey())
                .vendorHasVat(vendorDto.getVendorDto().getHasVat())
                .vendorVatType(vendorDto.getVendorDto().getVatType())
                .vendorCallbackUrl(vendorDto.getVendorDto().getCallbackUrl())
                .build();

        // Store VendorManager in redis
        String vendorExternalId = vendorManagementService.storeVendor(vendorManager);

        Optional.ofNullable(vendorExternalId).ifPresent(vendorExternalId1 -> {

            Optional<VendorDetails> vendorDetailsByCode = this.vendorService.findVendorDetailsByCode(vendorDto.getVendorDto().getVendorCode());
            if (vendorDetailsByCode.isPresent()) {

                // start updating vendorDetails in the database

                vendorDetailsByCode.get().getBusiness().setName(vendorDto.getBusiness().getName());
                vendorDetailsByCode.get().getBusiness().setBusinessType(vendorDto.getBusiness().getBusinessType());
                vendorDetailsByCode.get().setHasCommission(vendorDto.getVendorDto().getHasCommission());
                vendorDetailsByCode.get().setHasVat(vendorDto.getVendorDto().getHasVat());
                vendorDetailsByCode.get().setVatType(vendorDto.getVendorDto().getVatType());
                vendorDetailsByCode.get().setCharges(vendorDto.getVendorDto().getCharges());
                vendorDetailsByCode.get().setVendorExternalId(vendorDto.getVendorDto().getExternalId());
                vendorDetailsByCode.get().setStatus(vendorDto.getVendorDto().getStatus());
                vendorDetailsByCode.get().setBillNumber(vendorDto.getVendorDto().getBillNumber());
                vendorDetailsByCode.get().setVendorCode(vendorDto.getVendorDto().getVendorCode());
                vendorDetailsByCode.get().setVendorName(vendorDto.getVendorDto().getVendorName());
                vendorDetailsByCode.get().setCallbackUrl(vendorDto.getVendorDto().getCallbackUrl());

                VendorDetails vendorDetails = this.vendorService.updateVendorDetails(vendorDetailsByCode.get());

                MainAccount mainAccount = this.mainAccountService.findByVendorDetails(vendorDetails);
                mainAccount.setAccountNumber(vendorDto.getVendorAccountDto().getAccountNumber());
                mainAccount.setAccountName(vendorDto.getVendorAccountDto().getAccountName());
                mainAccount.setVendorDetails(vendorDetails);
                this.mainAccountService.update(mainAccount);

            } else {
                // start creating vendorx in the database
                Business business = Business.builder()
                        .name(vendorDto.getBusiness().getName())
                        .businessType(vendorDto.getBusiness().getBusinessType())
                        .build();

                Business businessResult = this.businessService.registerBusiness(business);

                //this.businessService.registerBusiness(business);
                Address address = Address
                        .builder()
                        .street(vendorDto.getAddress().getStreet())
                        .city(vendorDto.getAddress().getCity())
                        .state(vendorDto.getAddress().getState())
                        .zip(vendorDto.getAddress().getZip())
                        .country(vendorDto.getAddress().getCountry())
                        .houseNumber(vendorDto.getAddress().getHouseNumber())
                        .build();


                VendorDetails vendorDetails = VendorDetails
                        .builder()
                        .vendorName(vendorDto.getVendorDto().getVendorName())
                        .vendorCode(vendorDto.getVendorDto().getVendorCode())
                        .business(businessResult)
                        //.address(this.addressService.registerAddress(address))
                        .address(address)
                        .hasCommission(vendorDto.getVendorDto().getHasCommission())
                        .hasVat(vendorDto.getVendorDto().getHasVat())
                        .vatType(vendorDto.getVendorDto().getVatType())
                        .charges(vendorDto.getVendorDto().getCharges())
                        .vendorExternalId(vendorDto.getVendorDto().getExternalId())
                        .status(VendorStatus.ACTIVE)
                        .user(Optional.ofNullable(userManager.findUserByUsername("schoop@shoppingcart.co.tz")).orElseThrow(() -> new CustomExcpts.UnauthorizedException("Invalid authentication")))
                        .billNumber(vendorDto.getVendorDto().getBillNumber())
                        .callbackUrl(vendorDto.getVendorDto().getCallbackUrl())
                        .build();

                VendorDetails vendorDetails1 = this.vendorService.registerVendorDetails(vendorDetails);

                MainAccount mainAccount = MainAccount.builder()
                        .accountNumber(vendorDto.getVendorAccountDto().getAccountNumber())
                        .accountName(vendorDto.getVendorAccountDto().getAccountName())
                        .vendorDetails(vendorDetails1)
                        .currentAmount(0)
                        .desiredAmount(0)
                        .withdrowAmount(0)
                        .charges(0)
                        .vendorCharges(0)
                        .actualAmount(0)
                        .build();


                this.mainAccountService.createMainAccount(mainAccount);
            }
        });

    }

    public VendorCreatorDto createVendorObject(VendorInfo vendorData, OrderRequestDto orderRequestDto) {
        Optional<VendorDetails> vendorDetails = Optional.ofNullable(this.vendorService.findVendorDetailsByVendorExternalId(vendorData.getPartnerId()));
        if (vendorDetails.isPresent()) {

            // set VendorDetails Object

            VendorDto vendorDto = VendorDto.builder()
                    .billNumber(vendorDetails.get().getBillNumber())
                    .charges(Float.parseFloat(vendorData.getCharges()))
                    .hasCommission(vendorData.getHasCommission())
                    .hasVat(vendorData.getHasVat())
                    .vatType(vendorData.getVfdType())
                    .vendorCode(vendorDetails.get().getVendorCode())
                    .vendorName(vendorDetails.get().getVendorName())
                    .externalId(vendorData.getPartnerId())
                    .key(vendorData.getApiKey())
                    .callbackUrl(vendorData.getCallbackUrl())
                    .status(vendorDetails.get().getStatus())
                    .callbackUrl(vendorData.getCallbackUrl())
                    .build();
            VendorAccountDto vendorAccountDto = VendorAccountDto.builder()
                    .accountName("Gateway Account")
                    .accountNumber(this.mainAccountService.findByVendorDetails(vendorDetails.get()).getAccountNumber())
                    .build();
            BusinessDto businessDto = BusinessDto.builder()
                    .name("Shopping Business")
                    .businessType("Online")
                    .build();
            AddressDto address = AddressDto.builder()
                    .city("Dar es salaam")
                    .country("Tanzania")
                    .houseNumber("1234")
                    .state("Kinondoni")
                    .street("Wotebidi")
                    .zip("255")
                    .build();
            return VendorCreatorDto.builder()
                    .address(address)
                    .business(businessDto)
                    .dialCode("255")
                    .email(VendorUtilities.generateRandomEmail())
                    .vendorDto(vendorDto)
                    .vendorAccountDto(vendorAccountDto)
                    .build();
        } else {

            // set VendorDetails Object

            VendorDto vendorDto = VendorDto.builder()
                    .billNumber(vendorUtilities.getBillNumber())
                    .charges(Float.parseFloat(vendorData.getCharges()))
                    .hasCommission(vendorData.getHasCommission())
                    .hasVat(vendorData.getHasVat())
                    .vatType(vendorData.getVfdType())
                    .vendorCode(this.vendorUtilities.genVendorCode())
                    .vendorName(this.paymentUtilities.genReceiptNumber(10))
                    .status(VendorStatus.valueOf(vendorData.getStatus().toUpperCase()))
                    .key(vendorData.getApiKey())
                    .externalId(vendorData.getPartnerId())
                    .callbackUrl(vendorData.getCallbackUrl())
                    .build();
            VendorAccountDto vendorAccountDto = VendorAccountDto.builder()
                    .accountName("Gateway Account")
                    .accountNumber(this.vendorUtilities.genAccountNumber())
                    .build();
            BusinessDto businessDto = BusinessDto.builder()
                    .name("Shopping Business")
                    .businessType("Online")
                    .build();
            AddressDto address = AddressDto.builder()
                    .city("Dar es salaam")
                    .country("Tanzania")
                    .houseNumber("1234")
                    .state("Kinondoni")
                    .street("Wotebidi")
                    .zip("255")
                    .build();
            return VendorCreatorDto.builder()
                    .address(address)
                    .business(businessDto)
                    .dialCode("255")
                    .email(VendorUtilities.generateRandomEmail())
                    .vendorDto(vendorDto)
                    .vendorAccountDto(vendorAccountDto)
                    .build();
        }
    }

    /**
     * Update vendor details
     * @param vendorData
     */
    public void updateVendor(VendorInfo vendorData) {
        Optional<VendorDetails> vendorDetails = Optional.ofNullable(this.vendorService.findVendorDetailsByVendorExternalId(vendorData.getPartnerId()));
        if (vendorDetails.isPresent()) {
            VendorDetails vendor = vendorDetails.get();
            vendor.setHasCommission(vendorData.getHasCommission());
            vendor.setHasVat(vendorData.getHasVat());
            vendor.setCharges(Float.parseFloat(vendorData.getCharges()));
            vendor.setVendorExternalId(vendorData.getPartnerId());
            vendor.setStatus(VendorStatus.valueOf(vendorData.getStatus().toUpperCase()));
            vendor.setCallbackUrl(vendorData.getCallbackUrl());
            vendor.setApiKey(vendorData.getApiKey());
            this.vendorService.updateVendorDetails(vendor);
        }
    }
}
