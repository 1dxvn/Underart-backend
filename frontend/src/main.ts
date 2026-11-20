import './styles.css';
import { api, ApiError, currentSession, signOut } from './api/client';
import { account, isAccountSection, notificationPage } from './features/account';
import { auth, loginLink } from './features/auth';
import { catalog, home } from './features/catalog';
import { artworkDetails, auctionDetails } from './features/details';
import { publicProfile } from './features/profile';
import { orderDetails } from './features/orders';
import { publish, publishAuction } from './features/publish';
import { errorText, escapeHtml, heading } from './ui/html';

const header = document.querySelector<HTMLElement>('#header')!;
const main = document.querySelector<HTMLElement>('#main')!;

function renderHeader() {
  const session = currentSession();
  const path = location.hash.slice(1).split('?')[0] || '/';
  const active = (section: string) => path === section || path.startsWith(section + '/') ? 'aria-current="page"' : '';
  header.innerHTML = `<div class="container masthead">
    <a class="wordmark" href="#/" aria-label="Underart, inicio">underart<span>.</span></a>
    <nav class="main-nav" aria-label="Principal">
      <a href="#/auctions" ${active('/auctions')}>Subastas</a>
      <a href="#/artworks" ${active('/artworks')}>Obras</a>
      <a href="#/publish" ${active('/publish')}>Publicar</a>
    </nav>
    <div class="header-actions">${session
      ? `<a href="#/notifications" ${active('/notifications')}>Notificaciones<span id="notification-count"></span></a>
        <a class="text-link profile-link" href="#/account" ${active('/account')}>${escapeHtml(session.user.username)}</a>
        <button id="logout" class="button secondary compact" type="button">Salir</button>`
      : `<a href="#/login" ${active('/login')}>Iniciar sesión</a>
        <a href="#/register" class="button secondary compact" ${active('/register')}>Crear cuenta</a>`}</div>
  </div><p id="header-status" class="container feedback" role="status"></p>`;
  const badge = header.querySelector<HTMLElement>('#notification-count');
  if (badge) {
    void api<{ count: number }>('/users/me/notifications/unread-count').then(({ count }) => {
      if (badge.isConnected && count > 0) badge.textContent = ` (${count})`;
    }).catch(() => undefined);
  }
  header.querySelector('#logout')?.addEventListener('click', async () => {
    try {
      await signOut();
    } catch {
      header.querySelector<HTMLElement>('#header-status')!.textContent = 'Sesión cerrada en este navegador. No se pudo avisar al servidor.';
    }
    if (/^\/(account|orders|publish|notifications)(\/|$)/.test(path)) location.hash = '/';
    else void render(false);
  });
}

function signInRequired(root: HTMLElement) {
  root.innerHTML = `<div class="container page narrow">${heading('Tu cuenta', 'Inicia sesión para continuar')}
    <div class="panel"><p>Entra a tu cuenta para realizar esta acción.</p>
    <div class="actions"><a class="button" href="${loginLink()}">Iniciar sesión</a>
    <a class="text-link" href="#/register?next=${encodeURIComponent(location.hash.slice(1))}">Crear cuenta</a></div></div></div>`;
}

function renderError(root: HTMLElement, title: string, message: string, retry = false) {
  root.innerHTML = `<div class="container page narrow">${heading('Underart', title)}
    <div class="panel"><p role="status">${escapeHtml(message)}</p><div class="actions">
    ${retry ? '<button type="button" class="button">Volver a intentar</button>' : ''}
    <a class="text-link" href="#/">Ir al inicio</a></div></div></div>`;
  if (retry) root.querySelector('button')?.addEventListener('click', () => void render(false));
}

async function render(navigation: boolean) {
  renderHeader();
  const hash = location.hash.slice(1);
  if (hash === 'main') {
    main.focus();
    return;
  }
  const separator = hash.indexOf('?');
  const path = (separator < 0 ? hash : hash.slice(0, separator)) || '/';
  const params = new URLSearchParams(separator < 0 ? '' : hash.slice(separator + 1));
  const [, section, sub, identifier, extra] = path.split('/');
  const root = document.createElement('div');
  root.innerHTML = '<p class="container loading" role="status">Cargando…</p>';
  main.replaceChildren(root);
  if (navigation) {
    window.scrollTo(0, 0);
    main.focus({ preventScroll: true });
  }
  try {
    if (/^\/(account|orders|publish|notifications)(\/|$)/.test(path) && !currentSession()) return signInRequired(root);
    if (path === '/') return await home(root);
    if ((section === 'login' || section === 'register') && !sub) return auth(root, section === 'register', params);
    if ((section === 'artworks' || section === 'auctions') && !identifier) {
      if (!sub) return await catalog(root, section, params);
      if (section === 'artworks') return await artworkDetails(root, sub, params);
      return await auctionDetails(root, sub, params);
    }
    if (path === '/notifications' || path === '/account/notifications') return await notificationPage(root, params);
    if (section === 'users' && sub && !identifier) return await publicProfile(root, sub);
    if (section === 'account' && !identifier && (!sub || isAccountSection(sub))) {
      return await account(root, sub && isAccountSection(sub) ? sub : 'purchases', params);
    }
    if (section === 'orders' && sub && !identifier) return await orderDetails(root, sub);
    if (path === '/publish') return await publish(root);
    if (path === '/publish/auction') return await publishAuction(root, params.get('artworkId'));
    if (section === 'publish' && sub === 'media' && identifier && !extra) return await publish(root, identifier);
    renderError(root, 'Página no encontrada', 'Puedes seguir explorando desde el inicio.');
  } catch (error) {
    if (error instanceof ApiError && [400, 401, 403].includes(error.status) && !currentSession() && /^\/(account|orders|publish|notifications)(\/|$)/.test(path)) {
      return signInRequired(root);
    }
    renderError(root, 'La página no está disponible', errorText(error), true);
  }
}

window.addEventListener('hashchange', () => void render(true));
window.addEventListener('viewrefresh', () => void render(false));
window.addEventListener('sessionchange', renderHeader);
document.querySelector('.skip')?.addEventListener('click', event => {
  event.preventDefault();
  main.focus();
  main.scrollIntoView();
});
void render(false);
