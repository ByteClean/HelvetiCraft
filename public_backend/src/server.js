import dotenv from "dotenv";
dotenv.config();

import app from "./app.js";

const PORT = Number(process.env.PORT || 3002);

app.listen(PORT, "0.0.0.0", () => {
  console.log(`Public API laeuft auf Port ${PORT}`);
});
