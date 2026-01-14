// Backend/services/quiz.service.js
import { getQuizCollection, getRankingCollection } from './mongodb.service.js';


export async function getRandomQuizQuestion() {
  try {
    console.log('[QuizService] Fetching quiz collection...');
    const collection = await getQuizCollection();
    const count = await collection.countDocuments();
    console.log(`[QuizService] Quiz question count: ${count}`);
    if (count === 0) return null;
    const random = Math.floor(Math.random() * count);
    console.log(`[QuizService] Fetching question at index: ${random}`);
    const question = await collection.find().limit(1).skip(random).next();
    console.log('[QuizService] Got question:', question);
    return question;
  } catch (err) {
    console.error('[QuizService] Error in getRandomQuizQuestion:', err);
    throw err;
  }
}

export async function updateRanking(player, rank) {
  try {
    const collection = await getRankingCollection();
    const result = await collection.updateOne(
      { player },
      { $set: { player, rank, updatedAt: new Date() } },
      { upsert: true }
    );
    return result;
  } catch (err) {
    console.error('[QuizService] Error in updateRanking:', err);
    throw err;
  }
}
