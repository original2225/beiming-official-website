/**
 * 图片资源索引
 */

const IMG = {
  logo: 'logo.webp',
  heroBg: 'hero-bg.jpg',
  bgHero: 'bg-hero.webp',
  bgStatus: 'bg-status.webp',
  bgAnnounce: 'bg-announce.webp',
  bgMembers: 'bg-members.webp',
  texStone: 'tex-stone.webp',
  texDark: 'tex-dark.webp',
  texLight: 'tex-light.webp',
  texAccent: 'tex-accent.webp',
  texScene: 'tex-scene.webp',
} as const

export function img(name: keyof typeof IMG): string {
  return new URL(`../img/${IMG[name]}`, import.meta.url).href
}
