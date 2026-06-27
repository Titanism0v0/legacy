import fs from 'node:fs/promises'
import path from 'node:path'
import process from 'node:process'
import { createRequire } from 'node:module'

const require = createRequire(import.meta.url)
const sharpModulePath = process.env.SHARP_MODULE_PATH
  || 'C:/Users/20301/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/.pnpm/sharp@0.34.5/node_modules/sharp'
const sharp = require(sharpModulePath)
const root = path.resolve(import.meta.dirname, '../..')
const manifest = JSON.parse(await fs.readFile(path.join(import.meta.dirname, 'catalog-manifest.json'), 'utf8'))
const columns = 5
const cardWidth = 260
const cardHeight = 220
const rows = Math.ceil(manifest.length / columns)
const composites = []

const escapeXml = value => String(value)
  .replaceAll('&', '&amp;')
  .replaceAll('<', '&lt;')
  .replaceAll('>', '&gt;')

for (let index = 0; index < manifest.length; index += 1) {
  const item = manifest[index]
  const file = path.join(root, 'frontend/public', item.localPath.replace(/^\//, ''))
  const image = await sharp(file)
    .resize({ width: 240, height: 165, fit: 'contain', background: '#f5f5f5' })
    .png()
    .toBuffer()
  const title = escapeXml(`${item.productId} ${item.title}`.slice(0, 25))
  const label = Buffer.from(`<svg width="250" height="42" xmlns="http://www.w3.org/2000/svg">
    <rect width="250" height="42" fill="white"/>
    <text x="5" y="17" font-size="13" font-family="Arial, Microsoft YaHei" fill="#111">${title}</text>
    <text x="5" y="35" font-size="11" font-family="Arial" fill="#666">${escapeXml(item.selectedTitle ?? '')}</text>
  </svg>`)
  const left = (index % columns) * cardWidth + 10
  const top = Math.floor(index / columns) * cardHeight + 10
  composites.push({ input: image, left, top })
  composites.push({ input: label, left: left + 5, top: top + 168 })
}

const output = path.join(root, 'deliverables/demo-product-contact-sheet.png')
await fs.mkdir(path.dirname(output), { recursive: true })
await sharp({
  create: {
    width: columns * cardWidth,
    height: rows * cardHeight,
    channels: 3,
    background: '#e9edf2'
  }
}).composite(composites).png().toFile(output)

console.log(output)
