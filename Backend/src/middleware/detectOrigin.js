export function detectOrigin(req, res, next) {
  const source = req.headers["x-auth-from"];

  if (!source) {
    return res.status(400).json({ error: "Source missing (x-auth-from)" });
  }

  if (source === "minecraft") {
    req.source = "minecraft";
  } else if (source === "discord") {
    req.source = "discord";
  } else if (source === "web") {
    req.source = "web";
  } else {
    return res.status(400).json({ error: "Unknown source" });
  }

  next();
}
