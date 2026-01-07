import { Router } from "express";
import jwt from "jsonwebtoken";
import pool from "../services/mysql.service.js";
import bcrypt from "bcryptjs"; // AuthMe nutzt bcrypt-kompatible Hashes

const r = Router();
const JWT_SECRET = process.env.JWT_SECRET;

// 🟢 Login mit Benutzername + Passwort
r.post("/login", async (req, res, next) => {
  const { username, password } = req.body;

  try {
    const [rows] = await pool.query(
      "SELECT id, password FROM authme WHERE username = ?",
      [username]
    );
    if (rows.length === 0)
      return res.status(401).json({ error: "user_not_found" });

    const user = rows[0];

    // AuthMe-Hash prüfen ($2y$ ist kompatibel mit bcrypt)
    const valid = await bcrypt.compare(password, user.password);
    if (!valid)
      return res.status(401).json({ error: "invalid_password" });

    const token = jwt.sign(
      { sub: user.id, username },
      JWT_SECRET,
      { expiresIn: "12h" }
    );

    res.json({ token });
  } catch (err) {
    next(err);
  }
});

export default r;
