// Enhanced service worker for offline support with cache versioning
const CACHE_NAME = 'dhoonhub-cache-v2'; // Increment version to clear old cache
const urlsToCache = [
  '/',
  '/index.html',
  '/static/favicon.ico',
  // Add more static assets if needed
];

// Clean up old caches
self.addEventListener('activate', event => {
  event.waitUntil(
    caches.keys().then(cacheNames => {
      return Promise.all(
        cacheNames.map(cacheName => {
          if (cacheName !== CACHE_NAME) {
            console.log('Deleting old cache:', cacheName);
            return caches.delete(cacheName);
          }
        })
      );
    })
  );
});

self.addEventListener('install', event => {
  event.waitUntil(
    caches.open(CACHE_NAME)
      .then(cache => cache.addAll(urlsToCache))
  );
});

self.addEventListener('fetch', event => {
  // Always fetch latest for API and dynamic requests
  if (event.request.url.includes('/api/') || 
      event.request.url.includes('/static/songs/') ||
      event.request.method !== 'GET') {
    return fetch(event.request);
  }

  event.respondWith(
    caches.match(event.request)
      .then(response => {
        if (response) {
          // Try to update cache in background
          fetch(event.request).then(newResp => {
            if (newResp && newResp.status === 200 && newResp.type === 'basic') {
              caches.open(CACHE_NAME).then(cache => {
                cache.put(event.request, newResp.clone());
              });
            }
          });
          return response;
        }
        return fetch(event.request);
      })
  );
});

self.addEventListener('message', event => {
  if (event.data && event.data.type === 'SKIP_WAITING') {
    self.skipWaiting();
  }
});
