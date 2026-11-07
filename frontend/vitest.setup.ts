import '@testing-library/jest-dom/vitest'

// jsdom aún no implementa los métodos de <dialog>
HTMLDialogElement.prototype.showModal = function showModal() {
  this.open = true
}
HTMLDialogElement.prototype.close = function close() {
  this.open = false
}
