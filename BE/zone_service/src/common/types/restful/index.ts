/**
 * RESTful API Types and DTOs
 */

import { IsInt, IsOptional, Min, IsArray } from 'class-validator';
import { Type } from 'class-transformer';

/**
 * Use for response data
 * @template T - The type of the data, use for data
 * 
 * @param result - The data, use for data, will be null if error
 * @param message - The message, use for message, will be null if success with result
 * 
 * @example 
 * // basic response
 * const result = new BaseResponse<User>();
 * 
 * @example 
 * // response with paging data
 * const result = new BaseResponse<PagedData<User>>();
 */
export class BaseResponse<T> {
	result?: T;
	message?: string;

	constructor(data?: Partial<BaseResponse<T>>) {
		if (data) {
			if (data.result !== undefined) this.result = data.result;
			if (data.message !== undefined) this.message = data.message;
		}
	}

	static success<T>(result: T, message?: string): BaseResponse<T> {
		const response = new BaseResponse<T>();
		response.result = result;
		if (message) response.message = message;
		return response;
	}

	static error<T>(message: string): BaseResponse<T> {
		const response = new BaseResponse<T>();
		response.message = message;
		return response;
	}
}

/**
 * Use for response paging data
 * @template TData - The type of the data, use for data
 * 
 * @example
 * const result = new BaseResponse<PagedData<User>>();
 */
export class PagedData<TData extends { id: string }> {
	data: TData[];
    page: Paging<TData['id']>

    constructor(data: TData[], page: Paging<TData['id']>) {
        this.data = data;
        this.page = page;
    }
}

/**
 * Use for paging data
 * @template TKey - The type of the key of the data, use for selected data (export/bulk action)
 */
export class Paging<TKey> {
	page: number;
	size: number;
	totalElements: number;
	totalPages: number;
	filters?: any;
	sorts: any[];
    selected: TKey[];

    constructor() {
        this.page = 0;
        this.size = 0;
        this.totalElements = 0;
        this.totalPages = 0;
        this.sorts = [];
        this.selected = [];
    }
}

/**
 * Base Paging Request DTO
 * Can be extended by specific paging requests
 */
export class PagingRequest {
	@IsOptional()
	@IsInt()
	@Min(0)
	@Type(() => Number)
	page?: number = 0;

	@IsOptional()
	@IsInt()
	@Min(1)
	@Type(() => Number)
	size?: number = 10;

	@IsOptional()
	@IsArray()
	filters?: any[] = [];

	@IsOptional()
	@IsArray()
	sorts?: any[] = [];

	@IsOptional()
	@IsArray()
	selected?: string[] = [];

	/**
	 * Calculate skip value for database queries
	 */
	getSkip(): number {
		return (this.page || 0) * (this.size || 10);
	}

	/**
	 * Get take value for database queries
	 */
	getTake(): number {
		return this.size || 10;
	}

	/**
	 * Create Paging object from total count
	 */
	createPaging<TKey>(totalElements: number): Paging<TKey> {
		const paging = new Paging<TKey>();
		paging.page = this.page || 0;
		paging.size = this.size || 10;
		paging.totalElements = totalElements;
		paging.totalPages = Math.ceil(totalElements / (this.size || 10));
		paging.filters = this.filters || [];
		paging.sorts = this.sorts || [];
		paging.selected = (this.selected as TKey[]) || [];
		return paging;
	}
}

/**
 * Generic ID Param DTO
 */
export class IdParam {
	id!: string;
}
