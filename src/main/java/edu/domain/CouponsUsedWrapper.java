package edu.domain;

import com.fasterxml.jackson.annotation.JsonSetter;

import lombok.Data;

@Data
public class CouponsUsedWrapper {

    @JsonSetter("CouponCodes")
    public CouponsUsed couponsUsed;
    
}