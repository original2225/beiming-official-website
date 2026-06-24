/**
 * 图片资源索引
 */

const IMG = {
  logo: 'logo.png',
  heroBg: 'hero-bg.jpg',
  bgHero: 'bg-hero.png',
  bgStatus: 'bg-status.png',
  bgAnnounce: 'bg-announce.png',
  bgMembers: 'bg-members.png',
  texStone: 'tex-stone.webp',
  texDark: 'tex-dark.webp',
  texLight: 'tex-light.webp',
  texAccent: 'tex-accent.webp',
  texScene: 'tex-scene.webp',
} as const

export function img(name: keyof typeof IMG): string {
  return new URL(`../img/${IMG[name]}`, import.meta.url).href
}
