const AVATAR_COLORS = ['#f4b400', '#db4437', '#4285f4', '#0f9d58', '#ab47bc', '#00acc1', '#ff7043', '#9e9d24']

export function avatarColor(userId: number): string {
  return AVATAR_COLORS[userId % AVATAR_COLORS.length]
}
