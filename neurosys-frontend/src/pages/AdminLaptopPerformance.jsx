import React, { useState, useEffect } from 'react';
import StatusBadge from '../components/StatusBadge';
import HealthGauge from '../components/HealthGauge';
import ProcessTable from '../components/ProcessTable';
import FileAnalyzerCard from '../components/FileAnalyzerCard';
import StatCard from '../components/StatCard';
import { metricsService } from '../services/metricsService';
import { Laptop, Cpu, HardDrive, Wifi, ShieldAlert, Sparkles, Activity, RefreshCw } from 'lucide-react';
import { AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';

const AdminLaptopPerformance = () => {
  const [laptop, setLaptop] = useState(null);
  const [history, setHistory] = useState([]);
  const [health, setHealth] = useState(null);
  const [crashRisk, setCrashRisk] = useState(null);
  const [processes, setProcesses] = useState([]);
  const [fileReport, setFileReport] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchLaptopData();
    const interval = setInterval(fetchLaptopData, 1000); // 1-Second Live Stream
    return () => clearInterval(interval);
  }, []);

  const fetchLaptopData = async () => {
    try {
      const data = await metricsService.getAllComputers();
      const compList = Array.isArray(data) ? data : (data?.data || []);

      if (Array.isArray(compList) && compList.length > 0) {
        // Find registered admin laptop (matching LAPTOP-PALBUQS2 or first active computer)
        const laptopComp = compList.find(c => c.hostname === 'LAPTOP-PALBUQS2' || c.hostname?.toLowerCase().includes('laptop')) || compList[0];

        if (laptopComp) {
          const [fullComp, histData, healthData, crashData, procData, fileData] = await Promise.all([
            metricsService.getComputerById(laptopComp.id).catch(() => laptopComp),
            metricsService.getMetricHistory(laptopComp.id, 20).catch(() => []),
            metricsService.getHealthScore(laptopComp.id).catch(() => null),
            metricsService.getCrashPrediction(laptopComp.id).catch(() => null),
            metricsService.getProcesses(laptopComp.id).catch(() => null),
            metricsService.getFileAnalysis(laptopComp.id).catch(() => null)
          ]);

          const currentLaptop = fullComp?.data || fullComp || laptopComp;
          setLaptop(currentLaptop);

          const rawHist = histData?.data || (Array.isArray(histData) ? histData : []);
          if (Array.isArray(rawHist) && rawHist.length > 0) {
            const formatted = rawHist.map((m) => ({
              time: m.recordedAt ? new Date(m.recordedAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' }) : 'N/A',
              cpu: Math.round(m.cpuUsagePercent ?? 0),
              ram: Math.round(m.memoryUsagePercent ?? 0),
              disk: Math.round(m.diskUsagePercent ?? 35),
            })).reverse();
            setHistory(formatted);
          }

          if (healthData) setHealth(healthData.data || healthData);
          if (crashData) setCrashRisk(crashData.data || crashData);

          const procList = procData?.processes || procData?.data?.processes || [];
          if (Array.isArray(procList)) setProcesses(procList);

          if (fileData) setFileReport(fileData.data || fileData);
        }
      }
    } catch (e) {
      console.error('Failed to load laptop performance', e);
    } finally {
      setLoading(false);
    }
  };

  if (loading && !laptop) {
    return (
      <div className="p-12 text-center space-y-3">
        <RefreshCw className="w-8 h-8 text-primary animate-spin mx-auto" />
        <p className="text-sm font-semibold text-secondary">Scanning & Connecting Laptop Telemetry Stream...</p>
      </div>
    );
  }

  if (!laptop) {
    return (
      <div className="p-12 card-elevated text-center space-y-4 max-w-xl mx-auto">
        <Laptop className="w-12 h-12 text-primary mx-auto opacity-70" />
        <h3 className="text-lg font-bold text-on-surface">No Laptop Endpoint Connected</h3>
        <p className="text-xs text-secondary max-w-md mx-auto">
          Run the Monitoring Agent (<code className="text-primary font-bold">Run-NeuroSys-Agent.bat</code>) on your laptop to stream real-time hardware telemetry.
        </p>
      </div>
    );
  }

  const cpu = Math.round(laptop.currentCpuUsage ?? laptop.lastRecordedCpuUsage ?? 0);
  const ram = Math.round(laptop.currentRamUsage ?? laptop.lastRecordedRamUsage ?? 0);
  const disk = Math.round(laptop.currentDiskUsage ?? laptop.lastRecordedDiskUsage ?? 35);
  const healthScore = Math.round(health?.overallScore ?? laptop.currentHealthScore ?? laptop.healthScore ?? 100);

  return (
    <div className="max-w-7xl mx-auto space-y-6 animate-fade-in-up">
      {/* Top Banner Header */}
      <div className="card-elevated p-6 border-l-4 border-l-primary bg-gradient-to-r from-primary-container/10 via-surface to-surface flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div className="flex items-center space-x-4">
          <div className="p-3.5 rounded-2xl bg-primary text-white shadow-md">
            <Laptop className="w-8 h-8" />
          </div>
          <div>
            <div className="flex items-center space-x-3">
              <h2 className="text-2xl font-extrabold text-on-surface">{laptop.hostname}</h2>
              <span className="px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-[#10b981]/20 text-[#10b981] border border-[#10b981]/30 uppercase">
                {laptop.status || 'ONLINE'}
              </span>
              <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-primary/10 text-primary border border-primary/20 animate-pulse">
                LIVE 1s Stream
              </span>
            </div>
            <p className="text-xs text-secondary font-mono mt-1">
              IP: {laptop.ipAddress} • MAC: {laptop.macAddress} • OS: {laptop.osName}
            </p>
            <p className="text-[11px] text-primary font-medium mt-0.5">
              Processor: {laptop.cpuModel} • RAM: {laptop.totalRamMb ? (laptop.totalRamMb / 1024).toFixed(1) : 8.0} GB Total
            </p>
          </div>
        </div>

        <div className="flex items-center space-x-6">
          <HealthGauge score={healthScore} />
        </div>
      </div>

      {/* Stat Cards Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard title="CPU Utilization" value={`${cpu}%`} subtitle="Live load" icon={Cpu} color="purple" />
        <StatCard title="RAM Allocation" value={`${ram}%`} subtitle="Active memory" icon={HardDrive} color="cyan" />
        <StatCard title="Disk Usage" value={`${disk}%`} subtitle="Storage capacity" icon={HardDrive} color="emerald" />
        <StatCard title="Laptop Health" value={`${healthScore}/100`} subtitle="Overall score" icon={Activity} color="emerald" />
      </div>

      {/* Live Recharts Metric Stream Chart */}
      <div className="card-elevated p-6 space-y-4">
        <div className="flex items-center justify-between border-b border-outline-variant pb-3">
          <div>
            <h3 className="text-headline-md font-headline-md font-bold text-on-surface">Laptop Real-Time Telemetry Stream</h3>
            <p className="text-body-md font-body-md text-secondary mt-0.5">Live CPU % and RAM % utilization graph updated every 1 second</p>
          </div>
          <span className="px-3 py-1 bg-primary/10 text-primary border border-primary/20 rounded-full text-mono-sm font-mono-sm font-bold">
            1s Live Sampling
          </span>
        </div>

        {history.length > 0 ? (
          <div className="h-72 w-full pt-2">
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={history}>
                <defs>
                  <linearGradient id="laptopCpu" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#4f46e5" stopOpacity={0.4}/>
                    <stop offset="95%" stopColor="#4f46e5" stopOpacity={0}/>
                  </linearGradient>
                  <linearGradient id="laptopRam" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#10b981" stopOpacity={0.4}/>
                    <stop offset="95%" stopColor="#10b981" stopOpacity={0}/>
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
                <XAxis dataKey="time" stroke="#64748b" fontSize={11} />
                <YAxis stroke="#64748b" fontSize={11} domain={[0, 100]} unit="%" />
                <Tooltip contentStyle={{ backgroundColor: '#ffffff', borderColor: '#cbd5e1', borderRadius: '10px' }} />
                <Area type="monotone" dataKey="cpu" stroke="#4f46e5" strokeWidth={2} fillOpacity={1} fill="url(#laptopCpu)" name="CPU %" />
                <Area type="monotone" dataKey="ram" stroke="#10b981" strokeWidth={2} fillOpacity={1} fill="url(#laptopRam)" name="RAM %" />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        ) : (
          <div className="h-44 flex flex-col items-center justify-center border border-dashed border-outline-variant rounded-xl text-secondary text-body-md">
            <Activity className="w-8 h-8 text-secondary mb-2 opacity-50" />
            <span>Connecting to laptop live telemetry sample stream...</span>
          </div>
        )}
      </div>

      {/* AI Risk Prediction Card */}
      {crashRisk && (
        <div className={`p-5 rounded-xl card-elevated border-l-4 ${crashRisk.crashProbability > 0.5 ? 'border-l-error bg-error-container/10' : 'border-l-[#10b981]'}`}>
          <div className="flex items-center justify-between">
            <h3 className="font-headline-md text-headline-md font-bold text-on-surface flex items-center gap-2">
              <Sparkles className="w-5 h-5 text-primary" /> AI Crash Risk Engine ({laptop.hostname})
            </h3>
            <span className={`px-3 py-1 rounded-full text-xs font-bold ${crashRisk.crashProbability > 0.5 ? 'bg-error text-on-error' : 'bg-emerald-500/20 text-emerald-700'}`}>
              Crash Risk: {Math.round((crashRisk.crashProbability || 0.15) * 100)}%
            </span>
          </div>
          <p className="font-body-md text-body-md text-secondary mt-2">
            <strong className="text-on-surface font-semibold">Recommended Remediation:</strong> {crashRisk.recommendedAction || 'System operating under healthy load.'}
          </p>
        </div>
      )}

      {/* Top Running Processes Table & File Analyzer Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div className="space-y-3">
          <h3 className="font-headline-md text-headline-md font-bold text-on-surface">Top Laptop Active Processes</h3>
          <ProcessTable computerId={laptop.id} processes={processes} />
        </div>

        <div className="space-y-3">
          <h3 className="font-headline-md text-headline-md font-bold text-on-surface">Disk Storage Breakdown</h3>
          <FileAnalyzerCard computerId={laptop.id} report={fileReport} />
        </div>
      </div>
    </div>
  );
};

export default AdminLaptopPerformance;
