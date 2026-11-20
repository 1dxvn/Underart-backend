import { ApiError, apiOrigin } from '../api/client';

export const escapeHtml = (value: unknown) => String(value ?? '').replace(/[&<>"']/g, character => ({
  '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;',
})[character]!);

export function mediaUrl(value: string | null) {
  try {
    const url = new URL(value ?? '', value ? apiOrigin : undefined);
    return ['http:', 'https:'].includes(url.protocol) ? escapeHtml(url.href) : '';
  } catch {
    return '';
  }
}

export const money = (amount: number, currency = import.meta.env.VITE_CATALOG_CURRENCY || 'USD') =>
  new Intl.NumberFormat('es-CO', { style: 'currency', currency, currencyDisplay: 'code',
    ...(currency.toUpperCase() === 'COP' ? { minimumFractionDigits: 0, maximumFractionDigits: 2 } : {}) }).format(amount);

export const dateTime = (value: string) =>
  new Intl.DateTimeFormat('es-CO', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value));

export const empty = (message: string) => `<div class="empty"><p>${escapeHtml(message)}</p></div>`;

export const heading = (label: string, title: string, description = '') => `
  <header class="page-heading"><p class="eyebrow">${label}</p><h1>${title}</h1>
  ${description ? `<p class="lede">${description}</p>` : ''}</header>`;

export const feedback = '<p class="feedback" role="status"></p>';
export const value = (data: FormData, key: string) => String(data.get(key) ?? '').trim();

export function errorText(error: unknown) {
  if (!(error instanceof ApiError)) return error instanceof Error ? error.message : 'Ocurrió un error. Inténtalo de nuevo.';
  const fields = error.payload.fieldErrors?.map(field => `${field.field}: ${field.message}`).join(' · ');
  return [error.message, fields, error.payload.requestId ? `Solicitud ${error.payload.requestId}` : ''].filter(Boolean).join(' · ');
}

export function bindForm(root: HTMLElement, selector: string, action: (data: FormData, form: HTMLFormElement) => Promise<void>) {
  const form = root.querySelector<HTMLFormElement>(selector);
  form?.addEventListener('submit', async event => {
    event.preventDefault();
    if (root.dataset.busy) return;
    const data = new FormData(form);
    const buttons = Array.from(root.querySelectorAll<HTMLButtonElement>('button[type="submit"]:enabled'));
    const status = form.querySelector<HTMLElement>('.feedback');
    root.dataset.busy = 'true';
    buttons.forEach(button => { button.disabled = true; });
    if (status) status.textContent = '';
    try {
      await action(data, form);
    } catch (error) {
      if (status) status.textContent = errorText(error);
    } finally {
      delete root.dataset.busy;
      buttons.forEach(button => { button.disabled = false; });
    }
  });
}

export function cover(url: string | null, title: string) {
  const source = mediaUrl(url);
  return source
    ? `<img loading="lazy" src="${source}" alt="${escapeHtml(title)}">`
    : '<div class="art-placeholder"><span>Sin imagen</span></div>';
}

export function pageLinks(path: string, params: URLSearchParams, page: number, total: number) {
  if (total <= 1) return '';
  const url = (index: number) => {
    const query = new URLSearchParams(params);
    query.set('page', String(index));
    return `#${path}?${escapeHtml(query.toString())}`;
  };
  return `<nav class="pagination" aria-label="Paginación">
    ${page > 0 ? `<a class="button secondary" href="${url(page - 1)}">Anterior</a>` : ''}
    <span>${page + 1} de ${total}</span>
    ${page + 1 < total ? `<a class="button secondary" href="${url(page + 1)}">Siguiente</a>` : ''}
  </nav>`;
}

export function pageNumber(params: URLSearchParams) {
  const page = Number(params.get('page') ?? 0);
  return Number.isSafeInteger(page) && page >= 0 ? Math.min(page, 2147483647) : 0;
}

export function refreshView() {
  window.dispatchEvent(new Event('viewrefresh'));
}
