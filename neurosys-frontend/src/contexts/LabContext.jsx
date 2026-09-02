import React, { createContext, useContext, useState, useEffect } from 'react';
import { metricsService } from '../services/metricsService';

const LabContext = createContext(null);

export const LabProvider = ({ children }) => {
  const [labs, setLabs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [currentLab, setCurrentLabState] = useState(() => {
    const saved = localStorage.getItem('currentLab');
    if (saved) {
      try {
        const parsed = JSON.parse(saved);
        if (parsed && parsed.id && parsed.id !== 'ALL') {
          return parsed;
        }
      } catch (e) {
        // Fallback
      }
    }
    return null;
  });

  const fetchLabs = async () => {
    try {
      setLoading(true);
      const res = await metricsService.fetchRealApi('/labs');
      const list = Array.isArray(res) ? res : (res?.data || []);
      setLabs(list);

      if (list.length > 0) {
        const saved = localStorage.getItem('currentLab');
        let active = null;
        if (saved) {
          try {
            const parsed = JSON.parse(saved);
            if (parsed && parsed.id !== 'ALL') {
              active = list.find(l => l.id === parsed.id) || parsed;
            }
          } catch (e) {}
        }
        if (!active && list.length > 0) {
          active = list[0];
        }
        if (active) {
          setCurrentLabState(active);
          localStorage.setItem('currentLab', JSON.stringify(active));
        }
      }
    } catch (e) {
      console.error('Error fetching labs in LabContext:', e);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchLabs();
  }, []);

  const selectLab = (labOrId) => {
    if (!labOrId) return;
    if (typeof labOrId === 'string') {
      const found = labs.find(l => l.id === labOrId);
      if (found) {
        setCurrentLabState(found);
        localStorage.setItem('currentLab', JSON.stringify(found));
      }
    } else if (typeof labOrId === 'object' && labOrId.id && labOrId.id !== 'ALL') {
      setCurrentLabState(labOrId);
      localStorage.setItem('currentLab', JSON.stringify(labOrId));
    }
  };

  return (
    <LabContext.Provider
      value={{
        labs,
        currentLab,
        selectLab,
        refreshLabs: fetchLabs,
        loading
      }}
    >
      {children}
    </LabContext.Provider>
  );
};

export const useLab = () => {
  const context = useContext(LabContext);
  if (!context) {
    throw new Error('useLab must be used within a LabProvider');
  }
  return context;
};
