import React from 'react';
import ReactDOM from 'react-dom/client';
import App from './App';

const root = ReactDOM.createRoot(document.getElementById('root'));
root.render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
);

// Clear old caches and register service worker
if ('serviceWorker' in navigator) {
  window.addEventListener('load', async () => {
    try {
      // Clear old caches first
      const cacheNames = await caches.keys();
      await Promise.all(
        cacheNames.map(cacheName => {
          if (cacheName.startsWith('dhoonhub-cache-') && cacheName !== 'dhoonhub-cache-v2') {
            console.log('Clearing old cache:', cacheName);
            return caches.delete(cacheName);
          }
        })
      );

      // Register new service worker
      const registration = await navigator.serviceWorker.register('/service-worker.js');
      console.log('ServiceWorker registration successful:', registration);
      
      // Handle service worker updates
      registration.addEventListener('updatefound', () => {
        const newWorker = registration.installing;
        newWorker.addEventListener('statechange', () => {
          if (newWorker.state === 'installed' && navigator.serviceWorker.controller) {
            // New service worker available
            console.log('New service worker available');
            // Optionally show a notification to the user
          }
        });
      });
    } catch (error) {
      console.log('ServiceWorker registration failed:', error);
    }
  });
}