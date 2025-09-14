package com.ds.project.common.entities.base;

import java.util.Optional;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BaseResponse<TData> {
    private Optional<TData> result;
    private Optional<String> message;

    public BaseResponse(String message) {
        this.message = Optional.of(message);
    }

    public BaseResponse(TData result) {
        this.result = Optional.of(result);
    }
}
