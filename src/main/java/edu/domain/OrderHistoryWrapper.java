package edu.domain;

import com.fasterxml.jackson.annotation.JsonSetter;

import lombok.Data;

@Data
public class  OrderHistoryWrapper{

    @JsonSetter("OrderHistory")
    public OrderHistory orderHistory;
    
}