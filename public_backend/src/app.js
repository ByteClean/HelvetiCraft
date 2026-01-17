import express from "express";
import helmet from "helmet";
import morgan from "morgan";
import cors from "cors";
import { publicGetOnly } from "./middleware/publicGetOnly.middleware.js";
import publicRoutes from "./routes/public.routes.js";

const app = express();

app.use(helmet());
app.use(cors());
app.use(express.json());
app.use(morgan("dev"));

app.use(publicGetOnly);
app.use(publicRoutes);

app.use((req, res) => res.status(404).json({ error: "Route nicht gefunden" }));
app.use((err, req, res, next) => {
  console.error(err);
  res.status(500).json({ error: "Interner Fehler" });
});

export default app;
