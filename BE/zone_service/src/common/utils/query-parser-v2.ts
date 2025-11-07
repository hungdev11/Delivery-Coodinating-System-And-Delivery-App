/**
 * Enhanced Query Parser V2
 * Supports filter v2 with operations between each pair
 */

import { FilterGroupItemV2, FilterConditionItemV2, FilterOperatorItemV2, FilterItemType } from '../types/filter-v2';
import { logger } from '../logger/logger.service';

export class QueryParserV2 {
  /**
   * Parse V2 filter group to Prisma where clause
   */
  public static parseFilterGroup(filterGroup: FilterGroupItemV2 | undefined): any {
    if (!filterGroup || !filterGroup.items || filterGroup.items.length === 0) {
      return {};
    }

    const predicates: any[] = [];
    const operators: string[] = [];

    for (const item of filterGroup.items) {
      if (item.type === FilterItemType.CONDITION) {
        const condition = item as FilterConditionItemV2;
        predicates.push(this.parseCondition(condition));
      } else if (item.type === FilterItemType.GROUP) {
        const nestedGroup = item as FilterGroupItemV2;
        predicates.push(this.parseFilterGroup(nestedGroup));
      } else if (item.type === FilterItemType.OPERATOR) {
        const operator = item as FilterOperatorItemV2;
        operators.push(operator.value);
      }
    }

    return this.combinePredicates(predicates, operators);
  }

  /**
   * Combine predicates with operators between each pair
   */
  private static combinePredicates(predicates: any[], operators: string[]): any {
    if (predicates.length === 0) return {};
    if (predicates.length === 1) return predicates[0];

    // Build tree from left to right
    // pred1 OP1 pred2 OP2 pred3 => (pred1 OP1 pred2) OP2 pred3
    let result = predicates[0];

    for (let i = 1; i < predicates.length; i++) {
      const operator = operators[i - 1] || 'AND';
      const nextPredicate = predicates[i];

      if (operator === 'OR') {
        result = { OR: [result, nextPredicate] };
      } else {
        result = { AND: [result, nextPredicate] };
      }
    }

    return result;
  }

  /**
   * Parse individual filter condition to Prisma syntax
   */
  private static parseCondition(condition: FilterConditionItemV2): any {
    try {
      const { field, operator, value, caseSensitive } = condition;

      // Handle nested fields (e.g., "center.name")
      const fieldParts = field.split('.');

      let prismaCondition: any = {};

      // Build the condition based on operator
      let fieldCondition: any;
      switch (operator.toLowerCase()) {
        case 'equals':
        case 'eq':
          fieldCondition = { equals: value };
          break;
        case 'not_equals':
        case 'ne':
          fieldCondition = { not: value };
          break;
        case 'contains':
          fieldCondition = { contains: value, mode: caseSensitive ? 'default' : 'insensitive' };
          break;
        case 'starts_with':
          fieldCondition = { startsWith: value, mode: caseSensitive ? 'default' : 'insensitive' };
          break;
        case 'ends_with':
          fieldCondition = { endsWith: value, mode: caseSensitive ? 'default' : 'insensitive' };
          break;
        case 'in':
          fieldCondition = { in: Array.isArray(value) ? value : [value] };
          break;
        case 'not_in':
          fieldCondition = { notIn: Array.isArray(value) ? value : [value] };
          break;
        case 'gt':
        case 'greater_than':
          fieldCondition = { gt: value };
          break;
        case 'gte':
        case 'greater_than_or_equal':
          fieldCondition = { gte: value };
          break;
        case 'lt':
        case 'less_than':
          fieldCondition = { lt: value };
          break;
        case 'lte':
        case 'less_than_or_equal':
          fieldCondition = { lte: value };
          break;
        case 'is_null':
          fieldCondition = { equals: null };
          break;
        case 'is_not_null':
          fieldCondition = { not: null };
          break;
        default:
          logger.warn(`Unsupported operator: ${operator}`);
          return {};
      }

      // Build nested field structure
      if (fieldParts.length > 1) {
        // Nested field (e.g., center.name)
        let current = fieldCondition;
        for (let i = fieldParts.length - 1; i > 0; i--) {
          const part = fieldParts[i];
          if (part) {
            current = { [part]: current };
          }
        }
        const firstPart = fieldParts[0];
        if (firstPart) {
          prismaCondition[firstPart] = current;
        }
      } else {
        // Simple field
        prismaCondition[field] = fieldCondition;
      }

      return prismaCondition;
    } catch (error) {
      logger.error('Error parsing condition:', error);
      return {};
    }
  }
}
