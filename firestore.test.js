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

function nowTs() {
  return new Date(Date.now() - 2000);
}

test("Unauthenticated user: cannot list or read institutions or assignments", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/institutions`).get());
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/assignments`).get());
});

test("Owner isolation: Bob cannot read or write Alice's institution or assignment", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const adminDb = context.firestore();
    await adminDb.doc(`users/${ALICE_UID}/institutions/inst_1`).set({
      userId: ALICE_UID,
      name: "USP",
      shortName: "USP",
      type: "university",
      colorHex: "#1E3A5F",
      notes: "",
      createdAt: nowTs(),
      updatedAt: nowTs(),
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.doc(`users/${ALICE_UID}/institutions/inst_1`).get());
  await assertFails(bobDb.collection(`users/${ALICE_UID}/institutions`).get());
  await assertFails(
    bobDb.doc(`users/${ALICE_UID}/institutions/inst_2`).set({
      userId: BOB_UID,
      name: "MIT",
      shortName: "MIT",
      type: "university",
      colorHex: "#1E3A5F",
      notes: "",
      createdAt: nowTs(),
      updatedAt: nowTs(),
    })
  );
});

test("Shadow update & identity spoofing: rejects ghost fields and mismatched userId", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/institutions/inst_bad`).set({
      userId: BOB_UID,
      name: "USP",
      shortName: "USP",
      type: "university",
      colorHex: "#1E3A5F",
      notes: "",
      createdAt: nowTs(),
      updatedAt: nowTs(),
    })
  );

  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/institutions/inst_ghost`).set({
      userId: ALICE_UID,
      name: "USP",
      shortName: "USP",
      type: "university",
      colorHex: "#1E3A5F",
      notes: "",
      isAdmin: true,
      createdAt: nowTs(),
      updatedAt: nowTs(),
    })
  );
});

test("Relational chain: Institution -> Course -> Assignment -> Attachment succeeds for owner", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  // Creating a course before institution exists must fail (Parent existence guard)
  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/courses/course_1`).set({
      userId: ALICE_UID,
      institutionId: "inst_1",
      institutionName: "USP",
      name: "Cálculo II",
      code: "MAT0120",
      professor: "Prof. Silva",
      term: "2026.2",
      targetGrade: 7.0,
      maxScale: 10.0,
      colorHex: "#059669",
      createdAt: nowTs(),
      updatedAt: nowTs(),
    })
  );

  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/institutions/inst_1`).set({
      userId: ALICE_UID,
      name: "USP",
      shortName: "USP",
      type: "university",
      colorHex: "#1E3A5F",
      notes: "Campus Capital",
      createdAt: nowTs(),
      updatedAt: nowTs(),
    })
  );

  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/courses/course_1`).set({
      userId: ALICE_UID,
      institutionId: "inst_1",
      institutionName: "USP",
      name: "Cálculo II",
      code: "MAT0120",
      professor: "Prof. Silva",
      term: "2026.2",
      targetGrade: 7.0,
      maxScale: 10.0,
      colorHex: "#059669",
      createdAt: nowTs(),
      updatedAt: nowTs(),
    })
  );

  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/assignments/assign_1`).set({
      userId: ALICE_UID,
      institutionId: "inst_1",
      courseId: "course_1",
      categoryId: "cat_1",
      title: "Relatório de Integrais",
      description: "Resolver lista e anexar PDF com desenvolvimento.",
      maxPoints: 3.5,
      dueDateMillis: Date.now() + 86400000,
      priorityType: "grade_a",
      reminderEnabled: true,
      reminderHoursBefore: 48,
      status: "pending",
      requiresAttachment: true,
      requiredAttachmentSpec: "PDF com cálculos detalhados",
      aiVerdict: "none",
      aiSummary: "",
      aiEstimatedPercent: 0,
      createdAt: nowTs(),
      updatedAt: nowTs(),
    })
  );

  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/assignments/assign_1/attachments/att_1`).set({
      userId: ALICE_UID,
      assignmentId: "assign_1",
      fileName: "relatorio.pdf",
      mimeType: "application/pdf",
      sizeBytes: 24500,
      localFilePath: "/files/relatorio.pdf",
      textNote: "",
      aiStatus: "pending",
      aiFeedback: "",
      aiEstimatedPercent: 0,
      aiStrengths: [],
      aiGaps: [],
      aiNextSteps: [],
      createdAt: nowTs(),
      updatedAt: nowTs(),
    })
  );

  // Update AI evaluation on attachment
  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/assignments/assign_1/attachments/att_1`).update({
      aiStatus: "approved",
      aiFeedback: "Desenvolvimento completo e bem estruturado.",
      aiEstimatedPercent: 92,
      aiStrengths: ["Demonstração clara"],
      aiGaps: [],
      aiNextSteps: ["Revisar formatação final"],
      updatedAt: nowTs(),
    })
  );

  // Reject ghost field on update
  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/assignments/assign_1`).update({
      ghostField: "hack",
      updatedAt: nowTs(),
    })
  );
});

test("StudyNotes: owner can create/update/list notes, Bob and unauthenticated rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const unauthDb = testEnv.unauthenticatedContext().firestore();

  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/notes/note_1`).set({
      userId: ALICE_UID,
      title: "Resumo Cálculo II",
      content: "Revisar teorema fundamental do cálculo antes da prova.",
      courseId: "course_1",
      assignmentId: "",
      pinned: true,
      createdAt: nowTs(),
      updatedAt: nowTs(),
    })
  );

  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/notes/note_1`).update({
      content: "Revisar integrais impróprias também.",
      pinned: false,
      updatedAt: nowTs(),
    })
  );

  await assertFails(unauthDb.collection(`users/${ALICE_UID}/notes`).get());
  await assertFails(bobDb.collection(`users/${ALICE_UID}/notes`).get());
  await assertFails(
    bobDb.doc(`users/${ALICE_UID}/notes/note_1`).update({
      content: "Invasão",
      updatedAt: nowTs(),
    })
  );
  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/notes/note_bad`).set({
      userId: ALICE_UID,
      title: "Bad",
      content: "Ghost field",
      courseId: "",
      assignmentId: "",
      pinned: false,
      extraSecret: "hack",
      createdAt: nowTs(),
      updatedAt: nowTs(),
    })
  );
});
