/* Failure diagnostics contain geometry and call sites, never page HTML or visible text. */
((root) => {
  'use strict';
  const limit = 65536;
  const describe = (value, depth = 0) => {
    if (depth > 8) return "[nested detail limit]";
    if (value instanceof Error) {
      const parts = [value.stack || value.name + ': ' + value.message];
      if (value.code !== undefined) parts.push('code: ' + String(value.code));
      if (value.details !== undefined) parts.push('details: ' + describe(value.details, depth + 1));
      if (value.cause !== undefined && value.cause !== value) parts.push('cause: ' + describe(value.cause, depth + 1));
      return parts.join('\n');
    }
    if (typeof value === 'string') return value;
    try {
      const seen = new WeakSet();
      return JSON.stringify(value, (key, item) => {
        if (/^(password|apiKey|authorization|idToken|refreshToken|accessToken|private_key|secret)$/i.test(key)) return '[redacted]';
        if (typeof item === 'bigint') return item.toString();
        if (item && typeof item === 'object') { if (seen.has(item)) return '[circular]'; seen.add(item); }
        return item;
      }, 2) || String(value);
    } catch (_) { return String(value); }
  };
  const selector = el => {
    const part = node => node.tagName.toLowerCase() + (node.id ? '#' + node.id : '') +
      (typeof node.className === 'string' ? node.className.trim().split(/\s+/).filter(Boolean).map(name => '.' + name).join('') : '') +
      (node.dataset?.category ? '[data-category=' + JSON.stringify(node.dataset.category) + ']' : '');
    const own = part(el), parent = el.parentElement;
    return (parent ? part(parent) + ' > ' : '') + own;
  };
  const geometry = el => {
    const rect = el.getBoundingClientRect(), css = getComputedStyle(el);
    return {element:selector(el), client:[el.clientWidth,el.clientHeight], scroll:[el.scrollWidth,el.scrollHeight],
      bounds:{x:+rect.x.toFixed(2),y:+rect.y.toFixed(2),width:+rect.width.toFixed(2),height:+rect.height.toFixed(2)},
      fontSize:css.fontSize,lineHeight:css.lineHeight,overflow:[css.overflowX,css.overflowY],textOverflow:css.textOverflow};
  };
  root.__kadrDescribeFailure = (message, details) => {
    const sections = [describe(message)];
    if (details !== undefined) sections.push('Details:\n' + describe(details));
    sections.push('Failure call site:\n' + new Error('Kadr.fail').stack);
    try {
      const controls = root.Kadr?.controls || {width:0,height:0};
      sections.push('Viewport:\n' + describe({sdk:[root.Kadr?.width,root.Kadr?.height],inner:[innerWidth,innerHeight],
        documentClient:[document.documentElement.clientWidth,document.documentElement.clientHeight],
        devicePixelRatio,theme:root.Kadr?.theme,locale:root.Kadr?.locale,readyState:document.readyState,controls}));
      const all = [...document.querySelectorAll('body *')].filter(el => el.clientWidth || el.clientHeight);
      const overflow = all.filter(el => el.scrollWidth > el.clientWidth + 1 || el.scrollHeight > el.clientHeight + 1);
      sections.push('Overflow candidates (intentional CSS clipping is included):\n' + describe({count:overflow.length,elements:overflow.slice(0,32).map(geometry)}));
      const overlap = all.filter(el => !el.children.length && (() => {const r=el.getBoundingClientRect();
        return r.width>0 && r.height>0 && r.right>innerWidth-controls.width && r.bottom>innerHeight-controls.height;
      })());
      if (controls.width && controls.height) sections.push('Native-control overlap candidates:\n' + describe({count:overlap.length,elements:overlap.slice(0,16).map(geometry)}));
    } catch (error) { sections.push('Diagnostic collection failed:\n' + describe(error)); }
    const result = sections.join('\n\n');
    return result.length > limit ? result.slice(0,limit-80)+'\n[Diagnostic text truncated at 65,536 characters.]' : result;
  };
})(globalThis);
