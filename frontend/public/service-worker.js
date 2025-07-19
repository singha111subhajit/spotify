// Simple service worker for offline support (cache static assets and index.html)
const CACHE_NAME = 'dhoonhub-cache-v1';
const urlsToCache = [
  '/',
  '/index.html',
  '/static/favicon.ico',
  // Add more static assets if needed
];

self.addEventListener('install', event => {
  event.waitUntil(
    caches.open(CACHE_NAME)
      .then(cache => cache.addAll(urlsToCache))
  );
});

self.addEventListener('fetch', event => {
  event.respondWith(
    caches.match(event.request)
      .then(response => response || fetch(event.request))
  );
});
