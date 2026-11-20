import { api, saveSession } from '../api/client';
import type { Session } from '../api/types';
import { bindForm, errorText, escapeHtml, feedback, heading, value } from '../ui/html';

export function loginLink() {
  return `#/login?next=${encodeURIComponent(location.hash.slice(1) || '/')}`;
}

function destination(params: URLSearchParams) {
  const next = params.get('next') ?? '/account';
  return /^\/(?![\/\\])/.test(next) && !/^\/(login|register)([/?]|$)/.test(next) ? next : '/account';
}

function credentials(data: FormData) {
  return { email: value(data, 'email'), password: String(data.get('password') ?? '') };
}

async function login(root: HTMLElement, account: ReturnType<typeof credentials>, next: string) {
  const session = await api<Session>('/auth/login', { method: 'POST', anonymous: true, body: account });
  saveSession(session);
  if (root.isConnected) location.hash = next;
}

export function auth(root: HTMLElement, register: boolean, params: URLSearchParams) {
  const next = destination(params);
  const nextQuery = escapeHtml(encodeURIComponent(next));
  root.innerHTML = `<div class="container page narrow">
    <section id="account-step">${heading('Tu cuenta', register ? 'Crear cuenta' : 'Iniciar sesión')}
    <form class="panel form-stack" id="auth">
    ${register ? '<label>Nombre de usuario<input name="username" required maxlength="60" autocomplete="username"></label>' : ''}
    <label>Correo electrónico<input name="email" type="email" required maxlength="255" autocomplete="email"></label>
    <label>Contraseña<input name="password" type="password" required ${register
      ? 'minlength="8" maxlength="100" autocomplete="new-password" aria-describedby="password-help"'
      : 'autocomplete="current-password"'}></label>
    ${register ? '<small id="password-help">Entre 8 y 100 caracteres, con una letra y un número.</small>' : ''}
    <button class="button" type="submit">${register ? 'Registrarme' : 'Entrar'}</button>${feedback}
    <p>${register ? '¿Ya tienes cuenta?' : '¿Primera vez en Underart?'}
    <a class="text-link" href="#/${register ? 'login' : 'register'}?next=${nextQuery}">${register ? 'Iniciar sesión' : 'Registrarme'}</a></p>
    </form></section>
    ${register ? `<section id="verification-step" hidden>
      ${heading('Tu cuenta', 'Verifica tu correo')}
      <form class="panel form-stack" id="verification">
        <p>Escribe el código de seis dígitos enviado a <strong id="verification-email"></strong>.</p>
        <label>Código de verificación<input name="verificationCode" autocomplete="one-time-code"
          inputmode="numeric" pattern="[0-9]{6}" maxlength="6" required></label>
        <button class="button" type="submit">Siguiente</button>${feedback}
        <div class="actions">
          <button class="button secondary" type="button" id="resend-code">Reenviar código</button>
          <button class="button secondary" type="button" id="change-account">Cambiar correo</button>
        </div>
        <p id="code-status" class="feedback" role="status"></p>
      </form></section>` : ''}
    </div>`;
  if (!register) {
    bindForm(root, '#auth', data => login(root, credentials(data), next));
    return;
  }

  const accountStep = root.querySelector<HTMLElement>('#account-step')!;
  const verificationStep = root.querySelector<HTMLElement>('#verification-step')!;
  const verification = root.querySelector<HTMLFormElement>('#verification')!;
  const emailField = root.querySelector<HTMLInputElement>('#auth [name="email"]')!;
  const codeField = verification.querySelector<HTMLInputElement>('[name="verificationCode"]')!;
  const resend = root.querySelector<HTMLButtonElement>('#resend-code')!;
  const status = root.querySelector<HTMLElement>('#code-status')!;
  let pending: (ReturnType<typeof credentials> & { username: string }) | undefined;
  let registered = false;

  async function sendCode(email: string) {
    await api('/users/register/code', { method: 'POST', anonymous: true, body: { email } });
  }

  bindForm(root, '#auth', async data => {
    const account = { ...credentials(data), username: value(data, 'username') };
    if (!/\p{L}/u.test(account.password) || !/\p{Nd}/u.test(account.password)) {
      throw new Error('La contraseña debe incluir una letra y un número.');
    }
    await sendCode(account.email);
    pending = account;
    registered = false;
    verification.reset();
    status.textContent = '';
    verification.querySelector<HTMLElement>('.feedback')!.textContent = '';
    root.querySelector<HTMLElement>('#verification-email')!.textContent = account.email;
    accountStep.hidden = true;
    verificationStep.hidden = false;
    codeField.focus();
  });

  bindForm(root, '#verification', async data => {
    if (!pending) return;
    if (!registered) {
      await api('/users/register', { method: 'POST', anonymous: true,
        body: { ...pending, verificationCode: value(data, 'verificationCode') } });
      registered = true;
    }
    await login(root, pending, next);
  });

  resend.addEventListener('click', async () => {
    if (!pending || root.dataset.busy) return;
    resend.disabled = true;
    try {
      await sendCode(pending.email);
      status.textContent = 'Revisa tu bandeja de entrada. Puedes solicitar otro código en un minuto.';
      codeField.focus();
    } catch (error) {
      status.textContent = errorText(error);
    } finally {
      resend.disabled = false;
    }
  });

  root.querySelector('#change-account')?.addEventListener('click', () => {
    if (root.dataset.busy) return;
    pending = undefined;
    verificationStep.hidden = true;
    accountStep.hidden = false;
    emailField.focus();
  });
}
