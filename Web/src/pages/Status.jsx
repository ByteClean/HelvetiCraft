import React, { useState, useEffect } from "react";
import ServerStatus from "../components/ServerStatus";
import "./styles/_status.scss";

export default function Status() {
  const [isDropdownOpen, setIsDropdownOpen] = useState(false);
  const [prices, setPrices] = useState([]);
  const [filteredPrices, setFilteredPrices] = useState([]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState(null);
  const [searchTerm, setSearchTerm] = useState("");

  const fetchPrices = async () => {
    if (prices.length > 0) {
      setIsDropdownOpen(!isDropdownOpen);
      return;
    }

    setIsLoading(true);
    setError(null);
    try {
      const response = await fetch(
        "https://helveticraft.com/api/economy/prices/all"
      );
      const data = await response.json();
      const allPrices = data.prices || [];
      setPrices(allPrices);
      setFilteredPrices(allPrices);
      setIsDropdownOpen(true);
    } catch (err) {
      setError("Failed to load prices");
      console.error(err);
    } finally {
      setIsLoading(false);
    }
  };

  const handleSearch = (e) => {
    const value = e.target.value;
    setSearchTerm(value);

    if (!value) {
      setFilteredPrices(prices);
      return;
    }

    const filtered = prices.filter((item) => {
      const normalizedItem = item.item_type.replace(/_/g, " ").toLowerCase();
      const normalizedSearch = value.replace(/_/g, " ").toLowerCase();
      return normalizedItem.includes(normalizedSearch);
    });

    setFilteredPrices(filtered);
  };

  return (
    <div className="page container status-page">
      <h2>Status</h2>
      <p className="desc">
        Hier siehst du den aktuellen Status des Minecraft-Servers sowie die
        Live-Karte der Welt.
      </p>

      {/* SERVER STATUS */}
      <section className="status-section">
        <h3>Serverstatus</h3>
        <ServerStatus serverIP="mc.helveticraft.com" />
      </section>

      {/* PL3XMAP */}
      <section className="status-section">
        <h3>Live-Karte (Pl3xMap)</h3>
        <div className="map-panel">
          <iframe
            title="HelvetiCraft Live Map"
            src="https://map.helveticraft.com"
            loading="lazy"
            referrerPolicy="no-referrer"
          />
        </div>

        {/* ECONOMY PRICES DROPDOWN */}
        <div className="economy-dropdown">
          <button
            className="dropdown-toggle"
            onClick={fetchPrices}
            disabled={isLoading}
          >
            {isLoading ? "Loading..." : "Item Preisliste (Empfohlener Preis)"}
          </button>

          {isDropdownOpen && (
            <div className="dropdown-content">
              {error ? (
                <div className="error-message">{error}</div>
              ) : (
                <>
                  {/* Suchleiste */}
                  <div className="search-bar">
                    <input
                      type="text"
                      placeholder="Search items..."
                      value={searchTerm}
                      onChange={handleSearch}
                    />
                  </div>

                  <div className="prices-list">
                    <div className="prices-header">
                      <span className="item-name">Item</span>
                      <span className="price">Einzelpreis</span>
                      <span className="price">Stack (64)</span>
                    </div>

                    <div className="prices-scroll">
                      {filteredPrices.map((item) => (
                        <div key={item.item_type} className="price-item">
                          <span className="item-name">
                            {item.item_type.replace(/_/g, " ")}
                          </span>
                          <span className="price">{item.recommended_price}</span>
                          <span className="price">
                            {item.recommended_price * 64}
                          </span>
                        </div>
                      ))}

                      {filteredPrices.length === 0 && (
                        <div
                          className="price-item"
                          style={{ justifyContent: "center", color: "#ccc" }}
                        >
                          No items found
                        </div>
                      )}
                    </div>
                  </div>
                </>
              )}
            </div>
          )}
        </div>
      </section>
    </div>
  );
}
