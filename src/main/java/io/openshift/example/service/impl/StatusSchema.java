package io.openshift.example.service.impl;

import com.fasterxml.jackson.annotation.JsonAlias;
// import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

// @JsonIgnoreProperties(value = { "form_id", "user_approval_id", "status", "user_approval_name" })
public class StatusSchema {

    @JsonProperty("form_id")
    @JsonAlias({"formId"})
    public Integer formId;

    @JsonProperty("user_approval_id")
    @JsonAlias({"userApprovalId"})
    public Integer userApprovalId;

    @JsonProperty("status")
    @JsonAlias({"status"})
    public String status;

    @JsonProperty("user_approval_name")
    @JsonAlias({"userApprovalName"})
    public String userApprovalName;


    public StatusSchema() {}

    public StatusSchema(
        Integer formId, 
        Integer userApprovalId, 
        String status, 
        String userApprovalName
        ) {

        this.formId = formId;
        this.userApprovalId = userApprovalId;
        this.status = status;
        this.userApprovalName = userApprovalName;
    }
}


// mapper.readValue(calendarsData.getJsonObject(i).getJsonObject("calendar_detail").getString("value"), new TypeReference<List<CalendarSchema>>(){}); 