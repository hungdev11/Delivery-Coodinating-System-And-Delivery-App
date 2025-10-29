/**
 * useUserExport Composable
 *
 * Handle user export to CSV
 */

import { useToast } from '@nuxt/ui/runtime/composables/useToast.js'
import type { UserDto } from '../model.type'

export function useUserExport() {
  const toast = useToast()

  /**
   * Generate CSV from users
   */
  const generateCSV = (users: UserDto[]): string => {
    const headers = ['ID', 'Username', 'Email', 'Full Name', 'Phone', 'Status']
    const rows = users.map((u) => [
      u.id,
      u.username,
      u.email,
      u.fullName,
      u.phone || '',
      u.displayStatus,
    ])

    return [headers, ...rows].map((row) => row.join(',')).join('\n')
  }

  /**
   * Download CSV file
   */
  const downloadCSV = (content: string, filename: string) => {
    const blob = new Blob([content], { type: 'text/csv' })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = filename
    link.click()
    URL.revokeObjectURL(url)
  }

  /**
   * Export users to CSV
   */
  const exportUsers = (users: UserDto[], filename = 'users-export.csv') => {
    const csv = generateCSV(users)
    downloadCSV(csv, filename)

    toast.add({
      title: 'Success',
      description: `${users.length} user(s) exported`,
      color: 'success',
    })
  }

  return {
    exportUsers,
  }
}
