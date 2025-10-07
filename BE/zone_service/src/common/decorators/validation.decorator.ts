/**
 * Validation Decorators
 * Custom decorators for request validation
 */

import { Request, Response, NextFunction } from 'express';
import { validate, ValidationError } from 'class-validator';
import { plainToClass } from 'class-transformer';
import { BaseResponse } from '../types/restful';

/**
 * Validate request body against a DTO class
 */
export function ValidateBody(dtoClass: any) {
  return function (_target: any, _propertyKey: string, descriptor: PropertyDescriptor) {
    const originalMethod = descriptor.value;

    descriptor.value = async function (req: Request, res: Response, next: NextFunction) {
      try {
        const dtoInstance = plainToClass(dtoClass, req.body);
        const errors: ValidationError[] = await validate(dtoInstance as object);

        if (errors.length > 0) {
          return res.status(400).json(BaseResponse.error('Validation failed'));
        }

        req.body = dtoInstance;
        return originalMethod.apply(this, [req, res, next]);
      } catch (error) {
        next(error);
      }
    };

    return descriptor;
  };
}

/**
 * Validate request query parameters against a DTO class
 */
export function ValidateQuery(dtoClass: any) {
  return function (_target: any, _propertyKey: string, descriptor: PropertyDescriptor) {
    const originalMethod = descriptor.value;

    descriptor.value = async function (req: Request, res: Response, next: NextFunction) {
      try {
        const dtoInstance = plainToClass(dtoClass, req.query);
        const errors: ValidationError[] = await validate(dtoInstance as object);

        if (errors.length > 0) {
          return res.status(400).json(BaseResponse.error('Validation failed'));
        }

        req.query = dtoInstance as any;
        return originalMethod.apply(this, [req, res, next]);
      } catch (error) {
        next(error);
      }
    };

    return descriptor;
  };
}

/**
 * Validate request params against a DTO class
 */
export function ValidateParams(dtoClass: any) {
  return function (_target: any, _propertyKey: string, descriptor: PropertyDescriptor) {
    const originalMethod = descriptor.value;

    descriptor.value = async function (req: Request, res: Response, next: NextFunction) {
      try {
        const dtoInstance = plainToClass(dtoClass, req.params);
        const errors: ValidationError[] = await validate(dtoInstance as object);

        if (errors.length > 0) {
          return res.status(400).json(BaseResponse.error('Validation failed'));
        }

        req.params = dtoInstance as any;
        return originalMethod.apply(this, [req, res, next]);
      } catch (error) {
        next(error);
      }
    };

    return descriptor;
  };
}
