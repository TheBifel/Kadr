# Security and private data

Kadr has no backend account or built-in API credential. Demo pages use public services without keys. The public repository and release APK must never contain personal widget pages, tokens, signing keys or device storage.

Saved HTML stays in Android application storage. Exporting a page exports its complete source, including any credential its author embedded. Do not put credentials in shared templates or public issues. Prefer a server-side proxy for a shared page that needs a secret; credentials in client-side HTML cannot be hidden from the page's user.

The renderer permits HTTPS with normal CORS rules. It disables device file/content access, localStorage and new windows; its native bridge only signals ready/failure. Rendering is bounded and ends after capture. This is not an isolated execution environment for a multi-user service: execute pages you trust on your own device.

Build secrets belong in environment variables or GitHub Actions secrets. `.env.local`, local properties, signing files and operational artifacts are ignored. Before publishing, scan the current tree and Git history, then inspect packaged assets. Report concerns without posting live credentials or private page source.
