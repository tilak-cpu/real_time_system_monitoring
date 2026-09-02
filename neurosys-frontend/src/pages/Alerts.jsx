import React, { useState, useEffect } from 'react';
import { metricsService } from '../services/metricsService';
import { useLab } from '../contexts/LabContext';
import { 
  Bell, 
  AlertTriangle, 
  CheckCircle2, 
  XCircle, 
  Info, 
  RefreshCw, 
  Filter, 
  ShieldAlert, 
  Clock, 
  Search,
  Check,
  Monitor,
  Activity,
  FileText
} from 'lucide-react';

const Alerts = () => {
  const { currentLab } = useLab();
  const [alerts, setAlerts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [selectedFilter, setSelectedFilter] = useState('ALL'); // 'ALL' | 'ACTIVE' | 'RESOLVED'
  const [searchQuery, setSearchQuery] = useState('');
  const [resolvingId, setResolvingId] = useState(null);

  useEffect(() => {
    fetchAlerts();
    const interval = setInterval(fetchAlerts, 4000);
    return () => clearInterval(interval);
  }, [currentLab?.id]);

  const fetchAlerts = async () => {
    try {
      const res = await metricsService.getAllAlerts(currentLab?.id);
      const list = res?.data || (Array.isArray(res) ? res : []);
      if (Array.isArray(list)) {
        setAlerts(list);
      }
    } catch (e) {
      console.error('Error fetching alerts', e);
    } finally {
      setLoading(false);
    }
  };

  const handleResolveAlert = async (alertId) => {
    setResolvingId(alertId);
    try {
      await metricsService.resolveAlert(alertId);
      fetchAlerts();
    } catch (e) {
      console.error('Error resolving alert', e);
    } finally {
      setResolvingId(null);
    }
  };

  const filteredAlerts = alerts.filter((alert) => {
    const q = searchQuery.toLowerCase();
    const matchesSearch = 
      !q ||
      (alert.message || '').toLowerCase().includes(q) ||
      (alert.title || '').toLowerCase().includes(q) ||
      (alert.hostname || alert.computerHostname || '').toLowerCase().includes(q);

    const isResolved = alert.resolved || alert.status === 'RESOLVED';
    const matchesFilter = 
      selectedFilter === 'ALL' ||
      (selectedFilter === 'ACTIVE' && !isResolved) ||
      (selectedFilter === 'RESOLVED' && isResolved);

    return matchesSearch && matchesFilter;
  });

  const activeAlertsCount = alerts.filter(a => !a.resolved && a.status !== 'RESOLVED').length;
  const resolvedAlertsCount = alerts.filter(a => a.resolved || a.status === 'RESOLVED').length;

  return (
    <div className="max-w-7xl mx-auto space-y-6">
      {/* Header Banner */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 bg-surface-container-lowest border border-slate-200 p-6 rounded-xl shadow-sm">
        <div>
          <div className="flex items-center gap-3">
            <Bell className="w-8 h-8 text-primary" />
            <h1 className="font-display text-display text-slate-900 tracking-tight font-extrabold">Computer Lab Alerts &amp; Degradation Log</h1>
          </div>
          <p className="font-body-md text-body-md text-slate-700 mt-1 font-medium">
            Persistent degradation incident detection, baseline anomaly tracking, and audit history across computer lab workstations.
          </p>
        </div>

        <button
          onClick={fetchAlerts}
          className="px-4 py-2.5 bg-white border border-slate-300 rounded-xl text-slate-800 hover:bg-slate-100 hover:text-primary transition-colors flex items-center gap-2 text-xs font-bold shadow-sm cursor-pointer"
        >
          <RefreshCw className="w-4 h-4" />
          <span>Refresh Alert Feed</span>
        </button>
      </div>

      {/* Summary Tiles */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div className="card-elevated p-4 border border-slate-200 flex items-center justify-between">
          <div>
            <span className="text-label-md font-label-md text-slate-700 font-bold">Total Logged Incidents</span>
            <div className="text-display font-display text-slate-900 font-extrabold">{alerts.length}</div>
          </div>
          <Activity className="w-8 h-8 text-primary opacity-80" />
        </div>

        <div className="card-elevated p-4 border-l-4 border-l-amber-500 border-y border-r border-slate-200 flex items-center justify-between">
          <div>
            <span className="text-label-md font-label-md text-amber-700 font-bold">Active Incidents</span>
            <div className="text-display font-display text-amber-700 font-extrabold">{activeAlertsCount}</div>
          </div>
          <AlertTriangle className="w-8 h-8 text-amber-600 opacity-80" />
        </div>

        <div className="card-elevated p-4 border-l-4 border-l-emerald-500 border-y border-r border-slate-200 flex items-center justify-between">
          <div>
            <span className="text-label-md font-label-md text-emerald-700 font-bold">Resolved Incidents</span>
            <div className="text-display font-display text-emerald-700 font-extrabold">{resolvedAlertsCount}</div>
          </div>
          <CheckCircle2 className="w-8 h-8 text-emerald-600 opacity-80" />
        </div>
      </div>

      {/* Search & Filter Tabs Toolbar */}
      <div className="card-elevated p-4 flex flex-col md:flex-row items-center justify-between gap-4 border border-slate-200">
        <div className="relative w-full md:w-80">
          <Search className="w-4 h-4 text-slate-500 absolute left-3.5 top-1/2 -translate-y-1/2" />
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="Search incident explanation, PC..."
            className="w-full pl-10 pr-4 py-2 bg-slate-50 border border-slate-300 rounded-lg text-xs font-semibold text-slate-900 focus:outline-none focus:border-primary"
          />
        </div>

        <div className="flex items-center gap-1.5 bg-slate-100 p-1 rounded-lg border border-slate-200">
          {[
            { id: 'ALL', label: `All Alerts (${alerts.length})` },
            { id: 'ACTIVE', label: `Active Incidents (${activeAlertsCount})` },
            { id: 'RESOLVED', label: `Resolved (${resolvedAlertsCount})` },
          ].map((tab) => (
            <button
              key={tab.id}
              onClick={() => setSelectedFilter(tab.id)}
              className={`px-3 py-1.5 rounded-md text-xs font-bold transition-all cursor-pointer ${
                selectedFilter === tab.id
                  ? 'bg-primary text-white shadow-sm'
                  : 'text-slate-700 hover:text-slate-900'
              }`}
            >
              {tab.label}
            </button>
          ))}
        </div>
      </div>

      {/* Alerts Incident Feed */}
      <div className="space-y-4">
        {filteredAlerts.length > 0 ? (
          filteredAlerts.map((alert) => {
            const isResolved = alert.status === 'RESOLVED' || alert.resolved;
            const isCritical = alert.severity === 'CRITICAL';
            const evidenceList = Array.isArray(alert.evidence) ? alert.evidence : [];

            return (
              <div
                key={alert.id}
                className={`card-elevated p-6 space-y-4 border transition-all ${
                  isResolved 
                    ? 'opacity-80 bg-slate-50/80 border-slate-200' 
                    : isCritical 
                    ? 'border-l-4 border-l-red-600 bg-red-50/20 border-y border-r border-slate-200' 
                    : 'border-l-4 border-l-amber-500 bg-amber-50/20 border-y border-r border-slate-200'
                }`}
              >
                <div className="flex flex-col md:flex-row items-start md:items-center justify-between gap-4 border-b border-slate-200 pb-3">
                  <div className="flex items-center gap-3">
                    <div className={`w-9 h-9 rounded-full flex items-center justify-center font-bold shrink-0 ${
                      isResolved ? 'bg-emerald-100 text-emerald-700' : isCritical ? 'bg-red-100 text-red-700' : 'bg-amber-100 text-amber-700'
                    }`}>
                      {isResolved ? <CheckCircle2 className="w-5 h-5" /> : <AlertTriangle className="w-5 h-5" />}
                    </div>

                    <div>
                      <div className="flex items-center gap-2 flex-wrap mb-1">
                        <span className={`font-label-md text-label-md px-2.5 py-0.5 rounded-full font-bold uppercase ${
                          isResolved 
                            ? 'bg-emerald-500 text-white' 
                            : isCritical 
                            ? 'bg-red-600 text-white' 
                            : 'bg-amber-500 text-white'
                        }`}>
                          {isResolved ? '🟢 RESOLVED' : isCritical ? '🔴 URGENT' : '🟠 NEEDS ATTENTION'}
                        </span>

                        <span className="text-xs font-bold text-primary bg-primary/10 px-2 py-0.5 rounded border border-primary/20">
                          🏢 {alert.labName || 'Computer Lab 1'}
                        </span>

                        <span className="font-mono-sm text-mono-sm font-bold text-slate-900 bg-white px-2 py-0.5 rounded border border-slate-300">
                          💻 {alert.computerName || alert.hostname || 'Workstation'}
                        </span>

                        {alert.hostname && alert.computerName && alert.hostname !== alert.computerName && (
                          <span className="text-[11px] font-mono text-slate-500">
                            ({alert.hostname})
                          </span>
                        )}

                        {alert.occurrenceCount > 1 && (
                          <span className="font-label-md text-label-md bg-primary-container/20 text-primary px-2 py-0.5 rounded font-bold">
                            Detected {alert.occurrenceCount}x
                          </span>
                        )}

                        <span className="font-mono-sm text-mono-sm text-slate-600 flex items-center gap-1 font-semibold">
                          <Clock className="w-3.5 h-3.5" />
                          {alert.triggeredAt || alert.firstDetectedAt ? new Date(alert.triggeredAt || alert.firstDetectedAt).toLocaleString() : 'Recently'}
                        </span>
                      </div>

                      <h3 className="text-headline-md font-bold text-slate-900">
                        {alert.title || alert.message || 'System Performance Degradation'}
                      </h3>
                    </div>
                  </div>

                  {!isResolved && (
                    <button
                      onClick={() => handleResolveAlert(alert.id)}
                      disabled={resolvingId === alert.id}
                      className="px-4 py-2 bg-emerald-600 hover:bg-emerald-700 text-white rounded-lg font-bold text-xs flex items-center gap-2 shadow-sm cursor-pointer shrink-0 transition-transform active:scale-95"
                    >
                      <Check className="w-4 h-4" />
                      <span>{resolvingId === alert.id ? 'Resolving...' : 'Acknowledge & Resolve'}</span>
                    </button>
                  )}
                </div>

                {/* Explanation Message */}
                <div className="text-body-md text-slate-800 font-medium leading-relaxed">
                  {alert.message}
                </div>

                {/* Evidence Section */}
                {evidenceList.length > 0 && (
                  <div className="p-3.5 bg-white border border-slate-200 rounded-lg space-y-1.5 text-xs text-slate-800 font-medium">
                    <strong className="text-slate-900 font-extrabold block">Collected Telemetry Evidence:</strong>
                    <ul className="space-y-1 pl-1">
                      {evidenceList.map((ev, idx) => (
                        <li key={idx} className="flex items-start gap-2">
                          <span className="text-primary font-bold">•</span>
                          <span>{ev}</span>
                        </li>
                      ))}
                    </ul>
                  </div>
                )}

                {/* Recommended Action */}
                {alert.recommendedAction && (
                  <div className="p-3 bg-primary-container/10 border border-primary/20 rounded-lg text-xs font-semibold text-slate-900 flex items-start gap-2">
                    <Info className="w-4 h-4 text-primary shrink-0 mt-0.5" />
                    <div>
                      <strong className="text-primary font-bold">Recommended Administrator Action:</strong> {alert.recommendedAction}
                    </div>
                  </div>
                )}
              </div>
            );
          })
        ) : (
          <div className="card-elevated p-12 text-center text-slate-700 border border-slate-200">
            <CheckCircle2 className="w-12 h-12 text-emerald-600 mx-auto mb-3 opacity-80" />
            <h3 className="font-headline-md text-headline-md font-bold text-slate-900">All Clear — No Degradation Incidents Found</h3>
            <p className="font-body-md text-body-md text-slate-700 mt-1 font-semibold">There are no active or matching system degradation alerts at this time.</p>
          </div>
        )}
      </div>
    </div>
  );
};

export default Alerts;
