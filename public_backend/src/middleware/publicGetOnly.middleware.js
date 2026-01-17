export function publicGetOnly(req, res, next) {
  if (req.method === "GET" || req.method === "HEAD") return next();
  return res.status(405).json({ error: "method_not_allowed_public_api" });
}
