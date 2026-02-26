package com.hybrid9.pg.Lipanasi.services.pushussd;


import com.hybrid9.pg.Lipanasi.configs.CustomRoutingDataSource;
import com.hybrid9.pg.Lipanasi.entities.vendorx.VendorDetails;
import com.hybrid9.pg.Lipanasi.enums.CollectionStatus;
import com.hybrid9.pg.Lipanasi.interfaces.projections.PushUssdProj;
import com.hybrid9.pg.Lipanasi.entities.payments.pushussd.PushUssd;
import com.hybrid9.pg.Lipanasi.repositories.pushussd.PushUssdRepository;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class PushUssdService {
    private PushUssdRepository ussdRepository;

    @Transactional(readOnly = true)
    public PushUssd findByMsisdnAndReferenceAndNonce(String msisdn, String reference, String nonce) {
        CustomRoutingDataSource.setCurrentDataSource("replica");
        PushUssd pushUssd = this.ussdRepository.findByMsisdnAndReferenceAndNonce(msisdn, reference, nonce);
        CustomRoutingDataSource.clearCurrentDataSource();
        return pushUssd;
    }

    @Transactional
    public PushUssd update(PushUssd pushUssd) {
        CustomRoutingDataSource.setCurrentDataSource("primary");
        pushUssd = this.ussdRepository.save(pushUssd);
        CustomRoutingDataSource.clearCurrentDataSource();
        return pushUssd;
    }

    @Transactional(readOnly = true)
    public List<PushUssd> findByCollectionStatus(CollectionStatus collectionStatus, String collectionType) {
        CustomRoutingDataSource.setCurrentDataSource("replica");
        List<PushUssd> pushUssds = this.ussdRepository.findTop10ByCollectionStatusAndCollectionType(collectionStatus, collectionType);
        CustomRoutingDataSource.clearCurrentDataSource();
        return pushUssds;
    }

    @Transactional(readOnly = true)
    public Page<PushUssdProj> findAllByVendorDetails(VendorDetails vendorDetailsInfo, String searchKey, int page, int limit, String sort, String order) {
        CustomRoutingDataSource.setCurrentDataSource("replica");
        if (order.equalsIgnoreCase("asc")) {
            Page<PushUssdProj> pageResult = this.ussdRepository.findAllByVendorDetailsAndAllCols(vendorDetailsInfo.getId(), searchKey, PageRequest.of(page, limit, Sort.by(sort).ascending()));
            CustomRoutingDataSource.clearCurrentDataSource();
            return pageResult;
        } else {
            Page<PushUssdProj> pageResult = this.ussdRepository.findAllByVendorDetailsAndAllCols(vendorDetailsInfo.getId(), searchKey, PageRequest.of(page, limit, Sort.by(sort).descending()));
            CustomRoutingDataSource.clearCurrentDataSource();
            return pageResult;
        }
    }

    @Transactional(readOnly = true)
    public Page<PushUssdProj> findAllByVendorDetails(VendorDetails vendorDetailsInfo, String phoneNumber, String amount, String reference, String clientName, String status, String invoiceNo, int page, int limit, String sort, String order) {
        CustomRoutingDataSource.setCurrentDataSource("replica");
        if (order.equalsIgnoreCase("asc")) {
            Page<PushUssdProj> pageResult = this.ussdRepository.findAllByVendorDetailsAndCollectionTypeAndAmountAndAllColumns(vendorDetailsInfo.getId(), phoneNumber, amount, reference, clientName, status, invoiceNo, PageRequest.of(page, limit, Sort.by(sort).ascending()));
            CustomRoutingDataSource.clearCurrentDataSource();
            return pageResult;
        } else {
            Page<PushUssdProj> pageResult = this.ussdRepository.findAllByVendorDetailsAndCollectionTypeAndAmountAndAllColumns(vendorDetailsInfo.getId(), phoneNumber, amount, reference, clientName, status, invoiceNo, PageRequest.of(page, limit, Sort.by(sort).descending()));
            CustomRoutingDataSource.clearCurrentDataSource();
            return pageResult;
        }
    }

    @Transactional(readOnly = true)
    public Page<PushUssdProj> findAllByVendorDetails(VendorDetails vendorDetailsInfo, int page, int limit, String sort, String order) {
        CustomRoutingDataSource.setCurrentDataSource("replica");
        if (order.equalsIgnoreCase("asc")) {
            Page<PushUssdProj> pageResult = this.ussdRepository.findAllByVendorDetails(vendorDetailsInfo.getId(), PageRequest.of(page, limit, Sort.by(sort).ascending()));
            CustomRoutingDataSource.clearCurrentDataSource();
            return pageResult;
        } else {
            Page<PushUssdProj> pageResult = this.ussdRepository.findAllByVendorDetails(vendorDetailsInfo.getId(), PageRequest.of(page, limit, Sort.by(sort).descending()));
            CustomRoutingDataSource.clearCurrentDataSource();
            return pageResult;
        }
    }

    @Transactional(readOnly = true)
    public Page<PushUssdProj> findAll(String phoneNumber, String amount, String reference, String clientName, String status, String invoice_no, int page, int limit, String sort, String order) {
        CustomRoutingDataSource.setCurrentDataSource("replica");
        if (order.equalsIgnoreCase("asc")) {
            Page<PushUssdProj> pageResult = this.ussdRepository.findAllByCollectionStatusAndAmountAndAllColumns(phoneNumber, amount, reference, clientName, status, invoice_no, PageRequest.of(page, limit, Sort.by(sort).ascending()));
            CustomRoutingDataSource.clearCurrentDataSource();
            return pageResult;
        } else {
            Page<PushUssdProj> pageResult = this.ussdRepository.findAllByCollectionStatusAndAmountAndAllColumns(phoneNumber, amount, reference, clientName, status, invoice_no, PageRequest.of(page, limit, Sort.by(sort).descending()));
            CustomRoutingDataSource.clearCurrentDataSource();
            return pageResult;
        }
    }

    @Transactional(readOnly = true)
    public PushUssd findByMsisdnAndReference(String msisdn, String reference) {
        CustomRoutingDataSource.setCurrentDataSource("replica");
        PushUssd pushUssd = this.ussdRepository.findByMsisdnAndReference(msisdn, reference);
        CustomRoutingDataSource.clearCurrentDataSource();
        return pushUssd;
    }

    @Transactional(readOnly = true)
    public List<PushUssd> findByCollectionStatusAndOperator(List<CollectionStatus> collectionStatusList, List<String> mnoList) {
        CustomRoutingDataSource.setCurrentDataSource("replica");
        if (collectionStatusList == null || mnoList == null) {
            return new ArrayList<>();
        }
        List<PushUssd> pushUssds = this.ussdRepository.findTop1500ByCollectionStatusInAndOperatorIn(collectionStatusList, mnoList);
        CustomRoutingDataSource.clearCurrentDataSource();
        return pushUssds;
    }

    @Transactional
    public List<PushUssd> updateAllCollectionStatus(List<PushUssd> pushUssds) {
        CustomRoutingDataSource.setCurrentDataSource("primary");
        if (pushUssds == null || pushUssds.isEmpty()) {
            return new ArrayList<>();
        }
        List<PushUssd> updatedPushUssds = this.ussdRepository.saveAll(pushUssds);
        CustomRoutingDataSource.clearCurrentDataSource();
        return updatedPushUssds;
    }

    @Transactional(readOnly = true)
    public Optional<PushUssd> findPushUssdById(Long id) {
        CustomRoutingDataSource.setCurrentDataSource("replica");
        Optional<PushUssd> pushUssd = this.ussdRepository.findById(id);
        CustomRoutingDataSource.clearCurrentDataSource();
        return pushUssd;
    }

    @Transactional(readOnly = true)
    public PushUssd findByReference(String asString) {
        CustomRoutingDataSource.setCurrentDataSource("replica");
        PushUssd pushUssd = this.ussdRepository.findByReference(asString);
        CustomRoutingDataSource.clearCurrentDataSource();
        return pushUssd;
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public List<PushUssd> findByCollectionStatusAndOperatorWithLock(List<CollectionStatus> collectionStatusList, List<String> mnoList) {
        CustomRoutingDataSource.setCurrentDataSource("replica");
        if (collectionStatusList == null || mnoList == null) {
            return new ArrayList<>();
        }
        List<PushUssd> pushUssds = this.ussdRepository.findTop1500ByCollectionStatusInAndOperatorInWithLock(collectionStatusList, mnoList);
        CustomRoutingDataSource.clearCurrentDataSource();
        return pushUssds;
    }

    @Transactional(readOnly = true)
    public List<PushUssd> getNewTransactions(String operator) {
        CustomRoutingDataSource.setCurrentDataSource("replica");
        List<PushUssd> pushUssds = this.ussdRepository.getNewTransactions(operator);
        CustomRoutingDataSource.clearCurrentDataSource();
        return pushUssds;
    }

    @Transactional(readOnly = true)
    public List<PushUssd> getNewTransactionsTest(String operator) {
        CustomRoutingDataSource.setCurrentDataSource("replica");
        List<PushUssd> pushUssds = this.ussdRepository.getNewTransactionsTest(operator);
        CustomRoutingDataSource.clearCurrentDataSource();
        return pushUssds;
    }

    @Transactional(readOnly = true)
    public List<PushUssd> getNewTransactions() {
        CustomRoutingDataSource.setCurrentDataSource("replica");
        List<PushUssd> pushUssds = this.ussdRepository.getNewTransactions();
        CustomRoutingDataSource.clearCurrentDataSource();
        return pushUssds;
    }
    @Transactional(readOnly = true)
    public List<PushUssd> findByCollectionStatusAndOperatorLimited(List<String> collectionStatusList, List<String> mnoList, int batchSize) {
        CustomRoutingDataSource.setCurrentDataSource("replica");
        if (collectionStatusList == null || mnoList == null) {
            return new ArrayList<>();
        }
        List<PushUssd> pushUssds = this.ussdRepository.findPushUssdByCollectionStatusInAndOperatorIn(collectionStatusList, mnoList, batchSize);
        CustomRoutingDataSource.clearCurrentDataSource();
        return pushUssds;
    }
    @Transactional(readOnly = true)
    public Optional<PushUssd> findByPaymentSessionId(String sessionId) {
        CustomRoutingDataSource.setCurrentDataSource("replica");
        PushUssd pushUssd = this.ussdRepository.findBySessionId(sessionId);
        CustomRoutingDataSource.clearCurrentDataSource();
        return Optional.ofNullable(pushUssd);
    }
}
