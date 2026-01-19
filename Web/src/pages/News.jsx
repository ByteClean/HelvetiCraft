// src/pages/News.jsx
import React, { useEffect, useState } from "react";
import "./styles/_news.scss";

export default function News() {
  const [news, setNews] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    async function loadNews() {
      try {
        setLoading(true);
        const res = await fetch("https://helveticraft.com/api/news");

        if (!res.ok) {
          throw new Error("Fehler beim Laden der News");
        }

        const data = await res.json();
        setNews(data.news_posts || []);
      } catch (err) {
        setError(err.message);
      } finally {
        setLoading(false);
      }
    }

    loadNews();
  }, []);

  return (
    <div className="page container news-page">
      <h2>News</h2>
      <p className="desc">
        Aktuelle Ankündigungen, Updates und Informationen rund um HelvetiCraft.
      </p>

      {loading && <p className="info">Lade News…</p>}
      {error && <p className="error">{error}</p>}

      {!loading && !error && news.length === 0 && (
        <p className="info">Noch keine News vorhanden.</p>
      )}

      <div className="news-list">
        {news.map((post) => (
          <article key={post.id} className="news-card">
            <header className="news-head">
              <h3>{post.title}</h3>
              <span className="news-meta">
                von <strong>{post.author}</strong> ·{" "}
                {new Date(post.created_at).toLocaleDateString("de-CH")}
              </span>
            </header>

            {post.image_url && (
              <div className="news-image">
                <img
                  src={post.image_url}
                  alt={post.title}
                  loading="lazy"
                />
              </div>
            )}

            <div className="news-content">
              {post.content.split("\n").map((line, i) => (
                <p key={i}>{line}</p>
              ))}
            </div>
          </article>
        ))}
      </div>
    </div>
  );
}