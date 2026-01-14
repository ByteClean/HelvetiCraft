// Simuliert das Backend für Initiativen
export const MOCK_INITIATIVES = [
  {
    id: 1,
    title: "Steuersenkung für Spieler",
    desc: "Reduziert Steuern für kleine Betriebe.",
    yes: 62,
    no: 38,
    status: "In Abstimmung", // für Phase 3
  },
  {
    id: 2,
    title: "Neues Gemeindehaus",
    desc: "Bau eines öffentlichen Gemeindehauses im Spawn.",
    yes: 85,
    no: 15,
    status: "Angenommen",
  },
  {
    id: 3,
    title: "Verbote von TNT",
    desc: "Strengere Regeln gegen Griefing.",
    yes: 44,
    no: 56,
    status: "Abgelehnt",
  },
  {
    id: 4,
    title: "Spielerpark im Spawn",
    desc: "Errichtung eines öffentlichen Parks für alle Spieler.",
    yes: 70,
    no: 30,
    status: "In Abstimmung",
  },
  {
    id: 5,
    title: "Community-Event monatlich",
    desc: "Organisiere monatliche Community-Events auf dem Server.",
    yes: 90,
    no: 10,
    status: "Angenommen",
  },
  {
    id: 6,
    title: "Neue Ressourcengebiete",
    desc: "Erweiterung der Spawn-Region mit zusätzlichen Ressourcen.",
    yes: 50,
    no: 50,
    status: "In Abstimmung",
  },
];

// Globale Phasen (werden alle 4 Tage automatisch gewechselt)
export const PHASES = [
  { id: 1, label: "Phase 1: Stimmen sammeln", className: "phase-1" },
  { id: 2, label: "Phase 2: Bearbeitung durch Admins", className: "phase-2" },
  { id: 3, label: "Phase 3: Volksabstimmung", className: "phase-3" },
];

// Beispiel-Funktion, um die aktuelle Phase basierend auf Datum zu simulieren
export function getCurrentPhase() {
  const startDate = new Date("2026-01-01"); // Startdatum der ersten Phase
  const today = new Date();
  const diffDays = Math.floor((today - startDate) / (1000 * 60 * 60 * 24));
  const phaseIndex = Math.floor(diffDays / 4) % PHASES.length; // alle 4 Tage wechselt Phase
  return PHASES[phaseIndex];
}
