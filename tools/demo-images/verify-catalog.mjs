import fs from 'node:fs/promises'
import path from 'node:path'
import process from 'node:process'
import { createRequire } from 'node:module'

const require = createRequire(import.meta.url)
const sharpModulePath = process.env.SHARP_MODULE_PATH
  || 'C:/Users/20301/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/.pnpm/sharp@0.34.5/node_modules/sharp'
const sharp = require(sharpModulePath)
const root = path.resolve(import.meta.dirname, '../..')
const manifestPath = path.join(root, 'tools/demo-images/catalog-manifest.json')
const publicRoot = path.join(root, 'frontend/public')
const expectedCount = Number(process.env.EXPECTED_DEMO_PRODUCT_COUNT || 58)

const manifest = JSON.parse(await fs.readFile(manifestPath, 'utf8'))
const failures = []
const fail = message => failures.push(message)

if (manifest.length !== expectedCount) {
  fail(`expected ${expectedCount} entries, received ${manifest.length}`)
}

const ids = new Set()
const files = new Set()
for (const item of manifest) {
  if (!Number.isInteger(item.productId)) fail(`invalid productId: ${item.productId}`)
  if (ids.has(item.productId)) fail(`duplicate productId: ${item.productId}`)
  ids.add(item.productId)
  if (!item.title?.trim()) fail(`missing title for ${item.productId}`)
  if (!/^https:\/\//.test(item.sourcePage)) fail(`invalid sourcePage for ${item.productId}`)
  if (!/^https:\/\//.test(item.downloadUrl)) fail(`invalid downloadUrl for ${item.productId}`)
  if (!item.author?.trim() || !item.sourceSite?.trim()) fail(`missing attribution for ${item.productId}`)
  if (!/^\/demo\/products\/catalog\/[a-z0-9-]+\.webp$/.test(item.localPath)) {
    fail(`invalid localPath for ${item.productId}: ${item.localPath}`)
    continue
  }
  if (files.has(item.localPath)) fail(`duplicate localPath: ${item.localPath}`)
  files.add(item.localPath)
  const diskPath = path.join(publicRoot, item.localPath.replace(/^\//, ''))
  try {
    const metadata = await sharp(diskPath).metadata()
    if (metadata.format !== 'webp') fail(`not WebP: ${item.localPath}`)
    if ((metadata.width ?? 0) < 640 || (metadata.height ?? 0) < 480) {
      fail(`too small: ${item.localPath}`)
    }
    const ratio = metadata.width / metadata.height
    if (ratio < 0.55 || ratio > 2.2) fail(`extreme aspect ratio: ${item.localPath}`)
  } catch (error) {
    fail(`unreadable asset ${item.localPath}: ${error.message}`)
  }
}

if (failures.length) {
  for (const failure of failures) console.error(`FAIL: ${failure}`)
  process.exit(1)
}

console.log(`PASS: ${manifest.length} unique local WebP assets verified`)
