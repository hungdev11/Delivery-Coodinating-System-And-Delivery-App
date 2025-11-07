/**
 * V2 Filter System Types
 * Allows operations between each pair of conditions/groups
 */

export enum FilterItemType {
  CONDITION = 'condition',
  OPERATOR = 'operator',
  GROUP = 'group'
}

export interface FilterItemV2 {
  type: FilterItemType;
}

export interface FilterConditionItemV2 extends FilterItemV2 {
  type: FilterItemType.CONDITION;
  field: string;
  operator: string;
  value: any;
  caseSensitive?: boolean;
  id?: string;
}

export interface FilterOperatorItemV2 extends FilterItemV2 {
  type: FilterItemType.OPERATOR;
  value: 'AND' | 'OR';
}

export interface FilterGroupItemV2 extends FilterItemV2 {
  type: FilterItemType.GROUP;
  items: FilterItemV2[];
}

export interface PagingRequestV0 {
  page?: number;
  size?: number;
  sorts?: Array<{ field: string; direction: 'asc' | 'desc' }>;
  search?: string;
  selected?: string[];
}

export interface PagingRequestV2 {
  page?: number;
  size?: number;
  filters?: FilterGroupItemV2;
  sorts?: Array<{ field: string; direction: 'asc' | 'desc' }>;
  search?: string;
  selected?: string[];
}
