const FALLBACK_AMAP_API_KEY = "13725cc6ef2c302a407b3a2d12247ac5";

const normalizeEnv = (value?: string) => value?.trim() || "";

const apiKeyFromEnv = normalizeEnv(import.meta.env.VITE_AMAP_API_KEY);
const securityJsCodeFromEnv = normalizeEnv(
  import.meta.env.VITE_AMAP_SECURITY_JS_CODE
);
const serviceHostFromEnv = normalizeEnv(import.meta.env.VITE_AMAP_SERVICE_HOST);

let hasWarnedForMissingSecurityConfig = false;

export const AMAP_CONFIG = {
  apiKey: apiKeyFromEnv || FALLBACK_AMAP_API_KEY,
  securityJsCode: securityJsCodeFromEnv,
  serviceHost: serviceHostFromEnv,
  version: "2.0" as const,
  plugins: ["AMap.Geocoder", "AMap.PlaceSearch"],
  isUsingFallbackApiKey: !apiKeyFromEnv,
};

export const applyAmapSecurityConfig = () => {
  const nextConfig: NonNullable<Window["_AMapSecurityConfig"]> = {};

  if (AMAP_CONFIG.securityJsCode) {
    nextConfig.securityJsCode = AMAP_CONFIG.securityJsCode;
  }

  if (AMAP_CONFIG.serviceHost) {
    nextConfig.serviceHost = AMAP_CONFIG.serviceHost;
  }

  if (!nextConfig.securityJsCode && !nextConfig.serviceHost) {
    if (!hasWarnedForMissingSecurityConfig) {
      console.warn(
        "[AMap] Missing VITE_AMAP_SECURITY_JS_CODE/VITE_AMAP_SERVICE_HOST. New JSAPI keys may fail authentication."
      );
      hasWarnedForMissingSecurityConfig = true;
    }
    return;
  }

  window._AMapSecurityConfig = {
    ...window._AMapSecurityConfig,
    ...nextConfig,
  };
};
