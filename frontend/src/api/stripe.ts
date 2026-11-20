interface CardElement {
  mount(element: HTMLElement): void;
  destroy(): void;
}

interface StripeClient {
  elements(): { create(type: 'card', options: { hidePostalCode: boolean }): CardElement };
  createPaymentMethod(input: { type: 'card'; card: CardElement; billing_details: { email: string } }):
    Promise<{ paymentMethod?: { id: string }; error?: { message?: string } }>;
}

let loading: Promise<void> | undefined;

function stripeScript() {
  const browser = window as Window & { Stripe?: (key: string) => StripeClient };
  if (browser.Stripe) return Promise.resolve(browser.Stripe);
  loading ??= new Promise<void>((resolve, reject) => {
    const script = document.createElement('script');
    script.src = 'https://js.stripe.com/v3/';
    script.async = true;
    script.onload = () => resolve();
    script.onerror = () => {
      loading = undefined;
      script.remove();
      reject(new Error('No se pudo cargar el formulario seguro de Stripe.'));
    };
    document.head.append(script);
  });
  return loading.then(() => {
    if (!browser.Stripe) throw new Error('Stripe no está disponible.');
    return browser.Stripe;
  });
}

export async function mountCard(container: HTMLElement, publicKey: string) {
  if (!/^pk_(test|live)_/.test(publicKey)) throw new Error('Falta configurar la clave pública de Stripe.');
  const createStripe = await stripeScript();
  if (!container.isConnected) throw new Error('El formulario ya no está abierto.');
  const stripe = createStripe(publicKey);
  const card = stripe.elements().create('card', { hidePostalCode: false });
  card.mount(container);
  const lifecycle = new MutationObserver(() => {
    if (container.isConnected) return;
    lifecycle.disconnect();
    card.destroy();
  });
  lifecycle.observe(document.body, { childList: true, subtree: true });
  return async (email: string) => {
    const result = await stripe.createPaymentMethod({ type: 'card', card, billing_details: { email } });
    if (!result.paymentMethod) throw new Error(result.error?.message || 'Revisa los datos de la tarjeta.');
    return result.paymentMethod.id;
  };
}
