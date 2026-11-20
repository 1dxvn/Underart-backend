import { api } from '../api/client';
import { empty, escapeHtml, heading, mediaUrl } from '../ui/html';

interface PublicProfile {
  username: string;
  city: string | null;
  bio: string | null;
  avatarUrl: string | null;
  averageRating: number | null;
  reviewsCount: number;
}

export function avatar(url: string | null | undefined) {
  const source = url ? mediaUrl(url) : '';
  return source ? `<img class="avatar" src="${source}" alt="Foto de perfil">` : '';
}

export async function publicProfile(root: HTMLElement, id: string) {
  const user = await api<PublicProfile>(`/users/${encodeURIComponent(id)}`, { anonymous: true });
  root.innerHTML = `<div class="container page narrow">${heading('Perfil público', escapeHtml(user.username))}
    <section class="panel form-stack">${avatar(user.avatarUrl)}
    ${user.city ? `<p>${escapeHtml(user.city)}</p>` : ''}
    ${user.bio ? `<p class="profile-bio">${escapeHtml(user.bio)}</p>` : empty('Este perfil aún no tiene una descripción.')}
    ${user.reviewsCount ? `<p>Valoración ${escapeHtml(user.averageRating)} de 5 · ${user.reviewsCount} reseñas</p>` : ''}
    </section><p class="spaced"><a class="text-link" href="#/artworks">Explorar obras</a></p></div>`;
}
