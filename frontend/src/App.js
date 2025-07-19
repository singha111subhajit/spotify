import React, { useState } from 'react';

let renderCount = 0;
export default function App() {
  renderCount += 1;
  const [count, setCount] = useState(0);
  console.log('[DEBUG] Minimal App render', renderCount);
  return (
    <div style={{ padding: 40 }}>
      <h1>Minimal App Render Test</h1>
      <p>Render count: {renderCount}</p>
      <button onClick={() => setCount(c => c + 1)}>Increment: {count}</button>
    </div>
  );
}