const groupedPesos = /^[0-9]{1,3}(?:\.[0-9]{3})+$/;
const plainPesos = /^[0-9]+$/;
const pesos = new Intl.NumberFormat('es-CO', { maximumFractionDigits: 0, useGrouping: true });

function copNumber(raw: string) {
  return groupedPesos.test(raw) ? Number(raw.replaceAll('.', '')) : plainPesos.test(raw) ? Number(raw) : NaN;
}

export function parsePrice(raw: string, currency: string, label: string) {
  const amount = currency === 'COP' ? copNumber(raw) : Number(raw);
  const maximum = currency === 'COP' ? 999999999000 : 999999999999.99;
  if (!Number.isFinite(amount) || amount <= 0 || amount > maximum) {
    throw new Error(`${label}. Ingresa un importe válido.`);
  }
  if (currency === 'COP' && amount % 1000 !== 0) {
    throw new Error(`${label}. Usa múltiplos de 1.000 COP, por ejemplo 25.000.`);
  }
  return amount;
}

export function formatCopInput(input: HTMLInputElement) {
  const amount = copNumber(input.value.trim());
  if (Number.isSafeInteger(amount)) input.value = pesos.format(amount);
}

export function configurePriceInput(input: HTMLInputElement, currency: string) {
  const cop = currency === 'COP';
  if (!cop && input.type === 'text' && groupedPesos.test(input.value)) {
    input.value = input.value.replaceAll('.', '');
  }
  input.type = cop ? 'text' : 'number';
  input.inputMode = cop ? 'numeric' : 'decimal';
  input.placeholder = cop ? '25.000' : '';
  if (cop) formatCopInput(input);
  input.onblur = cop ? () => formatCopInput(input) : null;
}
