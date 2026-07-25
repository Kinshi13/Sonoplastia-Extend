const CACHE_NAME = "escala-church-v1";
const OFFLINE_URL = "/offline.html";

self.addEventListener("install", (event) => {
  event.waitUntil(
    caches.open(CACHE_NAME).then((cache) => cache.addAll([OFFLINE_URL]))
  );
  self.skipWaiting();
});

self.addEventListener("activate", (event) => {
  event.waitUntil(
    caches
      .keys()
      .then((keys) =>
        Promise.all(keys.filter((key) => key !== CACHE_NAME).map((key) => caches.delete(key)))
      )
  );
  self.clients.claim();
});

// Network-first for navigations, falling back to a cached offline page when unreachable.
// Data always comes from Supabase live, so we don't cache API responses or app pages.
self.addEventListener("fetch", (event) => {
  if (event.request.mode !== "navigate") return;

  event.respondWith(
    fetch(event.request).catch(() => caches.match(OFFLINE_URL))
  );
});

// Notificações (base) - dormant until a real sending job exists (see
// migrations/015_worship_recommendation_and_push.sql's doc comment / the delivery report's
// "próximos passos"). Nothing calls pushManager's server-side send yet, so this handler simply
// never fires today - it's here so subscribing now (see WorshipNotificationOptIn) already points
// at a worker that knows what to do with a push event once one actually arrives, instead of
// having to ship a second service worker update later just to add this.
self.addEventListener("push", (event) => {
  let payload = { title: "Música e Louvor", body: "Confira a recomendação de hoje.", url: "/" };
  try {
    if (event.data) payload = { ...payload, ...event.data.json() };
  } catch {
    // Non-JSON push payload - fall back to the generic message above rather than fail silently.
  }

  event.waitUntil(
    self.registration.showNotification(payload.title, {
      body: payload.body,
      icon: "/icons/icon-192.png",
      badge: "/icons/icon-192.png",
      data: { url: payload.url },
    })
  );
});

self.addEventListener("notificationclick", (event) => {
  event.notification.close();
  const targetUrl = event.notification.data?.url || "/";
  event.waitUntil(
    self.clients.matchAll({ type: "window", includeUncontrolled: true }).then((clientList) => {
      for (const client of clientList) {
        if (client.url === targetUrl && "focus" in client) return client.focus();
      }
      return self.clients.openWindow(targetUrl);
    })
  );
});
