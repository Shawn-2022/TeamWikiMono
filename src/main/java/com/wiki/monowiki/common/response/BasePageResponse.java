package com.wiki.monowiki.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;

import java.util.Collections;
import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonInclude(NON_NULL)
@Getter
@NoArgsConstructor(force = true)
@JsonPropertyOrder({"statusCode", "message", "messages", "error", "page", "size", "total", "data"})
public class BasePageResponse<S> extends BaseResponse<List<S>> {

    @Schema(description = "Start index of the paged data", example = "0")
    private final Integer page;

    @Schema(description = "Maximum number of results in the paged data", example = "10")
    private final Integer size;

    @Schema(description = "Total number of results in the paged data", example = "50")
    private final Long total;

    @Schema(description = "Data in the response")
    private final List<S> data;

    public BasePageResponse(Integer statusCode, String message, Boolean error,
	    Integer page, Integer size, Long total, List<S> data) {
	super(statusCode, message, error);
	this.page = page;
	this.size = size;
	this.total = total;
	this.data = data;
    }

    public BasePageResponse(Integer statusCode, List<String> messages, Boolean error,
	    Integer page, Integer size, Long total, List<S> data) {
	super(statusCode, messages, error);
	this.page = page;
	this.size = size;
	this.total = total;
	this.data = data;
    }

    public static <S> BasePageResponse<S> fromPage(Page<S> page, String successMessage) {
	return new BasePageResponse<>(
		HttpStatus.OK.value(),
		successMessage,
		false,
		page.getNumber(),
		page.getSize(),
		page.getTotalElements(),
		page.getContent()
	);
    }

    public static <S> BasePageResponse<S> empty(String message) {
	return new BasePageResponse<>(
		HttpStatus.OK.value(),
		message,
		false,
		0,
		0,
		0L,
		Collections.emptyList()
	);
    }
}
