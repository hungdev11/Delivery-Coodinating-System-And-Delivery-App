/**
 * RESTful API Types
 * Following RESTFUL.md standards
 */

export interface BaseResponse<T = any> {
  result?: T;
  message?: string;
}

export class BaseResponseBuilder {
  static success<T>(data?: T, message?: string): BaseResponse<T> {
    const response: BaseResponse<T> = {};
    if (data !== undefined) {
      response.result = data;
    }
    if (message) {
      response.message = message;
    }
    return response;
  }

  static error(message: string): BaseResponse {
    return { message };
  }
}
