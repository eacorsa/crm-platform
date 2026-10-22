const test = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')
const React = require('react')
const { renderToStaticMarkup } = require('react-dom/server')

function loadCharts() {
  const cache = new Map()
  function load(file) {
    const full = path.resolve(__dirname, '../src', file)
    if (cache.has(full)) return cache.get(full).exports
    const module = { exports: {} }; cache.set(full, module)
    const code = ts.transpileModule(fs.readFileSync(full, 'utf8'), { compilerOptions: {
      module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2020, jsx: ts.JsxEmit.ReactJSX, esModuleInterop: true,
    }}).outputText
    vm.runInNewContext(code, { module, exports: module.exports, Date, Intl,
      require: name => {
        if (!name.startsWith('.')) return require(name)
        const base = path.resolve(path.dirname(full), name)
        const resolved = ['.ts', '.tsx'].map(extension => base + extension).find(candidate => fs.existsSync(candidate))
        return load(path.relative(path.resolve(__dirname, '../src'), resolved))
      },
    }, { filename: full })
    return module.exports
  }
  return load('components/MetricCharts.tsx')
}

test('empty charts render explanatory text and never generate NaN geometry', () => {
  const { TaskChart, StageChart, GrowthChart } = loadCharts()
  const html = [
    renderToStaticMarkup(React.createElement(TaskChart, { statuses: [] })),
    renderToStaticMarkup(React.createElement(StageChart, { stages: [{ stage: 'NUEVO', count: 0, value: 0 }] })),
    renderToStaticMarkup(React.createElement(GrowthChart, { growth: [{ month: '2026-01', contacts: 0, companies: 0 }] })),
  ].join('')
  assert.match(html, /No hay tareas registradas/)
  assert.match(html, /No hay negocios registrados/)
  assert.match(html, /No hay altas/)
  assert.doesNotMatch(html, /NaN|Infinity/)
})

test('charts expose real counts, amounts and an accessible monthly data table', () => {
  const { StageChart, TaskChart, GrowthChart } = loadCharts()
  const stages = [{ stage: 'CERRADO', count: 3, value: 1200.5 }]
  assert.match(renderToStaticMarkup(React.createElement(StageChart, { stages, amount: true })), /1,200.50/)
  const tasks = renderToStaticMarkup(React.createElement(TaskChart, { statuses: [{ done: true, count: 1 }, { done: false, count: 3 }] }))
  assert.match(tasks, /25%/); assert.match(tasks, /1 tareas completadas y 3 pendientes/)
  const growth = renderToStaticMarkup(React.createElement(GrowthChart, { growth: [{ month: '2026-01', contacts: 4, companies: 2 }] }))
  assert.match(growth, /<caption>Altas mensuales/)
  assert.match(growth, /scope="col"/); assert.match(growth, /scope="row"/)
  assert.match(growth, /<td>4<\/td>/); assert.match(growth, /<td>2<\/td>/)
})
