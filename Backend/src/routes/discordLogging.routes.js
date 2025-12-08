import { Router } from "express"; 
import http from "http";

const r = Router();

function postToDiscordBot(payload) {
	return new Promise((resolve, reject) => {
		const data = JSON.stringify(payload);

		const options = {
			hostname: process.env.DISCORD_BOT_IP,
			port: process.env.DISCORD_BOT_PORT,
			path: "/made-admin",
			method: "POST",
			headers: {
				"Content-Type": "application/json",
				"Content-Length": Buffer.byteLength(data),
			},
		};

		const req = http.request(options, (res) => {
			let body = "";
			res.on("data", (chunk) => (body += chunk));
			res.on("end", () => resolve({ statusCode: res.statusCode, body }));
		});

		req.on("error", (err) => reject(err));
		req.write(data);
		req.end();
	});
}

// Helper to build the payload expected by the Discord bot
function buildPayload({ playername, playerId, reason, expires, at, previous_role, new_role, username }) {
	const payload = {};
	payload.username = username || playerId || playername || "Unknown#0000";
	payload.minecraft_name = playername || "Unknown";
	if (previous_role) payload.previous_role = previous_role;
	if (new_role) payload.new_role = new_role;
	// duration: prefer expires, else at, else empty string
	if (expires) payload.duration = expires;
	else if (at) payload.duration = `until ${at}`;
	else payload.duration = "";
	payload.reason = reason || "";
	return payload;
}

// Upgrade to admin (give admin rights)
r.post("/upgrade-admin", async (req, res, next) => {
	try {
		const { playername, playerId, reason, expires, at } = req.body;

		const payload = buildPayload({
			playername,
			playerId,
			reason,
			expires,
			at,
			previous_role: req.body.previous_role || "Player",
			new_role: req.body.new_role || "Administrator",
			username: req.body.username,
		});

		const result = await postToDiscordBot(payload);
		res.status(result.statusCode || 200).json({ ok: true, forwarded: true, botResponse: result.body });
	} catch (err) {
		next(err);
	}
});

// Downgrade from admin (remove admin rights)
r.post("/downgrade-admin", async (req, res, next) => {
	try {
		const { playername, playerId, reason, expires, at } = req.body;

		const payload = buildPayload({
			playername,
			playerId,
			reason,
			expires,
			at,
			previous_role: req.body.previous_role || "Administrator",
			new_role: req.body.new_role || "Player",
			username: req.body.username,
		});

		const result = await postToDiscordBot(payload);
		res.status(result.statusCode || 200).json({ ok: true, forwarded: true, botResponse: result.body });
	} catch (err) {
		next(err);
	}
});

export default r;

