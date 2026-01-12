// Backend/services/quiz.service.js
import { getQuizCollection, getRankingCollection } from './mongodb.service.js';

export async function getRandomQuizQuestion() {
  const collection = await getQuizCollection();
  const count = await collection.countDocuments();
  if (count === 0) return null;
  const random = Math.floor(Math.random() * count);
  const question = await collection.find().limit(1).skip(random).next();
  return question;
}

export async function updateRanking(player, rank) {
  const collection = await getRankingCollection();
  const result = await collection.updateOne(
    { player },
    { $set: { player, rank, updatedAt: new Date() } },
    { upsert: true }
  );
  return result;
}
