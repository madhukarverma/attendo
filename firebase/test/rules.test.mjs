// Tests for firebase/firestore.rules on the local Firestore emulator (nothing touches the real
// project). Every "app:" test makes the exact write or query SocialService.kt makes; every
// "attack:" test must be refused. Needs Node 18+ and Java 11+ (JAVA_HOME). From this folder:
//   npm install
//   npm test
import { readFileSync } from 'node:fs';
import { after, before, beforeEach, describe, test } from 'node:test';
import { assertFails, assertSucceeds, initializeTestEnvironment } from '@firebase/rules-unit-testing';
import {
  collection, deleteDoc, doc, endAt, getDoc, getDocs, limit, orderBy, query, serverTimestamp, setDoc,
  startAt, where, writeBatch,
} from 'firebase/firestore';

const ALICE = 'alicE1aaaaaaaaaaaaaaaaaaaaaa';
const BOB = 'bobbo2bbbbbbbbbbbbbbbbbbbbbb';
const CAROL = 'carol3cccccccccccccccccccccc';
const code = (uid) => uid.replace(/[^A-Za-z0-9]/g, '').slice(0, 6).toUpperCase();

let env;
const google = (uid) => env.authenticatedContext(uid, { firebase: { sign_in_provider: 'google.com' } }).firestore();
const nobody = () => env.unauthenticatedContext().firestore();

const profile = (uid, name) => ({
  name, searchName: name.toLowerCase(), code: code(uid), programme: 'MCA', updatedAt: serverTimestamp(),
});
const request = (from, to) => ({
  from, to, fromName: 'From', fromCode: code(from), toName: 'To', createdAt: serverTimestamp(),
});
const attendance = () => ({
  overall: 80.5,
  subjects: [{ code: 'CA711', name: 'Maths', present: 8, held: 10, percent: 80, target: 75 }],
  updatedAt: serverTimestamp(),
});

/** Seeds data with the rules switched off. */
async function seed(fn) {
  await env.withSecurityRulesDisabled(async (ctx) => fn(ctx.firestore()));
}

async function makeFriends(a, b) {
  await seed(async (db) => {
    await setDoc(doc(db, `users/${a}/friends/${b}`), { name: 'B', since: new Date() });
    await setDoc(doc(db, `users/${b}/friends/${a}`), { name: 'A', since: new Date() });
  });
}

before(async () => {
  const [host, port] = (process.env.FIRESTORE_EMULATOR_HOST ?? '127.0.0.1:8085').split(':');
  env = await initializeTestEnvironment({
    projectId: 'demo-attendo',
    firestore: { rules: readFileSync(process.env.RULES ?? new URL('../firestore.rules', import.meta.url), 'utf8'), host, port: Number(port) },
  });
});
after(async () => env.cleanup());
beforeEach(async () => {
  await env.clearFirestore();
  await seed(async (db) => {
    for (const [uid, name] of [[ALICE, 'Alice'], [BOB, 'Bob'], [CAROL, 'Carol']]) {
      await setDoc(doc(db, `users/${uid}`), { ...profile(uid, name), updatedAt: new Date() });
    }
  });
});

describe('profiles', () => {
  test('app: create and refresh your own profile', async () => {
    await assertSucceeds(setDoc(doc(google(ALICE), `users/${ALICE}`), profile(ALICE, 'Alice A')));
  });
  test('app: open another student', async () => {
    await assertSucceeds(getDoc(doc(google(BOB), `users/${ALICE}`)));
  });
  test('app: search by name (20 results)', async () => {
    const q = query(collection(google(BOB), 'users'), orderBy('searchName'), startAt('al'), endAt('al'), limit(20));
    await assertSucceeds(getDocs(q));
  });
  test('app: search by friend code (5 results)', async () => {
    const q = query(collection(google(BOB), 'users'), where('code', '==', code(ALICE)), limit(5));
    await assertSucceeds(getDocs(q));
  });
  test('attack: download every profile in one query', async () => {
    await assertFails(getDocs(collection(google(BOB), 'users')));
    await assertFails(getDocs(query(collection(google(BOB), 'users'), limit(21))));
  });
  test('attack: read profiles without signing in', async () => {
    await assertFails(getDoc(doc(nobody(), `users/${ALICE}`)));
  });
  test('attack: read profiles with an anonymous or email account', async () => {
    for (const provider of ['anonymous', 'password', 'custom']) {
      const db = env.authenticatedContext(CAROL, { firebase: { sign_in_provider: provider } }).firestore();
      await assertFails(getDoc(doc(db, `users/${ALICE}`)));
    }
  });
  test("attack: claim another student's friend code", async () => {
    await assertFails(setDoc(doc(google(CAROL), `users/${CAROL}`), { ...profile(CAROL, 'Carol'), code: code(ALICE) }));
  });
  test("attack: write someone else's profile", async () => {
    await assertFails(setDoc(doc(google(CAROL), `users/${ALICE}`), profile(ALICE, 'Hacked')));
  });
  test('attack: extra fields or a 500 KB timestamp', async () => {
    await assertFails(setDoc(doc(google(ALICE), `users/${ALICE}`), { ...profile(ALICE, 'Alice'), email: 'a@b.c' }));
    await assertFails(setDoc(doc(google(ALICE), `users/${ALICE}`), { ...profile(ALICE, 'Alice'), updatedAt: 'x'.repeat(500_000) }));
  });
  test('attack: names longer than 40 characters', async () => {
    await assertFails(setDoc(doc(google(ALICE), `users/${ALICE}`), profile(ALICE, 'A'.repeat(41))));
  });
});

describe('friend requests', () => {
  test('app: send a request', async () => {
    await assertSucceeds(setDoc(doc(google(ALICE), `requests/${ALICE}_${BOB}`), request(ALICE, BOB)));
  });
  test('app: see incoming and outgoing requests', async () => {
    await seed((db) => setDoc(doc(db, `requests/${ALICE}_${BOB}`), { ...request(ALICE, BOB), createdAt: new Date() }));
    await assertSucceeds(getDocs(query(collection(google(BOB), 'requests'), where('to', '==', BOB))));
    await assertSucceeds(getDocs(query(collection(google(ALICE), 'requests'), where('from', '==', ALICE))));
  });
  test('app: accept a request', async () => {
    await seed((db) => setDoc(doc(db, `requests/${ALICE}_${BOB}`), { ...request(ALICE, BOB), createdAt: new Date() }));
    const db = google(BOB);
    const batch = writeBatch(db);
    batch.set(doc(db, `users/${BOB}/friends/${ALICE}`), { name: 'From', since: serverTimestamp() });
    batch.set(doc(db, `users/${ALICE}/friends/${BOB}`), { name: 'Bob', since: serverTimestamp() });
    batch.delete(doc(db, `requests/${ALICE}_${BOB}`));
    await assertSucceeds(batch.commit());
  });
  test('app: decline or cancel a request', async () => {
    await seed((db) => setDoc(doc(db, `requests/${ALICE}_${BOB}`), { ...request(ALICE, BOB), createdAt: new Date() }));
    await assertSucceeds(deleteDoc(doc(google(BOB), `requests/${ALICE}_${BOB}`)));
  });
  test('attack: send a request as someone else, or to yourself', async () => {
    await assertFails(setDoc(doc(google(CAROL), `requests/${ALICE}_${BOB}`), request(ALICE, BOB)));
    await assertFails(setDoc(doc(google(ALICE), `requests/${ALICE}_${ALICE}`), request(ALICE, ALICE)));
  });
  test('attack: read other students\' requests', async () => {
    await seed((db) => setDoc(doc(db, `requests/${ALICE}_${BOB}`), { ...request(ALICE, BOB), createdAt: new Date() }));
    await assertFails(getDoc(doc(google(CAROL), `requests/${ALICE}_${BOB}`)));
    await assertFails(getDocs(query(collection(google(CAROL), 'requests'), where('to', '==', BOB))));
  });
  test('attack: overwrite a pending request', async () => {
    await seed((db) => setDoc(doc(db, `requests/${ALICE}_${BOB}`), { ...request(ALICE, BOB), createdAt: new Date() }));
    await assertFails(setDoc(doc(google(ALICE), `requests/${ALICE}_${BOB}`), request(ALICE, BOB)));
  });
  test('attack: request stuffed with a 500 KB field', async () => {
    await assertFails(setDoc(doc(google(ALICE), `requests/${ALICE}_${BOB}`), { ...request(ALICE, BOB), createdAt: 'x'.repeat(500_000) }));
  });
  test('attack: request to an existing friend', async () => {
    await makeFriends(ALICE, BOB);
    await assertFails(setDoc(doc(google(ALICE), `requests/${ALICE}_${BOB}`), request(ALICE, BOB)));
  });
});

describe('friendships', () => {
  test('attack: become friends without a request', async () => {
    const db = google(CAROL);
    await assertFails(setDoc(doc(db, `users/${CAROL}/friends/${ALICE}`), { name: 'Alice', since: serverTimestamp() }));
    await assertFails(setDoc(doc(db, `users/${ALICE}/friends/${CAROL}`), { name: 'Carol', since: serverTimestamp() }));
  });
  test('attack: the sender accepts their own request', async () => {
    await seed((db) => setDoc(doc(db, `requests/${ALICE}_${BOB}`), { ...request(ALICE, BOB), createdAt: new Date() }));
    const db = google(ALICE);
    await assertFails(setDoc(doc(db, `users/${BOB}/friends/${ALICE}`), { name: 'Alice', since: serverTimestamp() }));
    await assertFails(setDoc(doc(db, `users/${ALICE}/friends/${BOB}`), { name: 'Bob', since: serverTimestamp() }));
  });
  test('attack: friendship stuffed with a 500 KB field', async () => {
    await seed((db) => setDoc(doc(db, `requests/${ALICE}_${BOB}`), { ...request(ALICE, BOB), createdAt: new Date() }));
    await assertFails(setDoc(doc(google(BOB), `users/${BOB}/friends/${ALICE}`), { name: 'A', since: 'x'.repeat(500_000) }));
  });
  test('app: friends see each other\'s friend lists; strangers don\'t', async () => {
    await makeFriends(ALICE, BOB);
    await assertSucceeds(getDocs(collection(google(BOB), `users/${ALICE}/friends`)));
    await assertFails(getDocs(collection(google(CAROL), `users/${ALICE}/friends`)));
  });
  test('app: unfriend (either side)', async () => {
    await makeFriends(ALICE, BOB);
    const db = google(ALICE);
    const batch = writeBatch(db);
    batch.delete(doc(db, `users/${ALICE}/friends/${BOB}`));
    batch.delete(doc(db, `users/${BOB}/friends/${ALICE}`));
    await assertSucceeds(batch.commit());
  });
  test("attack: remove other people's friendships", async () => {
    await makeFriends(ALICE, BOB);
    await assertFails(deleteDoc(doc(google(CAROL), `users/${ALICE}/friends/${BOB}`)));
  });
});

describe('shared attendance and grades', () => {
  test('app: publish, then a friend reads it', async () => {
    await makeFriends(ALICE, BOB);
    await assertSucceeds(setDoc(doc(google(ALICE), `users/${ALICE}/shared/attendance`), attendance()));
    await assertSucceeds(getDoc(doc(google(BOB), `users/${ALICE}/shared/attendance`)));
  });
  test('app: turning sharing off deletes it', async () => {
    await seed((db) => setDoc(doc(db, `users/${ALICE}/shared/grades`), { ...attendance(), updatedAt: new Date() }));
    await assertSucceeds(deleteDoc(doc(google(ALICE), `users/${ALICE}/shared/grades`)));
  });
  test('attack: a stranger reads shared attendance', async () => {
    await seed((db) => setDoc(doc(db, `users/${ALICE}/shared/attendance`), { ...attendance(), updatedAt: new Date() }));
    await assertFails(getDoc(doc(google(CAROL), `users/${ALICE}/shared/attendance`)));
    await assertFails(getDoc(doc(nobody(), `users/${ALICE}/shared/attendance`)));
  });
  test("attack: write someone else's shared data, or an unknown document", async () => {
    await makeFriends(ALICE, BOB);
    await assertFails(setDoc(doc(google(BOB), `users/${ALICE}/shared/attendance`), attendance()));
    await assertFails(setDoc(doc(google(ALICE), `users/${ALICE}/shared/notes`), attendance()));
  });
  test('attack: more than 20 subjects, or a 500 KB timestamp', async () => {
    const many = Array.from({ length: 21 }, () => attendance().subjects[0]);
    await assertFails(setDoc(doc(google(ALICE), `users/${ALICE}/shared/attendance`), { ...attendance(), subjects: many }));
    await assertFails(setDoc(doc(google(ALICE), `users/${ALICE}/shared/attendance`), { ...attendance(), updatedAt: 'x'.repeat(500_000) }));
  });
});

describe('delete profile', () => {
  test('app: deletes shared data, both sides of friendships, requests, then the profile', async () => {
    await makeFriends(ALICE, BOB);
    await seed(async (db) => {
      await setDoc(doc(db, `users/${ALICE}/shared/attendance`), { ...attendance(), updatedAt: new Date() });
      await setDoc(doc(db, `requests/${ALICE}_${CAROL}`), { ...request(ALICE, CAROL), createdAt: new Date() });
      await setDoc(doc(db, `requests/${CAROL}_${ALICE}`), { ...request(CAROL, ALICE), createdAt: new Date() });
    });
    const db = google(ALICE);
    const reqs = [
      ...(await getDocs(query(collection(db, 'requests'), where('from', '==', ALICE)))).docs,
      ...(await getDocs(query(collection(db, 'requests'), where('to', '==', ALICE)))).docs,
    ];
    const batch = writeBatch(db);
    batch.delete(doc(db, `users/${ALICE}/shared/attendance`));
    batch.delete(doc(db, `users/${ALICE}/shared/grades`));
    batch.delete(doc(db, `users/${BOB}/friends/${ALICE}`));
    batch.delete(doc(db, `users/${ALICE}/friends/${BOB}`));
    reqs.forEach((r) => batch.delete(r.ref));
    await assertSucceeds(batch.commit());
    await assertSucceeds(deleteDoc(doc(db, `users/${ALICE}`)));
  });
});
