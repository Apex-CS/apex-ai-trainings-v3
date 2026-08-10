export type DemoUserId =
  | 'FULANO_SMITH'
  | 'SUTANO_DOE'
  | 'MENGANA_DAVIDSON'
  | 'BART_PEREZ'

export interface DemoUserProfile {
  id: DemoUserId
  displayName: string
  username: string
  roles: string[]
}

export const DEMO_USERS: DemoUserProfile[] = [
  {
    id: 'FULANO_SMITH',
    displayName: 'Fulano Smith',
    username: 'fulano.smith',
    roles: ['financial-admin', 'it-user', 'marketing-user', 'sales-user'],
  },
  {
    id: 'SUTANO_DOE',
    displayName: 'Sutano Doe',
    username: 'sutano.doe',
    roles: ['sales-admin', 'financial-user', 'it-user', 'marketing-user'],
  },
  {
    id: 'MENGANA_DAVIDSON',
    displayName: 'Mengana Davidson',
    username: 'mengana.davidson',
    roles: ['marketing-admin', 'financial-user', 'it-user', 'sales-user'],
  },
  {
    id: 'BART_PEREZ',
    displayName: 'Bart Perez',
    username: 'bart.perez',
    roles: ['it-admin', 'financial-user', 'marketing-user', 'sales-user'],
  },
]

export const DEMO_USER_STORAGE_KEY = 'example-company-demo-user'

export function findDemoUser(id: DemoUserId): DemoUserProfile | undefined {
  return DEMO_USERS.find((user) => user.id === id)
}
