package edu.domain;

import com.fasterxml.jackson.annotation.JsonSetter;

import lombok.Data;

@Data
public class OrderHistory {
    
    @JsonSetter("OrderID")
    public String orderID;
    @JsonSetter("DateShipped")
    public String dateShipped;
    @JsonSetter("Total")
    public String total;
}