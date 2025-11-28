import { defineStore } from 'pinia'
import * as addressesApi from '../api'

export const useAddresses = defineStore('addressesStore', {
  state: () => ({
    searchTerm: '' as string,
    searching: false as boolean,
    searchResult: null as addressesApi.SearchAddressesResult | null,
    creating: false as boolean,
    error: null as string | null,
    success: null as string | null,
    selected: null as {
      id?: string
      source: 'local' | 'track-asia'
      name: string
      lat: number
      lon: number
      addressText?: string
    } | null,
  }),
  actions: {
    async search(limit: number = 10) {
      if (!this.searchTerm.trim()) {
        this.searchResult = { local: [], external: [] }
        return
      }
      this.searching = true
      this.error = null
      try {
        const res = await addressesApi.searchAddresses({ q: this.searchTerm.trim(), limit })
        this.searchResult = res.result || { local: [], external: [] }
      } catch (e: unknown) {
        const message = e instanceof Error ? e.message : 'Search failed'
        this.error = message
      } finally {
        this.searching = false
      }
    },

    async create(payload: { name: string; addressText?: string; lat: number; lon: number }) {
      this.creating = true
      this.error = null
      this.success = null
      try {
        const res = await addressesApi.createAddress(payload)
        this.success = 'Address created successfully'
        return res
      } catch (e: unknown) {
        const message = e instanceof Error ? e.message : 'Create address failed'
        this.error = message
        throw e
      } finally {
        this.creating = false
      }
    },

    async findByPoint(params: { lat: number; lon: number; radius?: number; limit?: number }) {
      return addressesApi.findByPoint(params)
    },

    select(item: {
      id?: string
      source: 'local' | 'track-asia'
      name: string
      lat: number
      lon: number
      addressText?: string
    }) {
      this.selected = item
      this.searchTerm = item.name
    },

    clearSelection() {
      this.selected = null
    },

    async update(
      id: string,
      payload: { name?: string; addressText?: string | null; lat?: number; lon?: number },
    ) {
      this.creating = true
      this.error = null
      this.success = null
      try {
        const res = await addressesApi.updateAddress(id, payload)
        this.success = 'Address updated successfully'
        return res
      } catch (e: unknown) {
        const message = e instanceof Error ? e.message : 'Update address failed'
        this.error = message
        throw e
      } finally {
        this.creating = false
      }
    },

    async getAddressById(id: string) {
      const res = await addressesApi.getAddressById(id)
      if (!res.result) throw new Error('Address not found')
      const a = res.result
      this.select({
        source: 'local',
        id: a.id,
        name: a.name,
        lat: a.lat,
        lon: a.lon,
        addressText: a.addressText || undefined,
      })
      return a
    },

    async remove(id: string) {
      await addressesApi.deleteAddress(id)
      if (this.selected?.id === id) this.clearSelection()
      this.success = 'Address deleted'
    },
  },
})
