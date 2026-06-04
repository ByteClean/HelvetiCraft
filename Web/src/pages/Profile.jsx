import React, { useEffect, useMemo, useState } from "react";
import api from "../services/api";
import "./styles/_profile.scss";

const TRADED_ITEM_COUNT = 15;
const LOCAL_STORAGE_KEY = "hc_trade_state";
const TRADE_QUANTITIES = [1, 5, 10];

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

function stableSelectItems(items, username, count) {
  const seed = hashString(username || "guest");
  return [...items]
    .map((item) => ({
      item,
      score: seededRng(hashString(`${seed}-${item.item_type}`))(),
    }))
    .sort((a, b) => b.score - a.score)
    .slice(0, count)
    .map(({ item }) => item);
}

function buildPriceHistory(basePrice, seed, length = 10) {
  const rng = seededRng(seed);
  const history = [basePrice];
  let last = basePrice;
  for (let i = 1; i < length; i += 1) {
    const drift = (rng() - 0.5) * 0.04;
    const volatility = (rng() - 0.5) * 0.08;
    const next = Math.max(1, last * (1 + drift + volatility));
    history.push(Math.round(next));
    last = next;
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
  const isArrow = /_arrow$/.test(itemType);
  const imageName = isArrow ? "arrow" : itemType;
  return `https://minecraft-economy-price-guide.net/.netlify/images?url=/images/items/${imageName}.png&w=64&fm=webp&q=90`;
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
  const [tradeSizes, setTradeSizes] = useState({});

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

  useEffect(() => {
    localStorage.setItem(LOCAL_STORAGE_KEY, JSON.stringify(tradeState));
  }, [tradeState]);

  const tradeItems = useMemo(() => {
    if (!profile || marketPrices.length === 0) return [];
    return stableSelectItems(marketPrices, profile.username || "guest", TRADED_ITEM_COUNT);
  }, [marketPrices, profile]);

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
          Auf der linken Seite siehst du deine echten Finanzen. Rechts findest du einen simulierten Markt mit 15 ausgewählten Minecraft-Blöcken, die du handeln kannst.
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
            <span>Aktuelles Hauptkonto</span>
            <strong>{formatMoney(profile.main)}</strong>
          </div>
          <div className="finance-row">
            <span>Sparkonto</span>
            <strong>{formatMoney(profile.savings)}</strong>
          </div>
          <div className="finance-row highlight">
            <span>Trading-Guthaben</span>
            <strong>{formatMoney(cashBalance)}</strong>
          </div>
          <div className="finance-row highlight">
            <span>Portfoliowert</span>
            <strong>{formatMoney(portfolioValue)}</strong>
          </div>
          <div className="finance-row total">
            <span>Geschätztes Gesamtvermögen</span>
            <strong>{formatMoney(netWorth)}</strong>
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
                15 ausgewählte Items aus unserer Wirtschaftsliste. Die Preise bewegen sich realistisch mit einem leichten Trend.
              </p>
            </div>
            <div className="market-stats">
              <span>{TRADED_ITEM_COUNT} Items</span>
              <span>{profile.username}</span>
            </div>
          </div>

          <div className="market-grid">
            {tradeItems.map((item) => {
              const history = buildPriceHistory(
                item.recommended_price,
                hashString(item.item_type + profile.username),
                8
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
                      <img src={imageUrlForItem(item.item_type)} alt={item.item_type} onError={(e) => { e.target.style.display = "none" }} />
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
                    <small>{diff >= 0 ? "gestiegen" : "gefallen"} gegenüber gestern</small>
                  </div>

                  <div className="history-bars">
                    {history.map((value, index) => (
                      <div
                        key={`${item.item_type}-bar-${index}`}
                        className="history-bar"
                        style={{
                          height: `${Math.max(10, Math.min(100, (value / Math.max(...history)) * 100))}%`,
                        }}
                      />
                    ))}
                  </div>

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
