import type { FilterGroup, SortConfig } from './filter'

export interface IPaginationParams {
  page: number
  size: number
  totalElements: number
  totalPages: number
  filters: FilterGroup | null
  sorts: SortConfig[]
}

export interface IPagedData<TData, TPage extends IPaginationParams> {
  data: TData[]
  page: TPage
}

export interface IBaseEntity {
  id: string
  createdAt: string
  updatedAt: string
}

export interface IApiResponse<TData> {
  result?: TData
  message?: string
}
