import { useEffect, useMemo, useState } from "react";

type TrendPoint = {
  year: number;
  total: number;
  yearOnYearChange: number | null;
  yearOnYearPercent: number | null;
};

type TrendSeries = {
  category: string;
  points: TrendPoint[];
};

type TrendResponse = {
  fromYear: number;
  toYear: number;
  groupBy: string;
  series: TrendSeries[];
};

type Insight = {
  title: string;
  detail: string;
};

type FilterOptions = {
  fuels: string[];
  makes: string[];
  models: string[];
};

type AdminDataStatus = {
  totalRows: number;
  distinctMakes: number;
  minYear: number;
  maxYear: number;
};

type AdminImportResponse = {
  message: string;
  importedRows: number;
  detectedColumns: number;
};

type DashboardFilter = {
  groupBy: string;
  fromYear: number;
  toYear: number;
  fuel: string;
  make: string;
  model: string;
};

const ANALYTICS_API_BASES = ["http://localhost:8080/api/v1", "http://localhost:8081/api/v1"];

function App() {
  const [tab, setTab] = useState<"analytics" | "admin">("analytics");
  const [apiBaseUrl, setApiBaseUrl] = useState(ANALYTICS_API_BASES[0]);

  const [filter, setFilter] = useState<DashboardFilter>({
    groupBy: "fuel",
    fromYear: 2018,
    toYear: 2025,
    fuel: "",
    make: "",
    model: "",
  });

  const [options, setOptions] = useState<FilterOptions>({ fuels: [], makes: [], models: [] });
  const [trends, setTrends] = useState<TrendResponse | null>(null);
  const [insights, setInsights] = useState<Insight[]>([]);
  const [status, setStatus] = useState<AdminDataStatus | null>(null);
  const [loading, setLoading] = useState(false);
  const [adminLoading, setAdminLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [adminMessage, setAdminMessage] = useState<string | null>(null);
  const [selectedCsvFile, setSelectedCsvFile] = useState<File | null>(null);

  const years = useMemo(() => {
    const values: number[] = [];
    for (let year = 1994; year <= 2025; year += 1) {
      values.push(year);
    }
    return values;
  }, []);

  const visibleSeries = useMemo(() => {
    if (!trends) {
      return [];
    }
    return [...trends.series]
      .sort((a, b) => latestTotal(b) - latestTotal(a))
      .slice(0, 8);
  }, [trends]);

  useEffect(() => {
    void loadOptions();
  }, []);

  useEffect(() => {
    void loadDashboard();
  }, [filter]);

  useEffect(() => {
    if (tab === "admin") {
      void loadStatus();
    }
  }, [tab]);

  function getApiCandidates() {
    return [apiBaseUrl, ...ANALYTICS_API_BASES.filter((candidate) => candidate !== apiBaseUrl)];
  }

  async function apiRequest(path: string, optionsArg?: RequestInit) {
    let lastError: Error | null = null;

    for (const baseUrl of getApiCandidates()) {
      try {
        const response = await fetch(`${baseUrl}${path}`, optionsArg);
        if (response.ok) {
          setApiBaseUrl(baseUrl);
          return response;
        }
        if ([404, 502, 503].includes(response.status)) {
          continue;
        }
        return response;
      } catch (error) {
        lastError = error instanceof Error ? error : new Error("Network error");
      }
    }

    throw lastError ?? new Error("Failed to connect to backend");
  }

  async function loadOptions() {
    try {
      const response = await apiRequest("/analytics/options?limit=150");
      if (!response.ok) {
        throw new Error(`Could not load filter options (${response.status})`);
      }
      const data = (await response.json()) as FilterOptions;
      setOptions(data);
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : "Failed to load options");
    }
  }

  async function loadDashboard() {
    setLoading(true);
    setErrorMessage(null);

    try {
      const params = new URLSearchParams({
        fromYear: String(filter.fromYear),
        toYear: String(filter.toYear),
        groupBy: filter.groupBy,
      });
      if (filter.fuel) params.set("fuel", filter.fuel);
      if (filter.make) params.set("make", filter.make);
      if (filter.model) params.set("model", filter.model);

      const [trendResponse, insightResponse] = await Promise.all([
        apiRequest(`/analytics/trends?${params.toString()}`),
        apiRequest(`/analytics/highlights?${params.toString()}`),
      ]);

      if (!trendResponse.ok) {
        throw new Error(`Trend request failed (${trendResponse.status})`);
      }
      if (!insightResponse.ok) {
        throw new Error(`Highlights request failed (${insightResponse.status})`);
      }

      setTrends((await trendResponse.json()) as TrendResponse);
      setInsights((await insightResponse.json()) as Insight[]);
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : "Failed to load dashboard");
      setTrends(null);
      setInsights([]);
    } finally {
      setLoading(false);
    }
  }

  async function loadStatus() {
    setAdminLoading(true);
    setErrorMessage(null);
    try {
      const response = await apiRequest("/admin/data/status");
      if (!response.ok) {
        throw new Error(`Status request failed (${response.status})`);
      }
      setStatus((await response.json()) as AdminDataStatus);
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : "Failed to load admin status");
    } finally {
      setAdminLoading(false);
    }
  }

  async function refreshAnalyticsCache() {
    setAdminLoading(true);
    setErrorMessage(null);
    setAdminMessage(null);
    try {
      const response = await apiRequest("/admin/data/refresh", { method: "POST" });
      if (!response.ok) {
        throw new Error(`Refresh request failed (${response.status})`);
      }
      await Promise.all([loadStatus(), loadDashboard()]);
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : "Failed to refresh cache");
    } finally {
      setAdminLoading(false);
    }
  }

  async function importCsvData() {
    if (!selectedCsvFile) {
      setErrorMessage("Please choose a CSV file first.");
      return;
    }

    setAdminLoading(true);
    setErrorMessage(null);
    setAdminMessage(null);
    try {
      const formData = new FormData();
      formData.append("file", selectedCsvFile);

      const response = await apiRequest("/admin/data/import-csv", {
        method: "POST",
        body: formData,
      });

      if (!response.ok) {
        let details = `Import failed (${response.status})`;
        try {
          const errorBody = (await response.json()) as { message?: string; details?: string };
          details = errorBody.details ?? errorBody.message ?? details;
        } catch {
          // Keep default fallback details.
        }
        throw new Error(details);
      }

      const payload = (await response.json()) as AdminImportResponse;
      setAdminMessage(
        `${payload.message} Imported ${payload.importedRows.toLocaleString()} rows (${payload.detectedColumns} columns).`,
      );
      setSelectedCsvFile(null);

      await Promise.all([loadStatus(), loadOptions(), loadDashboard()]);
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : "CSV import failed");
    } finally {
      setAdminLoading(false);
    }
  }

  return (
    <main className="container">
      <header className="header">
        <h1>UK Vehicle Trends Explorer</h1>
        <p>Interactive analysis for total vehicles on-road and year-on-year trend changes.</p>
      </header>

      <div className="tabs">
        <button type="button" className={tab === "analytics" ? "tab-active" : "ghost"} onClick={() => setTab("analytics")}>
          Analytics
        </button>
        <button type="button" className={tab === "admin" ? "tab-active" : "ghost"} onClick={() => setTab("admin")}>
          Admin
        </button>
      </div>

      {errorMessage ? <div className="error-banner">{errorMessage}</div> : null}

      {tab === "analytics" ? (
        <>
          <section className="panel">
            <div className="panel-header">
              <h2>Filters</h2>
            </div>
            <div className="controls">
              <label>
                Group By
                <select value={filter.groupBy} onChange={(event) => setFilter({ ...filter, groupBy: event.target.value })}>
                  <option value="fuel">Powertrain</option>
                  <option value="make">Manufacturer</option>
                  <option value="genModel">Generation Model</option>
                  <option value="model">Model</option>
                  <option value="bodyType">Body Type</option>
                  <option value="licenceStatus">Licence Status</option>
                  <option value="total">Total</option>
                </select>
              </label>
              <label>
                From Year
                <select
                  value={filter.fromYear}
                  onChange={(event) => setFilter({ ...filter, fromYear: Number(event.target.value) })}
                >
                  {years.map((year) => (
                    <option key={`from-${year}`} value={year}>
                      {year}
                    </option>
                  ))}
                </select>
              </label>
              <label>
                To Year
                <select
                  value={filter.toYear}
                  onChange={(event) => setFilter({ ...filter, toYear: Number(event.target.value) })}
                >
                  {years.map((year) => (
                    <option key={`to-${year}`} value={year}>
                      {year}
                    </option>
                  ))}
                </select>
              </label>
              <label>
                Fuel
                <select value={filter.fuel} onChange={(event) => setFilter({ ...filter, fuel: event.target.value })}>
                  <option value="">All</option>
                  {options.fuels.map((fuel) => (
                    <option key={fuel} value={fuel}>
                      {fuel}
                    </option>
                  ))}
                </select>
              </label>
              <label>
                Make
                <select value={filter.make} onChange={(event) => setFilter({ ...filter, make: event.target.value })}>
                  <option value="">All</option>
                  {options.makes.map((make) => (
                    <option key={make} value={make}>
                      {make}
                    </option>
                  ))}
                </select>
              </label>
              <label>
                Model
                <select value={filter.model} onChange={(event) => setFilter({ ...filter, model: event.target.value })}>
                  <option value="">All</option>
                  {options.models.map((model) => (
                    <option key={model} value={model}>
                      {model}
                    </option>
                  ))}
                </select>
              </label>
            </div>
          </section>

          <section className="panel">
            <h2>Total Vehicles by Year</h2>
            {loading ? <p>Loading trend data...</p> : <TrendLineChart series={visibleSeries} />}
          </section>

          <section className="panel">
            <h2>Latest Year-on-Year Change</h2>
            {loading ? <p>Calculating changes...</p> : <YoyBars series={visibleSeries} />}
          </section>

          <section className="panel">
            <h2>Highlights</h2>
            {insights.length === 0 ? (
              <p>No highlights available for this filter.</p>
            ) : (
              <ul className="highlights">
                {insights.map((insight) => (
                  <li key={insight.title}>
                    <strong>{insight.title}:</strong> {insight.detail}
                  </li>
                ))}
              </ul>
            )}
          </section>
        </>
      ) : (
        <section className="panel">
          <h2>Data Administration</h2>
          <p>Use this panel after importing new CSV data to refresh cached analytics queries.</p>

          <div className="upload-row">
            <label className="upload-label">
              Quarterly CSV file
              <input
                type="file"
                accept=".csv,text/csv"
                onChange={(event) => setSelectedCsvFile(event.target.files?.[0] ?? null)}
                disabled={adminLoading}
              />
            </label>
            <button type="button" onClick={() => void importCsvData()} disabled={adminLoading || selectedCsvFile === null}>
              {adminLoading ? "Processing..." : "Import CSV"}
            </button>
          </div>
          {selectedCsvFile ? <p className="upload-hint">Selected: {selectedCsvFile.name}</p> : null}
          {adminMessage ? <div className="success-banner">{adminMessage}</div> : null}

          <div className="admin-actions">
            <button type="button" onClick={() => void refreshAnalyticsCache()} disabled={adminLoading}>
              {adminLoading ? "Refreshing..." : "Refresh Analytics Cache"}
            </button>
            <button type="button" className="ghost" onClick={() => void loadStatus()} disabled={adminLoading}>
              Reload Data Status
            </button>
          </div>

          {status ? (
            <dl className="status-grid">
              <div>
                <dt>Total Rows</dt>
                <dd>{status.totalRows.toLocaleString()}</dd>
              </div>
              <div>
                <dt>Distinct Makes</dt>
                <dd>{status.distinctMakes.toLocaleString()}</dd>
              </div>
              <div>
                <dt>Coverage Start</dt>
                <dd>{status.minYear}</dd>
              </div>
              <div>
                <dt>Coverage End</dt>
                <dd>{status.maxYear}</dd>
              </div>
            </dl>
          ) : (
            <p>No status loaded yet.</p>
          )}
        </section>
      )}
    </main>
  );
}

function latestTotal(series: TrendSeries) {
  if (series.points.length === 0) {
    return 0;
  }
  return series.points[series.points.length - 1].total;
}

function TrendLineChart({ series }: { series: TrendSeries[] }) {
  if (series.length === 0) {
    return <p>No trend data available.</p>;
  }

  const flattened = series.flatMap((item) => item.points);
  const years = Array.from(new Set(flattened.map((point) => point.year))).sort((a, b) => a - b);
  const maxValue = Math.max(...flattened.map((point) => point.total), 1);
  const colors = ["#2563eb", "#10b981", "#7c3aed", "#f59e0b", "#ef4444", "#0ea5e9", "#14b8a6", "#ec4899"];

  const width = 900;
  const height = 320;
  const chartLeft = 45;
  const chartRight = 20;
  const chartTop = 20;
  const chartBottom = 35;
  const chartWidth = width - chartLeft - chartRight;
  const chartHeight = height - chartTop - chartBottom;

  const x = (year: number) => {
    const index = years.indexOf(year);
    const denominator = Math.max(years.length - 1, 1);
    return chartLeft + (index * chartWidth) / denominator;
  };

  const y = (value: number) => chartTop + chartHeight - (value / maxValue) * chartHeight;

  return (
    <div className="chart-container">
      <svg viewBox={`0 0 ${width} ${height}`} className="line-chart" role="img" aria-label="Vehicle trend line chart">
        <line x1={chartLeft} y1={chartTop} x2={chartLeft} y2={chartTop + chartHeight} className="axis-line" />
        <line x1={chartLeft} y1={chartTop + chartHeight} x2={chartLeft + chartWidth} y2={chartTop + chartHeight} className="axis-line" />
        {series.map((item, index) => (
          <polyline
            key={item.category}
            fill="none"
            stroke={colors[index % colors.length]}
            strokeWidth="2.5"
            points={item.points.map((point) => `${x(point.year)},${y(point.total)}`).join(" ")}
          />
        ))}
        {years.map((year) => (
          <text key={year} x={x(year)} y={height - 8} textAnchor="middle" className="axis-text">
            {year}
          </text>
        ))}
      </svg>
      <div className="legend">
        {series.map((item, index) => (
          <span key={item.category} className="legend-item">
            <span className="legend-dot" style={{ backgroundColor: colors[index % colors.length] }} />
            {item.category}
          </span>
        ))}
      </div>
    </div>
  );
}

function YoyBars({ series }: { series: TrendSeries[] }) {
  const latestChanges = series
    .map((item) => {
      const latestWithYoy = [...item.points]
        .reverse()
        .find((point) => point.yearOnYearPercent !== null);
      return { category: item.category, point: latestWithYoy };
    })
    .filter((entry): entry is { category: string; point: TrendPoint } => entry.point !== undefined);

  if (latestChanges.length === 0) {
    return <p>No year-on-year change data available.</p>;
  }

  const maxPercent = Math.max(...latestChanges.map((entry) => Math.abs(entry.point.yearOnYearPercent ?? 0)), 1);

  return (
    <div className="yoy-bars">
      {latestChanges.map((entry) => {
        const percent = entry.point.yearOnYearPercent ?? 0;
        const width = Math.min((Math.abs(percent) / maxPercent) * 100, 100);
        const isPositive = percent >= 0;
        return (
          <div className="yoy-row" key={entry.category}>
            <span className="yoy-label">{entry.category}</span>
            <div className="yoy-bar-track">
              <div className={`yoy-bar ${isPositive ? "positive" : "negative"}`} style={{ width: `${width}%` }} />
            </div>
            <span className="yoy-value">{percent.toFixed(2)}%</span>
          </div>
        );
      })}
    </div>
  );
}

export default App;
