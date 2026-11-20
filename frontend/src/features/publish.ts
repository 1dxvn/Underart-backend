import { api, currentSession } from '../api/client';
import type { Artwork, ArtworkDetail, Auction, Media, Page } from '../api/types';
import { configurePriceInput, parsePrice } from '../ui/prices';
import { bindForm, errorText, escapeHtml, feedback, heading, money, value } from '../ui/html';

const fileTypes = ['image/jpeg', 'image/png', 'image/webp', 'video/mp4', 'video/webm', 'video/quicktime'];

export async function publish(root: HTMLElement, artworkId?: string) {
  const artwork = artworkId ? await api<ArtworkDetail>(`/artworks/${encodeURIComponent(artworkId)}`) : null;
  if (artwork && artwork.artwork.artistId !== currentSession()?.user.id) throw new Error('Solo puedes añadir multimedia a tus propias obras.');
  root.innerHTML = `<div class="container page narrow">${heading('Para artistas', artwork ? 'Multimedia de la obra' : 'Publicar una obra')}
    ${artwork ? `<p class="lede">${escapeHtml(artwork.artwork.title)}</p>
    <form class="panel form-stack" id="media">
      <label>Imágenes y videos<input type="file" name="files" multiple accept="${fileTypes.join(',')}" required></label>
      <p class="muted">Hasta 15 archivos por obra. Imágenes de hasta 10 MB y videos de hasta 50 MB.</p>
      <button class="button" type="submit">Subir archivos</button>${feedback}
      <ul id="uploads" class="plain-list"></ul></form>
    <div class="actions spaced"><a class="button" href="#/publish/auction?artworkId=${escapeHtml(artwork.artwork.id)}">Crear subasta</a>
      <a class="text-link" href="#/artworks/${escapeHtml(artwork.artwork.id)}">Ver ficha pública</a></div>`
    : `<form class="panel form-stack" id="artwork">
      <label>Título<input name="title" maxlength="255" required></label>
      <label>Descripción<textarea name="description" maxlength="2000" rows="4"></textarea></label>
      <label>Técnica<input name="technique" maxlength="255"></label>
      <div class="form-row"><label>Dimensiones<input name="dimensions" maxlength="255" placeholder="30 × 40 cm"></label>
      <label>Peso en kg<input name="weightKg" type="number" min="0.001" step="any"></label></div>
      <div class="panel form-stack"><label>Moneda de la estimación<select name="adviceCurrency">
        <option value="COP">COP</option><option value="USD">USD</option><option value="EUR">EUR</option>
      </select></label>
      <button class="button secondary" type="button" id="artwork-advice">Sugerir descripción y valor con IA</button>
      <p id="advice-status" class="feedback" role="status"></p>
      <button class="button secondary" type="button" id="apply-description" hidden>Usar esta descripción</button></div>
      <button class="button" type="submit">Guardar y añadir multimedia</button>${feedback}</form>`}
    </div>`;
  const advice = root.querySelector<HTMLButtonElement>('#artwork-advice');
  const apply = root.querySelector<HTMLButtonElement>('#apply-description');
  let draft = '';
  advice?.addEventListener('click', async () => {
    const form = root.querySelector<HTMLFormElement>('#artwork')!;
    const status = root.querySelector<HTMLElement>('#advice-status')!;
    const title = form.elements.namedItem('title') as HTMLInputElement;
    if (!title.reportValidity()) return;
    const data = new FormData(form);
    const weight = value(data, 'weightKg');
    advice.disabled = true;
    if (apply) apply.hidden = true;
    status.textContent = 'Generando sugerencia…';
    try {
      const result = await api<{ description: string; suggestedPrice: number; shippingEstimate: number; currency: string }>(
        '/ai/artwork-advice', { method: 'POST', body: { title: value(data, 'title'), description: value(data, 'description'),
          technique: value(data, 'technique'), dimensions: value(data, 'dimensions'),
          weightKg: weight ? Number(weight) : null, currency: value(data, 'adviceCurrency') } });
      draft = result.description;
      status.textContent = `${draft} Valor orientativo ${money(result.suggestedPrice, result.currency)}. Envío estimado por peso ${money(result.shippingEstimate, result.currency)}.`;
      if (apply) apply.hidden = false;
    } catch (error) {
      status.textContent = errorText(error);
    } finally {
      advice.disabled = false;
    }
  });
  apply?.addEventListener('click', () => {
    root.querySelector<HTMLTextAreaElement>('#artwork [name="description"]')!.value = draft;
  });
  bindForm(root, '#artwork', async data => {
    const weight = value(data, 'weightKg');
    const created = await api<Artwork>('/artworks', { method: 'POST', body: {
      title: value(data, 'title'), description: value(data, 'description') || null,
      technique: value(data, 'technique') || null, dimensions: value(data, 'dimensions') || null,
      weightKg: weight ? Number(weight) : null,
    } });
    if (root.isConnected) location.hash = `/publish/media/${created.id}`;
  });
  if (!artwork) return;
  const uploads = root.querySelector<HTMLElement>('#uploads')!;
  const renderFiles = () => { uploads.innerHTML = artwork.media.map((item, index) =>
    `<li>${item.kind === 'IMAGE' ? 'Imagen' : 'Video'} ${index + 1}${item.cover ? ' · Portada' : ''}</li>`).join(''); };
  renderFiles();
  const uploadedFiles = new Set<string>();
  bindForm(root, '#media', async (data, form) => {
    const entries = data.getAll('files').filter((file): file is File => file instanceof File && file.size > 0);
    const key = (file: File) => JSON.stringify([file.name, file.size, file.lastModified]);
    const files = [...new Map(entries.map(file => [key(file), file])).values()].filter(file => !uploadedFiles.has(key(file)));
    if (files.length + artwork.media.length > 15) throw new Error('La obra puede tener un máximo de 15 archivos.');
    for (const file of files) {
      const limit = file.type.startsWith('image/') ? 10 : 50;
      if (!fileTypes.includes(file.type) || file.size > limit * 1024 * 1024) throw new Error(`Tipo o tamaño no permitido para ${file.name}.`);
    }
    const status = form.querySelector<HTMLElement>('.feedback')!;
    for (const file of files) {
      status.textContent = `Subiendo ${file.name}…`;
      const body = new FormData();
      body.set('file', file);
      const media = await api<Media>(`/artworks/${encodeURIComponent(artwork.artwork.id)}/media`, { method: 'POST', body });
      artwork.media.push(media);
      uploadedFiles.add(key(file));
      renderFiles();
    }
    form.reset();
    status.textContent = 'Multimedia actualizada.';
  });
}

export async function publishAuction(root: HTMLElement, selected: string | null) {
  const results = await api<Page<Artwork>>('/users/me/artworks?size=100');
  const artworks = results.items;
  if (selected && !artworks.some(artwork => artwork.id === selected)) {
    const detail = await api<ArtworkDetail>(`/artworks/${encodeURIComponent(selected)}`);
    if (detail.artwork.artistId === currentSession()?.user.id) artworks.unshift(detail.artwork);
  }
  root.innerHTML = `<div class="container page narrow">${heading('Para artistas', 'Crear subasta')}
    ${!artworks.length ? '<div class="panel"><p>Publica una obra antes de crear una subasta.</p><a class="button" href="#/publish">Publicar obra</a></div>' : `
    <form class="panel form-stack" id="auction">
      <label>Obra<select name="artworkId" required>${artworks.map(artwork => `<option value="${escapeHtml(artwork.id)}" ${artwork.id === selected ? 'selected' : ''}>${escapeHtml(artwork.title)}</option>`).join('')}</select></label>
      <label>Modalidad<select name="type"><option value="SIMPLE">Solo pujas</option><option value="IMMEDIATE">Pujas y compra inmediata</option>
      <option value="DUTCH">Holandesa, precio descendente</option><option value="SEALED">Sobre cerrado</option></select></label>
      <label>Moneda<select name="currency"><option value="USD">Dólares estadounidenses (USD)</option>
      <option value="COP">Pesos colombianos (COP)</option><option value="EUR">Euros (EUR)</option></select></label>
      <p id="currency-help" class="muted" hidden>Usa múltiplos de 1.000 COP. Por ejemplo, 25.000 equivale a veinticinco mil pesos.</p>
      <p id="format-help" class="muted"></p>
      <div class="form-row"><label><span id="base-label">Precio base</span><input name="basePrice" type="number" min="0.01" max="999999999999.99" step="0.01" required></label>
      <label id="immediate" hidden>Precio de compra inmediata<input name="immediatePurchasePrice" type="number" min="0.01" max="999999999999.99" step="0.01" disabled></label>
      <label id="floor" hidden>Precio mínimo<input name="floorPrice" type="number" min="0.01" max="999999999999.99" step="0.01" disabled></label></div>
      <div class="form-row"><label>Inicio opcional<input name="startDate" type="datetime-local"></label>
      <label>Cierre<input name="endDate" type="datetime-local" required></label></div>
      <p class="muted">Las fechas se interpretan en la zona horaria de tu dispositivo.</p>
      <button class="button" type="submit">Publicar subasta</button>${feedback}
    </form>`}</div>`;
  const currency = root.querySelector<HTMLSelectElement>('[name="currency"]');
  function updateCurrency() {
    if (!currency) return;
    root.querySelector<HTMLElement>('#currency-help')!.hidden = currency.value !== 'COP';
    root.querySelectorAll<HTMLInputElement>('[name="basePrice"], [name="immediatePurchasePrice"], [name="floorPrice"]')
      .forEach(input => configurePriceInput(input, currency.value));
  }
  currency?.addEventListener('change', updateCurrency);
  updateCurrency();
  const type = root.querySelector<HTMLSelectElement>('[name="type"]');
  function updateFormat() {
    if (!type) return;
    const dutch = type.value === 'DUTCH';
    for (const [selector, visible] of [['#immediate', type.value === 'IMMEDIATE'], ['#floor', dutch]] as const) {
      const field = root.querySelector<HTMLElement>(selector)!;
      const input = field.querySelector('input')!;
      field.hidden = input.disabled = !visible;
      input.required = visible;
    }
    root.querySelector<HTMLElement>('#base-label')!.textContent = dutch ? 'Precio inicial' : 'Precio base';
    root.querySelector<HTMLElement>('#format-help')!.textContent = dutch
      ? 'El precio desciende hasta el mínimo durante la subasta. La venta se confirma cuando se aprueba el pago.'
      : type.value === 'SEALED' ? 'Cada cuenta envía una sola puja privada. Se revelan al cierre.' : '';
  }
  type?.addEventListener('change', updateFormat);
  updateFormat();
  bindForm(root, '#auction', async data => {
    const auctionCurrency = value(data, 'currency');
    const basePrice = parsePrice(value(data, 'basePrice'), auctionCurrency, 'Precio base o inicial');
    const immediate = value(data, 'immediatePurchasePrice');
    const floor = value(data, 'floorPrice');
    const floorPrice = floor ? parsePrice(floor, auctionCurrency, 'Precio mínimo') : null;
    if (floorPrice != null && floorPrice >= basePrice) throw new Error('El precio mínimo debe ser inferior al inicial.');
    const immediatePurchasePrice = immediate ? parsePrice(immediate, auctionCurrency, 'Precio de compra inmediata') : null;
    const start = value(data, 'startDate');
    const end = new Date(value(data, 'endDate'));
    if (immediatePurchasePrice != null && immediatePurchasePrice <= basePrice) throw new Error('El precio de compra inmediata debe superar el precio base.');
    if (end.getTime() <= Math.max(Date.now(), start ? Date.parse(start) : 0)) throw new Error('El cierre debe ser futuro y posterior al inicio.');
    const auction = await api<Auction>('/auctions', { method: 'POST', body: {
      artworkId: value(data, 'artworkId'), type: value(data, 'type'), currency: value(data, 'currency'), basePrice, immediatePurchasePrice, floorPrice,
      startDate: start ? new Date(start).toISOString() : null, endDate: end.toISOString(),
    } });
    if (root.isConnected) location.hash = `/auctions/${auction.id}`;
  });
}
