// src/routes/initiatives.routes.js
import { Router } from "express";
const r = Router();

// Alle Initiativen
r.get("/all", (req, res) => {
  res.json([
    { id: "abc123", title: "Bessere Strassen", status: "open" },
    { id: "def456", title: "Mehr Baeume", status: "voting" }
  ]);
});

// Eigene Initiativen
r.get("/own", (req, res) => {
  res.json([
    { id: "ghi789", title: "Meine Initiative", status: "draft" }
  ]);
});

// Akzeptierte Initiativen
r.get("/accepted", (req, res) => {
  res.json([
    { id: "jkl012", title: "Spielplatz erweitern", status: "accepted" }
  ]);
});

// Neue Initiative anlegen
r.post("/new", (req, res) => {
  const { title, body } = req.body;
  res.status(201).json({
    id: "new_abc123",
    title: title || "Dummy Titel",
    body: body || "Dummy Beschreibung"
  });
});

// Initiative bearbeiten
r.put("/edit", (req, res) => {
  const { id, ...patch } = req.body;
  res.json({
    id: id || "edit_abc123",
    updated: patch
  });
});

// Initiative löschen
r.delete("/del", (req, res) => {
  const { id } = req.body;
  res.json({
    id: id || "del_abc123",
    deleted: true
  });
});

// Abstimmung
r.post("/vote/:initiative_id", (req, res) => {
  const { initiative_id } = req.params;
  const { vote } = req.body;
  res.json({
    initiative_id,
    vote: vote || "yes",
    voter: "user_abc123"
  });
});

export default r;
