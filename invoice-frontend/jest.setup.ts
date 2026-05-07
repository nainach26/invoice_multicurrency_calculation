import '@testing-library/jest-dom';

// jsdom does not implement crypto.randomUUID
if (!globalThis.crypto?.randomUUID) {
  Object.defineProperty(globalThis, 'crypto', {
    value: {
      ...globalThis.crypto,
      randomUUID: () => `test-${Math.random().toString(36).slice(2)}`,
    },
    configurable: true,
  });
}
