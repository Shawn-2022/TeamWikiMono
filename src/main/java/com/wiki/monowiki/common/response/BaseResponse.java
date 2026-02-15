package com.wiki.monowiki.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonInclude(NON_NULL)
@Getter
@NoArgsConstructor(force = true)
@JsonPropertyOrder({"statusCode", "message", "messages", "error", "data"})
public class BaseResponse<T> {

    @Schema(description = "Status code of the response", example = "200")
    private final Integer statusCode;

    @Schema(description = "Message of the response", example = "Success/Failure")
    private final String message;

    @Schema(description = "Messages of the response", example = "[\"Success\"]")
    private final List<String> messages;

    @Schema(description = "Error in the response", example = "false")
    private final boolean error;

    @Schema(description = "Data in the response")
    private final T data;

    public BaseResponse(Integer statusCode, String message, Boolean error, T data) {
	this.statusCode = statusCode;
	this.message = message;
	this.messages = null;
	this.error = Boolean.TRUE.equals(error);
	this.data = Boolean.TRUE.equals(error) ? null : data;
    }

    public BaseResponse(Integer statusCode, List<String> messages, Boolean error, T data) {
	this.statusCode = statusCode;
	this.messages = messages;
	this.message = null;
	this.error = Boolean.TRUE.equals(error);
	this.data = data;
    }

    // for BasePageResponse
    public BaseResponse(Integer statusCode, String message, Boolean error) {
	this.statusCode = statusCode;
	this.message = message;
	this.messages = null;
	this.error = Boolean.TRUE.equals(error);
	this.data = null;
    }

    public BaseResponse(Integer statusCode, List<String> messages, Boolean error) {
	this.statusCode = statusCode;
	this.messages = messages;
	this.message = null;
	this.error = Boolean.TRUE.equals(error);
	this.data = null;
    }
}
