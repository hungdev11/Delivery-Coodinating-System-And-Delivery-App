/**
 * RESTful API Types
 * Following RESTFUL.md standards
 */
export interface BaseResponse<T = any> {
    result?: T;
    message?: string;
}
export declare class BaseResponseBuilder {
    static success<T>(data?: T, message?: string): BaseResponse<T>;
    static error(message: string): BaseResponse;
}
//# sourceMappingURL=restful.d.ts.map