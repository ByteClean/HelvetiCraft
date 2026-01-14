// src/app.js
import express from "express";
import cors from "cors";
import helmet from "helmet";
import morgan from "morgan";

import { detectOrigin } from "./middleware/detectOrigin.js";
import { publicReadOrAuth } from "./middleware/publicReadOrAuth.middleware.js";

import authRoutes from "./routes/auth.routes.js";
import phasesRouter from "./routes/phases.routes.js";
import discordLoggingRoutes from "./routes/discordLogging.routes.js";
import initiativesRoutes from "./routes/initiatives.routes.js";
import newsRoutes from "./routes/news.routes.js";
import financesRoutes from "./routes/finances.routes.js";
import quizRoutes from "./routes/quiz.routes.js";

const app = express();

app.use(helmet());
app.use(cors());
app.use(express.json());
app.use(morgan("dev"));

// Health (public)
app.get("/health", (req, res) => res.json({ ok: true }));

// Detect origin (setzt z.B. req.source/headers-Infos)
app.use(detectOrigin);

// Public-Read oder Auth (NEU)
// - erlaubt public GETs fuer Website
// - alles andere verlangt verifyAuth
app.use(publicReadOrAuth);

// Routes (laufen jetzt hinter publicReadOrAuth)
app.use("/initiatives", initiativesRoutes);
app.use("/quiz", quizRoutes);

app.use("/phases", phasesRouter);
app.use("/discord-logging", discordLoggingRoutes);
app.use("/auth", authRoutes);

app.use("/news", newsRoutes);
app.use("/finance", financesRoutes);

// 404 + Fehlerhandler
app.use((req, res) => res.status(404).json({ error: "Route nicht gefunden" }));

app.use((err, req, res, next) => {
  console.error(err);
  res.status(err.status || 500).json({ error: err.message || "Interner Fehler" });
});

export default app;
