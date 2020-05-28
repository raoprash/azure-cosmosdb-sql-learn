package edu.domain;

import com.fasterxml.jackson.annotation.JsonSetter;

import lombok.Data;

@Data
public class ShippingPreference {

    @JsonSetter("Priority")
    public int priority;

    @JsonSetter("AddressLine1")
    public String addressLine1;

    @JsonSetter("AddressLine2")
    public String addressLine2;

    @JsonSetter("City")
    public String city;

    @JsonSetter("State")
    public String state;

    @JsonSetter("ZipCode")
    public String zipCode;

    @JsonSetter("Country")
    public String country;
}

