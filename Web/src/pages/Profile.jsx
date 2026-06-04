import React, { useEffect, useMemo, useState, useRef } from "react";
import api from "../services/api";
import "./styles/_profile.scss";

const TRADED_ITEM_COUNT = 20;
const LOCAL_STORAGE_KEY = "hc_trade_state";
const TRADE_QUANTITIES = [1, 5, 10];

// Liste mit 20 guten Minecraft-Items (keine Potions, Bücher, Pfeile)
// Alle Spieler sehen die gleichen Items
const PREFERRED_ITEMS = [
  "diamond",
  "iron_ingot",
  "gold_ingot",
  "emerald",
  "copper_ingot",
  "coal",
  "redstone",
  "lapis_lazuli",
  "netherite_ingot",
  "quartz",
  "amethyst_shard",
  "diamond_ore",
  "iron_ore",
  "gold_ore",
  "copper_ore",
  "ancient_debris",
  "obsidian",
  "crying_obsidian",
  "blackstone",
  "deepslate",
];

function hashString(value) {
  let hash = 0;
  for (let i = 0; i < value.length; i += 1) {
    hash = (hash << 5) - hash + value.charCodeAt(i);
    hash |= 0;
  }
  return Math.abs(hash);
}

function seededRng(seed) {
  let state = seed % 2147483647;
  if (state <= 0) state += 2147483646;
  return () => {
    state = (state * 16807) % 2147483647;
    return (state - 1) / 2147483646;
  };
}

// Filtere und wähle aus globaler Liste
function getTradeItems(allItems) {
  // Filtere items die in unserer preferred list sind
  const preferred = allItems.filter((item) =>
    PREFERRED_ITEMS.some((p) =>
      item.item_type.toLowerCase().includes(p.toLowerCase())
    )
  );
  // Nimm die ersten 20
  return preferred.slice(0, TRADED_ITEM_COUNT);
}

function buildPriceHistoryForTime(basePrice, itemName, minutesSinceEpoch, length = 60) {
  const rng = seededRng(hashString(`${itemName}-${minutesSinceEpoch}`));
  const history = [];
  let current = basePrice;
  
  // Generiere History für die letzten 60 Minuten bis zur aktuellen Minute
  for (let i = 0; i < length; i += 1) {
    const drift = (rng() - 0.5) * 0.03;
    const volatility = (rng() - 0.5) * 0.06;
    current = Math.max(Math.round(basePrice * 0.7), Math.round(current * (1 + drift + volatility)));
    history.push(current);
  }
  return history;
}

function formatMoney(cents) {
  return new Intl.NumberFormat("de-CH", {
    style: "currency",
    currency: "CHF",
    minimumFractionDigits: 0,
    maximumFractionDigits: 0,
  }).format(cents / 100);
}

function imageUrlForItem(itemType) {
  const imageName = itemType.toLowerCase();
  return `https://minecraft-economy-price-guide.net/.netlify/images?url=/images/items/${imageName}.png&w=64&fm=webp&q=90`;
}

function PriceLineChart({ history, itemType }) {
  const canvasRef = useRef(null);

  useEffect(() => {
    if (!canvasRef.current || !history || history.length === 0) return;

    const canvas = canvasRef.current;
    const ctx = canvas.getContext("2d");
    const width = canvas.width;
    const height = canvas.height;
    const padding = 32;

    ctx.fillStyle = "#1a1a1a";
    ctx.fillRect(0, 0, width, height);

    const minPrice = Math.min(...history);
    const maxPrice = Math.max(...history);
    const priceRange = maxPrice - minPrice || 1;

    // Draw grid and axes
    ctx.strokeStyle = "#333";
    ctx.lineWidth = 1;
    ctx.fillStyle = "#888";
    ctx.font = "10px monospace";
    ctx.textAlign = "right";
    ctx.textBaseline = "middle";

    // Y-axis labels (price)
    for (let i = 0; i <= 3; i += 1) {
      const price = minPrice + (priceRange * i) / 3;
      const y = height - padding - (i / 3) * (height - padding * 1.5);
      ctx.fillText(Math.round(price), padding - 6, y);
      ctx.beginPath();
      ctx.moveTo(padding, y);
      ctx.lineTo(width - 4, y);
      ctx.stroke();
    }

    // X-axis
    ctx.strokeStyle = "#555";
    ctx.lineWidth = 1;
    ctx.beginPath();
    ctx.moveTo(padding, height - padding);
    ctx.lineTo(width - 4, height - padding);
    ctx.stroke();

    // Draw line chart
    ctx.strokeStyle = "#4a90e2";
    ctx.lineWidth = 2;
    ctx.beginPath();

    for (let i = 0; i < history.length; i += 1) {
      const x = padding + (i / (history.length - 1 || 1)) * (width - padding - 8);
      const y =
        height -
        padding -
        ((history[i] - minPrice) / priceRange) * (height - padding * 1.5);
      if (i === 0) ctx.moveTo(x, y);
      else ctx.lineTo(x, y);
    }
    ctx.stroke();

    // Draw points on line
    ctx.fillStyle = "#7eb3ff";
    for (let i = 0; i < history.length; i += Math.max(1, Math.floor(history.length / 8))) {
      const x = padding + (i / (history.length - 1 || 1)) * (width - padding - 8);
      const y =
        height -
        padding -
        ((history[i] - minPrice) / priceRange) * (height - padding * 1.5);
      ctx.beginPath();
      ctx.arc(x, y, 2, 0, Math.PI * 2);
      ctx.fill();
    }
  }, [history, itemType]);

  return <canvas ref={canvasRef} width={220} height={130} className="price-chart" />;
}

export default function Profile() {
  const [profile, setProfile] = useState(null);
  const [marketPrices, setMarketPrices] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);
  const [tradeState, setTradeState] = useState(() => {
    try {
      return JSON.parse(localStorage.getItem(LOCAL_STORAGE_KEY)) || {};
    } catch {
      return {};
    }
  });
  const [minuteTick, setMinuteTick] = useState(0);

  useEffect(() => {
    async function load() {
      setIsLoading(true);
      setError(null);

      try {
        const [{ data: profileData }, { data: pricesData }] = await Promise.all([
          api.get("/finances/me"),
          api.get("/economy/prices/all"),
        ]);

        setProfile(profileData);
        setMarketPrices(pricesData.prices || []);

        if (tradeState.cashBalance == null) {
          setTradeState((prev) => ({
            ...prev,
            cashBalance: profileData.main,
            portfolio: prev.portfolio || {},
          }));
        }
      } catch (err) {
        console.error(err);
        setError(
          err?.response?.data?.error
            ? `Fehler: ${err.response.data.error}`
            : "Fehler beim Laden deiner Daten"
        );
      } finally {
        setIsLoading(false);
      }
    }

    load();
  }, []);

  // Update prices every minute
  useEffect(() => {
    const interval = setInterval(() => {
      setMinuteTick((t) => t + 1);
    }, 60000);
    return () => clearInterval(interval);
  }, []);

  useEffect(() => {
    localStorage.setItem(LOCAL_STORAGE_KEY, JSON.stringify(tradeState));
  }, [tradeState]);

  const tradeItems = useMemo(() => {
    if (marketPrices.length === 0) return [];
    return getTradeItems(marketPrices);
  }, [marketPrices]);

  const portfolio = tradeState.portfolio || {};
  const cashBalance = tradeState.cashBalance ?? profile?.main ?? 0;

  const portfolioValue = useMemo(() => {
    return tradeItems.reduce((sum, item) => {
      const quantity = portfolio[item.item_type] || 0;
      return sum + quantity * item.recommended_price;
    }, 0);
  }, [tradeItems, portfolio]);

  const netWorth = useMemo(() => {
    return (profile?.savings || 0) + cashBalance + portfolioValue;
  }, [profile, cashBalance, portfolioValue]);

  function handleAmountChange(itemType, amount) {
    setTradeSizes((prev) => ({ ...prev, [itemType]: amount }));
  }

  function handleTrade(item, action) {
    const itemType = item.item_type;
    const amount = tradeSizes[itemType] || 1;
    const cost = item.recommended_price * amount;
    const currentAmount = portfolio[itemType] || 0;

    if (action === "buy") {
      if (cost > cashBalance) {
        setError("Nicht genug Guthaben für diesen Trade.");
        return;
      }
      setTradeState((prev) => ({
        ...prev,
        cashBalance: (prev.cashBalance ?? profile.main) - cost,
        portfolio: {
          ...prev.portfolio,
          [itemType]: currentAmount + amount,
        },
      }));
    } else {
      if (amount > currentAmount) {
        setError("Du besitzt nicht genug Einheiten zum Verkaufen.");
        return;
      }
      setTradeState((prev) => ({
        ...prev,
        cashBalance: (prev.cashBalance ?? profile.main) + cost,
        portfolio: {
          ...prev.portfolio,
          [itemType]: currentAmount - amount,
        },
      }));
    }
  }

  if (isLoading) {
    return (
      <div className="page container profile-page">
        <div className="loading-panel minecraft-panel">
          <h2>Lädt deine Finanz- und Handelsdaten...</h2>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="page container profile-page">
        <div className="error-panel minecraft-panel">
          <h2>Fehler</h2>
          <p>{error}</p>
          <p>Stelle sicher, dass du eingeloggt bist und versuche es erneut.</p>
        </div>
      </div>
    );
  }

  return (
    <div className="page container profile-page">
      <div className="profile-intro minecraft-panel">
        <h2>Mein Spielerbereich</h2>
        <p>
          Auf der linken Seite siehst du deine echten Finanzen. Rechts findest du einen simulierten Markt mit {TRADED_ITEM_COUNT} ausgewählten Minecraft-Blöcken, die sich jede Minute aktualisieren.
        </p>
      </div>

      <div className="profile-grid">
        <section className="finance-panel minecraft-panel">
          <h3>Deine Finanzen</h3>
          <div className="finance-row">
            <span>Spieler</span>
            <strong>{profile.username}</strong>
          </div>
          <div className="finance-row">
            <span>Hauptkonto</span>
            <strong className="money-value">{formatMoney(profile.main)}</strong>
          </div>
          <div className="finance-row">
            <span>Sparkonto</span>
            <strong className="money-value">{formatMoney(profile.savings)}</strong>
          </div>
          <div className="finance-row highlight">
            <span>Trading-Guthaben</span>
            <strong className="money-value">{formatMoney(cashBalance)}</strong>
          </div>
          <div className="finance-row highlight">
            <span>Portfoliowert</span>
            <strong className="money-value">{formatMoney(portfolioValue)}</strong>
          </div>
          <div className="finance-row total">
            <span>Geschätztes Gesamtvermögen</span>
            <strong className="money-value">{formatMoney(netWorth)}</strong>
          </div>

          <div className="portfolio-list">
            <h4>Dein Portfolio</h4>
            {Object.keys(portfolio).length === 0 ? (
              <p>Du besitzt noch keine Handelspositionen.</p>
            ) : (
              <div className="portfolio-items">
                {tradeItems
                  .filter((item) => (portfolio[item.item_type] || 0) > 0)
                  .map((item) => (
                    <div key={item.item_type} className="portfolio-item">
                      <span>{item.item_type.replace(/_/g, " ")}</span>
                      <strong>
                        {portfolio[item.item_type]} × {formatMoney(item.recommended_price)}
                      </strong>
                    </div>
                  ))}
              </div>
            )}
          </div>
        </section>

        <section className="market-panel minecraft-panel">
          <div className="market-header">
            <div>
              <h3>Block-Aktienmarkt</h3>
              <p>
                {TRADED_ITEM_COUNT} Items mit realistischen Preisbewegungen. Kurse aktualisieren sich jede Minute.
              </p>
            </div>
            <div className="market-stats">
              <span>{TRADED_ITEM_COUNT} Items</span>
              <span>{profile.username}</span>
            </div>
          </div>

          <div className="market-grid">
            {tradeItems.map((item) => {
              const minutesSinceEpoch = Math.floor(Date.now() / 60000);
              const history = buildPriceHistoryForTime(
                item.recommended_price,
                item.item_type,
                minutesSinceEpoch,
                60
              );
              const current = history[history.length - 1];
              const previous = history[history.length - 2] || current;
              const diff = current - previous;
              const ratio = previous > 0 ? Math.round((diff / previous) * 100) : 0;
              const quantity = portfolio[item.item_type] || 0;

              return (
                <article key={item.item_type} className="market-card">
                  <div className="card-head">
                    <div className="item-badge">
                      <img
                        src={imageUrlForItem(item.item_type)}
                        alt={item.item_type}
                        onError={(e) => { e.target.style.display = "none" }}
                      />
                    </div>
                    <div>
                      <h4>{item.item_type.replace(/_/g, " ")}</h4>
                      <p>{formatMoney(current)}</p>
                    </div>
                  </div>

                  <div className="price-change">
                    <span className={diff >= 0 ? "positive" : "negative"}>
                      {diff >= 0 ? "+" : ""}{ratio}%
                    </span>
                    <small>{diff >= 0 ? "gestiegen" : "gefallen"}</small>
                  </div>

                  <PriceLineChart history={history} itemType={item.item_type} />

                  <div className="market-actions">
                    <div className="quantity-control">
                      <label>
                        Menge
                        <select
                          value={tradeSizes[item.item_type] || 1}
                          onChange={(e) => handleAmountChange(item.item_type, Number(e.target.value))}
                        >
                          {TRADE_QUANTITIES.map((amount) => (
                            <option key={amount} value={amount}>
                              {amount}
                            </option>
                          ))}
                        </select>
                      </label>
                    </div>

                    <div className="action-buttons">
                      <button
                        className="buy"
                        type="button"
                        onClick={() => handleTrade(item, "buy")}
                        disabled={cashBalance < item.recommended_price}
                      >
                        Kaufen
                      </button>
                      <button
                        className="sell"
                        type="button"
                        onClick={() => handleTrade(item, "sell")}
                        disabled={quantity === 0}
                      >
                        Verkaufen
                      </button>
                    </div>
                  </div>

                  <div className="position-summary">
                    <span>Bestand</span>
                    <strong>{quantity} Stück</strong>
                  </div>
                </article>
              );
            })}
          </div>
        </section>
      </div>
    </div>
  );
}

