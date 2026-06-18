import React, { useEffect, useMemo, useState, useRef } from "react";
import api from "../services/api";
import "./styles/_profile.scss";

const TRADED_ITEM_COUNT = 20;
const LOCAL_STORAGE_KEY = "hc_trade_state";
const TRADE_QUANTITIES = [1, 5, 10];

// Präferierte, gemischte 20-Item-Liste (Ores, ganze Blöcke, Rüstungsteile, Edelsteine)
// Keine Potions, Bücher oder Pfeile. Reihenfolge bestimmt Auswahlpriorität.
const PREFERRED_ITEMS = [
  "diamond",
  "diamond_ore",
  "netherite_ingot",
  "ancient_debris",
  "iron_ingot",
  "iron_ore",
  "iron_chestplate",
  "gold_ingot",
  "gold_ore",
  "gold_chestplate",
  "emerald",
  "copper_ingot",
  "copper_ore",
  "copper_block",
  "obsidian",
  "blackstone",
  "deepslate",
  "coal",
  "redstone",
  "lapis_lazuli",
];

// Filtere und wähle aus globaler Liste
function getTradeItems(allItems) {
  if (!Array.isArray(allItems)) return [];

  // Exclude unwanted types (potions, books, arrows)
  const blacklistRe = /potion|book|arrow|tipped_arrow|enchanted_book/i;

  const normalized = allItems.filter((it) => !blacklistRe.test(it.item_type));

  const selected = [];

  // Pick items in the order defined by PREFERRED_ITEMS to ensure variety
  for (const pref of PREFERRED_ITEMS) {
    if (selected.length >= TRADED_ITEM_COUNT) break;
    const found = normalized.find(
      (it) => it.item_type.toLowerCase().includes(pref.toLowerCase()) && !selected.some(s => s.item_type === it.item_type)
    );
    if (found) selected.push(found);
  }

  // If we don't have enough yet, fill with remaining valid items (non-blacklisted), preserving original order
  for (const it of normalized) {
    if (selected.length >= TRADED_ITEM_COUNT) break;
    if (!selected.some((s) => s.item_type === it.item_type)) selected.push(it);
  }

  return selected.slice(0, TRADED_ITEM_COUNT);
}

// Preis-Cache mit lokaler Persistierung
const PRICE_CACHE_KEY = "hc_price_cache";

function getPriceCache() {
  try {
    const cached = localStorage.getItem(PRICE_CACHE_KEY);
    return cached ? JSON.parse(cached) : {};
  } catch {
    return {};
  }
}

function setPriceCache(cache) {
  try {
    localStorage.setItem(PRICE_CACHE_KEY, JSON.stringify(cache));
  } catch {
    // Silently fail if localStorage is full
  }
}

function calculateNextPrice(currentPrice, itemName, volatilityModifier = 1.0) {
  // Echte Zufälligkeit basierend auf Math.random()
  const randomShock = (Math.random() - 0.5) * 2; // -1 to 1
  
  // Item-Kategorien für korrelierte Bewegungen
  const group = (() => {
    const n = itemName.toLowerCase();
    if (/ore|deepslate|ancient|diamond_ore|iron_ore|gold_ore|copper_ore/.test(n)) return 'ore';
    if (/ingot|gem|emerald|diamond|netherite|amethyst_shard|quartz|coal|redstone|lapis/.test(n)) return 'commodity';
    if (/chestplate|helmet|legs|boots|armor/.test(n)) return 'armor';
    if (/block|obsidian|blackstone|deepslate/.test(n)) return 'block';
    return 'other';
  })();

  // Globale Marktmove (alle Items beeinflussend)
  const globalMarketMove = (Math.random() - 0.5) * 0.02; // ±1%
  
  // Gruppen-spezifische Bewegung
  const groupMove = (Math.random() - 0.5) * 0.015; // ±0.75%
  
  // Item-spezifische Bewegung
  const itemMove = randomShock * 0.025; // ±2.5%
  
  // Basis-Volatilität mit Clustering
  const baseVol = 0.015; // 1.5% Basis-Volatilität
  const extraVol = Math.abs(randomShock) * 0.01; // Höhere Volatilität bei großen Shocks
  
  // Kombiniere alles
  const changePct = globalMarketMove + groupMove * 0.7 + itemMove * (baseVol + extraVol);
  
  // Mean Reversion - Preise driften leicht zurück zum Durchschnitt
  const meanReversionStrength = 0.0005;
  const meanReversion = -meanReversionStrength * (currentPrice - currentPrice);
  
  const totalChange = changePct + meanReversion;
  
  // Berechne neuen Preis, minimum 1 Cent
  let newPrice = Math.max(1, Math.round(currentPrice * (1 + totalChange)));
  
  // Stelle sicher, dass der Preis sich mindestens um 1 Cent ändert wenn es sollte
  if (newPrice === currentPrice && totalChange !== 0) {
    newPrice = currentPrice + (totalChange > 0 ? 1 : -1);
  }
  
  return newPrice;
}

function buildPriceHistoryForTime(basePrice, itemName, minutesSinceEpoch, length = 60) {
  const cache = getPriceCache();
  const cacheKey = itemName;
  
  let itemCache = cache[cacheKey];
  
  // Initialisiere Cache wenn nicht vorhanden
  if (!itemCache) {
    itemCache = {
      basePrice,
      lastMinute: minutesSinceEpoch,
      lastPrice: basePrice,
      history: [basePrice]
    };
  }
  
  // Laufe durch alle fehlenden Minuten auf
  let currentPrice = itemCache.lastPrice;
  let currentMinute = itemCache.lastMinute;
  
  // Wenn wir zu weit in der Zeit sind (z.B. neuer Tag), reset
  if (minutesSinceEpoch - currentMinute > 1440) {
    itemCache = {
      basePrice,
      lastMinute: minutesSinceEpoch,
      lastPrice: basePrice,
      history: [basePrice]
    };
    currentPrice = basePrice;
    currentMinute = minutesSinceEpoch;
  }
  
  // Füge neue Minuten hinzu, falls wir über dem Cache sind
  while (currentMinute < minutesSinceEpoch) {
    currentMinute++;
    currentPrice = calculateNextPrice(currentPrice, itemName);
    itemCache.history.push(currentPrice);
    
    // Behalte nur die letzten 1440 Minuten (24 Stunden)
    if (itemCache.history.length > 1440) {
      itemCache.history.shift();
    }
  }
  
  // Update Cache
  itemCache.lastMinute = minutesSinceEpoch;
  itemCache.lastPrice = currentPrice;
  
  cache[cacheKey] = itemCache;
  setPriceCache(cache);
  
  // Gebe die letzten `length` Einträge zurück
  const start = Math.max(0, itemCache.history.length - length);
  return itemCache.history.slice(start);
}

function formatMoney(cents) {
  return new Intl.NumberFormat("de-CH", {
    style: "currency",
    currency: "CHF",
    minimumFractionDigits: 0,
    maximumFractionDigits: 0,
  }).format(cents / 100);
}

const FALLBACK_MARKET_PRICES = [
  { item_type: "diamond", recommended_price: 14000 },
  { item_type: "diamond_ore", recommended_price: 9800 },
  { item_type: "netherite_ingot", recommended_price: 26000 },
  { item_type: "ancient_debris", recommended_price: 19000 },
  { item_type: "iron_ingot", recommended_price: 4200 },
  { item_type: "iron_ore", recommended_price: 1800 },
  { item_type: "iron_chestplate", recommended_price: 12000 },
  { item_type: "gold_ingot", recommended_price: 6200 },
  { item_type: "gold_ore", recommended_price: 2200 },
  { item_type: "gold_chestplate", recommended_price: 14500 },
  { item_type: "emerald", recommended_price: 7700 },
  { item_type: "copper_ingot", recommended_price: 2100 },
  { item_type: "copper_ore", recommended_price: 1100 },
  { item_type: "copper_block", recommended_price: 8200 },
  { item_type: "obsidian", recommended_price: 6400 },
  { item_type: "blackstone", recommended_price: 1600 },
  { item_type: "deepslate", recommended_price: 1800 },
  { item_type: "coal", recommended_price: 900 },
  { item_type: "redstone", recommended_price: 1250 },
  { item_type: "lapis_lazuli", recommended_price: 1900 },
];

function getFallbackProfile() {
  const username = typeof window !== "undefined" ? localStorage.getItem("hc_username") : null;
  return {
    id: 0,
    username: username || "Gast",
    uuid: "00000000-0000-0000-0000-000000000000",
    main: 250000,
    savings: 0,
    networth: 250000,
  };
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
  const [minuteTick, setMinuteTick] = useState(Math.floor(Date.now() / 60000));
  const [tradeSizes, setTradeSizes] = useState(() => ({}));
  const [chartTimeRange, setChartTimeRange] = useState(60); // 60 = 1 hour default

  // Available time ranges: label, minutes
  const timeRanges = [
    { label: "15 Min", minutes: 15 },
    { label: "30 Min", minutes: 30 },
    { label: "1 Std", minutes: 60 },
    { label: "6 Std", minutes: 360 },
    { label: "12 Std", minutes: 720 },
  ];

  useEffect(() => {
    async function load() {
      setIsLoading(true);
      setError(null);

      let profileData = null;
      let pricesData = null;

      try {
        const profileResp = await api.get("/finances/me");
        profileData = profileResp.data;
      } catch (err) {
        console.warn("Profile load failed, using fallback", err?.message || err);
      }

      try {
        const pricesResp = await api.get("/economy/prices/all");
        pricesData = pricesResp.data;
      } catch (err) {
        console.warn("Prices load failed, using fallback", err?.message || err);
      }

      if (!profileData) {
        if (!localStorage.getItem("hc_token")) {
          setError("Fehler beim Laden deiner Daten. Bitte logge dich ein.");
          setIsLoading(false);
          return;
        }
        profileData = getFallbackProfile();
      }

      if (!pricesData) {
        pricesData = { prices: FALLBACK_MARKET_PRICES };
      }

      setProfile(profileData);
      setMarketPrices(pricesData.prices || []);

      if (tradeState.cashBalance == null) {
        setTradeState((prev) => ({
          ...prev,
          cashBalance: profileData.main,
          portfolio: prev.portfolio || {},
        }));
      }

      setIsLoading(false);
    }

    load();
  }, []);

  // Update prices every minute with forced re-render
  useEffect(() => {
    // Check for new minute on load
    setMinuteTick(Math.floor(Date.now() / 60000));
    
    // Synchronize to minute boundary for accurate updates
    const now = Date.now();
    const nextMinute = (Math.floor(now / 60000) + 1) * 60000;
    const delayToNextMinute = nextMinute - now;
    
    // Set timer to trigger at exact minute boundary
    const timeout = setTimeout(() => {
      setMinuteTick((t) => t + 1);
      
      // Then set interval for subsequent minutes
      const interval = setInterval(() => {
        setMinuteTick((t) => t + 1);
      }, 60000);
      
      return () => clearInterval(interval);
    }, delayToNextMinute);
    
    return () => clearTimeout(timeout);
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

  const marketData = useMemo(() => {
    const minutesSinceEpoch = Math.floor(Date.now() / 60000);
    return tradeItems.map((item) => {
      const history = buildPriceHistoryForTime(
        item.recommended_price,
        item.item_type,
        minutesSinceEpoch,
        chartTimeRange
      );
      const current = history[history.length - 1];
      const previous = history[history.length - 2] || current;
      const diff = current - previous;
      const ratio = previous > 0 ? Math.round((diff / previous) * 100) : 0;
      return {
        ...item,
        history,
        current,
        previous,
        diff,
        ratio,
      };
    });
  }, [tradeItems, minuteTick, chartTimeRange]);

  const portfolioValue = useMemo(() => {
    return marketData.reduce((sum, item) => {
      const quantity = portfolio[item.item_type] || 0;
      return sum + quantity * item.current;
    }, 0);
  }, [marketData, portfolio]);

  const netWorth = useMemo(() => {
    return (profile?.savings || 0) + cashBalance + portfolioValue;
  }, [profile, cashBalance, portfolioValue]);

  function handleAmountChange(itemType, amount) {
    setTradeSizes((prev) => ({ ...prev, [itemType]: amount }));
  }

  async function handleTrade(itemType, currentPrice, action) {
    setError(null);
    const amount = tradeSizes[itemType] || 1;
    const cost = currentPrice * amount;
    const currentAmount = portfolio[itemType] || 0;

    if (action === "buy") {
      if (cost > cashBalance) {
        setError("Nicht genug Guthaben für diesen Trade.");
        return;
      }

      const applyBuy = () => {
        setTradeState((prev) => ({
          ...prev,
          cashBalance: (prev.cashBalance ?? profile.main) - cost,
          portfolio: {
            ...prev.portfolio,
            [itemType]: currentAmount + amount,
          },
        }));

        setProfile((p) => ({
          ...p,
          main: (p?.main ?? 0) - cost,
        }));
      };

      const revertBuy = () => {
        setTradeState((prev) => ({
          ...prev,
          cashBalance: (prev.cashBalance ?? profile.main) + cost,
          portfolio: {
            ...prev.portfolio,
            [itemType]: currentAmount,
          },
        }));

        setProfile((p) => ({
          ...p,
          main: (p?.main ?? 0) + cost,
        }));
      };

      applyBuy();

      try {
        const response = await api.post("/finances/me/adjustMain", {
          deltaCents: -cost,
          transactionType: "TRADE_BUY",
        });

        setProfile((p) => ({ ...p, main: response.data.main }));
      } catch (err) {
        console.error("Trade buy failed", err);
        if (err?.response?.data?.error === "insufficient_funds") {
          revertBuy();
          setError("Nicht genug Guthaben für diesen Trade.");
          return;
        }
      }
    } else {
      if (amount > currentAmount) {
        setError("Du besitzt nicht genug Einheiten zum Verkaufen.");
        return;
      }

      const applySell = () => {
        setTradeState((prev) => ({
          ...prev,
          cashBalance: (prev.cashBalance ?? profile.main) + cost,
          portfolio: {
            ...prev.portfolio,
            [itemType]: currentAmount - amount,
          },
        }));

        setProfile((p) => ({
          ...p,
          main: (p?.main ?? 0) + cost,
        }));
      };

      const revertSell = () => {
        setTradeState((prev) => ({
          ...prev,
          cashBalance: (prev.cashBalance ?? profile.main) - cost,
          portfolio: {
            ...prev.portfolio,
            [itemType]: currentAmount,
          },
        }));

        setProfile((p) => ({
          ...p,
          main: (p?.main ?? 0) - cost,
        }));
      };

      applySell();

      try {
        const response = await api.post("/finances/me/adjustMain", {
          deltaCents: cost,
          transactionType: "TRADE_SELL",
        });

        setProfile((p) => ({ ...p, main: response.data.main }));
      } catch (err) {
        console.error("Trade sell failed", err);
        if (err?.response?.data?.error) {
          revertSell();
          setError("Fehler beim Aktualisieren deines Guthabens.");
          return;
        }
      }
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

          <div className="chart-time-selector">
            {timeRanges.map((range) => (
              <button
                key={range.minutes}
                className={`time-btn ${chartTimeRange === range.minutes ? "active" : ""}`}
                onClick={() => setChartTimeRange(range.minutes)}
              >
                {range.label}
              </button>
            ))}
          </div>

          <div className="market-grid">
            {marketData.map((item) => {
              const {
                item_type,
                history,
                current,
                diff,
                ratio,
              } = item;
              const quantity = portfolio[item_type] || 0;

              return (
                <article key={item_type} className="market-card">
                  <div className="card-head">
                    <div className="item-badge">
                      <img
                        src={imageUrlForItem(item_type)}
                        alt={item_type}
                        onError={(e) => { e.target.style.display = "none" }}
                      />
                    </div>
                    <div>
                      <h4>{item_type.replace(/_/g, " ")}</h4>
                      <p>{formatMoney(current)}</p>
                    </div>
                  </div>

                  <div className="price-change">
                    <span className={diff >= 0 ? "positive" : "negative"}>
                      {diff >= 0 ? "+" : ""}{ratio}%
                    </span>
                    <small>{diff >= 0 ? "gestiegen" : "gefallen"}</small>
                  </div>

                  <PriceLineChart history={history} itemType={item_type} />

                  <div className="market-actions">
                    <div className="quantity-control">
                      <label>
                        Menge
                        <select
                          value={tradeSizes[item_type] || 1}
                          onChange={(e) => handleAmountChange(item_type, Number(e.target.value))}
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
                        onClick={() => handleTrade(item_type, current, "buy")}
                        disabled={cashBalance < current}
                      >
                        Kaufen
                      </button>
                      <button
                        className="sell"
                        type="button"
                        onClick={() => handleTrade(item_type, current, "sell")}
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

