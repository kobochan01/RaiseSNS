import { avatarColor } from '../utils/avatar'

type Props = {
  userId: number
  displayName: string
  avatarUrl?: string | null
  size?: 'sm' | 'md' | 'lg'
}

export function Avatar({ userId, displayName, avatarUrl, size = 'md' }: Props) {
  if (avatarUrl) {
    return (
      <div
        className={`avatar avatar--${size}`}
        style={{ backgroundImage: `url(${avatarUrl})` }}
        role="img"
        aria-label={displayName}
      />
    )
  }
  return (
    <div className={`avatar avatar--${size}`} style={{ backgroundColor: avatarColor(userId) }}>
      {displayName.charAt(0)}
    </div>
  )
}
