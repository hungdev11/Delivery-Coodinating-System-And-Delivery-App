/**
 * useProposals Composable
 *
 * Business logic for proposal management
 */

import { ref } from 'vue'
import { useToast } from '@nuxt/ui/runtime/composables/useToast.js'
import {
  createProposal,
  respondToProposal,
  getAvailableConfigs,
  getProposalConfigs,
  createProposalConfig,
  updateProposalConfig,
  deleteProposalConfig,
} from '../api'
import { getParcelsV2 } from '@/modules/Parcels/api'
import type { QueryPayload } from '@/common/types/filter'
import type {
  FilterGroupItemV2,
  FilterConditionItemV2,
  FilterOperatorItemV2,
} from '@/common/types/filter-v2'
import { FilterItemType } from '@/common/types/filter-v2'
import type {
  CreateProposalRequest,
  InteractiveProposalResponseDTO,
  ProposalConfigDTO,
  ProposalResponseRequest,
  ProposalTypeConfig,
} from '../model.type'

export function useProposals() {
  const toast = useToast()

  const proposals = ref<InteractiveProposalResponseDTO[]>([])
  const proposalConfigs = ref<ProposalTypeConfig[]>([])
  const availableConfigs = ref<ProposalTypeConfig[]>([])
  const loading = ref(false)

  /**
   * Create a new proposal
   */
  const create = async (data: CreateProposalRequest) => {
    loading.value = true
    try {
      const response = await createProposal(data)
      if (response.result) {
        toast.add({
          title: 'Thành công',
          description: 'Đã tạo đề xuất thành công',
          color: 'success',
        })
        return response.result
      }
      return null
    } catch (error) {
      console.error('Failed to create proposal:', error)
      toast.add({
        title: 'Lỗi',
        description: 'Không thể tạo đề xuất',
        color: 'error',
      })
      return null
    } finally {
      loading.value = false
    }
  }

  /**
   * Respond to a proposal
   */
  const respond = async (proposalId: string, userId: string, data: ProposalResponseRequest) => {
    loading.value = true
    try {
      const response = await respondToProposal(proposalId, userId, data)
      if (response.result) {
        toast.add({
          title: 'Thành công',
          description: 'Đã gửi phản hồi đề xuất thành công',
          color: 'success',
        })
        return response.result
      }
      return null
    } catch (error) {
      console.error('Failed to respond to proposal:', error)
      toast.add({
        title: 'Lỗi',
        description: 'Không thể phản hồi đề xuất',
        color: 'error',
      })
      return null
    } finally {
      loading.value = false
    }
  }

  /**
   * Check if user has undelivered parcels (as recipient)
   */
  const checkUndeliveredParcels = async (userId: string): Promise<boolean> => {
    try {
      console.log('🔍 Checking undelivered parcels for user:', userId)

      // Query parcels where user is recipient and status != DELIVERED
      // Use V2 filter format with type property
      const filterGroup: FilterGroupItemV2 = {
        type: FilterItemType.GROUP,
        items: [
          {
            type: FilterItemType.CONDITION,
            field: 'receiverId',
            operator: 'EQUALS',
            value: userId,
          } as FilterConditionItemV2,
          {
            type: FilterItemType.OPERATOR,
            value: 'AND',
          } as FilterOperatorItemV2,
          {
            type: FilterItemType.CONDITION,
            field: 'status',
            operator: 'NOT_EQUALS',
            value: 'DELIVERED',
          } as FilterConditionItemV2,
        ],
      }

      const queryPayload: QueryPayload = {
        filters: filterGroup as unknown as QueryPayload['filters'], // Cast to FilterGroup for QueryPayload compatibility
        page: 0,
        size: 1, // Only need to check if any exist
      }

      const response = await getParcelsV2(queryPayload)

      if (response.result && response.result.data) {
        const hasUndelivered = response.result.data.length > 0
        console.log('📦 User has undelivered parcels:', hasUndelivered)
        return hasUndelivered
      }

      return false
    } catch (error) {
      console.warn('⚠️ Failed to check undelivered parcels:', error)
      // On error, don't add USER role (fail-safe)
      return false
    }
  }

  /**
   * Get available proposal configs for roles
   * If ADMIN role exists and userId is provided, check for undelivered parcels
   * and add USER role if user has undelivered parcels
   */
  const loadAvailableConfigs = async (roles: string[], userId?: string) => {
    loading.value = true
    try {
      console.log('🔍 Loading proposal configs for roles:', roles, 'userId:', userId)

      // If ADMIN role exists and userId is provided, check for undelivered parcels
      const effectiveRoles = [...roles]
      if (userId && roles.includes('ADMIN')) {
        const hasUndelivered = await checkUndeliveredParcels(userId)
        if (hasUndelivered && !effectiveRoles.includes('USER')) {
          console.log('✅ ADMIN user has undelivered parcels, adding USER role')
          effectiveRoles.push('USER')
        }
      }

      console.log('📋 Effective roles for config lookup:', effectiveRoles)
      const configs = await getAvailableConfigs(effectiveRoles)
      console.log('📋 Raw configs response:', configs)
      
      // Handle different response formats
      let configsArray: ProposalTypeConfig[] = []
      if (Array.isArray(configs)) {
        // Direct array response
        configsArray = configs
      } else if (configs && typeof configs === 'object') {
        // Check if wrapped in 'result' (IApiResponse format)
        const configsObj = configs as Record<string, unknown>
        if ('result' in configsObj && Array.isArray(configsObj.result)) {
          configsArray = configsObj.result as ProposalTypeConfig[]
        } else if ('data' in configsObj && Array.isArray(configsObj.data)) {
          // Alternative format with 'data' field
          configsArray = configsObj.data as ProposalTypeConfig[]
      } else {
        console.warn('⚠️ Unexpected response format from getAvailableConfigs:', configs)
        }
      }
      
      availableConfigs.value = configsArray
      console.log('📋 Loaded', configsArray.length, 'available proposal configs')
    } catch (error) {
      console.error('Failed to load available configs:', error)
      toast.add({
        title: 'Lỗi',
        description: 'Không thể tải danh sách cấu hình đề xuất',
        color: 'error',
      })
    } finally {
      loading.value = false
    }
  }

  /**
   * Load all proposal configs (Admin only)
   */
  const loadProposalConfigs = async () => {
    loading.value = true
    try {
      const response = await getProposalConfigs()
      if (response.result) {
        proposalConfigs.value = response.result
      }
    } catch (error) {
      console.error('Failed to load proposal configs:', error)
      toast.add({
        title: 'Lỗi',
        description: 'Không thể tải cấu hình đề xuất',
        color: 'error',
      })
    } finally {
      loading.value = false
    }
  }

  /**
   * Create a proposal config (Admin only)
   */
  const createConfig = async (data: ProposalConfigDTO) => {
    loading.value = true
    try {
      const response = await createProposalConfig(data)
      if (response.result) {
        toast.add({
          title: 'Thành công',
          description: 'Đã tạo cấu hình đề xuất thành công',
          color: 'success',
        })
        await loadProposalConfigs()
        return response.result
      }
      return null
    } catch (error) {
      console.error('Failed to create proposal config:', error)
      toast.add({
        title: 'Lỗi',
        description: 'Không thể tạo cấu hình đề xuất',
        color: 'error',
      })
      return null
    } finally {
      loading.value = false
    }
  }

  /**
   * Update a proposal config (Admin only)
   */
  const updateConfig = async (configId: string, data: ProposalConfigDTO) => {
    loading.value = true
    try {
      const response = await updateProposalConfig(configId, data)
      if (response.result) {
        toast.add({
          title: 'Thành công',
          description: 'Đã cập nhật cấu hình đề xuất thành công',
          color: 'success',
        })
        await loadProposalConfigs()
        return response.result
      }
      return null
    } catch (error) {
      console.error('Failed to update proposal config:', error)
      toast.add({
        title: 'Lỗi',
        description: 'Không thể cập nhật cấu hình đề xuất',
        color: 'error',
      })
      return null
    } finally {
      loading.value = false
    }
  }

  /**
   * Delete a proposal config (Admin only)
   */
  const deleteConfig = async (configId: string) => {
    loading.value = true
    try {
      await deleteProposalConfig(configId)
      toast.add({
        title: 'Thành công',
        description: 'Đã xóa cấu hình đề xuất thành công',
        color: 'success',
      })
      await loadProposalConfigs()
      return true
    } catch (error) {
      console.error('Failed to delete proposal config:', error)
      toast.add({
        title: 'Lỗi',
        description: 'Không thể xóa cấu hình đề xuất',
        color: 'error',
      })
      return false
    } finally {
      loading.value = false
    }
  }

  return {
    proposals,
    proposalConfigs,
    availableConfigs,
    loading,
    create,
    respond,
    loadAvailableConfigs,
    loadProposalConfigs,
    createConfig,
    updateConfig,
    deleteConfig,
  }
}
