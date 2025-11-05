/**
 * Address Router
 *
 * Routes for address management
 */

import { Router } from 'express'
import { AddressController } from './address.controller'
import { AddressService } from './address.service'
import { prisma } from '../../common/database/prisma.client'

const addressService = new AddressService(prisma)
const addressController = new AddressController(addressService)

const router = Router()

/**
 * @route POST /api/v1/addresses
 * @desc Create a new address
 * @access Public
 */
router.post('/', addressController.createAddress)

/**
 * @route POST /api/v1/addresses/batch
 * @desc Batch import addresses
 * @access Public
 */
router.post('/batch', addressController.batchImport)

/**
 * @route GET /api/v1/addresses/by-point
 * @desc Local-first lookup by point, fallback to TrackAsia
 * @query lat, lon (required), radius (5-15m suggested), limit
 * @access Public
 */
router.get('/by-point', addressController.findByPoint)

/**
 * @route GET /api/v1/addresses/search
 * @desc Search by address text (local + TrackAsia)
 * @query q (or query), limit
 * @access Public
 */
router.get('/search', addressController.searchByText)

/**
 * @route GET /api/v1/addresses/nearest
 * @desc Find nearest addresses to a point
 * @query lat, lon (required), limit, maxDistance, addressType, segmentId, zoneId
 * @access Public
 */
router.get('/nearest', addressController.findNearestAddresses)

/**
 * @route GET /api/v1/addresses/segments/:segmentId
 * @desc Get addresses on a specific road segment
 * @access Public
 */
router.get('/segments/:segmentId', addressController.getAddressesBySegment)

/**
 * @route GET /api/v1/addresses/zones/:zoneId
 * @desc Get addresses in a specific zone
 * @access Public
 */
router.get('/zones/:zoneId', addressController.getAddressesByZone)

/**
 * @route GET /api/v1/addresses/:id
 * @desc Get address by ID
 * @access Public
 */
router.get('/:id', addressController.getAddress)

/**
 * @route GET /api/v1/addresses
 * @desc List addresses with pagination and filters
 * @query page, limit, search, addressType, segmentId, zoneId, wardName, districtName
 * @access Public
 */
router.get('/', addressController.listAddresses)

/**
 * @route PUT /api/v1/addresses/:id
 * @desc Update an address
 * @access Public
 */
router.put('/:id', addressController.updateAddress)

/**
 * @route DELETE /api/v1/addresses/:id
 * @desc Delete an address
 * @access Public
 */
router.delete('/:id', addressController.deleteAddress)

export default router
