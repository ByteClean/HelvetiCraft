import express from "express";
import cors from "cors";
import helmet from "helmet";
import morgan from "morgan";
import { detectOrigin } from "./middleware/detectOrigin.js";


import authRoutes from "./routes/auth.routes.js";
import initiativesRoutes from "./routes/initiatives.routes.js";
import newsRoutes from "./routes/news.routes.js";
//import financesRoutes from "./routes/finances.routes.js";
//import statusRoutes from "./routes/status.routes.js";
//import docsRoutes from "./routes/docs.routes.js";

const app = express();
app.use(detectOrigin); // gilt für ALLE Anfragen
app.use(helmet());
app.use(cors());
app.use(express.json());
app.use(morgan("dev"));

app.get("/health", (req, res) => res.json({ ok: true }));

app.use("/auth", authRoutes); // /login 
app.use("/initiatives", initiativesRoutes); // /all /own /accepted /new /edit /del /vote/:id
app.use("/news", newsRoutes); // /  /:id  /create ...
//app.use("/", financesRoutes);    // /  /taxes /pay/... /sell /networth...
//app.use("/", statusRoutes);      // /mc-web /project /helveticraft
//app.use("/", docsRoutes);        // /news /news/current /blogs /blogs/current /guides ...

// 404 + Fehlerhandler
app.use((req, res) => res.status(404).json({ error: "Route nicht gefunden" }));
app.use((err, req, res, next) => {
  console.error(err);
  res.status(err.status || 500).json({ error: err.message || "Interner Fehler" });
});

export default app;
