import React, { useState, useEffect } from 'react';

interface CountdownTimerProps {
  startTime: string;
  endTime: string;
  onStatusChange?: (status: 'upcoming' | 'active' | 'ended') => void;
}

interface TimeLeft {
  days: number;
  hours: number;
  minutes: number;
  seconds: number;
}

const CountdownTimer: React.FC<CountdownTimerProps> = ({ 
  startTime, 
  endTime, 
  onStatusChange 
}) => {
  const [timeLeft, setTimeLeft] = useState<TimeLeft>({ days: 0, hours: 0, minutes: 0, seconds: 0 });
  const [status, setStatus] = useState<'upcoming' | 'active' | 'ended'>('upcoming');

  useEffect(() => {
    const calculateTimeLeft = () => {
      const now = new Date().getTime();
      const start = new Date(startTime).getTime();
      const end = new Date(endTime).getTime();

      let targetTime: number;
      let newStatus: 'upcoming' | 'active' | 'ended';

      if (now < start) {
        targetTime = start;
        newStatus = 'upcoming';
      } else if (now < end) {
        targetTime = end;
        newStatus = 'active';
      } else {
        newStatus = 'ended';
        setStatus('ended');
        setTimeLeft({ days: 0, hours: 0, minutes: 0, seconds: 0 });
        if (onStatusChange) onStatusChange('ended');
        return;
      }

      if (newStatus !== status) {
        setStatus(newStatus);
        if (onStatusChange) onStatusChange(newStatus);
      }

      const difference = targetTime - now;

      if (difference > 0) {
        const days = Math.floor(difference / (1000 * 60 * 60 * 24));
        const hours = Math.floor((difference / (1000 * 60 * 60)) % 24);
        const minutes = Math.floor((difference / 1000 / 60) % 60);
        const seconds = Math.floor((difference / 1000) % 60);

        setTimeLeft({ days, hours, minutes, seconds });
      } else {
        setTimeLeft({ days: 0, hours: 0, minutes: 0, seconds: 0 });
        if (newStatus === 'active') {
          setStatus('ended');
          if (onStatusChange) onStatusChange('ended');
        }
      }
    };

    calculateTimeLeft();
    const timer = setInterval(calculateTimeLeft, 1000);

    return () => clearInterval(timer);
  }, [startTime, endTime, status, onStatusChange]);

  const formatTimeUnit = (unit: number) => {
    return unit.toString().padStart(2, '0');
  };

  const getStatusText = () => {
    switch (status) {
      case 'upcoming':
        return '距离开始还有';
      case 'active':
        return '距离结束还有';
      case 'ended':
        return '已结束';
      default:
        return '';
    }
  };

  const getStatusColor = () => {
    switch (status) {
      case 'upcoming':
        return 'text-blue-600';
      case 'active':
        return 'text-red-600';
      case 'ended':
        return 'text-gray-500';
      default:
        return 'text-gray-600';
    }
  };

  if (status === 'ended') {
    return (
      <div className="text-center">
        <div className={`text-lg font-semibold ${getStatusColor()}`}>
          {getStatusText()}
        </div>
      </div>
    );
  }

  return (
    <div className="text-center">
      <div className={`text-sm mb-2 ${getStatusColor()}`}>
        {getStatusText()}
      </div>
      <div className="flex justify-center space-x-2">
        {timeLeft.days > 0 && (
          <div className="text-center">
            <div className="bg-red-600 text-white rounded-lg px-3 py-2 text-lg font-bold">
              {formatTimeUnit(timeLeft.days)}
            </div>
            <div className="text-xs text-gray-500 mt-1">天</div>
          </div>
        )}
        <div className="text-center">
          <div className="bg-red-600 text-white rounded-lg px-3 py-2 text-lg font-bold">
            {formatTimeUnit(timeLeft.hours)}
          </div>
          <div className="text-xs text-gray-500 mt-1">时</div>
        </div>
        <div className="text-center">
          <div className="bg-red-600 text-white rounded-lg px-3 py-2 text-lg font-bold">
            {formatTimeUnit(timeLeft.minutes)}
          </div>
          <div className="text-xs text-gray-500 mt-1">分</div>
        </div>
        <div className="text-center">
          <div className="bg-red-600 text-white rounded-lg px-3 py-2 text-lg font-bold">
            {formatTimeUnit(timeLeft.seconds)}
          </div>
          <div className="text-xs text-gray-500 mt-1">秒</div>
        </div>
      </div>
    </div>
  );
};

export default CountdownTimer;