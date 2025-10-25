import { Router } from "express";
import jwt from "jsonwebtoken";
import pool from "../services/mysql.service.js";
import bcrypt from "bcryptjs";   // AuthMe nutzt Bcrypt-ähnliche Hashes

const r = Router();
const JWT_SECRET = process.env.JWT_SECRET || "devsecret";

// Login mit Benutzername + Passwort
r.post("/login", async (req, res, next) => {
  const { username, password } = req.body;
  try {
    const [rows] = await pool.query("SELECT id, password FROM authme WHERE username = ?", [username]);
    if (rows.length === 0) return res.status(401).json({ error: "user_not_found" });

    const user = rows[0];

    // Hashvergleich – AuthMe nutzt bcrypt-ähnliche Hashes (z. B. $2y$)
    const valid = await bcrypt.compare(password, user.password);
    if (!valid) return res.status(401).json({ error: "invalid_password" });

    const token = jwt.sign({ sub: user.id, username }, JWT_SECRET, { expiresIn: "12h" });
    res.json({ token });
  } catch (err) {
    next(err);
  }
});

export default r;
