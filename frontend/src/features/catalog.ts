import { api } from '../api/client';
import type { Artwork, Auction, Page } from '../api/types';
import { cover, dateTime, empty, escapeHtml, heading, money, pageLinks, pageNumber, value } from '../ui/html';

export const catalogNav = (active: string) => `<nav class="tabs" aria-label="Explorar">
  <a href="#/auctions" ${active === 'auctions' ? 'aria-current="page"' : ''}>Subastas</a>
  <a href="#/artworks" ${active === 'artworks' ? 'aria-current="page"' : ''}>Obras</a>
</nav>`;

export const artworkCard = (artwork: Artwork) => `<article class="art-card">
  <a class="art-image" href="#/artworks/${escapeHtml(artwork.id)}">${cover(artwork.coverUrl, artwork.title)}</a>
  <div class="art-info"><p class="eyebrow">${escapeHtml(artwork.technique || 'Obra original')}</p>
  <h3><a href="#/artworks/${escapeHtml(artwork.id)}">${escapeHtml(artwork.title)}</a></h3>
  <span class="muted">${escapeHtml(artwork.dimensions)}</span></div>
</article>`;

export function auctionLabel(auction: Auction) {
  if (auction.status === 'CANCELLED') return 'Cancelada';
  if (auction.status === 'CLOSED' || Date.parse(auction.endDate) <= Date.now()) return 'Finalizada';
  return Date.parse(auction.startDate) > Date.now() ? 'Programada' : 'En curso';
}

export const formatLabel = (auction: Auction) => ({
  SIMPLE: 'Puja abierta', IMMEDIATE: 'Compra inmediata disponible', DUTCH: 'Subasta holandesa', SEALED: 'Sobre cerrado',
})[auction.type];

function auctionCard(auction: Auction) {
  return `<article class="art-card">
    <a class="art-image" href="#/auctions/${escapeHtml(auction.id)}">${cover(auction.coverUrl, auction.artworkTitle)}
    <span class="badge">${auctionLabel(auction)}</span></a>
    <div class="art-info"><p class="eyebrow">${formatLabel(auction)}</p>
    <h3><a href="#/auctions/${escapeHtml(auction.id)}">${escapeHtml(auction.artworkTitle)}</a></h3>
    <div class="card-meta"><strong>${money(auction.currentPrice, auction.currency ?? undefined)}</strong><span>${auction.bidCount} pujas</span></div>
    <p class="muted">Cierre · ${dateTime(auction.endDate)}</p></div>
  </article>`;
}

export async function home(root: HTMLElement) {
  root.innerHTML = `<section class="hero">
    <div class="hero-copy"><p class="eyebrow">Galería de subastas</p>
    <h1>Arte que merece<br><em>ser descubierto.</em></h1>
    <p>Descubre obras originales, sigue tus favoritas y puja por la próxima pieza de tu colección.</p>
    <div class="actions"><a class="button" href="#/auctions">Explorar subastas</a><a class="hero-link" href="#/artworks">Ver obras</a></div></div>
    <div class="hero-image" role="img" aria-label="Pintura de un paisaje montañoso en una galería"></div>
  </section>
  <section class="container collection"><div class="section-top"><div><p class="eyebrow">Catálogo</p><h2>Subastas activas</h2></div>
  <a class="text-link" href="#/auctions">Ver todas las subastas</a></div>
  <div id="home-list"><p role="status">Cargando subastas…</p></div></section>`;
  const list = root.querySelector<HTMLElement>('#home-list')!;
  try {
    const data = await api<Page<Auction>>('/auctions?status=ACTIVE&page=0&size=6', { anonymous: true });
    list.innerHTML = data.items.length ? `<div class="art-grid">${data.items.map(auctionCard).join('')}</div>` : empty('Aún no hay subastas activas.');
  } catch {
    list.innerHTML = empty('El catálogo está temporalmente no disponible.') + '<button class="button secondary" type="button">Volver a cargar</button>';
    list.querySelector('button')?.addEventListener('click', () => void home(root));
  }
}

export async function catalog(root: HTMLElement, kind: 'auctions' | 'artworks', params: URLSearchParams) {
  const auctions = kind === 'auctions';
  const path = `/${kind}`;
  const filters = new URLSearchParams({ page: String(pageNumber(params)), size: '12' });
  const fields = auctions ? ['status', 'type'] : ['q', 'technique'];
  for (const name of fields) {
    const entry = params.get(name);
    if (entry) filters.set(name, entry);
  }
  const options = (name: string, label: string, entries: [string, string][]) => `<label>${label}<select name="${name}">${entries.map(([key, text]) =>
    `<option value="${key}" ${(params.get(name) ?? '') === key ? 'selected' : ''}>${text}</option>`).join('')}</select></label>`;
  root.innerHTML = `<div class="container page">${heading('Explorar', auctions ? 'Subastas' : 'Catálogo de obras')}
    ${catalogNav(kind)}<form class="filters">
    ${auctions
      ? options('status', 'Estado', [['', 'Todos'], ['ACTIVE', 'Activas'], ['CLOSED', 'Cerradas'], ['CANCELLED', 'Canceladas']])
        + options('type', 'Modalidad', [['', 'Todas'], ['SIMPLE', 'Pujas'], ['IMMEDIATE', 'Compra inmediata'], ['DUTCH', 'Holandesa'], ['SEALED', 'Sobre cerrado']])
      : `<label>Buscar obra<input name="q" type="search" value="${escapeHtml(params.get('q'))}" placeholder="Título o descripción"></label>
         <label>Técnica<input name="technique" value="${escapeHtml(params.get('technique'))}" placeholder="Óleo, fotografía…"></label>`}
    <button class="button" type="submit">Aplicar filtros</button></form><div id="catalog-results" aria-live="polite">Cargando catálogo…</div></div>`;
  root.querySelector('form')?.addEventListener('submit', event => {
    event.preventDefault();
    const data = new FormData(event.currentTarget as HTMLFormElement);
    const query = new URLSearchParams();
    for (const name of fields) {
      const entry = value(data, name);
      if (entry) query.set(name, entry);
    }
    location.hash = path + (query.size ? '?' + query : '');
  });
  const results = root.querySelector<HTMLElement>('#catalog-results')!;
  const response = await api<Page<Artwork | Auction>>(`${path}?${filters}`, { anonymous: true });
  results.innerHTML = response.items.length
    ? `<div class="art-grid">${response.items.map(item => 'artworkId' in item ? auctionCard(item) : artworkCard(item)).join('')}</div>`
    : empty('No hay resultados para estos filtros.');
  results.innerHTML += pageLinks(path, filters, response.page, response.totalPages);
}
