/**
 * Query Parser for Zone Service
 * Converts frontend filter/sort payloads to Prisma queries
 */

import { Prisma } from '@prisma/client';
import { FilterGroup, FilterCondition, SortConfig, PagingRequest } from '../types/filter';

export class QueryParser {
  /**
   * Parse filter group to Prisma where clause
   */
  static parseFilterGroup(filterGroup: FilterGroup): Prisma.zonesWhereInput {
    if (!filterGroup || !filterGroup.conditions || filterGroup.conditions.length === 0) {
      return {};
    }

    const conditions = filterGroup.conditions.map(condition => {
      if ('logic' in condition) {
        // Nested FilterGroup
        return this.parseFilterGroup(condition as FilterGroup);
      } else {
        // FilterCondition
        return this.parseFilterCondition(condition as FilterCondition);
      }
    });

    if (filterGroup.logic === 'OR') {
      return {
        OR: conditions
      };
    } else {
      return {
        AND: conditions
      };
    }
  }

  /**
   * Parse single filter condition to Prisma where clause
   */
  private static parseFilterCondition(condition: FilterCondition): Prisma.zonesWhereInput {
    const { field, operator, value, caseSensitive = false } = condition;

    switch (operator) {
      case 'eq':
        return { [field]: { equals: value } };
      
      case 'ne':
        return { [field]: { not: { equals: value } } };
      
      case 'contains':
        if (caseSensitive) {
          return { [field]: { contains: value } };
        } else {
          return { [field]: { contains: value, mode: 'insensitive' } };
        }
      
      case 'startsWith':
        if (caseSensitive) {
          return { [field]: { startsWith: value } };
        } else {
          return { [field]: { startsWith: value, mode: 'insensitive' } };
        }
      
      case 'endsWith':
        if (caseSensitive) {
          return { [field]: { endsWith: value } };
        } else {
          return { [field]: { endsWith: value, mode: 'insensitive' } };
        }
      
      case 'gt':
        return { [field]: { gt: value } };
      
      case 'gte':
        return { [field]: { gte: value } };
      
      case 'lt':
        return { [field]: { lt: value } };
      
      case 'lte':
        return { [field]: { lte: value } };
      
      case 'between':
        if (Array.isArray(value) && value.length >= 2) {
          return {
            [field]: {
              gte: value[0],
              lte: value[1]
            }
          };
        }
        return {};
      
      case 'in':
        return { [field]: { in: Array.isArray(value) ? value : [value] } };
      
      case 'notIn':
        return { [field]: { notIn: Array.isArray(value) ? value : [value] } };
      
      case 'isNull':
        return { [field]: { equals: null } };
      
      case 'isNotNull':
        return { [field]: { not: { equals: null } } };
      
      case 'isEmpty':
        return { [field]: { equals: '' } };
      
      case 'isNotEmpty':
        return { [field]: { not: { equals: '' } } };
      
      default:
        console.warn(`Unsupported operator: ${operator}`);
        return {};
    }
  }

  /**
   * Parse sort configs to Prisma orderBy
   */
  static parseSortConfigs(sorts: SortConfig[]): Prisma.zonesOrderByWithRelationInput | Prisma.zonesOrderByWithRelationInput[] {
    if (!sorts || sorts.length === 0) {
      return { zone_id: 'desc' as const }; // Default sort
    }

    if (sorts.length === 1) {
      const sort = sorts[0];
      if (!sort) {
        return { zone_id: 'desc' as const };
      }
      return {
        [sort.field]: sort.direction
      } as Prisma.zonesOrderByWithRelationInput;
    }

    return sorts.map(sort => ({
      [sort.field]: sort.direction
    })) as Prisma.zonesOrderByWithRelationInput[];
  }

  /**
   * Parse paging request to Prisma pagination
   */
  static parsePagingRequest(request: PagingRequest) {
    const page = request.page || 0;
    const size = request.size || 10;
    const skip = page * size;
    const take = size;

    return {
      skip,
      take,
      where: request.filters ? this.parseFilterGroup(request.filters) : {},
      orderBy: request.sorts ? this.parseSortConfigs(request.sorts) : { zone_id: 'desc' as const }
    };
  }

  /**
   * Build global search query
   */
  static buildGlobalSearch(searchTerm: string): Prisma.zonesWhereInput {
    if (!searchTerm || searchTerm.trim() === '') {
      return {};
    }

    return {
      OR: [
        { name: { contains: searchTerm, mode: 'insensitive' } },
        { code: { contains: searchTerm, mode: 'insensitive' } }
      ]
    };
  }
}
