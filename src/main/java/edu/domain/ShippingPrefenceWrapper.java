package edu.domain;

import com.fasterxml.jackson.annotation.JsonSetter;

import lombok.Data;

@Data
public class ShippingPrefenceWrapper {
    
    @JsonSetter("ShippingPreference")
    public ShippingPreference shippingPreference;
    
}