/**
 * Center Models and DTOs
 */

import { IsString, IsNotEmpty, IsOptional, IsNumber, IsObject } from 'class-validator';
import { Type } from 'class-transformer';
import { PagingRequest } from '../../common/types/restful';

/**
 * Center DTO
 */
export class CenterDto {
  id!: string;
  code!: string;
  name!: string;
  address?: string | null;
  lat?: number | null;
  lon?: number | null;
  polygon?: any | null;
}

/**
 * Create Center DTO
 */
export class CreateCenterDto {
  @IsString()
  @IsNotEmpty()
  code!: string;

  @IsString()
  @IsNotEmpty()
  name!: string;

  @IsString()
  @IsOptional()
  address?: string;

  @IsNumber()
  @IsOptional()
  @Type(() => Number)
  lat?: number;

  @IsNumber()
  @IsOptional()
  @Type(() => Number)
  lon?: number;

  @IsObject()
  @IsOptional()
  polygon?: any;
}

/**
 * Update Center DTO
 */
export class UpdateCenterDto {
  @IsString()
  @IsOptional()
  code?: string;

  @IsString()
  @IsOptional()
  name?: string;

  @IsString()
  @IsOptional()
  address?: string;

  @IsNumber()
  @IsOptional()
  @Type(() => Number)
  lat?: number;

  @IsNumber()
  @IsOptional()
  @Type(() => Number)
  lon?: number;

  @IsObject()
  @IsOptional()
  polygon?: any;
}

/**
 * Center Paging Request
 */
export class CenterPagingRequest extends PagingRequest {
  @IsString()
  @IsOptional()
  search?: string;

  @IsString()
  @IsOptional()
  code?: string;
}

