package edu.domain;

import com.fasterxml.jackson.annotation.JsonSetter;

import lombok.Data;

@Data
public class CouponsUsed {

    @JsonSetter("CouponCode")
    public String couponCode;
}
