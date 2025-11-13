/**
 * useParcelExport Composable
 *
 * Handle parcel export to CSV
 */

import { useToast } from '@nuxt/ui/runtime/composables/useToast.js'
import type { ParcelDto } from '../model.type'

export function useParcelExport() {
  const toast = useToast()

  /**
   * Generate CSV from parcels
   */
  const generateCSV = (parcels: ParcelDto[]): string => {
    const headers = [
      'Code',
      'Sender ID',
      'Receiver ID',
      'Receiver Phone',
      'Delivery Type',
      'Status',
      'Weight (kg)',
      'Value (VND)',
      'Receive From',
      'Target Destination',
      'Latitude',
      'Longitude',
      'Created At',
      'Updated At',
      'Delivered At',
    ]
    const rows = parcels.map((p) => [
      p.code,
      p.senderId,
      p.receiverId,
      p.receiverPhoneNumber || '',
      p.deliveryType,
      p.status,
      p.weight.toString(),
      p.value.toString(),
      p.receiveFrom,
      p.targetDestination,
      p.lat?.toString() || '',
      p.lon?.toString() || '',
      p.createdAt,
      p.updatedAt,
      p.deliveredAt || '',
    ])

    return [headers, ...rows].map((row) => row.map((cell) => `"${cell}"`).join(',')).join('\n')
  }

  /**
   * Download CSV file
   */
  const downloadCSV = (content: string, filename: string) => {
    const blob = new Blob([content], { type: 'text/csv;charset=utf-8;' })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = filename
    link.click()
    URL.revokeObjectURL(url)
  }

  /**
   * Export parcels to CSV
   */
  const exportParcels = (parcels: ParcelDto[], filename = 'parcels-export.csv') => {
    if (parcels.length === 0) {
      toast.add({
        title: 'Warning',
        description: 'No parcels to export',
        color: 'warning',
      })
      return
    }

    const csv = generateCSV(parcels)
    downloadCSV(csv, filename)

    toast.add({
      title: 'Success',
      description: `${parcels.length} parcel(s) exported`,
      color: 'success',
    })
  }

  return {
    exportParcels,
  }
}
