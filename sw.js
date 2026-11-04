const CACHE='finance-static-v2';
const CORE=['./','./index.html','./manifest.webmanifest','./icon.svg','./src/main.js','./src/style.css'];
self.addEventListener('install',event=>{self.skipWaiting();event.waitUntil(caches.open(CACHE).then(c=>c.addAll(CORE)));});
self.addEventListener('activate',event=>event.waitUntil(caches.keys().then(keys=>Promise.all(keys.filter(k=>k!==CACHE).map(k=>caches.delete(k))))));
self.addEventListener('fetch',event=>{if(event.request.method!=='GET')return;const sameOrigin=new URL(event.request.url).origin===location.origin;event.respondWith(fetch(event.request).then(response=>{if(sameOrigin&&response&&response.ok){const copy=response.clone();caches.open(CACHE).then(c=>c.put(event.request,copy));}return response;}).catch(()=>caches.match(event.request).then(c=>c||caches.match('./index.html'))));});
