import type { DemoUserProfile } from '../data/demoUsers'
import { DEMO_USERS } from '../data/demoUsers'

interface CharacterSelectModalProps {
  open: boolean
  onSelect: (user: DemoUserProfile) => void
}

export function CharacterSelectModal({ open, onSelect }: CharacterSelectModalProps) {
  if (!open) return null

  return (
    <div className="modal-overlay" role="presentation">
      <div
        className="modal-dialog"
        role="dialog"
        aria-modal="true"
        aria-labelledby="character-select-title"
      >
        <p className="eyebrow">Demo mode</p>
        <h2 id="character-select-title">What character are you?</h2>
        <p className="modal-subtitle">
          Your choice sets the JWT identity sent to corporate APIs. Permissions and tool access
          follow this user&apos;s roles.
        </p>

        <ul className="character-grid">
          {DEMO_USERS.map((user) => (
            <li key={user.id}>
              <button type="button" className="character-card" onClick={() => onSelect(user)}>
                <span className="character-name">{user.displayName}</span>
                <span className="character-username">{user.username}</span>
                <span className="character-roles">
                  {user.roles.map((role) => (
                    <span key={role} className="role-chip">
                      {role}
                    </span>
                  ))}
                </span>
              </button>
            </li>
          ))}
        </ul>
      </div>
    </div>
  )
}
