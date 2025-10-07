/**
 * Zone Models and DTOs
 */

import { IsString, IsNotEmpty, IsOptional, IsObject } from 'class-validator';
import { PagingRequest } from '../../common/types/restful';

/**
 * Zone DTO
 */
export class ZoneDto {
  id!: string;
  code!: string;
  name!: string;
  polygon?: any | null;
  centerId!: string;
  centerCode?: string;
  centerName?: string;
}

/**
 * Create Zone DTO
 */
export class CreateZoneDto {
  @IsString()
  @IsNotEmpty()
  code!: string;

  @IsString()
  @IsNotEmpty()
  name!: string;

  @IsObject()
  @IsOptional()
  polygon?: any;

  @IsString()
  @IsNotEmpty()
  centerId!: string;
}

/**
 * Update Zone DTO
 */
export class UpdateZoneDto {
  @IsString()
  @IsOptional()
  code?: string;

  @IsString()
  @IsOptional()
  name?: string;

  @IsObject()
  @IsOptional()
  polygon?: any;

  @IsString()
  @IsOptional()
  centerId?: string;
}

/**
 * Zone Paging Request
 */
export class ZonePagingRequest extends PagingRequest {
  @IsString()
  @IsOptional()
  search?: string;

  @IsString()
  @IsOptional()
  code?: string;

  @IsString()
  @IsOptional()
  centerId?: string;
}

