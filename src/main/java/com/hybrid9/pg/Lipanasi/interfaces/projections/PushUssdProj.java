package com.hybrid9.pg.Lipanasi.interfaces.projections;

public interface PushUssdProj {
    public float getAmount();
    public String getPhoneNumber();
    public String getInvoiceNumber();
    public String getCollectionStatus();
    public String getClientName();
    public String getCollectionTime();
    public String getAccountId();
    public String getCollectionType();
}
