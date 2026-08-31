const functions = require("firebase-functions");
const admin = require("firebase-admin");

admin.initializeApp();
const db = admin.firestore();

/**
 * Callable Cloud Function: joinWorkshopBySyncCode
 * Request payload: { "syncCode": "WORKSHOP-XXXX" }
 * Uses request.auth.uid securely from Firebase Auth context.
 */
exports.joinWorkshopBySyncCode = functions.https.onCall(async (data, context) => {
  // 1. Authentication check
  if (!context.auth || !context.auth.uid) {
    throw new functions.https.HttpsError(
      "unauthenticated",
      "يجب تسجيل الدخول أولاً للانضمام إلى الورشة."
    );
  }

  const uid = context.auth.uid;
  const rawSyncCode = data ? data.syncCode : null;

  if (!rawSyncCode || typeof rawSyncCode !== "string" || !rawSyncCode.trim()) {
    throw new functions.https.HttpsError(
      "invalid-argument",
      "رمز المزامنة مطلوب."
    );
  }

  const cleanCode = rawSyncCode.trim().toUpperCase();

  // 2. Read user document to verify role
  const userRef = db.collection("users").doc(uid);
  const userDoc = await userRef.get();

  if (!userDoc.exists) {
    throw new functions.https.HttpsError(
      "not-found",
      "بيانات المستخدم غير موجودة."
    );
  }

  const userData = userDoc.data();
  const userRole = (userData.role || "").toString().toUpperCase();

  if (userRole !== "WORKER") {
    throw new functions.https.HttpsError(
      "permission-denied",
      "عفواً، فقط العمال يحق لهم الانضمام عبر رمز المزامنة."
    );
  }

  // 3. Search for workshop by syncCode
  let targetWorkshopId = null;
  let workshopName = "ورشة جديدة";

  const workshopsQuery = await db.collection("workshops")
    .where("syncCode", "==", cleanCode)
    .get();

  if (!workshopsQuery.empty) {
    const workshopDoc = workshopsQuery.docs[0];
    targetWorkshopId = workshopDoc.id;
    workshopName = workshopDoc.data().name || workshopName;
  } else {
    // Check if cleanCode is the direct workshop document ID
    const directDoc = await db.collection("workshops").doc(cleanCode).get();
    if (directDoc.exists) {
      targetWorkshopId = directDoc.id;
      workshopName = directDoc.data().name || workshopName;
    }
  }

  if (!targetWorkshopId) {
    throw new functions.https.HttpsError(
      "not-found",
      "INVALID_SYNC_CODE"
    );
  }

  // 4. Update database via Admin SDK
  const batch = db.batch();

  const memberRef = db.collection("workshops")
    .doc(targetWorkshopId)
    .collection("members")
    .doc(uid);

  batch.set(memberRef, {
    role: "worker",
    joinedAt: admin.firestore.FieldValue.serverTimestamp()
  }, { merge: true });

  batch.update(userRef, {
    workshopId: targetWorkshopId
  });

  await batch.commit();

  return {
    success: true,
    workshopId: targetWorkshopId,
    workshopName: workshopName,
    message: "تم الانضمام للورشة بنجاح 🎉"
  };
});
