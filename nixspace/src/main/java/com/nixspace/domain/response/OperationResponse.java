package com.nixspace.domain.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nixspace.api.constants.ResponseCode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OperationResponse<T> {
    private String responseCode;
    private String responseMessage;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private T entity;

    public OperationResponse(String responseCode, String responseMessage) {
        this.responseCode = responseCode;
        this.responseMessage = responseMessage;
    }

    public boolean isSuccessful() {
        return ResponseCode.CODE_01.equalsIgnoreCase(this.responseCode);
    }

    @Override
    public String toString() {
        return "OperationResponse{" +
                "responseCode='" + responseCode + '\'' +
                ", responseMessage='" + responseMessage + '\'' +
//                ", entity=" + entity +
                '}';
    }
}
