import express from "express";
import cors from "cors";
import helmet from "helmet";
import morgan from "morgan";

import { detectOrigin } from "./middleware/detectOrigin.js";
import { verifyAuth } from "./middleware/auth.middleware.js";

import authRoutes from "./routes/auth.routes.js";
import phasesRouter from "./routes/phases.routes.js";
import discordLoggingRoutes from "./routes/discordLogging.routes.js";
import initiativesRoutes from "./routes/initiatives.routes.js";
import newsRoutes from "./routes/news.routes.js";
import financesRoutes from "./routes/finances.routes.js";
import quizRoutes from "./routes/quiz.routes.js";
import economyRoutes from "./routes/economy.routes.js";

const app = express();

app.use(helmet());
app.use(cors());
app.use(express.json());
app.use(morgan("dev"));

/* -------------------------
   PUBLIC ROUTES (NO AUTH)
--------------------------*/

// Health check
app.get("/health", (req, res) => res.json({ ok: true }));

// Auth MUST be public
app.use("/auth", authRoutes);

// Public read-only routes (optional public access)
app.use("/news", newsRoutes);

/* -------------------------
   AUTH MIDDLEWARE (ONLY BELOW)
--------------------------*/

// attach origin detection globally (safe)
app.use(detectOrigin);

// protect everything below
app.use(verifyAuth);

/* -------------------------
   PROTECTED ROUTES
--------------------------*/

app.use("/initiatives", initiativesRoutes);
app.use("/quiz", quizRoutes);
app.use("/phases", phasesRouter);
app.use("/discord-logging", discordLoggingRoutes);
app.use("/finances", financesRoutes);
app.use("/economy", economyRoutes);

/* -------------------------
   404 + ERROR HANDLER
--------------------------*/

app.use((req, res) =>
  res.status(404).json({ error: "Route nicht gefunden" })
);

app.use((err, req, res, next) => {
  console.error(err);
  res.status(err.status || 500).json({
    error: err.message || "Interner Fehler",
  });
});

export default app;