package ptithcm.graduation.apigateway.models;

public class BaseResponse<T> {
    public String message = null;
    public T result = null;

    public BaseResponse() {}

    public BaseResponse(String message, T result) {
        this.message = message;
        this.result = result;
    }
}
