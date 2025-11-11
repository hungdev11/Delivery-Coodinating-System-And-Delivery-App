/**
 * Address Controller
 *
 * HTTP endpoints for address management
 */

import { Request, Response } from 'express'
import { AddressService } from './address.service'
import {
  CreateAddressDto,
  UpdateAddressDto,
  NearestAddressQuery,
  AddressPagingRequest,
  BatchAddressDto
} from './address.model'
import { BaseResponse } from '../../common/types/restful'

export class AddressController {
  constructor(private service: AddressService) {}

  /**
   * POST /addresses/get-or-create
   * Get existing address by coordinates or create new one
   * Finds nearest address within threshold (default 50m), if found returns it, otherwise creates new
   */
  getOrCreateAddress = async (req: Request, res: Response) => {
    try {
      const dto: CreateAddressDto = req.body
      const thresholdMeters = req.query.threshold 
        ? parseInt(req.query.threshold as string) 
        : 50

      // Validation
      if (!dto.name || !dto.lat || !dto.lon) {
        return res.status(400).json(BaseResponse.error('Missing required fields: name, lat, lon'))
      }

      // Validate coordinates
      if (dto.lat < -90 || dto.lat > 90 || dto.lon < -180 || dto.lon > 180) {
        return res.status(400).json(BaseResponse.error('Invalid coordinates'))
      }

      const address = await this.service.getOrCreateAddress(dto, thresholdMeters)

      return res.json(BaseResponse.success(address, 'Address retrieved or created successfully'))
    } catch (error: any) {
      console.error('Error getting or creating address:', error)
      return res.status(500).json(BaseResponse.error(error.message || 'Failed to get or create address'))
    }
  }

  /**
   * POST /addresses
   * Create a new address
   */
  createAddress = async (req: Request, res: Response) => {
    try {
      const dto: CreateAddressDto = req.body

      // Validation
      if (!dto.name || !dto.lat || !dto.lon) {
        return res.status(400).json(BaseResponse.error('Missing required fields: name, lat, lon'))
      }

      // Validate coordinates
      if (dto.lat < -90 || dto.lat > 90 || dto.lon < -180 || dto.lon > 180) {
        return res.status(400).json(BaseResponse.error('Invalid coordinates'))
      }

      const address = await this.service.createAddress(dto)

      return res.status(201).json(BaseResponse.success(address, 'Address created successfully'))
    } catch (error: any) {
      console.error('Error creating address:', error)
      return res.status(500).json(BaseResponse.error(error.message || 'Failed to create address'))
    }
  }

  /**
   * GET /addresses/:id
   * Get address by ID
   */
  getAddress = async (req: Request, res: Response) => {
    try {
      const id = req.params.id
      if (!id) {
        return res.status(400).json(BaseResponse.error('Missing address ID'))
      }

      const address = await this.service.getAddress(id)

      if (!address) {
        return res.status(404).json(BaseResponse.error('Address not found'))
      }

      return res.json(BaseResponse.success(address))
    } catch (error: any) {
      console.error('Error getting address:', error)
      return res.status(500).json(BaseResponse.error(error.message || 'Failed to get address'))
    }
  }

  /**
   * GET /addresses
   * List addresses with pagination and filters
   */
  listAddresses = async (req: Request, res: Response) => {
    try {
      const request = new AddressPagingRequest(req.query)
      const result = await this.service.listAddresses(request)

      return res.json(BaseResponse.success(result))
    } catch (error: any) {
      console.error('Error listing addresses:', error)
      return res.status(500).json(BaseResponse.error(error.message || 'Failed to list addresses'))
    }
  }

  /**
   * GET /addresses/by-point
   * Local-first address lookup by point, fallback to TrackAsia nearby
   */
  findByPoint = async (req: Request, res: Response) => {
    try {
      const { lat, lon, radius, limit } = req.query

      if (!lat || !lon) {
        return res.status(400).json(BaseResponse.error('Missing required parameters: lat, lon'))
      }

      const result = await this.service.findByPointWithFallback({
        lat: parseFloat(lat as string),
        lon: parseFloat(lon as string),
        radiusMeters: radius ? parseInt(radius as string) : 75,
        limit: limit ? parseInt(limit as string) : 10
      })

      return res.json(BaseResponse.success(result))
    } catch (error: any) {
      console.error('Error finding by point:', error)
      return res.status(500).json(BaseResponse.error(error.message || 'Failed to find by point'))
    }
  }

  /**
   * GET /addresses/search
   * Search by address text, returning local matches and TrackAsia text results
   */
  searchByText = async (req: Request, res: Response) => {
    try {
      const { q, query, limit } = req.query
      const term = (q || query) as string
      if (!term) {
        return res.status(400).json(BaseResponse.error('Missing required parameter: q'))
      }

      const result = await this.service.searchByTextWithFallback({
        query: term,
        limit: limit ? parseInt(limit as string) : 10
      })

      return res.json(BaseResponse.success(result))
    } catch (error: any) {
      console.error('Error searching by text:', error)
      return res.status(500).json(BaseResponse.error(error.message || 'Failed to search by text'))
    }
  }

  /**
   * GET /addresses/nearest
   * Find nearest addresses to a point
   */
  findNearestAddresses = async (req: Request, res: Response) => {
    try {
      const { lat, lon, limit, maxDistance, addressType, segmentId, zoneId } = req.query

      // Validation
      if (!lat || !lon) {
        return res.status(400).json(BaseResponse.error('Missing required parameters: lat, lon'))
      }

      const query: NearestAddressQuery = {
        lat: parseFloat(lat as string),
        lon: parseFloat(lon as string),
        limit: limit ? parseInt(limit as string) : 10,
        maxDistance: maxDistance ? parseInt(maxDistance as string) : 5000,
        addressType: addressType as any,
        segmentId: segmentId as string,
        zoneId: zoneId as string
      }

      // Validate coordinates
      if (query.lat < -90 || query.lat > 90 || query.lon < -180 || query.lon > 180) {
        return res.status(400).json(BaseResponse.error('Invalid coordinates'))
      }

      const addresses = await this.service.findNearestAddresses(query)

      return res.json(BaseResponse.success(addresses))
    } catch (error: any) {
      console.error('Error finding nearest addresses:', error)
      return res.status(500).json(BaseResponse.error(error.message || 'Failed to find nearest addresses'))
    }
  }

  /**
   * PUT /addresses/:id
   * Update an address
   */
  updateAddress = async (req: Request, res: Response) => {
    try {
      const id = req.params.id
      if (!id) {
        return res.status(400).json(BaseResponse.error('Missing address ID'))
      }

      const dto: UpdateAddressDto = req.body

      // Validate coordinates if provided
      if (dto.lat !== undefined && (dto.lat < -90 || dto.lat > 90)) {
        return res.status(400).json(BaseResponse.error('Invalid latitude'))
      }

      if (dto.lon !== undefined && (dto.lon < -180 || dto.lon > 180)) {
        return res.status(400).json(BaseResponse.error('Invalid longitude'))
      }

      const address = await this.service.updateAddress(id, dto)

      return res.json(BaseResponse.success(address, 'Address updated successfully'))
    } catch (error: any) {
      console.error('Error updating address:', error)

      if (error.message === 'Address not found') {
        return res.status(404).json(BaseResponse.error('Address not found'))
      }

      return res.status(500).json(BaseResponse.error(error.message || 'Failed to update address'))
    }
  }

  /**
   * DELETE /addresses/:id
   * Delete an address
   */
  deleteAddress = async (req: Request, res: Response) => {
    try {
      const id = req.params.id
      if (!id) {
        return res.status(400).json(BaseResponse.error('Missing address ID'))
      }

      await this.service.deleteAddress(id)

      return res.json(BaseResponse.success(null, 'Address deleted successfully'))
    } catch (error: any) {
      console.error('Error deleting address:', error)

      if (error.code === 'P2025') {
        return res.status(404).json(BaseResponse.error('Address not found'))
      }

      return res.status(500).json(BaseResponse.error(error.message || 'Failed to delete address'))
    }
  }

  /**
   * POST /addresses/batch
   * Batch import addresses
   */
  batchImport = async (req: Request, res: Response) => {
    try {
      const dto: BatchAddressDto = req.body

      // Validation
      if (!dto.addresses || !Array.isArray(dto.addresses) || dto.addresses.length === 0) {
        return res.status(400).json(BaseResponse.error('Missing or invalid addresses array'))
      }

      // Limit batch size
      if (dto.addresses.length > 1000) {
        return res.status(400).json(BaseResponse.error('Batch size too large (max: 1000)'))
      }

      const result = await this.service.batchImport(dto)

      return res
        .status(result.failed > 0 ? 207 : 201)
        .json(
          BaseResponse.success(
            result,
            `Batch import completed: ${result.successful} successful, ${result.failed} failed`
          )
        )
    } catch (error: any) {
      console.error('Error batch importing addresses:', error)
      return res.status(500).json(BaseResponse.error(error.message || 'Failed to batch import addresses'))
    }
  }

  /**
   * GET /addresses/segments/:segmentId
   * Get all addresses on a specific road segment
   */
  getAddressesBySegment = async (req: Request, res: Response) => {
    try {
      const { segmentId } = req.params
      const request = new AddressPagingRequest({ ...req.query, segmentId })
      const result = await this.service.listAddresses(request)

      return res.json(BaseResponse.success(result))
    } catch (error: any) {
      console.error('Error getting addresses by segment:', error)
      return res.status(500).json(BaseResponse.error(error.message || 'Failed to get addresses by segment'))
    }
  }

  /**
   * GET /addresses/zones/:zoneId
   * Get all addresses in a specific zone
   */
  getAddressesByZone = async (req: Request, res: Response) => {
    try {
      const { zoneId } = req.params
      const request = new AddressPagingRequest({ ...req.query, zoneId })
      const result = await this.service.listAddresses(request)

      return res.json(BaseResponse.success(result))
    } catch (error: any) {
      console.error('Error getting addresses by zone:', error)
      return res.status(500).json(BaseResponse.error(error.message || 'Failed to get addresses by zone'))
    }
  }
}
