import { MongoClient } from 'mongodb';
import dotenv from 'dotenv';
dotenv.config();

const uri = process.env.MONGODB_URI || 'mongodb://admin:strongpassword123@helveticraft-mongodb:27017/helveticraft?authSource=admin';
const dbName = process.env.MONGO_INITDB_DATABASE || 'helveticraft';

let client;
let db;

export async function connectMongo() {
  if (!client) {
    client = new MongoClient(uri);
    await client.connect();
    console.log('Connected to MongoDB');
    db = client.db(dbName);
  }
  return db;
}

export async function getQuizCollection() {
  const database = await connectMongo();
  console.log('Accessing quiz_questions collection');
  return database.collection('quiz_questions');
}

export async function getRankingCollection() {
  const database = await connectMongo();
  return database.collection('quiz_rankings');
}

export async function closeMongo() {
  if (client) {
    await client.close();
    client = null;
    db = null;
  }
}
