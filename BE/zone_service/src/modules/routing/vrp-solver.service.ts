/**
 * VRP (Vehicle Routing Problem) Solver Service
 * 
 * Implements heuristic-based VRP solver with:
 * - Workload balancing (fairness constraint)
 * - Session time limits (maxSessionTime constraint)
 * - Zone-based filtering
 * - P0 parcel prioritization (max 3 per shipper, optimize shipper count)
 */

import { logger } from '../../common/logger/logger.service';
import { VRPOrderDto, VRPShipperDto, VRPTaskDto } from './routing.model';

interface InternalOrder {
  orderId: string;
  lat: number;
  lon: number;
  serviceTime: number; // seconds
  priority: number;
  zoneId?: string;
  deliveryAddressId?: string;
}

interface InternalShipper {
  shipperId: string;
  lat: number;
  lon: number;
  shiftStart: Date;
  shiftEnd: Date;
  maxSessionTime: number; // hours
  capacity: number;
  zoneIds?: string[];
  vehicle?: 'car' | 'motorbike';
}

interface AssignmentResult {
  shipperId: string;
  tasks: VRPTaskDto[];
  totalDuration: number; // seconds
  totalDistance: number; // meters
}

interface DistanceMatrix {
  durations: number[][];
  distances: number[][];
}

export class VRPSolverService {

  /**
   * Solve VRP assignment problem
   * 
   * Algorithm:
   * 1. Separate P0 orders (priority 0) - max 3 per shipper, optimize shipper count
   * 2. For each shipper, assign orders using nearest neighbor + savings
   * 3. Respect time window constraints (maxSessionTime)
   * 4. Balance workload across shippers
   * 5. Zone-based filtering (prefer orders in shipper's working zones)
   */
  static async solveVRP(
    shippers: VRPShipperDto[],
    orders: VRPOrderDto[],
    vehicle: 'car' | 'motorbike' = 'motorbike',
    mode: string = 'v2-full'
  ): Promise<{ assignments: Record<string, VRPTaskDto[]>; unassignedOrders: string[] }> {
    logger.info(`[VRPSolver] Solving VRP: ${shippers.length} shippers, ${orders.length} orders`);

    if (shippers.length === 0 || orders.length === 0) {
      return { assignments: {}, unassignedOrders: orders.map(o => o.orderId) };
    }

    // Convert DTOs to internal format
    const internalOrders = orders.map(this.convertOrder);
    const internalShippers = shippers.map(s => this.convertShipper(s));

    // Step 1: Get distance matrix for all locations
    const allLocations = [
      ...internalShippers.map(s => ({ lat: s.lat, lon: s.lon })),
      ...internalOrders.map(o => ({ lat: o.lat, lon: o.lon }))
    ];

    logger.info(`[VRPSolver] Fetching distance matrix for ${allLocations.length} locations`);
    const matrix = await this.getDistanceMatrix(allLocations, vehicle, mode);
    
    // Create lookup: shipper index = 0..N-1, order index = N..N+M-1
    const shipperCount = internalShippers.length;

    // Step 2: Separate P0 orders (priority 0) - handle specially
    const p0Orders = internalOrders.filter(o => o.priority === 0);
    const normalOrders = internalOrders.filter(o => o.priority !== 0);

    logger.info(`[VRPSolver] Found ${p0Orders.length} P0 orders, ${normalOrders.length} normal orders`);

    // Step 3: Assign P0 orders first (max 3 per shipper, optimize shipper count)
    const p0Assignments = this.assignP0Orders(
      internalShippers,
      p0Orders
    );

    // Step 4: Assign normal orders with workload balancing
    const normalAssignments = this.assignNormalOrders(
      internalShippers,
      normalOrders,
      matrix,
      shipperCount,
      p0Assignments
    );

    // Step 5: Combine assignments and calculate arrival times
    const finalAssignments = this.combineAssignments(
      internalShippers,
      internalOrders,
      p0Assignments,
      normalAssignments,
      matrix,
      shipperCount
    );

    // Step 6: Identify unassigned orders
    const assignedOrderIds = new Set<string>();
    Object.values(finalAssignments).forEach(tasks => {
      tasks.forEach(task => assignedOrderIds.add(task.orderId));
    });
    const unassignedOrders = internalOrders
      .filter(o => !assignedOrderIds.has(o.orderId))
      .map(o => o.orderId);

    logger.info(`[VRPSolver] Assignment complete: ${Object.keys(finalAssignments).length} shippers assigned, ${unassignedOrders.length} unassigned orders`);

    return {
      assignments: finalAssignments,
      unassignedOrders
    };
  }

  /**
   * Assign P0 orders (priority 0) - max 3 per shipper, optimize shipper count
   */
  private static assignP0Orders(
    shippers: InternalShipper[],
    p0Orders: InternalOrder[]
  ): AssignmentResult[] {
    const assignments: AssignmentResult[] = shippers.map(s => ({
      shipperId: s.shipperId,
      tasks: [],
      totalDuration: 0,
      totalDistance: 0
    }));

    if (p0Orders.length === 0) {
      return assignments;
    }

    // Sort P0 orders by priority (all are 0, but we can sort by service time for efficiency)
    const sortedP0Orders = [...p0Orders].sort((a, b) => a.serviceTime - b.serviceTime);

    // Assign P0 orders to shippers (max 3 per shipper)
    let orderIndex = 0;
    let shipperIndex = 0;

    while (orderIndex < sortedP0Orders.length && shipperIndex < shippers.length) {
      const order = sortedP0Orders[orderIndex]!;
      const assignment = assignments[shipperIndex]!;

      // Check if shipper can take more P0 orders (max 3)
      const p0Count = assignment.tasks.length;
      if (p0Count < 3) {
        // Add order to this shipper
        const taskIndex = assignment.tasks.length;
        assignment.tasks.push({
          orderId: order.orderId,
          sequenceIndex: taskIndex,
          estimatedArrivalTime: '', // Will be calculated later
          travelTimeFromPreviousStop: 0
        });
        orderIndex++;
      } else {
        // Move to next shipper
        shipperIndex++;
      }
    }

    return assignments;
  }

  /**
   * Assign normal orders with workload balancing and time constraints
   */
  private static assignNormalOrders(
    shippers: InternalShipper[],
    orders: InternalOrder[],
    matrix: DistanceMatrix,
    shipperCount: number,
    p0Assignments: AssignmentResult[]
  ): AssignmentResult[] {
    const assignments = p0Assignments.map(a => ({ ...a, tasks: [...a.tasks] }));
    const remainingOrders = [...orders];
    const unassigned: InternalOrder[] = [];

    // Create order index map for matrix lookup
    const orderIndexMap = new Map<string, number>();
    orders.forEach((order, idx) => {
      orderIndexMap.set(order.orderId, idx);
    });

    // Sort orders by priority (ascending - higher priority first)
    remainingOrders.sort((a, b) => a.priority - b.priority);

    // Assign orders using nearest neighbor + workload balancing
    while (remainingOrders.length > 0) {
      let bestAssignment: { shipperIdx: number; orderIdx: number; cost: number } | null = null;

      // Find best shipper-order pair
      for (let sIdx = 0; sIdx < shippers.length; sIdx++) {
        const shipper = shippers[sIdx]!;
        const assignment = assignments[sIdx]!;
        
        // Check capacity
        if (assignment.tasks.length >= shipper.capacity) {
          continue;
        }

        // Check time constraint (approximate - will refine later)
        const currentDuration = assignment.totalDuration;
        const maxDurationSeconds = shipper.maxSessionTime * 3600;
        if (currentDuration >= maxDurationSeconds * 0.9) { // 90% threshold
          continue;
        }

        // Find nearest unassigned order for this shipper
        for (let oIdx = 0; oIdx < remainingOrders.length; oIdx++) {
          const order = remainingOrders[oIdx]!;
          
          // Zone-based filtering: prefer orders in shipper's zones
          if (shipper.zoneIds && shipper.zoneIds.length > 0 && order.zoneId) {
            if (!shipper.zoneIds.includes(order.zoneId)) {
              continue; // Skip orders not in shipper's zones
            }
          }

          // Calculate cost (distance + workload penalty)
          const shipperLocationIdx = sIdx;
          const orderOriginalIdx = orderIndexMap.get(order.orderId) || 0;
          const orderLocationIdx = shipperCount + orderOriginalIdx;
          const travelTime = matrix.durations[shipperLocationIdx]?.[orderLocationIdx] || Infinity;
          
          // Workload balancing penalty (prefer shippers with fewer tasks)
          const workloadPenalty = assignment.tasks.length * 60; // 60 seconds per task
          const cost = travelTime + workloadPenalty;

          if (!bestAssignment || cost < bestAssignment.cost) {
            bestAssignment = { shipperIdx: sIdx, orderIdx: oIdx, cost };
          }
        }
      }

      if (bestAssignment) {
        const order = remainingOrders.splice(bestAssignment.orderIdx, 1)[0]!;
        const assignment = assignments[bestAssignment.shipperIdx]!;
        
        assignment.tasks.push({
          orderId: order.orderId,
          sequenceIndex: assignment.tasks.length,
          estimatedArrivalTime: '',
          travelTimeFromPreviousStop: 0
        });
      } else {
        // No valid assignment found, mark as unassigned
        unassigned.push(remainingOrders.shift()!);
      }
    }

    logger.info(`[VRPSolver] Normal orders assigned: ${orders.length - unassigned.length} assigned, ${unassigned.length} unassigned`);
    
    return assignments;
  }

  /**
   * Combine P0 and normal assignments, optimize route order, and calculate arrival times
   */
  private static combineAssignments(
    shippers: InternalShipper[],
    orders: InternalOrder[],
    _p0Assignments: AssignmentResult[],
    normalAssignments: AssignmentResult[],
    matrix: DistanceMatrix,
    shipperCount: number
  ): Record<string, VRPTaskDto[]> {
    const result: Record<string, VRPTaskDto[]> = {};

    // Create order index map for matrix lookup
    const orderIndexMap = new Map<string, number>();
    orders.forEach((order, idx) => {
      orderIndexMap.set(order.orderId, idx);
    });

    for (let sIdx = 0; sIdx < shippers.length; sIdx++) {
      const shipper = shippers[sIdx]!;
      const allTasks = [...normalAssignments[sIdx]!.tasks];

      if (allTasks.length === 0) {
        continue; // Skip shippers with no assignments
      }

      // Get order details for route optimization
      const taskOrders = allTasks.map(task => {
        const orderIdx = orders.findIndex(o => o.orderId === task.orderId);
        return orderIdx >= 0 ? orders[orderIdx]! : null;
      }).filter(Boolean) as InternalOrder[];

      // Optimize route order using nearest neighbor
      const optimizedOrder = this.optimizeRouteOrder(
        { lat: shipper.lat, lon: shipper.lon },
        taskOrders,
        matrix,
        shipperCount,
        orderIndexMap
      );

      // Calculate arrival times and travel times
      const optimizedTasks: VRPTaskDto[] = [];
      let currentTime = shipper.shiftStart;

      for (let i = 0; i < optimizedOrder.length; i++) {
        const order = optimizedOrder[i]!;
        const orderIdx = orderIndexMap.get(order.orderId) || 0;
        const locationIdx = shipperCount + orderIdx;

        // Get travel time from current location to order
        const currentLocationIdx = i === 0 
          ? sIdx 
          : shipperCount + (orderIndexMap.get(optimizedOrder[i - 1]!.orderId) || 0);
        
        const travelTimeSeconds = matrix.durations[currentLocationIdx]?.[locationIdx] || 0;

        // Update current time
        currentTime = new Date(currentTime.getTime() + (travelTimeSeconds + order.serviceTime) * 1000);

        optimizedTasks.push({
          orderId: order.orderId,
          sequenceIndex: i,
          estimatedArrivalTime: currentTime.toISOString(),
          travelTimeFromPreviousStop: travelTimeSeconds
        });
      }

      result[shipper.shipperId] = optimizedTasks;
    }

    return result;
  }

  /**
   * Optimize route order using nearest neighbor heuristic
   */
  private static optimizeRouteOrder(
    _start: { lat: number; lon: number },
    orders: InternalOrder[],
    matrix: DistanceMatrix,
    shipperCount: number,
    orderIndexMap: Map<string, number>
  ): InternalOrder[] {
    if (orders.length <= 1) {
      return orders;
    }

    const remaining = new Set(orders);
    const route: InternalOrder[] = [];
    let currentLocationIdx = 0; // Start at shipper location (index 0 in matrix)

    while (remaining.size > 0) {
      let nearest: InternalOrder | null = null;
      let nearestCost = Infinity;

      for (const order of remaining) {
        const orderIdx = orderIndexMap.get(order.orderId) || 0;
        const locationIdx = shipperCount + orderIdx;
        const travelTime = matrix.durations[currentLocationIdx]?.[locationIdx] || Infinity;

        if (travelTime < nearestCost) {
          nearestCost = travelTime;
          nearest = order;
        }
      }

      if (nearest) {
        route.push(nearest);
        remaining.delete(nearest);
        // Update current location index for next iteration
        const nearestOrderIdx = orderIndexMap.get(nearest.orderId) || 0;
        currentLocationIdx = shipperCount + nearestOrderIdx;
      }
    }

    return route;
  }

  /**
   * Get distance matrix from OSRM
   */
  private static async getDistanceMatrix(
    locations: Array<{ lat: number; lon: number }>,
    vehicle: 'car' | 'motorbike',
    mode: string
  ): Promise<DistanceMatrix> {
    try {
      // Call OSRM table API
      const vehicleType = vehicle === 'motorbike' ? 'motorbike' : 'driving';
      const coordString = locations.map(l => `${l.lon},${l.lat}`).join(';');

      // Determine OSRM URL based on mode
      const modeUrls: Record<string, string> = {
        'v2-full': process.env.OSRM_V2_FULL_URL || 'http://osrm-v2-full:5000',
        'v2-rating-only': process.env.OSRM_V2_RATING_URL || 'http://osrm-v2-rating-only:5000',
        'v2-blocking-only': process.env.OSRM_V2_BLOCKING_URL || 'http://osrm-v2-blocking-only:5000',
        'v2-base': process.env.OSRM_V2_BASE_URL || 'http://osrm-v2-base:5000',
        'v2-car-full': process.env.OSRM_V2_CAR_FULL_URL || 'http://osrm-v2-car-full:5000',
        'v2-car-rating-only': process.env.OSRM_V2_CAR_RATING_URL || 'http://osrm-v2-car-rating-only:5000',
        'v2-car-blocking-only': process.env.OSRM_V2_CAR_BLOCKING_URL || 'http://osrm-v2-car-blocking-only:5000',
        'v2-car-base': process.env.OSRM_V2_CAR_BASE_URL || 'http://osrm-v2-car-base:5000',
      };
      const baseUrl = modeUrls[mode] || modeUrls['v2-full'] || 'http://osrm-v2-full:5000';

      const url = `${baseUrl}/table/v1/${vehicleType}/${coordString}?annotations=duration,distance`;

      const axios = await import('axios');
      const response = await axios.default.get(url);

      if (response.data.code !== 'Ok') {
        throw new Error(`OSRM table API error: ${response.data.code}`);
      }

      return {
        durations: response.data.durations || [],
        distances: response.data.distances || []
      };
    } catch (error: any) {
      logger.error(`[VRPSolver] Failed to get distance matrix: ${error.message}`);
      throw error;
    }
  }

  /**
   * Convert VRPOrderDto to InternalOrder
   */
  private static convertOrder(dto: VRPOrderDto): InternalOrder {
    const result: InternalOrder = {
      orderId: dto.orderId,
      lat: dto.lat,
      lon: dto.lon,
      serviceTime: dto.serviceTime,
      priority: dto.priority
    };
    if (dto.zoneId !== undefined) {
      result.zoneId = dto.zoneId;
    }
    if (dto.deliveryAddressId !== undefined) {
      result.deliveryAddressId = dto.deliveryAddressId;
    }
    return result;
  }

  /**
   * Convert VRPShipperDto to InternalShipper
   */
  private static convertShipper(dto: VRPShipperDto): InternalShipper {
    // Parse shiftStart time
    let shiftStart: Date;
    if (dto.shiftStart.includes('T') || dto.shiftStart.includes(' ')) {
      shiftStart = new Date(dto.shiftStart);
    } else {
      // Assume HH:mm:ss format, use today's date
      const [hours, minutes, seconds] = dto.shiftStart.split(':').map(Number);
      shiftStart = new Date();
      shiftStart.setHours(hours || 0, minutes || 0, seconds || 0, 0);
    }

    // Calculate shiftEnd = shiftStart + maxSessionTime
    const shiftEnd = new Date(shiftStart.getTime() + dto.maxSessionTime * 3600 * 1000);

    const result: InternalShipper = {
      shipperId: dto.shipperId,
      lat: dto.lat,
      lon: dto.lon,
      shiftStart,
      shiftEnd,
      maxSessionTime: dto.maxSessionTime,
      capacity: dto.capacity
    };
    if (dto.zoneIds !== undefined) {
      result.zoneIds = dto.zoneIds;
    }
    if (dto.vehicle !== undefined) {
      result.vehicle = dto.vehicle;
    }
    return result;
  }
}
