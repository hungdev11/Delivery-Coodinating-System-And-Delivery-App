import 'vue-router'

declare module 'vue-router' {
  interface RouteMeta {
    requiresAuth?: boolean
    layout?: 'default' | 'blank'
    roles?: string[]
    title?: string
  }
}
