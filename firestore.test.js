const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user: cannot read notes", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("notes").get());
});

test("Unauthenticated user: cannot create note", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("notes").doc("note_1").set({
      title: "Test Note",
      content: "Test Content",
      category: "Personal",
      isPinned: false,
      userId: ALICE_UID,
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );
});

test("Authenticated user: can create, read, and update their own note", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const noteRef = aliceDb.collection("users").doc(ALICE_UID).collection("notes").doc("note_1");

  await assertSucceeds(
    noteRef.set({
      title: "Alice Note",
      content: "Content for Alice",
      category: "Work",
      tags: "Meeting, Urgent",
      isPinned: true,
      userId: ALICE_UID,
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );

  await assertSucceeds(noteRef.get());

  await assertSucceeds(
    noteRef.update({
      title: "Alice Note Updated",
      content: "Updated Content",
      updatedAt: new Date(),
    })
  );
});

test("Cross-user isolation: Bob cannot read or modify Alice's note", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(ALICE_UID).collection("notes").doc("alice_note").set({
      title: "Alice Secret",
      content: "Secret content",
      category: "Personal",
      tags: "Private",
      isPinned: false,
      userId: ALICE_UID,
      createdAt: new Date(),
      updatedAt: new Date(),
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const aliceNoteRef = bobDb.collection("users").doc(ALICE_UID).collection("notes").doc("alice_note");

  await assertFails(aliceNoteRef.get());
  await assertFails(
    aliceNoteRef.update({
      title: "Hacked by Bob",
      updatedAt: new Date(),
    })
  );
  await assertFails(aliceNoteRef.delete());
});
