package edu.domain;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonSetter;

import lombok.Data;

@Data
public class User {
    
            @JsonSetter("id")
            public String id;

            @JsonSetter("UserId")
            public String userId;

            @JsonSetter("LastName")
            public String lastName;

            @JsonSetter("FirstName")
            public String firstName;

            @JsonSetter("Email")
            public String email;

            @JsonSetter("Dividend")
            public String dividend;

            @JsonSetter("OrderHistories")
            public List<OrderHistoryWrapper>  orderHistories;

            @JsonSetter("ShippingPreferences")
            public List<ShippingPrefenceWrapper> shippingPreferences;

            @JsonSetter("CouponsUsed")
            public List<CouponsUsedWrapper> couponsUsed;
}