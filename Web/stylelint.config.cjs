// stylelint.config.cjs
module.exports = {
  extends: ["stylelint-config-standard-scss"],
  plugins: ["stylelint-scss"],
  rules: {
    // erlauben führende _ in Partials
    "scss/load-no-partial-leading-underscore": null,

    // break-word nicht mehr als deprecated markieren
    "declaration-property-value-keyword-no-deprecated": null,

    // Leerzeichen um / Operator ignorieren
    "scss/operator-no-unspaced": null,

    // mehr als 1 Deklaration pro Zeile erlaubt
    "declaration-block-single-line-max-declarations": null,

    // @import Positionen nicht prüfen
    "no-invalid-position-at-import-rule": null
  }
};