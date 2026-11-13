package com.thebifel.kadr

object Templates {
    private val css="""<style>body{font-family:system-ui,sans-serif;padding:18px 20px}small,.label{font-size:10px;letter-spacing:1px;color:var(--kadr-muted)}h1{font-size:32px;letter-spacing:-1px;margin:12px 0 8px;line-height:1.1}p{font-size:12px;line-height:1.6;margin:0;color:var(--kadr-muted)}b{color:var(--kadr-fg)}.row{display:flex;align-items:center;justify-content:space-between}.badge{background:var(--kadr-accent);color:#344825;border-radius:50%;width:32px;height:32px;display:grid;place-items:center;font-size:20px}.bars{display:flex;gap:4px;margin:18px 0 7px}.bars i{height:18px;flex:1;border-radius:3px}.axis{display:flex;justify-content:space-between;font-size:8px;color:var(--kadr-muted)}</style>"""
    val power="""<!doctype html>
<html lang="uk"><head><meta charset="utf-8">$css
<script src="https://kadr.local/vue.global.prod.js"></script></head>
<body><div id="app">
<div class="label">ЕЛЕКТРОЕНЕРГІЯ · ПРИКЛАД</div>
<div class="row"><h1>{{ title }}</h1><span class="badge">↗</span></div>
<p>Умовний графік <b>16:00–18:00</b></p>
<div class="bars"><i v-for="(on,i) in slots" :key="i" :style="{background:on?'var(--kadr-accent)':'#84916d33'}"></i></div>
<div class="axis"><span>00:00</span><span>12:00</span><span>24:00</span></div>
</div><script>
// Це дизайн-приклад. Замініть дані на fetch вашого API.
const app = Vue.createApp({
  data: () => ({title:'Світло є',slots:[1,1,1,1,0,0,1,1,1,0,0,1]})
});
app.mount('#app');
Vue.nextTick(() => Kadr.ready());
</script></body></html>"""
    val weather="""<!doctype html>
<html lang="uk"><head><meta charset="utf-8">$css</head>
<body><div class="label">КИЇВ · OPEN-METEO</div>
<div class="row"><h1 id="temperature">Завантаження…</h1><span style="font-size:42px;color:var(--kadr-accent)">☀</span></div>
<p id="details"></p><p id="time" style="margin-top:16px;font-size:10px"></p>
<script>
async function update() {
  const url = 'https://api.open-meteo.com/v1/forecast?latitude=50.45&longitude=30.52&current=temperature_2m,apparent_temperature,wind_speed_10m&timezone=Europe%2FKyiv';
  const response = await fetch(url);
  if (!response.ok) throw Error('API: HTTP '+response.status);
  const {current:d} = await response.json();
  document.querySelector('#temperature').textContent = (d.temperature_2m>0?'+':'')+d.temperature_2m+'°';
  document.querySelector('#details').textContent = 'Відчувається '+d.apparent_temperature+'° · вітер '+d.wind_speed_10m+' км/год';
  document.querySelector('#time').textContent = 'Дані прогнозу: '+d.time.replace('T',' ');
  await Kadr.ready();
}
update().catch(e => Kadr.fail(e.message));
</script></body></html>"""
    val blank="""<!doctype html>
<html lang="uk"><head><meta charset="utf-8">
<style>
body { font-family:system-ui; padding:24px; }
h1 { font-size:30px; margin:0 0 12px; letter-spacing:-1px; }
p { color:var(--kadr-muted); font-size:13px; }
</style></head><body>
<h1>Мій кадр ↗</h1>
<p id="message"></p>
<script>
// Тут можуть бути fetch, обчислення, SVG або Canvas.
document.querySelector('#message').textContent =
  'Побудовано о '+new Date().toLocaleTimeString('uk-UA');
Kadr.ready();
</script></body></html>"""
}
