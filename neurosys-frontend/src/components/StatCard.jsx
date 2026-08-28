import React from 'react';

const StatCard = ({ title, value, subtitle, icon: Icon, color = 'cyan', trend }) => {
  const colorMap = {
    cyan: 'bg-indigo-50 border-indigo-200 text-indigo-700',
    emerald: 'bg-emerald-50 border-emerald-200 text-emerald-700',
    amber: 'bg-amber-50 border-amber-200 text-amber-700',
    red: 'bg-red-50 border-red-200 text-red-700',
    purple: 'bg-primary-container/20 border-primary/30 text-primary',
  };

  return (
    <div className="p-5 rounded-xl border border-slate-200 bg-white shadow-sm transition-all duration-300 hover:shadow-md space-y-2">
      <div className="flex items-center justify-between">
        <span className="text-xs font-extrabold text-slate-700 uppercase tracking-wider">{title}</span>
        {Icon && (
          <div className={`p-2.5 rounded-xl font-bold ${colorMap[color]}`}>
            <Icon className="w-5 h-5" />
          </div>
        )}
      </div>
      <div className="flex items-baseline justify-between">
        <h3 className="text-2xl font-extrabold text-slate-900 tracking-tight">{value}</h3>
        {trend && (
          <span className={`text-xs font-extrabold ${trend.startsWith('+') ? 'text-emerald-700' : 'text-red-600'}`}>
            {trend}
          </span>
        )}
      </div>
      {subtitle && <p className="text-xs text-slate-600 font-semibold">{subtitle}</p>}
    </div>
  );
};

export default StatCard;
