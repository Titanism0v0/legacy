const fs = require('fs');
const path = require('path');
const file = path.join(__dirname, 'init_categories.sql');
let sql = fs.readFileSync(file, 'utf8');

function hasChinese(s) {
  return /[\u4e00-\u9fff\u3000-\u303f\uff00-\uffef]/.test(s);
}
function toHex(s) {
  return Buffer.from(s, 'utf8').toString('hex');
}

// Replace every string literal '...' that contains Chinese with CONVERT(UNHEX('hex') USING utf8mb4)
sql = sql.replace(/'([^']*(?:''[^']*)*)'/g, (match, content) => {
  const unescaped = content.replace(/''/g, "'");
  if (!hasChinese(unescaped)) return match;
  const hex = toHex(unescaped);
  return "CONVERT(UNHEX('" + hex + "') USING utf8mb4)";
});

fs.writeFileSync(file, sql, 'utf8');
console.log('Done: all Chinese in init_categories.sql replaced with CONVERT(UNHEX(...) USING utf8mb4)');
