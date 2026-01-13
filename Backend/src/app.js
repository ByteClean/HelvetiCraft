import express from "express";
import cors from "cors";
import helmet from "helmet";
import morgan from "morgan";
import { detectOrigin } from "./middleware/detectOrigin.js";


import authRoutes from "./routes/auth.routes.js";
import phasesRouter from "./routes/phases.routes.js";
import discordLoggingRoutes from "./routes/discordLogging.routes.js";
import initiativesRoutes from "./routes/initiatives.routes.js";
import newsRoutes from "./routes/news.routes.js";
import { verifyAuth } from "./middleware/auth.middleware.js";
import financesRoutes from "./routes/finances.routes.js";
import quizRoutes from "./routes/quiz.routes.js";
//import statusRoutes from "./routes/status.routes.js";
//import docsRoutes from "./routes/docs.routes.js";

const app = express();
app.use(helmet());
app.use(cors());
app.use(express.json());
app.use(morgan("dev"));

app.use("/quiz", quizRoutes);          // /question /ranking
app.get("/health", (req, res) => res.json({ ok: true }));
app.use(detectOrigin); // gilt für ALLE Anfragen
app.use(verifyAuth); // gilt für ALLE NACHFOLGENDEN Routen
app.use("/phases", phasesRouter); // /current /advance
app.use("/discord-logging", discordLoggingRoutes);
app.use("/auth", authRoutes); // /login 
app.use("/initiatives", initiativesRoutes); // /  /:id  /create ... /finalvote/...
app.use("/news", newsRoutes); // /  /:id  /create ...
app.use("/finance", financesRoutes);    // /  /taxes /pay/... /sell /networth...
//app.use("/", statusRoutes);      // /mc-web /project /helveticraft
//app.use("/", docsRoutes);        // /news /news/current /blogs /blogs/current /guides ...

// 404 + Fehlerhandler
app.use((req, res) => res.status(404).json({ error: "Route nicht gefunden" }));
app.use((err, req, res, next) => {
  console.error(err);
  res.status(err.status || 500).json({ error: err.message || "Interner Fehler" });
});

export default app;
