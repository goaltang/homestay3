# AMap Integration

This project now uses one clear split for AMap credentials:

- Frontend Vue map and `amap-gui` use a Web JSAPI key.
- Backend geocoding uses a Web Service key.
- The security code is only needed for JSAPI-side integrations.

## Credential mapping

| Scope | Variable | Purpose |
|---|---|---|
| Frontend | `VITE_AMAP_API_KEY` | AMap Web JSAPI key for interactive map loading |
| Frontend | `VITE_AMAP_SECURITY_JS_CODE` | JSAPI security code for newer AMap keys |
| Frontend | `VITE_AMAP_SERVICE_HOST` | Optional proxy host for JSAPI secure mode |
| Backend | `AMAP_API_KEY` | AMap Web Service key for geocoding REST calls |
| CLI / Skill | `AMAP_KEY` | AMap Web JSAPI key for `amap-gui` |
| CLI / Skill | `AMAP_SECURITY_KEY` | Security code for `amap-gui` and `amap-cli-skill` |

## Recommended local setup

### 1. Frontend

Create `homestay-front/.env.local`:

```dotenv
VITE_AMAP_API_KEY=your_web_jsapi_key
VITE_AMAP_SECURITY_JS_CODE=your_security_js_code
```

The frontend now injects `window._AMapSecurityConfig` before calling `AMapLoader.load(...)`.

### 2. Backend

Use a local properties file or environment variable:

```properties
app.map.amap.api-key=${AMAP_API_KEY:}
```

`AMAP_API_KEY` should be a Web Service key, not the JSAPI key.

### 3. CLI / Skill

You can export variables directly:

```powershell
$env:AMAP_KEY="your_web_jsapi_key"
$env:AMAP_SECURITY_KEY="your_security_js_code"
```

Or use the helper script, which reads `homestay-front/.env.local` automatically:

```powershell
.\tools\amap-gui.ps1 status
.\tools\amap-gui.ps1 start
```

## What this changes for the project

- `useMapSearch.ts` no longer owns a separate inline AMap config block.
- `mapService.ts` and `useMapSearch.ts` now share one `amapConfig` source.
- The frontend is ready for newer AMap JSAPI keys that require `securityJsCode`.
- `amap-gui` can be launched against the same frontend JSAPI credentials without duplicating setup by hand.

## Current fit with map-search feature

This helps your existing map-search flow in practical ways:

- The interactive map in `homestay-front/src/composables/useMapSearch.ts` and the CLI now use the same JSAPI credential model.
- The backend geocoder in `homestay-backend/src/main/java/com/homestay3/homestaybackend/service/GeocodingService.java` stays isolated on the Web Service key, which matches AMap's product split.
- When a map-search bug appears, you can compare frontend behavior and `amap-gui` behavior against the same JSAPI key, which shortens debugging.

## JSAPI loader singleton

`homestay-front/src/utils/amapLoader.ts` provides a single `ensureAMapLoaded()` function that caches the `AMapLoader.load()` promise. Both `useMapSearch.ts` (map init) and `mapService.ts` (POI autocomplete) share this loader, so the JSAPI SDK is only loaded once regardless of call order.

Loaded plugins: `AMap.Geocoder`, `AMap.Autocomplete`, `AMap.PlaceSearch`.

## POI autocomplete (landmark search)

The landmark search input previously called the REST `inputtips` endpoint (`restapi.amap.com/v3/assistant/inputtips`) directly from the browser, which:

- Exposed `webServiceKey` in network requests.
- Returned lower-quality tips without reliable coordinates.
- Was subject to CORS and QPS limits.

It now uses the native `AMap.Autocomplete` JSAPI plugin via `searchAmapPoiSuggestions()` in `mapService.ts`. The function signature and `AmapPoiSuggestion` interface are unchanged, so callers (`MapSearch.vue`) required no modifications.
