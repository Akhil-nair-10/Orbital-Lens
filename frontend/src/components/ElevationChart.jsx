import { useEffect, useRef } from "react";
import Chart from "chart.js/auto";
import timeUtils from "../utils/time.js";

/**
 * Elevation profile line chart for the currently selected pass using Chart.js.
 * Plots local time vs satellite elevation angle in degrees above observer's horizon.
 */
function ElevationChart({ selectedPass, minElevation }) {
  const canvasRef = useRef(null);
  const chartInstanceRef = useRef(null);

  useEffect(() => {
    if (!canvasRef.current || !selectedPass?.samples || selectedPass.samples.length === 0) {
      if (chartInstanceRef.current) {
        chartInstanceRef.current.destroy();
        chartInstanceRef.current = null;
      }
      return;
    }

    const ctx = canvasRef.current.getContext("2d");

    if (chartInstanceRef.current) {
      chartInstanceRef.current.destroy();
    }

    const labels = selectedPass.samples.map((s) => {
      const local = timeUtils.formatLocal(s.time);
      return local.split(" ")[1] || local;
    });

    const elevations = selectedPass.samples.map((s) => Math.max(0, s.elevationDeg));

    const datasets = [
      {
        label: "Elevation (°)",
        data: elevations,
        borderColor: "#00e5ff",
        backgroundColor: "rgba(0, 229, 255, 0.15)",
        borderWidth: 2,
        fill: true,
        tension: 0.3,
        pointRadius: 0,
        pointHoverRadius: 4,
      },
    ];

    if (minElevation !== undefined && minElevation !== null) {
      datasets.push({
        label: `Min Elevation (${minElevation}°)`,
        data: labels.map(() => minElevation),
        borderColor: "rgba(251, 191, 36, 0.7)",
        borderWidth: 1.5,
        borderDash: [6, 6],
        pointRadius: 0,
        fill: false,
      });
    }

    const chart = new Chart(ctx, {
      type: "line",
      data: {
        labels,
        datasets,
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        animation: { duration: 300 },
        interaction: {
          mode: "index",
          intersect: false,
        },
        scales: {
          x: {
            title: {
              display: true,
              text: "Time (local)",
              color: "#8fa0bc",
              font: { size: 12 },
            },
            ticks: {
              color: "#8fa0bc",
              maxTicksLimit: 8,
              font: { family: "ui-monospace, monospace", size: 11 },
            },
            grid: {
              color: "rgba(255, 255, 255, 0.05)",
            },
          },
          y: {
            min: 0,
            max: 90,
            title: {
              display: true,
              text: "Elevation (degrees)",
              color: "#8fa0bc",
              font: { size: 12 },
            },
            ticks: {
              color: "#8fa0bc",
              stepSize: 15,
              font: { family: "ui-monospace, monospace", size: 11 },
            },
            grid: {
              color: "rgba(255, 255, 255, 0.05)",
            },
          },
        },
        plugins: {
          legend: {
            display: true,
            position: "top",
            labels: {
              color: "#e6edf7",
              boxWidth: 12,
              font: { size: 11 },
            },
          },
          tooltip: {
            callbacks: {
              label: (context) => `${context.dataset.label}: ${context.parsed.y.toFixed(1)}°`,
            },
          },
        },
      },
    });

    chartInstanceRef.current = chart;

    return () => {
      chart.destroy();
      chartInstanceRef.current = null;
    };
  }, [selectedPass, minElevation]);

  return (
    <div className="panel-card chart-card">
      <h2 className="card-title">Elevation Profile</h2>
      {!selectedPass ? (
        <div className="empty-state">
          <span>Select a pass to see its elevation curve</span>
        </div>
      ) : (
        <div className="chart-wrapper">
          <canvas ref={canvasRef} />
        </div>
      )}
    </div>
  );
}

export default ElevationChart;
