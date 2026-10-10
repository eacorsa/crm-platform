const test = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')

// Exercise production TypeScript in memory with HTTP/browser boundaries mocked.
// No JS files are emitted into src and no additional test dependency is required.
function harness() {
  const cache = new Map(), storage = new Map(), calls = [], events = []
  const localStorage = {getItem: k => storage.get(k) ?? null, setItem: (k,v) => storage.set(k,v), removeItem: k => storage.delete(k)}
  const client = {interceptors: {request: {use: fn => client.request = fn}, response: {use: (_,fn) => client.reject = fn}}}
  for (const method of ['get','post','put','patch','delete']) client[method] = async (url,data) => {
    calls.push({method,url,data}); return {data}
  }
  const axios = {create: config => {client.config = config; return client}, isAxiosError: err => err?.isAxiosError === true}
  function load(file) {
    const full = path.resolve(__dirname,'../src',file)
    if (cache.has(full)) return cache.get(full).exports
    const module = {exports:{}}; cache.set(full,module)
    const source = fs.readFileSync(full,'utf8').replace('import.meta.env', '{}')
    const code = ts.transpileModule(source,{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2020,esModuleInterop:true}}).outputText
    vm.runInNewContext(code,{module,exports:module.exports,require: name => name === 'axios' ? axios : load(path.relative(path.resolve(__dirname,'../src'),path.resolve(path.dirname(full),name))+'.ts'),
      localStorage,window:{dispatchEvent: event => events.push(event.type)},Event,atob,Date,console,URLSearchParams,TextEncoder},{filename:full})
    return module.exports
  }
  return {load,client,storage,calls,events}
}
const token = exp => 'e30.' + Buffer.from(JSON.stringify({exp})).toString('base64url') + '.signature'

test('JWT expiry rejects malformed, missing and expired tokens', () => {
  const h=harness(), session=h.load('auth/session.ts')
  assert.equal(session.isTokenCurrent(token(200),100000),true)
  assert.equal(session.isTokenCurrent(token(100),100000),false)
  for(const value of [null,'bad','e30.e30.sig',token('200')]) assert.equal(session.isTokenCurrent(value,0),false)
})

test('login does not attach an old bearer token and uses the Vite proxy', () => {
  const h=harness(); h.load('api/client.ts'); h.storage.set('crm_token','old')
  assert.equal(h.client.config.baseURL,'/api')
  const login=h.client.request({url:'/auth/login',headers:{}})
  assert.equal(login.headers.Authorization,undefined)
  assert.equal(h.client.request({url:'/tasks',headers:{}}).headers.Authorization,'Bearer old')
})

test('wrong login credentials preserve the session and readable error without redirect', async () => {
  const h=harness(); h.load('api/client.ts'); h.storage.set('crm_token','current')
  await assert.rejects(h.client.reject({isAxiosError:true,config:{url:'/auth/login'},response:{status:401,data:{error:'Credenciales incorrectas.'},headers:{}}}),
    error => error.message === 'Credenciales incorrectas.' && error.status === 401)
  assert.equal(h.storage.get('crm_token'),'current'); assert.equal(h.events.length,0)
})

test('expired current session clears storage and notifies AuthContext', async () => {
  const h=harness(); h.load('api/client.ts'); h.storage.set('crm_token','current'); h.storage.set('crm_user','user')
  await assert.rejects(h.client.reject({isAxiosError:true,config:{url:'/tasks',headers:{Authorization:'Bearer current'}},response:{status:401,data:{error:'Expired'},headers:{}}}))
  assert.equal(h.storage.size,0); assert.deepEqual(h.events,['crm:session-expired'])
})

test('401 from an older request does not invalidate a newer login', async () => {
  const h=harness(); h.load('api/client.ts'); h.storage.set('crm_token','new')
  await assert.rejects(h.client.reject({isAxiosError:true,config:{url:'/tasks',headers:{Authorization:'Bearer old'}},response:{status:401,data:{error:'Expired'},headers:{}}}))
  assert.equal(h.storage.get('crm_token'),'new'); assert.equal(h.events.length,0)
})

test('HTTP error retains field validation and retry information', async () => {
  const h=harness(); h.load('api/client.ts')
  await assert.rejects(h.client.reject({isAxiosError:true,response:{status:429,data:{error:'Wait',fields:{title:'required'}},headers:{'retry-after':'30'}}}),
    error => error.status === 429 && error.fields.title === 'required' && error.retryAfter === '30')
})

test('quick actions use dedicated PATCH endpoints and only send changed state', async () => {
  const h=harness()
  await h.load('api/tasks.ts').updateTaskDone(3,true)
  await h.load('api/deals.ts').updateDealStage(4,'CERRADO')
  assert.equal(h.calls[0].method,'patch'); assert.equal(h.calls[0].url,'/tasks/3/done')
  assert.equal(JSON.stringify(h.calls[0].data),'{"done":true}')
  assert.equal(h.calls[1].method,'patch'); assert.equal(h.calls[1].url,'/deals/4/stage')
  assert.equal(JSON.stringify(h.calls[1].data),'{"stage":"CERRADO"}')
})

test('empty optional form fields send explicit null, zero amount is preserved', () => {
  const h=harness(), form=h.load('api/forms.ts')
  assert.equal(form.optionalAmount(''),null); assert.equal(form.optionalAmount('0'),0)
  assert.equal(form.optionalAmount('12.34'),12.34); assert.equal(form.optionalDate(''),null)
  assert.equal(form.optionalText('  '),null); assert.equal(form.optionalText('  Ana  '),'Ana')
})


test('recovery endpoints omit stale bearer and use POST payloads', async () => {
  const h=harness(), auth=h.load('api/auth.ts'); h.storage.set('crm_token','old')
  for(const url of ['/auth/forgot-password','/auth/reset-password'])
    assert.equal(h.client.request({url,headers:{}}).headers.Authorization,undefined)
  await auth.forgotPassword(' person@example.com ')
  await auth.resetPassword('a'.repeat(43),'new-password')
  assert.equal(h.calls[0].url,'/auth/forgot-password')
  assert.equal(h.calls[0].data.email,'person@example.com')
  assert.equal(h.calls[1].url,'/auth/reset-password')
  assert.equal(h.calls[1].data.token,'a'.repeat(43))
  assert.equal(h.calls[1].data.password,'new-password')
})

test('reset validates confirmation, UTF-8 length and fragment token', () => {
  const h=harness(), form=h.load('auth/passwordReset.ts')
  assert.equal(form.readResetToken('#token='+'a'.repeat(43)),'a'.repeat(43))
  for(const value of ['', '#token=short', '#token='+':'.repeat(43)]) assert.equal(form.readResetToken(value),'')
  assert.equal(form.passwordResetError('new-password','new-password'),null)
  assert.ok(form.passwordResetError('short','short'))
  assert.ok(form.passwordResetError('new-password','different'))
  assert.ok(form.passwordResetError('é'.repeat(37),'é'.repeat(37)))
  assert.equal(form.passwordResetError('é'.repeat(36),'é'.repeat(36)),null)
})
