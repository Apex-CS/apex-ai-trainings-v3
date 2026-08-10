import type { DemoUserProfile } from '../data/demoUsers'

interface DemoUserBannerProps {
  user: DemoUserProfile
}

export function DemoUserBanner({ user }: DemoUserBannerProps) {
  return (
    <div className="demo-user-banner" aria-label={`Signed in as ${user.displayName}`}>
      <div className="demo-user-identity">
        <span className="demo-user-label">Playing as</span>
        <strong className="demo-user-name">{user.displayName}</strong>
        <span className="demo-user-username">({user.username})</span>
      </div>
      <div className="demo-user-roles" aria-label="User roles">
        {user.roles.map((role) => (
          <span key={role} className="role-chip role-chip-banner">
            {role}
          </span>
        ))}
      </div>
    </div>
  )
}
