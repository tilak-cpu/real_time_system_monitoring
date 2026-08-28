import React, { useState, useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import StatusBadge from '../components/StatusBadge';
import { metricsService } from '../services/metricsService';
import { 
  Laptop, 
  Search, 
  Filter, 
  Power, 
  RotateCcw, 
  Lock, 
  CheckSquare, 
  Square,
  AlertTriangle,
  ArrowUpDown,
  RefreshCw,
  X
} from 'lucide-react';

const Computers = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const statusParam = searchParams.get('status');

  const [computers, setComputers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [isRefreshing, setIsRefreshing] = useState(false);

  // Filters
  const [searchQuery, setSearchQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState(statusParam || 'ALL');
  const [healthFilter, setHealthFilter] = useState('ALL');
  const [sortBy, setSortBy] = useState('hostname');
  const [sortOrder, setSortOrder] = useState('asc');

  // Bulk Actions Modal
  const [selectedIds, setSelectedIds] = useState([]);
  const [powerModal, setPowerModal] = useState({ open: false, action: null });
  const [actionStatusMsg, setActionStatusMsg] = useState('');

  useEffect(() => {
    fetchComputers();
    const interval = setInterval(fetchComputers, 1000);
    return () => clearInterval(interval);
  }, []);

  const fetchComputers = async () => {
    try {
      const data = await metricsService.getAllComputers();
      const compList = Array.isArray(data) ? data : (data?.data || []);
      if (Array.isArray(compList)) {
        setComputers(compList);
      }
    } catch (e) {
      console.error('Failed to fetch computer lab workstations', e);
    } finally {
      setLoading(false);
      setIsRefreshing(false);
    }
  };

  const handleRefresh = () => {
    setIsRefreshing(true);
    fetchComputers();
  };

  // Filter Logic
  const filteredComputers = computers
    .filter((comp) => {
      const q = searchQuery.toLowerCase();
      const matchesSearch = 
        !q ||
        (comp.hostname || '').toLowerCase().includes(q) ||
        (comp.ipAddress || '').toLowerCase().includes(q) ||
        (comp.osName || '').toLowerCase().includes(q);

      const matchesStatus = 
        statusFilter === 'ALL' || 
        (statusFilter === 'ONLINE' && comp.status === 'ONLINE') ||
        (statusFilter === 'OFFLINE' && comp.status === 'OFFLINE') ||
        (statusFilter === 'WARNING' && comp.status === 'WARNING') ||
        (statusFilter === 'CRITICAL' && (comp.status === 'CRITICAL' || comp.status === 'OFFLINE'));

      const ram = Math.round(comp.currentRamUsage ?? comp.lastRecordedRamUsage ?? 0);
      const matchesHealth = 
        healthFilter === 'ALL' ||
        (healthFilter === 'HEALTHY' && ram < 80 && comp.status === 'ONLINE') ||
        (healthFilter === 'ATTENTION' && (ram >= 80 || comp.status !== 'ONLINE'));

      return matchesSearch && matchesStatus && matchesHealth;
    })
    .sort((a, b) => {
      let valA = a[sortBy] ?? '';
      let valB = b[sortBy] ?? '';
      if (typeof valA === 'string') valA = valA.toLowerCase();
      if (typeof valB === 'string') valB = valB.toLowerCase();
      
      if (valA < valB) return sortOrder === 'asc' ? -1 : 1;
      if (valA > valB) return sortOrder === 'asc' ? 1 : -1;
      return 0;
    });

  // Checkbox handlers
  const handleSelectAll = (e) => {
    if (e.target.checked) {
      setSelectedIds(filteredComputers.map((c) => c.id));
    } else {
      setSelectedIds([]);
    }
  };

  const handleSelectOne = (id) => {
    if (selectedIds.includes(id)) {
      setSelectedIds(selectedIds.filter((item) => item !== id));
    } else {
      setSelectedIds([...selectedIds, id]);
    }
  };

  const handleTriggerBulkAction = (action) => {
    if (selectedIds.length === 0) return;
    setPowerModal({ open: true, action });
  };

  const handleExecutePowerAction = async () => {
    setActionStatusMsg(`${powerModal.action} command sent to ${selectedIds.length} workstations...`);
    try {
      await Promise.all(selectedIds.map(id => metricsService.remoteAction(id, powerModal.action.toLowerCase()).catch(() => null)));
      setTimeout(() => {
        setPowerModal({ open: false, action: null });
        setSelectedIds([]);
        setActionStatusMsg('');
        fetchComputers();
      }, 1200);
    } catch (e) {
      console.error('Error executing power action', e);
      setPowerModal({ open: false, action: null });
    }
  };

  return (
    <div className="max-w-7xl mx-auto space-y-6">
      {/* Top Header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 bg-surface-container-lowest border border-slate-200 p-6 rounded-xl shadow-sm">
        <div>
          <h1 className="font-display text-display text-slate-900 tracking-tight font-extrabold">Computer Lab Workstations</h1>
          <p className="font-body-md text-body-md text-slate-700 mt-1 font-medium">
            Manage all active computers in your college computer lab.
          </p>
        </div>

        <button
          onClick={handleRefresh}
          className="px-4 py-2 bg-white border border-slate-300 rounded-lg text-slate-800 hover:bg-slate-100 hover:text-primary transition-colors flex items-center gap-2 text-xs font-bold shadow-sm cursor-pointer"
        >
          <RefreshCw className={`w-4 h-4 ${isRefreshing ? 'animate-spin text-primary' : ''}`} />
          <span>Refresh Fleet</span>
        </button>
      </div>

      {/* Filter Toolbar */}
      <div className="card-elevated p-4 flex flex-wrap items-center justify-between gap-4 border border-slate-200">
        <div className="flex flex-wrap items-center gap-3 w-full md:w-auto">
          {/* Search Bar */}
          <div className="relative w-full md:w-64">
            <Search className="w-4 h-4 text-slate-500 absolute left-3.5 top-1/2 -translate-y-1/2" />
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="Search computer name, IP..."
              className="w-full pl-10 pr-4 py-2 bg-slate-50 border border-slate-300 rounded-lg text-xs font-semibold text-slate-900 focus:outline-none focus:border-primary"
            />
          </div>

          {/* Status Filter */}
          <select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
            className="h-10 px-3 py-1.5 rounded-lg border border-slate-300 bg-white text-xs font-bold text-slate-900 focus:outline-none"
          >
            <option value="ALL">All Statuses</option>
            <option value="ONLINE">Online</option>
            <option value="WARNING">Warning</option>
            <option value="OFFLINE">Offline</option>
            <option value="CRITICAL">Critical</option>
          </select>

          {/* Health Filter */}
          <select
            value={healthFilter}
            onChange={(e) => setHealthFilter(e.target.value)}
            className="h-10 px-3 py-1.5 rounded-lg border border-slate-300 bg-white text-xs font-bold text-slate-900 focus:outline-none"
          >
            <option value="ALL">All Health</option>
            <option value="HEALTHY">Healthy</option>
            <option value="ATTENTION">Needs Attention</option>
          </select>
        </div>

        {/* Bulk Action Buttons */}
        {selectedIds.length > 0 && (
          <div className="flex items-center gap-2 animate-fade-in-up">
            <span className="text-xs font-bold text-primary mr-1">{selectedIds.length} Selected</span>
            <button
              onClick={() => handleTriggerBulkAction('RESTART')}
              className="px-3 py-1.5 bg-amber-500 text-white rounded-lg text-xs font-bold hover:bg-amber-600 transition-all cursor-pointer shadow-sm flex items-center gap-1"
            >
              <RotateCcw className="w-3.5 h-3.5" /> Restart
            </button>
            <button
              onClick={() => handleTriggerBulkAction('SHUTDOWN')}
              className="px-3 py-1.5 bg-red-600 text-white rounded-lg text-xs font-bold hover:bg-red-700 transition-all cursor-pointer shadow-sm flex items-center gap-1"
            >
              <Power className="w-3.5 h-3.5" /> Shutdown
            </button>
          </div>
        )}
      </div>

      {/* Computers Fleet Table */}
      <div className="card-elevated overflow-hidden border border-slate-200 p-6 space-y-4">
        <div className="flex items-center justify-between border-b border-slate-200 pb-3">
          <h3 className="font-headline-md text-headline-md font-bold text-slate-900 flex items-center gap-2">
            <Laptop className="w-5 h-5 text-primary" />
            Computer Lab Fleet ({filteredComputers.length})
          </h3>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse min-w-[750px]">
            <thead>
              <tr className="bg-slate-100 border-b border-slate-200 text-label-md font-label-md text-slate-900 font-extrabold">
                <th className="p-3 w-10">
                  <input
                    type="checkbox"
                    onChange={handleSelectAll}
                    checked={selectedIds.length > 0 && selectedIds.length === filteredComputers.length}
                    className="accent-primary cursor-pointer"
                  />
                </th>
                <th className="p-3">Status</th>
                <th className="p-3">Computer Name</th>
                <th className="p-3">IP Address</th>
                <th className="p-3">CPU %</th>
                <th className="p-3">RAM %</th>
                <th className="p-3">Storage %</th>
                <th className="p-3 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-200 font-body-md text-body-md text-slate-800 font-medium">
              {filteredComputers.length > 0 ? (
                filteredComputers.map((comp) => {
                  const cpu = Math.round(comp.currentCpuUsage ?? comp.lastRecordedCpuUsage ?? 0);
                  const ram = Math.round(comp.currentRamUsage ?? comp.lastRecordedRamUsage ?? 0);
                  const disk = Math.round(comp.currentDiskUsage ?? comp.lastRecordedDiskUsage ?? 0);
                  const isLaptop = comp.hostname === 'LAPTOP-PALBUQS2';
                  const isSelected = selectedIds.includes(comp.id);

                  return (
                    <tr key={comp.id} className={`hover:bg-slate-50 transition-colors ${isSelected ? 'bg-primary-container/10' : ''}`}>
                      <td className="p-3">
                        <input
                          type="checkbox"
                          checked={isSelected}
                          onChange={() => handleSelectOne(comp.id)}
                          className="accent-primary cursor-pointer"
                        />
                      </td>
                      <td className="p-3 whitespace-nowrap">
                        <div className="flex items-center gap-2">
                          <div className={`w-2.5 h-2.5 rounded-full ${comp.status === 'ONLINE' ? 'bg-emerald-500 status-dot-active' : comp.status === 'WARNING' ? 'bg-amber-500 status-dot-active' : 'bg-red-600'}`}></div>
                          <span className={`text-mono-sm font-mono-sm font-bold ${comp.status === 'ONLINE' ? 'text-emerald-700' : comp.status === 'WARNING' ? 'text-amber-700' : 'text-red-700'}`}>
                            {comp.status}
                          </span>
                        </div>
                      </td>
                      <td 
                        onClick={() => navigate(`/computers/${comp.id}`)}
                        className="p-3 font-bold text-primary cursor-pointer hover:underline"
                      >
                        {comp.hostname} {isLaptop ? '(Your Admin Laptop)' : ''}
                      </td>
                      <td className="p-3 font-mono-sm text-slate-700 font-semibold">{comp.ipAddress || '10.33.199.161'}</td>
                      <td className="p-3 font-mono-sm font-bold text-primary">{cpu}%</td>
                      <td className="p-3 font-mono-sm font-bold text-emerald-700">{ram}%</td>
                      <td className="p-3 font-mono-sm font-bold text-slate-700">{disk}%</td>
                      <td className="p-3 text-right">
                        <button
                          onClick={() => navigate(`/computers/${comp.id}`)}
                          className="px-3 py-1 text-xs font-bold rounded border border-slate-300 hover:border-primary hover:text-primary transition-colors bg-white cursor-pointer text-slate-800"
                        >
                          View Telemetry
                        </button>
                      </td>
                    </tr>
                  );
                })
              ) : (
                <tr>
                  <td colSpan="8" className="p-8 text-center text-slate-700 text-body-md font-semibold">
                    {loading ? 'Loading computer lab workstations...' : 'No computers found matching selected filters.'}
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Bulk Action Modal */}
      {powerModal.open && (
        <div className="fixed inset-0 bg-black/50 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white border border-slate-200 rounded-xl max-w-md w-full p-6 shadow-2xl space-y-4">
            <div className="flex items-center justify-between border-b border-slate-200 pb-3">
              <h3 className="text-headline-md font-bold text-slate-900 flex items-center gap-2">
                <Power className="w-5 h-5 text-primary" />
                Confirm {powerModal.action} Command
              </h3>
              <button 
                onClick={() => setPowerModal({ open: false, action: null })}
                className="p-1 rounded text-slate-500 hover:bg-slate-100 cursor-pointer"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <p className="text-body-md text-slate-700 font-medium">
              Are you sure you want to send <strong className="text-primary font-bold">{powerModal.action}</strong> command to <strong className="text-slate-900 font-bold">{selectedIds.length}</strong> selected workstations?
            </p>

            {actionStatusMsg && (
              <div className="p-3 bg-primary-container/10 border border-primary/20 rounded-lg text-xs font-bold text-primary">
                {actionStatusMsg}
              </div>
            )}

            <div className="flex justify-end gap-3 pt-2">
              <button 
                onClick={() => setPowerModal({ open: false, action: null })}
                className="px-4 py-2 rounded-lg border border-slate-300 text-slate-800 hover:bg-slate-100 text-xs font-bold cursor-pointer"
              >
                Cancel
              </button>
              <button 
                onClick={handleExecutePowerAction}
                className="px-4 py-2 rounded-lg bg-primary text-white hover:bg-primary-container text-xs font-bold cursor-pointer shadow-sm"
              >
                Send {powerModal.action} Command
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default Computers;
