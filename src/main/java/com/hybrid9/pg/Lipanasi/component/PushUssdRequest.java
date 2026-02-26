package com.hybrid9.pg.Lipanasi.component;

import com.hybrid9.pg.Lipanasi.enums.CollectionStatus;
import com.hybrid9.pg.Lipanasi.entities.payments.pushussd.PushUssd;

import java.io.Serializable;
import java.util.Objects;

public class PushUssdRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private CollectionStatus collectionStatus;
    private String msisdn;
    private String mobileMoneyName;
    private String message;
    private String mainAccountId;
    private String pushUssdId;
    private PushUssd pushUssd;

    public PushUssdRequest(CollectionStatus collectionStatus, String msisdn, String mobileMoneyName, String message, String mainAccountId, String pushUssdId, PushUssd pushUssd) {
        this.collectionStatus = collectionStatus;
        this.msisdn = msisdn;
        this.mobileMoneyName = mobileMoneyName;
        this.message = message;
        this.mainAccountId = mainAccountId;
        this.pushUssdId = pushUssdId;
        this.pushUssd = pushUssd;
    }

    public CollectionStatus getCollectionStatus() {
        return collectionStatus;
    }

    public void setCollectionStatus(CollectionStatus collectionStatus) {
        this.collectionStatus = collectionStatus;
    }

    public String getMsisdn() {
        return msisdn;
    }

    public void setMsisdn(String msisdn) {
        this.msisdn = msisdn;
    }

    public String getMobileMoneyName() {
        return mobileMoneyName;
    }



    public void setMobileMoneyName(String mobileMoneyName) {
        this.mobileMoneyName = mobileMoneyName;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getMainAccountId() {
        return mainAccountId;
    }

    public void setMainAccountId(String mainAccountId) {
        this.mainAccountId = mainAccountId;
    }

    public String getPushUssdId() {
        return pushUssdId;
    }

    public void setPushUssdId(String pushUssdId) {
        this.pushUssdId = pushUssdId;
    }

    public PushUssd getPushUssd() {
        return pushUssd;
    }

    public void setPushUssd(PushUssd pushUssd) {
        this.pushUssd = pushUssd;
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PushUssdRequest that = (PushUssdRequest) o;
        return collectionStatus == that.collectionStatus &&
                Objects.equals(msisdn, that.msisdn) &&
                Objects.equals(mobileMoneyName, that.mobileMoneyName) &&
                Objects.equals(message, that.message) &&
                Objects.equals(mainAccountId, that.mainAccountId) &&
                Objects.equals(pushUssdId, that.pushUssdId) &&
                Objects.equals(pushUssd, that.pushUssd);
    }

    @Override
    public int hashCode() {
        return Objects.hash(collectionStatus, msisdn, mobileMoneyName, message, mainAccountId, pushUssdId, pushUssd);
    }

    @Override
    public String toString() {
        return "CollectionStatus: " + collectionStatus + "\n"
                + "Msisdn: " + msisdn + "\n"
                + "MobileMoneyName: " + mobileMoneyName + "\n"
                + "Message: " + message + "\n"
                + "MainAccountId: " + mainAccountId + "\n"
                + "PushUssdId: " + pushUssdId + "\n"
                + "PushUssd: " + pushUssd;
    }

    // Builder pattern for flexible object creation
    public static class Builder {
        private CollectionStatus collectionStatus;
        private String msisdn;
        private String mobileMoneyName;
        private String message;
        private String mainAccountId;
        private String pushUssdId;
        private PushUssd pushUssd;

        public Builder collectionStatus(CollectionStatus collectionStatus) {
            this.collectionStatus = collectionStatus;
            return this;
        }

        public Builder msisdn(String msisdn) {
            this.msisdn = msisdn;
            return this;
        }

        public Builder mobileMoneyName(String mobileMoneyName) {
            this.mobileMoneyName = mobileMoneyName;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder mainAccountId(String mainAccountId) {
            this.mainAccountId = mainAccountId;
            return this;
        }

        public Builder pushUssdId(String pushUssdId) {
            this.pushUssdId = pushUssdId;
            return this;
        }

        public Builder pushUssd(PushUssd pushUssd) {
            this.pushUssd = pushUssd;
            return this;
        }

        public PushUssdRequest build() {
            return new PushUssdRequest(collectionStatus, msisdn, mobileMoneyName, message, mainAccountId, pushUssdId, pushUssd);
        }
    }
}
