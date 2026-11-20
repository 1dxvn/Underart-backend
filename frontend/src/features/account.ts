import { api, currentSession, saveSession } from '../api/client';
import type { Artwork, Auction, AuctionDetail, Bid, Notification, Order, Page, User } from '../api/types';
import { bindForm, dateTime, empty, escapeHtml, feedback, heading, money, pageLinks, pageNumber, refreshView, value } from '../ui/html';
import { avatar } from './profile';
import { artworkCard, auctionLabel } from './catalog';
import { paymentLabel, shippingLabel } from './orders';

const sections = {
  purchases: 'Compras', sales: 'Ventas', shipping: 'Envíos',
  favorites: 'Favoritos',
  artworks: 'Mis obras', auctions: 'Mis subastas', bids: 'Mis pujas', profile: 'Perfil',
};
type Section = keyof typeof sections;

export function isAccountSection(section: string): section is Section {
  return Object.hasOwn(sections, section);
}

const orderRows = (orders: Order[]) => orders.length ? `<ul class="rows">${orders.map(order => `
  <li><div><a class="text-link" href="#/orders/${escapeHtml(order.id)}">${escapeHtml(order.snapshot.title)}</a>
  <p>${paymentLabel(order)} · ${shippingLabel(order)}</p>${order.trackingNumber ? `<p>${escapeHtml(order.carrier)} · ${escapeHtml(order.trackingNumber)}</p>` : ''}</div>
  <strong>${money(order.totalPaid, order.snapshot.currency)}</strong></li>`).join('')}</ul>` : empty('Todavía no hay órdenes en esta sección.');

async function shipping(root: HTMLElement, content: HTMLElement, params: URLSearchParams) {
  const sales = params.get('view') === 'sales';
  const orders = await api<Page<Order>>(`/users/me/${sales ? 'sales' : 'orders'}?page=${pageNumber(params)}&size=12`);
  content.innerHTML = `<nav class="tabs" aria-label="Envíos"><a href="#/account/shipping" ${!sales ? 'aria-current="page"' : ''}>Envíos de compras</a>
    <a href="#/account/shipping?view=sales" ${sales ? 'aria-current="page"' : ''}>Envíos de ventas</a></nav>${orderRows(orders.items)}`;
  paginate(root, content, 'shipping', params, orders);
}

function paginate(root: HTMLElement, content: HTMLElement, section: Section, params: URLSearchParams, page: Page<unknown>) {
  if (root.contains(content)) content.insertAdjacentHTML('beforeend', pageLinks(`/account/${section}`, params, page.page, page.totalPages));
}

export async function notificationPage(root: HTMLElement, params: URLSearchParams) {
  root.innerHTML = `<div class="container page">${heading('Tu actividad', 'Notificaciones')}
    <section id="notification-list" aria-live="polite"><p>Cargando…</p></section></div>`;
  const content = root.querySelector<HTMLElement>('#notification-list')!;
  const unread = params.get('unread') === 'true';
  const path = '/users/me/notifications';
  const [list, count] = await Promise.all([
    api<Page<Notification>>(`${path}?unreadOnly=${unread}&page=${pageNumber(params)}&size=12`),
    api<{ count: number }>(`${path}/unread-count`),
  ]);
  const related = (note: Notification) => note.data?.orderId ? `#/orders/${escapeHtml(note.data.orderId)}`
    : note.data?.auctionId ? `#/auctions/${escapeHtml(note.data.auctionId)}` : '';
  content.innerHTML = `<div class="section-top"><a class="text-link" href="#/notifications${unread ? '' : '?unread=true'}">${unread ? 'Ver todas' : `Ver sin leer (${count.count})`}</a>
    ${count.count ? `<form id="read-all"><button class="button secondary" type="submit">Marcar todas como leídas</button>${feedback}</form>` : ''}</div>
    ${list.items.length ? `<ul class="rows">${list.items.map((note, index) => `<li><div>
    <strong>${escapeHtml(note.title)}</strong>${note.read ? '' : ' <span class="pill">Sin leer</span>'}
    <p>${escapeHtml(note.message)}</p><small>${dateTime(note.createdAt)}</small>
    ${related(note) ? `<p><a class="text-link" href="${related(note)}">Ver detalle</a></p>` : ''}</div>
    ${note.read ? '' : `<form id="note-${index}"><button class="button secondary" type="submit">Marcar como leída</button>${feedback}</form>`}</li>`).join('')}</ul>`
    : empty('No hay notificaciones para mostrar.')}`;
  bindForm(root, '#read-all', async () => {
    await api(`${path}/read-all`, { method: 'POST' });
    if (root.isConnected) refreshView();
  });
  list.items.forEach((note, index) => bindForm(root, `#note-${index}`, async () => {
    await api(`${path}/${encodeURIComponent(note.id)}/read`, { method: 'POST' });
    if (root.isConnected) refreshView();
  }));
  content.insertAdjacentHTML('beforeend', pageLinks('/notifications', params, list.page, list.totalPages));
}

async function profile(root: HTMLElement, content: HTMLElement) {
  const user = await api<User>('/users/me');
  content.innerHTML = `<div class="form-stack narrow">
    <form id="avatar" class="panel form-stack"><h2>Foto de perfil</h2>${avatar(user.avatarUrl)}
      <label>Elige una imagen<input name="file" type="file" accept="image/jpeg,image/png,image/webp" required></label>
      <button class="button secondary" type="submit">Subir foto</button>${feedback}
    </form>
    <form id="profile" class="panel form-stack narrow">
    <label>Correo electrónico<input type="email" value="${escapeHtml(user.email)}" readonly></label>
    <label>Nombre de usuario<input name="username" value="${escapeHtml(user.username)}" required maxlength="60"></label>
    <label>Ciudad<input name="city" value="${escapeHtml(user.city)}" maxlength="255" autocomplete="address-level2"></label>
    <label>Dirección de entrega<input name="shippingAddress" value="${escapeHtml(user.shippingAddress)}" maxlength="255" autocomplete="street-address"></label>
    <label>Sobre ti<textarea name="bio" maxlength="500" rows="5" placeholder="Cuéntanos sobre tu trabajo o tus intereses.">${escapeHtml(user.bio)}</textarea></label>
    <button class="button" type="submit">Guardar perfil</button>${feedback}</form>
    <a class="text-link" href="#/users/${escapeHtml(user.id)}">Ver mi perfil público</a></div>`;
  bindForm(root, '#avatar', async (data, form) => {
    const file = data.get('file');
    if (!(file instanceof File) || !file.size || file.size > 5 * 1024 * 1024) throw new Error('Elige una imagen de hasta 5 MB.');
    const body = new FormData();
    body.set('file', file);
    const updated = await api<User>('/users/me/avatar', { method: 'PUT', body });
    const active = currentSession();
    if (active?.user.id === updated.id) saveSession({ ...active, user: updated });
    form.querySelector('.avatar')?.remove();
    form.querySelector('h2')?.insertAdjacentHTML('afterend', avatar(updated.avatarUrl));
    form.querySelector<HTMLElement>('.feedback')!.textContent = 'Foto actualizada.';
  });
  bindForm(root, '#profile', async (data, form) => {
    const updated = await api<User>('/users/me', { method: 'PUT', body: {
      username: value(data, 'username'), bio: value(data, 'bio'), city: value(data, 'city') || null, shippingAddress: value(data, 'shippingAddress') || null,
    } });
    const active = currentSession();
    if (active?.user.id === updated.id) saveSession({ ...active, user: updated });
    form.querySelector<HTMLElement>('.feedback')!.textContent = 'Perfil actualizado.';
  });
}

export async function account(root: HTMLElement, section: Section, params: URLSearchParams) {
  root.innerHTML = `<div class="container page">${heading('Mi cuenta', sections[section])}
    <nav class="tabs" aria-label="Mi cuenta">${Object.entries(sections).map(([key, label]) =>
      `<a href="#/account/${key}" ${key === section ? 'aria-current="page"' : ''}>${label}</a>`).join('')}</nav>
    <section id="account-content" aria-label="${sections[section]}" aria-live="polite"><p>Cargando…</p></section></div>`;
  const content = root.querySelector<HTMLElement>('#account-content')!;
  const query = `page=${pageNumber(params)}&size=12`;
  if (section === 'shipping') return shipping(root, content, params);
  if (section === 'profile') return profile(root, content);

  if (section === 'purchases' || section === 'sales') {
    const list = await api<Page<Order>>(`/users/me/${section === 'purchases' ? 'orders' : 'sales'}?${query}`);
    content.innerHTML = orderRows(list.items);
    paginate(root, content, section, params, list);
    return;
  }
  if (section === 'artworks' || section === 'favorites') {
    const list = await api<Page<Artwork>>(`/users/me/${section}?${query}`);
    const own = section === 'artworks';
    content.innerHTML = (own ? '<p><a class="button" href="#/publish">Publicar obra</a></p>' : '') +
      (list.items.length ? `<div class="art-grid">${list.items.map(item => `<div>${artworkCard(item)}
        ${own ? `<div class="actions spaced"><a class="text-link" href="#/publish/media/${escapeHtml(item.id)}">Multimedia</a>
        <a class="text-link" href="#/publish/auction?artworkId=${escapeHtml(item.id)}">Crear subasta</a></div>` : ''}</div>`).join('')}</div>`
        : empty(own ? 'Aún no has publicado obras.' : 'Aún no has guardado favoritos.'));
    paginate(root, content, section, params, list);
    return;
  }
  if (section === 'auctions') {
    const list = await api<Page<Auction>>(`/users/me/auctions?${query}`);
    content.innerHTML = '<p><a class="button" href="#/publish/auction">Crear subasta</a></p>' +
      (list.items.length ? `<ul class="rows">${list.items.map(item => `<li>
      <a class="text-link" href="#/auctions/${escapeHtml(item.id)}">${escapeHtml(item.artworkTitle)}</a>
      <span>${auctionLabel(item)} · ${item.bidCount} pujas</span></li>`).join('')}</ul>` : empty('Aún no tienes subastas.'));
    paginate(root, content, section, params, list);
    return;
  }
  const list = await api<Page<Bid & { auctionId: string }>>(`/users/me/bids?${query}`);
  const auctionIds = Array.from(new Set(list.items.map(item => item.auctionId)));
  const currencies = new Map(await Promise.all(auctionIds.map(async id => {
    const detail = await api<AuctionDetail>(`/auctions/${encodeURIComponent(id)}`, { anonymous: true }).catch(() => null);
    return [id, detail?.auction.currency ?? undefined] as const;
  })));
  content.innerHTML = list.items.length ? `<ul class="rows">${list.items.map(item => `<li>
    <div><strong>${money(item.amount, currencies.get(item.auctionId))}</strong><p>${dateTime(item.bidTime)}</p></div>
    <a class="text-link" href="#/auctions/${escapeHtml(item.auctionId)}">Ver subasta</a></li>`).join('')}</ul>` : empty('Aún no has realizado pujas.');
  paginate(root, content, section, params, list);
}
