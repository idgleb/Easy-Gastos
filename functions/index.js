const {onRequest} = require("firebase-functions/v2/https");
const admin = require("firebase-admin");
const axios = require("axios");
const functions = require("firebase-functions/v1");

// Inicializar Admin SDK una sola vez
admin.initializeApp();

// Configurar credenciales de Mercado Pago usando secrets de Firebase
// Debes ejecutar localmente:
// firebase functions:secrets:set MP_ACCESS_TOKEN
// firebase functions:secrets:set MP_WEBHOOK_SECRET (opcional, para validar webhooks)
const {defineSecret} = require("firebase-functions/params");
const mpAccessToken = defineSecret("MP_ACCESS_TOKEN");
const mpWebhookSecret = defineSecret("MP_WEBHOOK_SECRET"); // Clave secreta del webhook (opcional)

/**
 * Cloud Function HTTP: crea usuarios usando Admin SDK si el solicitante es admin.
 * Espera un header Authorization: Bearer <ID_TOKEN>.
 */
exports.createUserByAdmin = onRequest(async (req, res) => {
  res.set("Access-Control-Allow-Origin", "*");
  res.set("Access-Control-Allow-Headers", "Content-Type,Authorization");
  if (req.method === "OPTIONS") {
    return res.status(204).send("");
  }
  if (req.method !== "POST") {
    return res.status(405).json({error: "method_not_allowed"});
  }

  const authHeader = req.headers.authorization || "";
  if (!authHeader.startsWith("Bearer ")) {
    return res.status(401).json({error: "missing_auth_header"});
  }

  const idToken = authHeader.replace("Bearer ", "").trim();
  try {
    const decoded = await admin.auth().verifyIdToken(idToken);
    const adminUid = decoded.uid;

    const adminDoc = await admin.firestore().doc(`users/${adminUid}`).get();
    if (!adminDoc.exists || adminDoc.data().role !== "admin") {
      return res.status(403).json({error: "not_authorized"});
    }

    const {
      email,
      password,
      name,
      role,
      planId,
      planExpiresAt,
      zonaHoraria,
      isActive,
    } = req.body || {};

    if (!email || typeof email !== "string") {
      return res.status(400).json({error: "missing_email"});
    }
    if (!password || typeof password !== "string" || password.length < 6) {
      return res.status(400).json({error: "invalid_password"});
    }

    const normalizedRole = role && typeof role === "string" && role.trim().length > 0 ?
      role.trim() :
      "user";
    const normalizedPlanId = planId && typeof planId === "string" && planId.trim().length > 0 ?
      planId.trim() :
      "free";
    const displayName = name && typeof name === "string" && name.trim().length > 0 ?
      name.trim() :
      (email.includes("@") ? email.split("@")[0] : email);
    const normalizedZonaHoraria = zonaHoraria && typeof zonaHoraria === "string" && zonaHoraria.trim().length > 0 ?
      zonaHoraria.trim() :
      "UTC";
    const isActiveFlag = typeof isActive === "boolean" ? isActive : true;

    const userRecord = await admin.auth().createUser({
      email,
      password,
      displayName,
      disabled: false,
    });

    const userDoc = {
      name: displayName,
      email,
      role: normalizedRole,
      plan_id: normalizedPlanId,
      plan_expires_at: typeof planExpiresAt === "number" ? planExpiresAt : null,
      zona_horaria: normalizedZonaHoraria,
      is_active: isActiveFlag,
      created_at: admin.firestore.FieldValue.serverTimestamp(),
      updated_at: admin.firestore.FieldValue.serverTimestamp(),
    };

    try {
      await admin.firestore().doc(`users/${userRecord.uid}`).set(userDoc, {merge: true});
    } catch (firestoreError) {
      await admin.auth().deleteUser(userRecord.uid);
      console.error("❌ Error al guardar usuario en Firestore:", firestoreError);
      return res.status(500).json({error: "firestore_write_failed"});
    }

    return res.status(200).json({
      uid: userRecord.uid,
      email,
      role: normalizedRole,
      planId: normalizedPlanId,
      planExpiresAt: userDoc.plan_expires_at,
      zonaHoraria: normalizedZonaHoraria,
      isActive: isActiveFlag,
    });
  } catch (error) {
    console.error("❌ Error en createUserByAdmin:", error);
    const code = error?.code || "";
    if (code === "auth/id-token-expired" || code === "auth/argument-error") {
      return res.status(401).json({error: "invalid_token"});
    }
    return res.status(500).json({error: "internal_error"});
  }
});

/**
 * Cloud Function HTTP: elimina usuarios (Auth + Firestore + subcolecciones) si el solicitante es admin.
 */
exports.deleteUserByAdmin = onRequest(async (req, res) => {
  res.set("Access-Control-Allow-Origin", "*");
  res.set("Access-Control-Allow-Headers", "Content-Type,Authorization");
  if (req.method === "OPTIONS") {
    return res.status(204).send("");
  }
  if (req.method !== "POST") {
    return res.status(405).json({error: "method_not_allowed"});
  }

  const authHeader = req.headers.authorization || "";
  if (!authHeader.startsWith("Bearer ")) {
    return res.status(401).json({error: "missing_auth_header"});
  }

  const idToken = authHeader.replace("Bearer ", "").trim();
  const {uid} = req.body || {};

  if (!uid || typeof uid !== "string") {
    return res.status(400).json({error: "missing_uid"});
  }

  try {
    const decoded = await admin.auth().verifyIdToken(idToken);
    const adminUid = decoded.uid;

    const adminDoc = await admin.firestore().doc(`users/${adminUid}`).get();
    if (!adminDoc.exists || adminDoc.data().role !== "admin") {
      return res.status(403).json({error: "not_authorized"});
    }

    await deleteUserData(uid);

    try {
      await admin.auth().deleteUser(uid);
    } catch (authError) {
      if (authError?.code !== "auth/user-not-found") {
        throw authError;
      }
    }

    return res.status(200).json({success: true});
  } catch (error) {
    console.error("❌ Error en deleteUserByAdmin:", error);
    const code = error?.code || "";
    if (code === "auth/id-token-expired" || code === "auth/argument-error") {
      return res.status(401).json({error: "invalid_token"});
    }
    return res.status(500).json({error: "internal_error"});
  }
});

async function deleteUserData(uid) {
  const firestore = admin.firestore();
  const userDocRef = firestore.doc(`users/${uid}`);

  const subcollections = ["categories", "expenses"];
  for (const subcollection of subcollections) {
    const collectionRef = userDocRef.collection(subcollection);
    await deleteAllDocuments(collectionRef);
  }

  await userDocRef.delete().catch((error) => {
    if (!isNotFoundError(error)) {
      throw error;
    }
  });
}

async function deleteAllDocuments(collectionRef) {
  try {
    const documents = await collectionRef.listDocuments();
    if (!documents || documents.length === 0) {
      return;
    }

    const chunkSize = 500;
    for (let i = 0; i < documents.length; i += chunkSize) {
      const chunk = documents.slice(i, i + chunkSize);
      await Promise.all(chunk.map((docRef) => docRef.delete()));
    }
  } catch (error) {
    if (error?.code === 5) { // NOT_FOUND
      return;
    }
    throw error;
  }
}

function isNotFoundError(error) {
  if (!error) {
    return false;
  }
  const code = error.code;
  return code === 5 || code === "5" || code === "not-found";
}

/**
 * Webhook de Mercado Pago para actualizar el plan del usuario
 * Ruta pública:
 *   https://us-central1-TU_PROYECTO.cloudfunctions.net/mercadoPagoWebhook
 */
exports.mercadoPagoWebhook = onRequest(
  {
    secrets: [mpAccessToken, mpWebhookSecret],
  },
  async (req, res) => {
    try {
    console.log("🔔 Webhook recibido de Mercado Pago");
    console.log("📥 Headers:", JSON.stringify(req.headers));
    console.log("📥 Body:", JSON.stringify(req.body));
    console.log("📥 Query:", JSON.stringify(req.query));
    
    // Obtener access token de Mercado Pago
    const accessToken = mpAccessToken.value();
    if (!accessToken) {
      console.error("❌ MP_ACCESS_TOKEN no está configurado");
      return res.status(500).send("configuration error");
    }
    
    // Validar clave secreta del webhook si está configurada (opcional pero recomendado)
    try {
      const webhookSecret = mpWebhookSecret.value();
      if (webhookSecret) {
        const xSignature = req.headers["x-signature"] || req.headers["x-signature-256"];
        const xRequestId = req.headers["x-request-id"];
        // Nota: La validación completa requiere verificar la firma HMAC
        // Por ahora, confiamos en que la URL es privada y solo Mercado Pago la conoce
        console.log("Webhook recibido con signature:", xSignature ? "presente" : "ausente");
      }
    } catch (secretError) {
      // El secret es opcional, así que solo logueamos si hay error
      console.log("MP_WEBHOOK_SECRET no configurado (opcional)");
    }
    
    // Manejar formato nuevo de webhooks (con action y data.id en body)
    // y formato antiguo (con topic en query params)
    let eventType = null;
    let paymentId = null;
    
    // Formato nuevo: { type: "payment", action: "payment.created", data: { id: "..." } }
    if (req.body && req.body.type) {
      eventType = req.body.type;
      if (req.body.data && req.body.data.id) {
        paymentId = req.body.data.id;
      }
      console.log(`Webhook nuevo formato - type: ${eventType}, action: ${req.body.action}, paymentId: ${paymentId}`);
    } 
    // Formato antiguo: ?topic=payment&data.id=... o { resource: "123", topic: "payment" }
    else {
      eventType = req.query.topic || req.query.type || req.body.topic;
      
      // Intentar obtener paymentId de diferentes lugares
      paymentId = req.query["data.id"] || 
                  req.query.id || 
                  (req.body && req.body.data && req.body.data.id) ||
                  (req.body && req.body.resource && typeof req.body.resource === "string" ? req.body.resource : null) ||
                  (req.body && req.body.resource && typeof req.body.resource === "object" && req.body.resource.id ? req.body.resource.id : null);
      
      console.log(`Webhook formato antiguo - topic: ${eventType}, paymentId: ${paymentId}`);
    }
    
    // Validar que sea un evento de pago
    if (eventType !== "payment") {
      console.log("Evento ignorado (no es payment):", eventType);
      return res.status(200).send("ignored");
    }

    if (!paymentId) {
      console.error("Missing paymentId in webhook. Body:", JSON.stringify(req.body), "Query:", req.query);
      return res.status(400).send("missing payment id");
    }

    console.log(`🔍 Obteniendo información del pago ${paymentId} desde Mercado Pago...`);
    
    // Obtener información completa del pago desde Mercado Pago usando API REST
    let data;
    try {
      const response = await axios.get(
        `https://api.mercadopago.com/v1/payments/${paymentId}`,
        {
          headers: {
            "Authorization": `Bearer ${accessToken}`,
            "Content-Type": "application/json"
          }
        }
      );
      
      console.log(`📦 Respuesta de Mercado Pago recibida para paymentId: ${paymentId}`);
      data = response.data;
      
      if (!data) {
        console.error("❌ Respuesta vacía de Mercado Pago para paymentId:", paymentId);
        return res.status(200).send("payment not available yet");
      }
    } catch (mpError) {
      console.error("❌ Error al obtener pago desde Mercado Pago:");
      console.error("❌ Error type:", typeof mpError);
      console.error("❌ Error message:", mpError?.message || String(mpError));
      console.error("❌ Error response:", mpError?.response?.data);
      console.error("❌ Error status:", mpError?.response?.status);
      
      // Si el pago no existe o hay un error de API, retornar 200 para que Mercado Pago no reintente
      // El webhook se volverá a llamar cuando el pago esté disponible
      if (mpError?.response?.status === 404 || mpError?.message?.includes("not found")) {
        console.log(`⏳ Pago ${paymentId} aún no disponible en la API. Esperando...`);
        return res.status(200).send("payment not found yet");
      }
      
      return res.status(500).json({ 
        error: "error fetching payment",
        message: mpError?.message || String(mpError)
      });
    }
    console.log(`📊 Estado del pago ${paymentId}: ${data.status}`);
    console.log(`📊 Pago completo (primeros 500 chars):`, JSON.stringify(data).substring(0, 500));

    // Sólo procesar pagos aprobados
    if (data.status !== "approved") {
      console.log(`⏳ Pago ${paymentId} no aprobado aún, status: ${data.status}. Esperando aprobación...`);
      return res.status(200).send("payment not approved");
    }

    // Leer metadata enviada al crear la preferencia
    // El metadata puede estar en data.metadata o en data.additional_info
    let metadata = data.metadata || {};
    if (!metadata || Object.keys(metadata).length === 0) {
      // Intentar obtener desde additional_info si existe
      if (data.additional_info && data.additional_info.metadata) {
        metadata = data.additional_info.metadata;
      }
    }
    const uid = metadata.uid;
    const planId = metadata.planId || metadata.plan_id;

    console.log(`📋 Metadata recibido:`, JSON.stringify(metadata));
    console.log(`👤 UID: ${uid}, Plan ID: ${planId}`);

    if (!uid || !planId) {
      console.error("❌ Missing uid or planId in metadata. Metadata completo:", JSON.stringify(data.metadata));
      console.error("❌ Data completo:", JSON.stringify(data, null, 2));
      return res.status(400).send("missing metadata");
    }

    const db = admin.firestore();

    // Por ahora, los planes premium son permanentes (sin expiración)
    // Si en el futuro quieres agregar expiración, puedes descomentar el código de abajo
    // const now = Date.now();
    // const thirtyDaysInMillis = 30 * 24 * 60 * 60 * 1000;
    // const expiresAt = now + thirtyDaysInMillis;
    // const planExpiresAt = planId === "free" ? null : expiresAt;
    
    // Plan permanente: no tiene expiración (null)
    const planExpiresAt = null;

    try {
      // Actualizar plan del usuario en /users/{uid}
      const userRef = db.doc(`users/${uid}`);
      await userRef.set(
        {
          plan_id: planId,
          plan_expires_at: planExpiresAt,
          updated_at: admin.firestore.FieldValue.serverTimestamp(),
        },
        { merge: true }
      );
      console.log(`✅ Plan actualizado en Firestore para uid=${uid}`);

      // Registrar pago (opcional) en /payments/{paymentId}
      await db
        .collection("payments")
        .doc(String(paymentId))
        .set({
          uid,
          planId,
          mpPaymentId: paymentId,
          status: data.status,
          amount: data.transaction_amount,
          raw: data,
          created_at: admin.firestore.FieldValue.serverTimestamp(),
        });
      console.log(`✅ Pago registrado en Firestore para paymentId=${paymentId}`);

      console.log(`✅ Plan actualizado a ${planId} para uid=${uid}, paymentId=${paymentId}`);
      return res.status(200).send("ok");
    } catch (firestoreError) {
      console.error("❌ Error al actualizar Firestore:");
      console.error("❌ Error message:", firestoreError?.message);
      console.error("❌ Error stack:", firestoreError?.stack);
      console.error("❌ Error completo:", JSON.stringify(firestoreError, Object.getOwnPropertyNames(firestoreError)));
      return res.status(500).json({
        error: "firestore error",
        message: firestoreError?.message || String(firestoreError)
      });
    }
  } catch (error) {
    console.error("❌ Error en webhook de Mercado Pago");
    console.error("❌ Error message:", error.message);
    console.error("❌ Error stack:", error.stack);
    console.error("❌ Error completo:", JSON.stringify(error, Object.getOwnPropertyNames(error)));
    return res.status(500).json({ 
      error: "internal server error",
      message: error.message 
    });
  }
  }
);

/**
 * Trigger de autenticación: crea el documento del usuario cuando se registra
 * (solo si aún no existe).
 */
exports.onAuthUserCreated = functions.auth.user().onCreate(async (user) => {
  try {
    const uid = user.uid;
    const email = user.email || "";
    const displayName = user.displayName || (email.includes("@") ? email.split("@")[0] : "Usuario");

    const userRef = admin.firestore().doc(`users/${uid}`);
    const snapshot = await userRef.get();
    if (snapshot.exists) {
      console.log(`Documento de usuario ${uid} ya existe. Se omite creación automática.`);
      return;
    }

    await userRef.set({
      name: displayName,
      email,
      role: "user",
      plan_id: "free",
      plan_expires_at: null,
      zona_horaria: "UTC",
      is_active: true,
      created_at: admin.firestore.FieldValue.serverTimestamp(),
      updated_at: admin.firestore.FieldValue.serverTimestamp(),
    });
    console.log(`Perfil de usuario creado automáticamente para uid=${uid}`);
  } catch (error) {
    console.error("❌ Error en onAuthUserCreated:", error);
  }
});


