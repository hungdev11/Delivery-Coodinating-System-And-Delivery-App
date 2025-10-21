/**
 * Filter and Sort Types for Zone Service
 * Compatible with the new paging/filter/sort system
 */

export interface FilterCondition {
  field: string;
  operator: string;
  value: any;
  caseSensitive?: boolean;
  id?: string;
}

export interface FilterGroup {
  logic: 'AND' | 'OR';
  conditions: (FilterCondition | FilterGroup)[];
}

export interface SortConfig {
  field: string;
  direction: 'asc' | 'desc';
  priority?: number;
}

export interface PagingRequest {
  filters?: FilterGroup;
  sorts?: SortConfig[];
  page?: number;
  size?: number;
  search?: string;
  selected?: string[];
}

export interface Paging {
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  filters?: FilterGroup;
  sorts?: SortConfig[];
  selected?: string[];
}

export interface PagedData<T> {
  data: T[];
  page: Paging;
}

export interface BaseResponse<T> {
  result?: T;
  message?: string;
}
