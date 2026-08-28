import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { metricsService } from '../services/metricsService';
import { 
  Monitor, 
  Cpu, 
  Activity, 
  HardDrive, 
  Thermometer, 
  FileText, 
  BrainCircuit, 
  Terminal, 
  ArrowLeft,
  RefreshCw,
  Clock,
  LineChart as LineChartIcon
} from 'lucide-react';
import { AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';
import ProcessTable from '../components/ProcessTable';
import RemotePowerManagement from '../components/RemotePowerManagement';
import LogAnalyzer from '../components/LogAnalyzer';
import FileAnalyzerCard from '../components/FileAnalyzerCard';

const ComputerDetails = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const [computer, setComputer] = useState(null);
  const [metricHistory, setMetricHistory] = useState([]);
  const [aiDiagnosis, setAiDiagnosis] = useState(null);
  const [aiPrediction, setAiPrediction] = useState(null);
  const [activeTab, setActiveTab] = useState('metrics'); // 'metrics' | 'processes' | 'ai' | 'logs'
  const [selectedTimeRange, setSelectedTimeRange] = useState('24h'); // '1h' | '6h' | '24h' | '7d'
  const [loading, setLoading] = useState(true);
  const [isRefreshing, setIsRefreshing] = useState(false);

  useEffect(() => {
    fetchComputerDetails();
    const interval = setInterval(fetchComputerDetails, 1000);
    return () => clearInterval(interval);
  }, [id, selectedTimeRange]);

  const fetchComputerDetails = async () => {
    setIsRefreshing(true);
    try {
      const limitMap = { '1h': 20, '6h': 40, '24h': 80, '7d': 150 };
      const limit = limitMap[selectedTimeRange] || 50;

      const [compRes, histRes, diagRes, predRes] = await Promise.all([
        metricsService.getComputerById(id).catch(() => null),
        metricsService.getMetricHistory(id, limit).catch(() => null),
        metricsService.getAIDiagnosis(id).catch(() => null),
        metricsService.getCrashPrediction(id).catch(() => null)
      ]);

      if (compRes?.data || compRes) {
        setComputer(compRes.data || compRes);
      }
      if (histRes?.data || Array.isArray(histRes)) {
        const rawList = histRes.data || histRes;
        if (Array.isArray(rawList)) {
          setMetricHistory(rawList);
        }
      }
      if (diagRes?.data || diagRes) {
        setAiDiagnosis(diagRes.data || diagRes);
      }
      if (predRes?.data || predRes) {
        setAiPrediction(predRes.data || predRes);
      }
    } catch (e) {
      console.error('Error loading computer details', e);
    } finally {
      setLoading(false);
      setTimeout(() => setIsRefreshing(false), 400);
    }
  };

  if (loading && !computer) {
    return (
      <div className="max-w-7xl mx-auto p-8 text-center text-secondary font-label-md text-label-md">
        Loading computer telemetry details...
      </div>
    );
  }

  const latestMetric = metricHistory.length > 0 ? metricHistory[0] : null;
  const cpu = latestMetric?.cpuUsagePercent ?? computer?.currentCpuUsage ?? computer?.lastRecordedCpuUsage ?? 0;
  const ram = latestMetric?.memoryUsagePercent ?? computer?.currentRamUsage ?? computer?.lastRecordedRamUsage ?? 0;
  const diskFree = latestMetric?.diskFreeGb ?? (computer?.totalRamMb ? Math.round(computer.totalRamMb / 100) : 120);
  const temp = latestMetric?.cpuTemperature ?? 45;
  const isOnline = computer?.status === 'ONLINE' || computer?.status === 'WARNING';

  // Format Recharts Chart Data (Chronological Order)
  const chartData = metricHistory.map((m) => ({
    time: m.recordedAt ? new Date(m.recordedAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' }) : 'N/A',
    cpu: Math.round(m.cpuUsagePercent ?? 0),
    ram: Math.round(m.memoryUsagePercent ?? 0),
    diskFree: Math.round(m.diskFreeGb ?? 0),
    temp: Math.round(m.cpuTemperature ?? 45)
  })).reverse();

  return (
    <div className="max-w-7xl mx-auto space-y-6">
      {/* Back Button & Page Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 bg-surface-container-lowest border border-outline-variant p-6 rounded-xl animate-fade-in-up shadow-sm">
        <div className="flex items-center gap-4">
          <button
            onClick={() => navigate('/computers')}
            className="p-2 rounded-lg border border-outline-variant text-secondary hover:bg-surface-container transition-colors cursor-pointer"
          >
            <ArrowLeft className="w-5 h-5" />
          </button>
          <div>
            <div className="flex items-center gap-3">
              <h1 className="font-headline-lg text-headline-lg text-on-background font-bold">{computer?.hostname || 'PC Details'}</h1>
              <span className="px-2.5 py-1 bg-surface-container rounded-md font-mono-sm text-mono-sm text-on-surface-variant border border-outline-variant font-bold">
                {computer?.labName || 'Lab Alpha'}
              </span>
            </div>
            <div className="flex items-center gap-2 mt-1">
              <span className={`status-dot ${isOnline ? 'status-healthy' : 'status-critical'}`} />
              <span className="font-label-md text-label-md text-on-surface-variant uppercase tracking-wider font-bold">
                Status: {computer?.status || 'ONLINE'}
              </span>
              <span className="text-secondary text-xs font-mono ml-2">• IP: {computer?.ipAddress || '10.33.199.161'}</span>
            </div>
          </div>
        </div>

        {/* Quick Actions & Refresh Button */}
        <div className="flex items-center gap-2">
          <button
            onClick={fetchComputerDetails}
            title="Refresh Details"
            className="p-2 rounded-lg border border-outline-variant text-secondary hover:bg-surface-container hover:text-primary transition-colors cursor-pointer"
          >
            <RefreshCw className={`w-5 h-5 ${isRefreshing ? 'animate-spin text-primary' : ''}`} />
          </button>
        </div>
      </div>

      {/* Remote Power Management Section */}
      {computer && (
        <RemotePowerManagement 
          computer={computer} 
          onStatusUpdate={fetchComputerDetails} 
        />
      )}

      {/* Hardware Telemetry Metric Cards */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 animate-fade-in-up">
        {/* CPU */}
        <div className="card-elevated p-5 flex flex-col justify-between">
          <div className="flex items-center justify-between mb-3">
            <span className="font-label-md text-label-md text-secondary uppercase font-bold flex items-center gap-1.5">
              <Cpu className="w-4 h-4 text-primary" /> Processor Load
            </span>
            <span className="font-mono-sm text-mono-sm font-bold text-primary">{Math.round(cpu)}%</span>
          </div>
          <div>
            <div className="font-display text-display text-on-surface">{Math.round(cpu)}%</div>
            <div className="w-full h-2 bg-surface-container-high rounded-full overflow-hidden mt-2">
              <div 
                className={`h-full rounded-full ${cpu >= 85 ? 'bg-error' : 'bg-primary'}`} 
                style={{ width: `${Math.min(100, Math.max(5, cpu))}%` }} 
              />
            </div>
          </div>
        </div>

        {/* RAM */}
        <div className="card-elevated p-5 flex flex-col justify-between">
          <div className="flex items-center justify-between mb-3">
            <span className="font-label-md text-label-md text-secondary uppercase font-bold flex items-center gap-1.5">
              <Activity className="w-4 h-4 text-[#10b981]" /> Memory Usage
            </span>
            <span className="font-mono-sm text-mono-sm font-bold text-[#10b981]">{Math.round(ram)}%</span>
          </div>
          <div>
            <div className="font-display text-display text-on-surface">{Math.round(ram)}%</div>
            <div className="w-full h-2 bg-surface-container-high rounded-full overflow-hidden mt-2">
              <div 
                className={`h-full rounded-full ${ram >= 90 ? 'bg-error' : 'bg-[#10b981]'}`} 
                style={{ width: `${Math.min(100, Math.max(5, ram))}%` }} 
              />
            </div>
          </div>
        </div>

        {/* Disk Free */}
        <div className="card-elevated p-5 flex flex-col justify-between">
          <div className="flex items-center justify-between mb-3">
            <span className="font-label-md text-label-md text-secondary uppercase font-bold flex items-center gap-1.5">
              <HardDrive className="w-4 h-4 text-secondary" /> Free Storage
            </span>
            <span className="font-mono-sm text-mono-sm font-bold text-on-surface">{diskFree} GB</span>
          </div>
          <div>
            <div className="font-display text-display text-on-surface">{diskFree} <span className="text-body-md font-body-md text-secondary">GB</span></div>
            <div className="w-full h-2 bg-surface-container-high rounded-full overflow-hidden mt-2">
              <div className="h-full rounded-full bg-secondary" style={{ width: `${Math.min(100, Math.max(10, (diskFree / 256) * 100))}%` }} />
            </div>
          </div>
        </div>

        {/* CPU Temp */}
        <div className="card-elevated p-5 flex flex-col justify-between">
          <div className="flex items-center justify-between mb-3">
            <span className="font-label-md text-label-md text-secondary uppercase font-bold flex items-center gap-1.5">
              <Thermometer className="w-4 h-4 text-[#f59e0b]" /> Thermal Temp
            </span>
            <span className="font-mono-sm text-mono-sm font-bold text-[#f59e0b]">{Math.round(temp)}°C</span>
          </div>
          <div>
            <div className="font-display text-display text-on-surface">{Math.round(temp)}°C</div>
            <div className="w-full h-2 bg-surface-container-high rounded-full overflow-hidden mt-2">
              <div 
                className={`h-full rounded-full ${temp >= 80 ? 'bg-error' : 'bg-[#f59e0b]'}`} 
                style={{ width: `${Math.min(100, Math.max(10, (temp / 100) * 100))}%` }} 
              />
            </div>
          </div>
        </div>
      </div>

      {/* Real Historical Metric Trend Graphs */}
      <div className="card-elevated p-6 space-y-4">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-outline-variant pb-3">
          <div className="flex items-center gap-2">
            <LineChartIcon className="w-5 h-5 text-primary" />
            <h3 className="font-headline-md text-headline-md font-bold text-on-surface">
              Historical Telemetry Trends ({computer?.hostname})
            </h3>
          </div>

          {/* Time Range Selector */}
          <div className="flex items-center gap-1 bg-surface-container-low p-1 rounded-lg border border-outline-variant">
            {['1h', '6h', '24h', '7d'].map((range) => (
              <button
                key={range}
                onClick={() => setSelectedTimeRange(range)}
                className={`px-3 py-1 rounded text-xs font-bold transition-colors cursor-pointer ${
                  selectedTimeRange === range
                    ? 'bg-primary text-white shadow-sm'
                    : 'text-secondary hover:text-on-surface'
                }`}
              >
                {range}
              </button>
            ))}
          </div>
        </div>

        {chartData.length > 0 ? (
          <div className="h-72 w-full pt-2">
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={chartData}>
                <defs>
                  <linearGradient id="compCpuGrad" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#4f46e5" stopOpacity={0.3}/>
                    <stop offset="95%" stopColor="#4f46e5" stopOpacity={0}/>
                  </linearGradient>
                  <linearGradient id="compRamGrad" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#10b981" stopOpacity={0.3}/>
                    <stop offset="95%" stopColor="#10b981" stopOpacity={0}/>
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
                <XAxis dataKey="time" stroke="#64748b" fontSize={11} />
                <YAxis stroke="#64748b" fontSize={11} domain={[0, 100]} unit="%" />
                <Tooltip contentStyle={{ backgroundColor: '#ffffff', borderColor: '#cbd5e1', borderRadius: '10px' }} />
                <Area type="monotone" dataKey="cpu" stroke="#4f46e5" strokeWidth={2} fillOpacity={1} fill="url(#compCpuGrad)" name="CPU Usage %" />
                <Area type="monotone" dataKey="ram" stroke="#10b981" strokeWidth={2} fillOpacity={1} fill="url(#compRamGrad)" name="RAM Usage %" />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        ) : (
          <div className="h-44 flex flex-col items-center justify-center border border-dashed border-outline-variant rounded-xl text-secondary text-body-md">
            <Clock className="w-8 h-8 text-secondary mb-2 opacity-50" />
            <span>No historical data available for selected time period.</span>
          </div>
        )}
      </div>

      {/* AI Diagnostic Summary Banner */}
      {aiDiagnosis && (
        <div className="ai-diagnostic-card rounded-xl bg-surface-container-lowest border border-tertiary p-6 shadow-sm">
          <div className="flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
            <div className="flex items-start gap-4">
              <div className="w-10 h-10 rounded-full bg-tertiary-container/20 text-tertiary flex items-center justify-center font-bold flex-shrink-0 mt-1">
                <BrainCircuit className="w-6 h-6" />
              </div>
              <div>
                <div className="flex items-center gap-2">
                  <span className="font-label-md text-label-md bg-tertiary text-white px-2 py-0.5 rounded font-bold uppercase">
                    AI Diagnosis Status: {aiDiagnosis.confirmationStatus || 'CONFIRMED'}
                  </span>
                </div>
                <h3 className="font-headline-md text-headline-md font-bold text-on-surface mt-1">
                  {aiDiagnosis.problemDetected || 'No Critical Failure Detected'}
                </h3>
                <p className="font-body-md text-body-md text-secondary mt-1">
                  {aiDiagnosis.exactReason || 'Computer is operating within healthy parameters.'}
                </p>
              </div>
            </div>

            {aiDiagnosis.solution && (
              <div className="bg-surface-container-low p-4 rounded-lg border border-outline-variant max-w-sm w-full">
                <span className="font-label-md text-label-md font-bold text-primary block mb-1 uppercase">Recommended Solution</span>
                <p className="font-body-md text-body-md text-on-surface">{aiDiagnosis.solution}</p>
              </div>
            )}
          </div>
        </div>
      )}

      {/* Navigation Tabs */}
      <div className="flex border-b border-outline-variant gap-2 overflow-x-auto">
        {[
          { id: 'metrics', label: 'Real-Time Metrics', icon: Activity },
          { id: 'processes', label: 'Active Processes', icon: Terminal },
          { id: 'ai', label: 'AI Trend Prediction', icon: BrainCircuit },
          { id: 'logs', label: 'Log & File Analyzer', icon: FileText },
        ].map((tab) => {
          const Icon = tab.icon;
          const isActive = activeTab === tab.id;
          return (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id)}
              className={`flex items-center gap-2 px-5 py-3 border-b-2 font-headline-md text-body-md transition-colors cursor-pointer ${
                isActive
                  ? 'border-primary text-primary font-bold bg-surface-container/60'
                  : 'border-transparent text-secondary hover:text-on-surface hover:bg-surface-container-high'
              }`}
            >
              <Icon className="w-4 h-4" />
              <span>{tab.label}</span>
            </button>
          );
        })}
      </div>

      {/* Tab Content Canvas */}
      <div className="space-y-6">
        {activeTab === 'metrics' && (
          <div className="card-elevated p-6 space-y-6">
            <h3 className="font-headline-md text-headline-md font-bold text-on-surface">Telemetry Sample History</h3>
            <div className="overflow-x-auto">
              <table className="w-full text-left border-collapse">
                <thead>
                  <tr className="bg-surface-container-low border-b border-outline-variant font-label-md text-label-md text-secondary">
                    <th className="p-3">Timestamp</th>
                    <th className="p-3 text-right">CPU Usage</th>
                    <th className="p-3 text-right">RAM Usage</th>
                    <th className="p-3 text-right">Free Memory</th>
                    <th className="p-3 text-right">Free Storage</th>
                    <th className="p-3 text-right">CPU Temp</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-outline-variant font-body-md text-body-md">
                  {metricHistory.slice(0, 15).map((m, idx) => (
                    <tr key={idx} className="hover:bg-surface-container-low transition-colors">
                      <td className="p-3 font-mono-sm text-mono-sm text-secondary">
                        {m.recordedAt ? new Date(m.recordedAt).toLocaleTimeString() : 'N/A'}
                      </td>
                      <td className="p-3 text-right font-mono-sm text-mono-sm font-bold text-primary">{Math.round(m.cpuUsagePercent ?? 0)}%</td>
                      <td className="p-3 text-right font-mono-sm text-mono-sm font-bold text-[#10b981]">{Math.round(m.memoryUsagePercent ?? 0)}%</td>
                      <td className="p-3 text-right font-mono-sm text-mono-sm text-secondary">{Math.round(m.memoryFreeMb ?? 0)} MB</td>
                      <td className="p-3 text-right font-mono-sm text-mono-sm text-secondary">{Math.round(m.diskFreeGb ?? 0)} GB</td>
                      <td className="p-3 text-right font-mono-sm text-mono-sm text-[#f59e0b]">{Math.round(m.cpuTemperature ?? 45)}°C</td>
                    </tr>
                  ))}
                </tbody>
              </table>
          </div>
          <ProcessTable computerId={id} />
        </div>
        )}

        {activeTab === 'processes' && (
          <ProcessTable computerId={id} />
        )}

        {activeTab === 'ai' && aiPrediction && (
          <div className="card-elevated p-6 space-y-6">
            <div className="flex items-center justify-between border-b border-outline-variant pb-4">
              <div>
                <span className="font-label-md text-label-md px-2.5 py-1 rounded bg-primary-container/20 text-primary border border-primary/30 font-bold uppercase">
                  Model: {aiPrediction.modelVersion || 'NeuroSys Trend Model v1.0'}
                </span>
                <h3 className="font-headline-lg text-headline-lg font-bold text-on-surface mt-2">
                  {aiPrediction.predictedIssue || 'Optimal System Performance'}
                </h3>
              </div>
              <div className="text-right">
                <span className="font-display text-display text-primary">{aiPrediction.confidencePercent || 92}%</span>
                <span className="block font-label-md text-label-md text-secondary uppercase">Statistical Confidence</span>
              </div>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div className="p-4 bg-surface-container-low rounded-lg border border-outline-variant">
                <span className="font-label-md text-label-md text-secondary uppercase font-bold block mb-2">Estimated Timeframe</span>
                <span className="font-headline-md text-headline-md text-on-surface font-bold">{aiPrediction.estimatedTimeframe || 'No issue predicted'}</span>
              </div>
              <div className="p-4 bg-surface-container-low rounded-lg border border-outline-variant">
                <span className="font-label-md text-label-md text-secondary uppercase font-bold block mb-2">Risk Level</span>
                <span className="font-headline-md text-headline-md font-bold text-amber-600">{aiPrediction.riskLevel || 'LOW'}</span>
              </div>
            </div>

            {aiPrediction.contributingFactors && (
              <div className="space-y-2">
                <h4 className="font-label-md text-label-md text-secondary uppercase font-bold">Contributing Evidence Factors</h4>
                <div className="space-y-2">
                  {aiPrediction.contributingFactors.map((factor, idx) => (
                    <div key={idx} className="p-3 bg-surface-container-low border border-outline-variant rounded-lg font-body-md text-body-md text-on-surface flex items-center gap-2">
                      <span className="w-2 h-2 rounded-full bg-primary flex-shrink-0" />
                      <span>{factor}</span>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </div>
        )}

        {activeTab === 'logs' && (
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            <LogAnalyzer computerId={id} />
            <FileAnalyzerCard computerId={id} />
          </div>
        )}
      </div>
    </div>
  );
};

export default ComputerDetails;
