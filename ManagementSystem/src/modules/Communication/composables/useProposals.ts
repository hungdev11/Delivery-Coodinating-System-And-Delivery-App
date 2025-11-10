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
  type CreateProposalRequest,
  type ProposalResponseRequest,
  type ProposalConfigDTO,
  type InteractiveProposalResponseDTO,
  type ProposalTypeConfig,
} from '../api'

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
          title: 'Success',
          description: 'Proposal created successfully',
          color: 'success',
        })
        return response.result
      }
      return null
    } catch (error) {
      console.error('Failed to create proposal:', error)
      toast.add({
        title: 'Error',
        description: 'Failed to create proposal',
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
  const respond = async (
    proposalId: string,
    userId: string,
    data: ProposalResponseRequest,
  ) => {
    loading.value = true
    try {
      const response = await respondToProposal(proposalId, userId, data)
      if (response.result) {
        toast.add({
          title: 'Success',
          description: 'Proposal response sent successfully',
          color: 'success',
        })
        return response.result
      }
      return null
    } catch (error) {
      console.error('Failed to respond to proposal:', error)
      toast.add({
        title: 'Error',
        description: 'Failed to respond to proposal',
        color: 'error',
      })
      return null
    } finally {
      loading.value = false
    }
  }

  /**
   * Get available proposal configs for roles
   */
  const loadAvailableConfigs = async (roles: string[]) => {
    loading.value = true
    try {
      const response = await getAvailableConfigs(roles)
      if (response.result) {
        availableConfigs.value = response.result
      }
    } catch (error) {
      console.error('Failed to load available configs:', error)
      toast.add({
        title: 'Error',
        description: 'Failed to load available proposal configs',
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
        title: 'Error',
        description: 'Failed to load proposal configs',
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
          title: 'Success',
          description: 'Proposal config created successfully',
          color: 'success',
        })
        await loadProposalConfigs()
        return response.result
      }
      return null
    } catch (error) {
      console.error('Failed to create proposal config:', error)
      toast.add({
        title: 'Error',
        description: 'Failed to create proposal config',
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
          title: 'Success',
          description: 'Proposal config updated successfully',
          color: 'success',
        })
        await loadProposalConfigs()
        return response.result
      }
      return null
    } catch (error) {
      console.error('Failed to update proposal config:', error)
      toast.add({
        title: 'Error',
        description: 'Failed to update proposal config',
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
        title: 'Success',
        description: 'Proposal config deleted successfully',
        color: 'success',
      })
      await loadProposalConfigs()
      return true
    } catch (error) {
      console.error('Failed to delete proposal config:', error)
      toast.add({
        title: 'Error',
        description: 'Failed to delete proposal config',
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
