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
const rawDir = path.join(import.meta.dirname, 'raw')
const publicRoot = path.join(root, 'frontend/public')

for (const item of manifest) {
  const source = path.join(rawDir, `${String(item.productId).padStart(3, '0')}.source`)
  const output = path.join(publicRoot, item.localPath.replace(/^\//, ''))
  await fs.mkdir(path.dirname(output), { recursive: true })
  await sharp(source)
    .rotate()
    .resize({ width: 1200, height: 1200, fit: 'inside', withoutEnlargement: true })
    .webp({ quality: 78, effort: 5 })
    .toFile(output)
  console.log(`${item.productId}: ${item.localPath}`)
}

console.log(`Built ${manifest.length} WebP assets`)
