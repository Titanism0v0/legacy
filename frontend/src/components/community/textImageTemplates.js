export const TEXT_IMAGE_TEMPLATES = [
  {
    id: 'aurora',
    name: '极光卡片',
    preview: 'linear-gradient(145deg, #0f766e, #1d4ed8)',
    accent: ['#0f766e', '#1d4ed8'],
    textColor: '#ffffff',
    badgeBackground: 'rgba(255, 255, 255, 0.16)',
    tagBackground: 'rgba(255, 255, 255, 0.16)',
    align: 'left',
    titleSize: 48,
    contentSize: 24,
    radius: 34,
    shadow: '0 24px 50px rgba(15, 23, 42, 0.18)'
  },
  {
    id: 'paper',
    name: '纸感留白',
    preview: 'linear-gradient(180deg, #fffaf1, #f5ead7)',
    accent: ['#fffaf1', '#f5ead7'],
    textColor: '#382f2d',
    badgeBackground: 'rgba(56, 47, 45, 0.1)',
    tagBackground: 'rgba(56, 47, 45, 0.08)',
    align: 'left',
    titleSize: 46,
    contentSize: 23,
    radius: 30,
    shadow: '0 20px 40px rgba(112, 84, 62, 0.14)'
  },
  {
    id: 'pop',
    name: '撞色海报',
    preview: 'linear-gradient(145deg, #f97316, #ec4899)',
    accent: ['#f97316', '#ec4899'],
    textColor: '#ffffff',
    badgeBackground: 'rgba(17, 24, 39, 0.18)',
    tagBackground: 'rgba(17, 24, 39, 0.16)',
    align: 'center',
    titleSize: 50,
    contentSize: 24,
    radius: 36,
    shadow: '0 24px 54px rgba(190, 24, 93, 0.22)'
  },
  {
    id: 'forest',
    name: '森系杂志',
    preview: 'linear-gradient(145deg, #14532d, #65a30d)',
    accent: ['#14532d', '#65a30d'],
    textColor: '#f7fee7',
    badgeBackground: 'rgba(247, 254, 231, 0.16)',
    tagBackground: 'rgba(247, 254, 231, 0.14)',
    align: 'left',
    titleSize: 48,
    contentSize: 24,
    radius: 34,
    shadow: '0 24px 48px rgba(20, 83, 45, 0.18)'
  }
]

export function getTextImageTemplate(templateId) {
  return TEXT_IMAGE_TEMPLATES.find((item) => item.id === templateId) || TEXT_IMAGE_TEMPLATES[0]
}
