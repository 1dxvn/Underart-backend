import type { Session } from './types';

const defaultBaseUrl = import.meta.env.DEV ? 'http://localhost:8080/api/v1' : '/api/v1';
const baseUrl = (import.meta.env.VITE_API_BASE_URL || defaultBaseUrl).replace(/\/+$/, '');
export const apiOrigin = new URL(baseUrl, location.origin).origin;
const sessionKey = 'underart.session';

function restoreSession(): Session | null {
  try {
    const saved = JSON.parse(sessionStorage.getItem(sessionKey) ?? 'null') as Session | null;
    return saved?.accessToken && saved.refreshToken && saved.user?.id ? saved : null;
  } catch {
    return null;
  }
}

let session = restoreSession();
let refreshRequest: Promise<void> | undefined;

export const currentSession = () => session;

export function saveSession(value: Session | null) {
  session = value;
  try {
    if (value) sessionStorage.setItem(sessionKey, JSON.stringify(value));
    else sessionStorage.removeItem(sessionKey);
  } catch {
    // The active tab can continue with an in-memory session.
  }
  window.dispatchEvent(new Event('sessionchange'));
}

interface ErrorPayload {
  message?: string;
  requestId?: string;
  fieldErrors?: { field: string; message: string }[];
}

export class ApiError extends Error {
  constructor(public status: number, public payload: ErrorPayload) {
    super(payload.message || (status ? 'No se pudo completar la solicitud.' : 'No se pudo conectar con la API de Underart.'));
  }
}

interface RequestOptions {
  method?: string;
  body?: object;
  anonymous?: boolean;
}

async function request<T>(path: string, options: RequestOptions, token?: string): Promise<T> {
  const headers = new Headers({ Accept: 'application/json' });
  if (token) headers.set('Authorization', `Bearer ${token}`);
  const multipart = options.body instanceof FormData;
  if (options.body && !multipart) headers.set('Content-Type', 'application/json');
  let response: Response;
  try {
    response = await fetch(baseUrl + path, {
      method: options.method,
      headers,
      body: multipart ? options.body as FormData : JSON.stringify(options.body),
    });
  } catch {
    throw new ApiError(0, {});
  }
  if (!response.ok) {
    const payload = await response.json().catch(() => ({})) as ErrorPayload;
    throw new ApiError(response.status, payload ?? {});
  }
  return response.status === 204 ? undefined as T : response.json();
}

function refresh(expired: Session) {
  refreshRequest ??= request<Session>('/auth/refresh', { method: 'POST', body: { refreshToken: expired.refreshToken } })
    .then(value => { if (session === expired) saveSession(value); })
    .catch(error => {
      if (session === expired && error instanceof ApiError && [400, 401, 403].includes(error.status)) saveSession(null);
      throw error;
    })
    .finally(() => { refreshRequest = undefined; });
  return refreshRequest;
}

export async function api<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const active = options.anonymous ? null : session;
  try {
    return await request<T>(path, options, active?.accessToken);
  } catch (error) {
    if (!active || !(error instanceof ApiError) || error.status !== 401) throw error;
    if (session === active) await refresh(active);
    if (!session || session.user.id !== active.user.id) throw error;
    return request<T>(path, options, session.accessToken);
  }
}

export async function signOut() {
  await refreshRequest?.catch(() => undefined);
  const active = session;
  saveSession(null);
  if (active) await request<void>('/auth/logout', { method: 'POST', body: { refreshToken: active.refreshToken } });
}
