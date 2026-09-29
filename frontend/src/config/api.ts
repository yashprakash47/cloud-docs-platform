const configuredApiUrl = import.meta.env.VITE_API_URL as string | undefined;

export const apiConfig = {
  baseUrl: configuredApiUrl ?? 'http://localhost:8080',
} as const;
