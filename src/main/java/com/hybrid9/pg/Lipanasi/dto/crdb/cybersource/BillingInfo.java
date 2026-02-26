package com.hybrid9.pg.Lipanasi.dto.crdb.cybersource;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class BillingInfo {
    private String firstName;
    private String lastName;
    private String address1;
    private String city;
    private String state;
    private String zipCode;
    private String country;
    private String email;
    private String phone;

    // Getters and setters
   /* public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getAddress1() { return address1; }
    public void setAddress1(String address1) { this.address1 = address1; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public String getZipCode() { return zipCode; }
    public void setZipCode(String zipCode) { this.zipCode = zipCode; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }*/
}
