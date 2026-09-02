import React from 'react';
import { useNavigate } from 'react-router-dom';
import { useLab } from '../contexts/LabContext';
import { Building2, Laptop, CheckCircle2, AlertTriangle, ArrowRight, ShieldCheck, Plus, Sparkles, RefreshCw } from 'lucide-react';

const LabSelection = () => {
  const { labs, selectLab, loading, refreshLabs } = useLab();
  const navigate = useNavigate();

  const handleSelect = (lab) => {
    selectLab(lab);
    navigate('/dashboard');
  };

  return (
    <div className="min-h-screen bg-background text-on-surface p-4 sm:p-8 flex flex-col items-center justify-center selection:bg-primary/20 selection:text-primary">
      <div className="w-full max-w-5xl space-y-8">
        
        {/* Header */}
        <div className="text-center space-y-3">
          <div className="inline-flex items-center gap-2 px-3.5 py-1.5 rounded-full bg-primary/10 border border-primary/20 text-primary text-xs font-bold uppercase tracking-wider">
            <Building2 className="w-4 h-4" />
            <span>Multi-Lab Campus Infrastructure</span>
          </div>
          <h1 className="font-display text-3xl sm:text-4xl font-extrabold text-slate-900 tracking-tight">
            Select a Computer Laboratory
          </h1>
          <p className="text-sm font-medium text-slate-600 max-w-xl mx-auto">
            Choose a laboratory room to open its dedicated real-time telemetry dashboard, active alert monitor, and remote management suite.
          </p>
        </div>

        {/* Refresh / Actions */}
        <div className="flex justify-between items-center border-b border-slate-200 pb-4">
          <span className="text-xs font-bold text-slate-500 uppercase tracking-wider">
            Available Laboratories ({labs.length})
          </span>
          <div className="flex items-center gap-3">
            <button
              onClick={refreshLabs}
              className="px-3 py-1.5 rounded-lg border border-slate-200 bg-white text-slate-700 hover:bg-slate-50 text-xs font-bold flex items-center gap-1.5 transition-all shadow-sm cursor-pointer"
            >
              <RefreshCw className="w-3.5 h-3.5" />
              <span>Refresh</span>
            </button>
            <button
              onClick={() => navigate('/labs')}
              className="px-3 py-1.5 rounded-lg bg-primary/10 hover:bg-primary/20 text-primary text-xs font-bold flex items-center gap-1.5 transition-all cursor-pointer"
            >
              <Plus className="w-3.5 h-3.5" />
              <span>Manage Labs</span>
            </button>
          </div>
        </div>

        {/* Loading State */}
        {loading ? (
          <div className="p-12 text-center text-slate-400 font-bold text-sm">
            Loading computer laboratories from database...
          </div>
        ) : labs.length === 0 ? (
          /* Empty State */
          <div className="bg-white p-12 rounded-2xl border border-slate-200 text-center space-y-4 shadow-sm">
            <Building2 className="w-12 h-12 text-slate-300 mx-auto" />
            <h3 className="text-lg font-bold text-slate-900">No Computer Labs Configured</h3>
            <p className="text-xs text-slate-500 max-w-md mx-auto font-medium">
              There are currently no active laboratories created in the system database. Click below to initialize your first computer lab.
            </p>
            <button
              onClick={() => navigate('/labs')}
              className="px-6 py-2.5 rounded-xl bg-primary text-white text-xs font-bold hover:bg-primary-container transition-all cursor-pointer shadow-md"
            >
              Create New Lab
            </button>
          </div>
        ) : (
          /* Lab Cards Grid */
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            {labs.map((lab) => (
              <div
                key={lab.id}
                className="bg-white p-6 rounded-2xl border border-slate-200 shadow-sm hover:shadow-xl hover:border-primary/40 transition-all duration-300 flex flex-col justify-between space-y-6 group"
              >
                <div className="space-y-4">
                  <div className="flex items-center justify-between">
                    <div className="w-10 h-10 rounded-xl bg-primary/10 text-primary flex items-center justify-center font-bold text-sm group-hover:bg-primary group-hover:text-white transition-colors">
                      {lab.code || 'LAB'}
                    </div>
                    <span className="text-[10px] font-bold px-2 py-0.5 rounded-full bg-emerald-100 text-emerald-800 border border-emerald-200">
                      {lab.status || 'ACTIVE'}
                    </span>
                  </div>

                  <div>
                    <h2 className="text-lg font-extrabold text-slate-900 group-hover:text-primary transition-colors">
                      {lab.name}
                    </h2>
                    <p className="text-xs text-slate-500 font-medium mt-1 truncate">
                      {lab.location || 'College Campus'}
                    </p>
                    {lab.description && (
                      <p className="text-[11px] text-slate-400 font-medium mt-1 line-clamp-2">
                        {lab.description}
                      </p>
                    )}
                  </div>

                  {/* Real DB Metrics Breakdown */}
                  <div className="grid grid-cols-2 gap-2 pt-2 border-t border-slate-100">
                    <div className="p-2.5 bg-slate-50 rounded-xl border border-slate-100">
                      <span className="text-[10px] font-bold text-slate-400 block uppercase">Total PCs</span>
                      <span className="text-sm font-extrabold text-slate-900 block mt-0.5">{lab.totalComputers || 0}</span>
                    </div>

                    <div className="p-2.5 bg-emerald-50/80 rounded-xl border border-emerald-100">
                      <span className="text-[10px] font-bold text-emerald-700 block uppercase flex items-center gap-1">
                        <CheckCircle2 className="w-3 h-3" /> Online
                      </span>
                      <span className="text-sm font-extrabold text-emerald-800 block mt-0.5">{lab.onlineComputers || 0}</span>
                    </div>

                    <div className="p-2.5 bg-slate-100 rounded-xl border border-slate-200">
                      <span className="text-[10px] font-bold text-slate-600 block uppercase">Offline</span>
                      <span className="text-sm font-extrabold text-slate-800 block mt-0.5">{lab.offlineComputers || 0}</span>
                    </div>

                    <div className="p-2.5 bg-amber-50/80 rounded-xl border border-amber-100">
                      <span className="text-[10px] font-bold text-amber-700 block uppercase flex items-center gap-1">
                        <AlertTriangle className="w-3 h-3" /> Attention
                      </span>
                      <span className="text-sm font-extrabold text-amber-800 block mt-0.5">{lab.needsAttentionComputers || 0}</span>
                    </div>
                  </div>
                </div>

                <button
                  onClick={() => handleSelect(lab)}
                  className="w-full py-3 rounded-xl bg-primary hover:bg-primary-container text-white text-xs font-bold flex items-center justify-center gap-2 transition-all shadow-md shadow-primary/20 cursor-pointer group-hover:scale-[1.02]"
                >
                  <span>Open {lab.name}</span>
                  <ArrowRight className="w-4 h-4" />
                </button>
              </div>
            ))}
          </div>
        )}

        {/* Footer info */}
        <div className="pt-6 border-t border-slate-200 text-center flex flex-col sm:flex-row items-center justify-between text-xs text-slate-500 font-medium">
          <span>Role-Based Access Control Active • Campus Laboratory Portal</span>
          <span className="text-slate-400 font-bold mt-1 sm:mt-0">Select a specific laboratory room above to proceed</span>
        </div>

      </div>
    </div>
  );
};

export default LabSelection;
