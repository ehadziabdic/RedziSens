import React, { useEffect, useState } from 'react';
import { db } from './firebase';
import { ref, onValue, remove } from 'firebase/database';
import './App.css';

function App() {
  const [readings, setReadings] = useState([]);

  useEffect(() => {
    const readingsRef = ref(db, 'readings');
    const unsubscribe = onValue(readingsRef, (snapshot) => {
      const data = snapshot.val();
      if (data) {
        const list = Object.entries(data).map(([id, values]) => ({
          id,
          ...values,
        })).sort((a, b) => b.timestamp - a.timestamp);
        setReadings(list);
      } else {
        setReadings([]);
      }
    });
    return () => unsubscribe();
  }, []);

  const deleteReading = (id) => {
    const recordRef = ref(db, `readings/${id}`);
    remove(recordRef);
  };

  const clearAllData = () => {
    if (window.confirm("Wipe all database records?")) {
      remove(ref(db, 'readings'));
    }
  };

  return (
    <div className="App">
      <nav className="navbar">
        <h1>RedziSens Dashboard</h1>
        <div className="nav-actions">
           {readings.length > 0 && (
             <button onClick={clearAllData} className="clear-btn">Clear All</button>
           )}
           <div className="status-indicator">● Live</div>
        </div>
      </nav>

      <main className="dashboard-content">
        {readings.length === 0 ? (
          <div className="empty-state">No data found. Waiting for watch...</div>
        ) : (
          <div className="card-grid">
            {readings.map((r) => (
              <div key={r.id} className={`card ${r.label.toLowerCase().includes('stressed') ? 'stressed' : r.label.toLowerCase().includes('interrupted') ? 'interrupted' : 'relaxed'}`}>
                <button className="delete-card-btn" onClick={() => deleteReading(r.id)}>×</button>
                <div className="card-header">
                  <span className="timestamp">{new Date(r.timestamp).toLocaleTimeString()}</span>
                  <span className="label-badge">{r.label}</span>
                </div>
                <div className="card-body">
                  <div className="bpm-display">
                    <span className="bpm-value">{r.bpm}</span>
                    <span className="bpm-unit">Avg BPM</span>
                  </div>
                  <div className="chart-preview">
                    {r.raw_data && r.raw_data.map((val, i) => (
                      <div key={i} className="bar" style={{ height: `${(val - 40) * 0.8}%` }}></div>
                    ))}
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </main>
    </div>
  );
}

export default App;