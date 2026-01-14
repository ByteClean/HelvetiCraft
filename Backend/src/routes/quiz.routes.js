// src/routes/quiz.routes.js
import { Router } from "express";

import { getRandomQuizQuestion, updateRanking } from "../services/quiz.service.js";

const r = Router();

// GET /quiz/question - fetch a random quiz question
r.get("/question", async (req, res) => {
	try {
		const question = await getRandomQuizQuestion();
		if (!question) return res.status(404).json({ error: "No quiz questions found" });
		// Remove MongoDB _id for cleaner response
		const { _id, ...rest } = question;
		res.json(rest);
	} catch (err) {
		res.status(500).json({ error: "Failed to fetch question", err});
	}
});

// POST /quiz/ranking - update player ranking
r.post("/ranking", async (req, res) => {
	try {
		const { player, rank } = req.body;
		if (!player || typeof rank !== "number") {
			return res.status(400).json({ error: "Missing player or rank" });
		}
		await updateRanking(player, rank);
		res.json({ status: "ok", player, rank });
	} catch (err) {
		res.status(500).json({ error: "Failed to update ranking" });
	}
});

export default r;
