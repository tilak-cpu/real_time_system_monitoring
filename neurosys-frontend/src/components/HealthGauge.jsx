import React from 'react';

const HealthGauge = ({ score = 100 }) => {
  const roundedScore = Math.round(score);

  let colorClass = 'text-emerald-600 stroke-emerald-600';
  let categoryLabel = 'Healthy';

  if (roundedScore < 50) {
    colorClass = 'text-red-600 stroke-red-600';
    categoryLabel = 'Critical';
  } else if (roundedScore < 80) {
    colorClass = 'text-amber-600 stroke-amber-600';
    categoryLabel = 'Warning';
  }

  const circumference = 2 * Math.PI * 40;
  const strokeDashoffset = circumference - (roundedScore / 100) * circumference;

  return (
    <div className="flex flex-col items-center justify-center">
      <div className="relative w-28 h-28 flex items-center justify-center">
        <svg className="w-full h-full transform -rotate-90">
          <circle
            cx="56"
            cy="56"
            r="40"
            className="stroke-slate-200"
            strokeWidth="8"
            fill="transparent"
          />
          <circle
            cx="56"
            cy="56"
            r="40"
            className={`${colorClass} transition-all duration-1000 ease-out`}
            strokeWidth="8"
            strokeDasharray={circumference}
            strokeDashoffset={strokeDashoffset}
            strokeLinecap="round"
            fill="transparent"
          />
        </svg>
        <div className="absolute flex flex-col items-center justify-center">
          <span className="text-2xl font-extrabold text-slate-900">{roundedScore}</span>
          <span className="text-[10px] font-extrabold uppercase text-slate-500">/ 100</span>
        </div>
      </div>
      <span className={`mt-2 px-3 py-0.5 rounded-full text-xs font-extrabold uppercase ${
        categoryLabel === 'Healthy' ? 'bg-emerald-500/20 text-emerald-700 border border-emerald-500/30' : 
        categoryLabel === 'Warning' ? 'bg-amber-500/20 text-amber-700 border border-amber-500/30' : 
        'bg-red-500/20 text-red-700 border border-red-500/30'
      }`}>
        {categoryLabel}
      </span>
    </div>
  );
};

export default HealthGauge;
