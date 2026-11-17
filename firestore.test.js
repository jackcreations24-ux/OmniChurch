const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ADMIN_UID = "admin_jackson_01";
const MEMBER_UID = "member_marie_02";
const OUTSIDER_UID = "outsider_bob_03";
const CHURCH_A = "church_omnichurch_main";
const CHURCH_B = "church_other_tenant";

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

async function seedBaseTenantData() {
  const now = new Date();
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await db.collection("churches").doc(CHURCH_A).set({
      name: "OmniChurch Pro Tabernacle",
      ownerId: ADMIN_UID,
      createdAt: now,
    });
    await db.collection("churches").doc(CHURCH_B).set({
      name: "Other Tenant Church",
      ownerId: OUTSIDER_UID,
      createdAt: now,
    });
    await db.collection("users").doc(ADMIN_UID).set({
      userId: ADMIN_UID,
      email: "admin@omnichurch.org",
      displayName: "Jackson Charles",
      churchId: CHURCH_A,
      churchName: "OmniChurch Pro Tabernacle",
      role: "admin",
      createdAt: now,
    });
    await db.collection("users").doc(MEMBER_UID).set({
      userId: MEMBER_UID,
      email: "marie@omnichurch.org",
      displayName: "Marie Pierre",
      churchId: CHURCH_A,
      churchName: "OmniChurch Pro Tabernacle",
      role: "member",
      createdAt: now,
    });
    await db.collection("users").doc(OUTSIDER_UID).set({
      userId: OUTSIDER_UID,
      email: "bob@otherchurch.org",
      displayName: "Bob Outsider",
      churchId: CHURCH_B,
      churchName: "Other Tenant Church",
      role: "admin",
      createdAt: now,
    });
    await db.collection("members").doc("mem_marie").set({
      userId: MEMBER_UID,
      churchId: CHURCH_A,
      churchName: "OmniChurch Pro Tabernacle",
      fullName: "Marie Pierre",
      memberCode: "OM-0258743",
      department: "Adorasyon",
      role: "Mizisyen",
      status: "active",
      barcodeToken: "OMNI_TOKEN_MARIE_998877",
      createdAt: now,
    });
    await db.collection("services").doc("srv_sunday").set({
      churchId: CHURCH_A,
      churchName: "OmniChurch Pro Tabernacle",
      title: "Sèvis Dimanch",
      dateFormatted: "26 Okt. 2026 • 9:00 AM",
      startTimeEpochMillis: 1793000000000,
      lateToleranceMinutes: 15,
      isOpenForAttendance: true,
      isLive: true,
      authorId: ADMIN_UID,
      authorName: "Jackson Charles",
      reactionsCount: 0,
      commentsCount: 0,
      createdAt: now,
    });
  });
}

test("Matrikil+PIN session (no Google Auth) can read churches, members, services, and notifications for existing church and link Google account, but cannot read users PII", async () => {
  await seedBaseTenantData();
  const unauthDb = testEnv.unauthenticatedContext().firestore();

  // Strictly blocked: users PII without Firebase Auth
  await assertFails(unauthDb.collection("users").doc(MEMBER_UID).get());

  // Allowed for Matrikil + PIN member login without Google account
  await assertSucceeds(unauthDb.collection("churches").get());
  await assertSucceeds(unauthDb.collection("members").where("churchId", "==", CHURCH_A).get());
  await assertSucceeds(unauthDb.collection("services").where("churchId", "==", CHURCH_A).get());
  await assertSucceeds(unauthDb.collection("notifications").where("userId", "==", MEMBER_UID).get());

  // Allowed: linking optional Google account fields after PIN verification
  await assertSucceeds(
    unauthDb.collection("members").doc("mem_marie").update({
      googleEmail: "marie.linked@gmail.com",
      googleUid: "google_uid_marie_123",
      email: "marie.linked@gmail.com",
    })
  );

  // Blocked: tampering with immutable barcodeToken or memberCode
  await assertFails(
    unauthDb.collection("members").doc("mem_marie").update({
      barcodeToken: "TAMPERED_BARCODE",
    })
  );
});

test("PII Isolation: Authenticated user cannot read another user's profile or cross-tenant member card", async () => {
  await seedBaseTenantData();
  const memberDb = testEnv.authenticatedContext(MEMBER_UID).firestore();
  await assertFails(memberDb.collection("users").doc(ADMIN_UID).get());

  const outsiderDb = testEnv.authenticatedContext(OUTSIDER_UID).firestore();
  await assertFails(outsiderDb.collection("members").doc("mem_marie").get());
});

test("Multi-tenant query alignment: Member can query services of own church, fails on other church or unfiltered query", async () => {
  await seedBaseTenantData();
  const memberDb = testEnv.authenticatedContext(MEMBER_UID).firestore();

  await assertSucceeds(
    memberDb.collection("services").where("churchId", "==", CHURCH_A).get()
  );
  await assertFails(
    memberDb.collection("services").where("churchId", "==", CHURCH_B).get()
  );
  await assertFails(
    memberDb.collection("services").get()
  );
});

test("Shadow Update & Self-Promotion Prevention: Member cannot promote own role to admin or inject ghost fields", async () => {
  await seedBaseTenantData();
  const memberDb = testEnv.authenticatedContext(MEMBER_UID).firestore();

  // Attempt role escalation
  await assertFails(
    memberDb.collection("users").doc(MEMBER_UID).update({
      role: "admin",
    })
  );

  // Attempt ghost field injection
  await assertFails(
    memberDb.collection("users").doc(MEMBER_UID).update({
      displayName: "Marie Updated",
      isVerifiedGhostField: true,
    })
  );
});

test("Email Spoofing Prevention: Unverified super-admin email cannot bypass tenant isolation", async () => {
  await seedBaseTenantData();
  const spoofDb = testEnv.authenticatedContext("spoof_uid", {
    email: "charlesjj3186@gmail.com",
    email_verified: false,
  }).firestore();

  await assertFails(spoofDb.collection("users").doc(ADMIN_UID).get());
});

test("Secure Attendance Registration: Admin of Church A can record attendance for valid open service and verified barcodeToken, outsider or closed service fails", async () => {
  await seedBaseTenantData();
  const adminDb = testEnv.authenticatedContext(ADMIN_UID).firestore();
  const outsiderDb = testEnv.authenticatedContext(OUTSIDER_UID).firestore();
  const now = new Date();

  const validAttendancePayload = {
    churchId: CHURCH_A,
    serviceId: "srv_sunday",
    serviceName: "Sèvis Dimanch",
    memberDocId: "mem_marie",
    memberCode: "OM-0258743",
    memberName: "Marie Pierre",
    userId: MEMBER_UID,
    recordedByUid: ADMIN_UID,
    status: "on_time",
    checkInMethod: "qr_scan",
    barcodeToken: "OMNI_TOKEN_MARIE_998877",
    createdAt: now,
  };

  await assertSucceeds(
    adminDb.collection("attendances").doc("srv_sunday_mem_marie").set(validAttendancePayload)
  );

  // Fake barcode token must fail
  await assertFails(
    adminDb.collection("attendances").doc("srv_sunday_fake").set({
      ...validAttendancePayload,
      barcodeToken: "FAKE_SPOOFED_TOKEN",
    })
  );

  // Cross-tenant admin must fail
  await assertFails(
    outsiderDb.collection("attendances").doc("srv_sunday_outsider").set({
      ...validAttendancePayload,
      recordedByUid: OUTSIDER_UID,
    })
  );
});

test("Church security fields: owner matching adminGoogleUid can update quickPinEnabled and autoLockMinutes, outsider and invalid ranges fail", async () => {
  await seedBaseTenantData();
  const churchC = "church_uid_owner";
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("churches").doc(churchC).set({
      name: "Legliz Nouvo",
      adminGoogleEmail: "owner.c@omnichurch.org",
      adminGoogleUid: ADMIN_UID,
      adminAuthMethod: "email_password",
      quickPinEnabled: false,
      autoLockMinutes: 5,
    });
  });

  const adminDb = testEnv.authenticatedContext(ADMIN_UID, { email: "owner.c@omnichurch.org" }).firestore();
  const outsiderDb = testEnv.authenticatedContext(OUTSIDER_UID, { email: "bob@otherchurch.org" }).firestore();

  await assertSucceeds(
    adminDb.collection("churches").doc(churchC).update({
      quickPinEnabled: true,
      autoLockMinutes: 15,
      adminPasscode: "sha256:v1:" + "a".repeat(64),
      adminPasscodeUpdatedAt: new Date().toISOString(),
    })
  );

  // Invalid autoLockMinutes (> 120) must fail
  await assertFails(
    adminDb.collection("churches").doc(churchC).update({
      autoLockMinutes: 999,
    })
  );

  // Invalid adminAuthMethod must fail
  await assertFails(
    adminDb.collection("churches").doc(churchC).update({
      adminAuthMethod: "invalid_method",
    })
  );

  // Outsider cannot update claimed church security settings
  await assertFails(
    outsiderDb.collection("churches").doc(churchC).update({
      quickPinEnabled: false,
    })
  );
});

test("Legacy unclaimed church can be claimed by signed-in admin, and member cannot self-promote role on update", async () => {
  await seedBaseTenantData();
  const legacyChurchId = "church_legacy_pin";
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("churches").doc(legacyChurchId).set({
      name: "Legliz Ansyen",
      adminPasscode: "sha256:v1:" + "b".repeat(64),
    });
  });

  const claimingAdminDb = testEnv.authenticatedContext("new_owner_uid", { email: "pastor@ansyen.org" }).firestore();
  await assertSucceeds(
    claimingAdminDb.collection("churches").doc(legacyChurchId).update({
      adminGoogleEmail: "pastor@ansyen.org",
      adminGoogleUid: "new_owner_uid",
      adminAuthMethod: "email_password",
    })
  );

  // Signed-in member cannot mutate immutable role or memberId
  const memberDb = testEnv.authenticatedContext(MEMBER_UID).firestore();
  await assertFails(
    memberDb.collection("members").doc("mem_marie").update({
      role: "admin",
    })
  );
});

test("Dyak role can create services & announcements; Temporary Akèy can record attendance ONLY while assigned to an unfinished service", async () => {
  await seedBaseTenantData();
  const now = new Date();
  const USHER_UID = "usher_jean_04";
  const DEACON_UID = "deacon_paul_05";

  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await db.collection("users").doc(USHER_UID).set({
      userId: USHER_UID,
      email: "jean.akey@omnichurch.org",
      displayName: "Jean Akèy",
      churchId: CHURCH_A,
      churchName: "OmniChurch Pro Tabernacle",
      role: "member",
      memberId: "mem_jean_akey",
      createdAt: now,
    });
    await db.collection("users").doc(DEACON_UID).set({
      userId: DEACON_UID,
      email: "paul.dyak@omnichurch.org",
      displayName: "Paul Dyak",
      churchId: CHURCH_A,
      churchName: "OmniChurch Pro Tabernacle",
      role: "member",
      memberId: "mem_paul_dyak",
      createdAt: now,
    });
  });

  const deaconDb = testEnv.authenticatedContext(DEACON_UID).firestore();
  await assertSucceeds(
    deaconDb.collection("services").doc("srv_dyak_01").set({
      churchId: CHURCH_A,
      churchName: "OmniChurch Pro Tabernacle",
      title: "Etid Biblik Dyak",
      dateFormatted: "2026-11-15 • 18:00 - 20:00",
      startTimeEpochMillis: 1794765600000,
      lateToleranceMinutes: 15,
      isOpenForAttendance: true,
      isLive: true,
      ushers: "Jean Akèy",
      usherMemberIds: ["mem_jean_akey"],
      usherUserIds: [USHER_UID],
      authorId: DEACON_UID,
      authorName: "Paul Dyak",
      reactionsCount: 0,
      commentsCount: 0,
      createdAt: now,
    })
  );
  await assertSucceeds(
    deaconDb.collection("announcements").doc("ann_dyak_01").set({
      churchId: CHURCH_A,
      title: "Anons Dyak",
      content: "Reyinyon aswè a a 6è.",
      department: "Dyak",
      isPinned: false,
      authorId: DEACON_UID,
      authorName: "Paul Dyak",
      reactionsCount: 0,
      commentsCount: 0,
      createdAt: now,
    })
  );

  // Unassigned member CANNOT record attendance
  const unassignedMemberDb = testEnv.authenticatedContext(MEMBER_UID).firestore();
  const baseAttendance = {
    churchId: CHURCH_A,
    serviceId: "srv_dyak_01",
    serviceName: "Etid Biblik Dyak",
    memberDocId: "mem_marie",
    memberCode: "OM-0258743",
    memberName: "Marie Pierre",
    userId: MEMBER_UID,
    status: "on_time",
    checkInMethod: "qr_scan",
    barcodeToken: "OMNI_TOKEN_MARIE_998877",
    createdAt: now,
  };
  await assertFails(
    unassignedMemberDb.collection("attendances").doc("att_unassigned").set({
      ...baseAttendance,
      recordedByUid: MEMBER_UID,
    })
  );

  // Selected temporary Akèy CANNOT scan on unassigned srv_sunday, but CAN scan on srv_dyak_01
  const usherDb = testEnv.authenticatedContext(USHER_UID).firestore();
  await assertFails(
    usherDb.collection("attendances").doc("att_other_srv").set({
      ...baseAttendance,
      serviceId: "srv_sunday",
      serviceName: "Sèvis Dimanch",
      recordedByUid: USHER_UID,
    })
  );
  await assertSucceeds(
    usherDb.collection("attendances").doc("att_temp_usher_ok").set({
      ...baseAttendance,
      recordedByUid: USHER_UID,
    })
  );

  // Once service finishes (status = 'completed'), temporary Akèy permission is immediately revoked
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("services").doc("srv_dyak_01").update({
      status: "completed",
      isOpenForAttendance: false,
    });
  });
  await assertFails(
    usherDb.collection("attendances").doc("att_after_end").set({
      ...baseAttendance,
      recordedByUid: USHER_UID,
    })
  );
});

test("Cross-church Visitor (Vizitè): Host admin/assigned usher can check in active member from another church with strict schema & origin read isolation", async () => {
  await seedBaseTenantData();
  const now = new Date();
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await db.collection("services").doc("srv_church_b").set({
      churchId: CHURCH_B,
      churchName: "Other Tenant Church",
      title: "Sèvis Dimanch B",
      dateFormatted: "2026-11-15 • 09:00 - 11:00",
      startTimeEpochMillis: 1794733200000,
      lateToleranceMinutes: 15,
      isOpenForAttendance: true,
      isLive: true,
      authorId: OUTSIDER_UID,
      authorName: "Bob Outsider",
      reactionsCount: 0,
      commentsCount: 0,
      createdAt: now,
    });
  });

  const hostAdminDb = testEnv.authenticatedContext(OUTSIDER_UID).firestore();
  const visitorAttId = "srv_church_b_mem_marie";
  const validVisitorPayload = {
    id: visitorAttId,
    churchId: CHURCH_B,
    serviceId: "srv_church_b",
    serviceCategory: "dimanche_matin",
    serviceTitle: "Sèvis Dimanch B",
    serviceDate: "2026-11-15",
    memberId: "OM-0258743",
    memberDbId: "mem_marie",
    memberName: "Marie Pierre",
    memberDepartment: "Adorasyon",
    barcodeToken: "OMNI_TOKEN_MARIE_998877",
    timestamp: new Date().toISOString(),
    checkInMethod: "qr_scan",
    recordedBy: OUTSIDER_UID,
    attendanceStatus: "on_time",
    lateMinutes: 0,
    lateToleranceMinutes: 15,
    attendanceRole: "visitor",
    isVisitor: true,
    homeChurchId: CHURCH_A,
    homeChurchName: "OmniChurch Pro Tabernacle",
    hostChurchName: "Other Tenant Church",
  };

  // Valid cross-church visitor check-in succeeds
  await assertSucceeds(
    hostAdminDb.collection("attendances").doc(visitorAttId).set(validVisitorPayload)
  );

  // Origin church member can query their own cross-church visits via homeChurchId filter
  const originMemberDb = testEnv.authenticatedContext(MEMBER_UID).firestore();
  await assertSucceeds(
    originMemberDb.collection("attendances").where("homeChurchId", "==", CHURCH_A).get()
  );

  // Origin church member CANNOT query Church B's general attendances
  await assertFails(
    originMemberDb.collection("attendances").where("churchId", "==", CHURCH_B).get()
  );

  // Extra undocumented key must fail hasOnly allowlist
  await assertFails(
    hostAdminDb.collection("attendances").doc("srv_church_b_extra").set({
      ...validVisitorPayload,
      id: "srv_church_b_extra",
      extraGhostKey: "not_allowed",
    })
  );

  // Mismatched homeChurchId vs members/{memberDbId}.churchId must fail
  await assertFails(
    hostAdminDb.collection("attendances").doc("srv_church_b_forged_home").set({
      ...validVisitorPayload,
      id: "srv_church_b_forged_home",
      homeChurchId: "nonexistent_church",
    })
  );

  // Same church cannot masquerade as cross-church visitor
  const adminADb = testEnv.authenticatedContext(ADMIN_UID).firestore();
  await assertFails(
    adminADb.collection("attendances").doc("srv_sunday_self_visitor").set({
      ...validVisitorPayload,
      id: "srv_sunday_self_visitor",
      churchId: CHURCH_A,
      serviceId: "srv_sunday",
      recordedBy: ADMIN_UID,
      homeChurchId: CHURCH_A,
    })
  );
});

test("11. Remote Developer Engine config (app_config/developer_engine) allows get for all clients and restricts writes to super-admin", async () => {
  await seedBaseTenantData();

  const superAdminDb = testEnv
    .authenticatedContext("super_uid", { email: "charlesjj3186@gmail.com", email_verified: true })
    .firestore();
  const adminDb = testEnv.authenticatedContext(ADMIN_UID).firestore();
  const memberDb = testEnv.authenticatedContext(MEMBER_UID).firestore();
  const unauthDb = testEnv.unauthenticatedContext().firestore();

  // Super-admin can write app_config/developer_engine
  await assertSucceeds(
    superAdminDb.collection("app_config").doc("developer_engine").set({
      adMob: {
        enabled: true,
        environment: "test",
        mobileEnabled: true,
        mobileForceTestIds: true,
      },
      licenses: [
        {
          key: "LIC-PRO-001",
          churchId: CHURCH_A,
          status: "active",
          expiresAt: "never",
          allowAdFree: true,
        },
      ],
    })
  );

  // Regular church admin, member, and unauthenticated Matrikil+PIN clients can all read app_config/developer_engine
  await assertSucceeds(unauthDb.collection("app_config").doc("developer_engine").get());
  await assertSucceeds(memberDb.collection("app_config").doc("developer_engine").get());
  await assertSucceeds(adminDb.collection("app_config").doc("developer_engine").get());

  // Non-super-admin cannot overwrite app_config/developer_engine or read arbitrary config docs
  await assertFails(
    adminDb.collection("app_config").doc("developer_engine").set({ adMob: { enabled: false } })
  );
  await assertFails(unauthDb.collection("app_config").doc("other_secret_doc").get());
});

test("12. Member 3-field photo update and safe Google unlink (clearing googleUid to empty string) succeed", async () => {
  await seedBaseTenantData();
  const memberDb = testEnv.authenticatedContext(MEMBER_UID).firestore();
  const customPhoto = "data:image/jpeg;base64,/9j/CUSTOM_MARIE_AVATAR";

  // 1. Update all 3 photo fields
  await assertSucceeds(
    memberDb.collection("members").doc("mem_marie").set(
      {
        avatarUrl: customPhoto,
        photoUrl: customPhoto,
        googlePhoto: customPhoto,
        updatedAt: new Date().toISOString(),
      },
      { merge: true }
    )
  );

  // 2. Link Google account
  await assertSucceeds(
    memberDb.collection("members").doc("mem_marie").set(
      {
        googleEmail: "marie@gmail.com",
        googleUid: MEMBER_UID,
        avatarUrl: customPhoto,
        photoUrl: customPhoto,
        googlePhoto: customPhoto,
        updatedAt: new Date().toISOString(),
      },
      { merge: true }
    )
  );

  // 3. Unlink Google account while keeping custom church photo
  await assertSucceeds(
    memberDb.collection("members").doc("mem_marie").set(
      {
        googleEmail: "",
        googleUid: "",
        avatarUrl: customPhoto,
        photoUrl: customPhoto,
        googlePhoto: customPhoto,
        updatedAt: new Date().toISOString(),
      },
      { merge: true }
    )
  );
});


