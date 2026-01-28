package org.opentrafficsim.fosim.plots;

import java.util.Optional;
import java.util.function.BiFunction;

import org.djunits.value.vdouble.scalar.Duration;
import org.djutils.exceptions.Throw;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.data.DomainOrder;
import org.jfree.data.xy.IntervalXYDataset;
import org.opentrafficsim.core.dsol.OtsSimulatorInterface;
import org.opentrafficsim.draw.graphs.AbstractPlot;
import org.opentrafficsim.draw.graphs.GraphPath;
import org.opentrafficsim.draw.graphs.GraphPath.Section;
import org.opentrafficsim.draw.graphs.GraphType;
import org.opentrafficsim.kpi.interfaces.GtuData;
import org.opentrafficsim.kpi.interfaces.LaneData;
import org.opentrafficsim.kpi.sampling.SamplerData;
import org.opentrafficsim.kpi.sampling.Trajectory;
import org.opentrafficsim.kpi.sampling.TrajectoryGroup;
import org.opentrafficsim.swing.graphs.OtsPlotScheduler;

/**
 * Distribution plot for extended data.
 * @author wjschakel
 */
public class DistributionPlotExtendedData<G extends GtuData> extends AbstractPlot implements IntervalXYDataset
{

    /** Sampler data. */
    private final SamplerData<G> samplerData;

    /** KPI lane directions registered in the sampler. */
    private final GraphPath<? extends LaneData<?>> path;

    /** Function to obtain n'th value from trajectory. */
    private final BiFunction<Trajectory<? super G>, Integer, Double> function;

    /** Time of most recent update. */
    private double lastUpdateTime = Double.NEGATIVE_INFINITY;

    /** X-values. */
    private final double[] x;

    /** Y-values. */
    private final int[] y;

    /**
     * Constructor.
     * @param samplerData sampler data
     * @param path path
     * @param function function to obtain n'th value from trajectory
     * @param caption caption
     * @param xLabel label on x-axis
     * @param simulator simulator
     * @param xMin minimum x-value
     * @param xStep step value
     * @param xMax maximum x-value
     */
    public DistributionPlotExtendedData(final SamplerData<G> samplerData, final GraphPath<? extends LaneData<?>> path,
            final BiFunction<Trajectory<? super G>, Integer, Double> function, final String caption, final String xLabel,
            final OtsSimulatorInterface simulator, final double xMin, final double xStep, final double xMax)
    {
        super(new OtsPlotScheduler(simulator), caption, Duration.ofSI(10.0), Duration.ZERO);
        Throw.when(xMax <= xMin, IllegalArgumentException.class, "xMax must be greater than xMin");
        int n = (int) ((xMax - xMin + xStep / 1e9) / xStep) + 1;
        this.x = new double[n];
        for (int i = 0; i < n; i++)
        {
            this.x[i] = xMin + i * xStep;
        }
        this.y = new int[n];
        this.samplerData = samplerData;
        this.path = path;
        this.function = function;
        setChart(createChart(xLabel));
    }

    /**
     * Create a chart.
     * @param xLabel label on x-axis
     * @return JFreeChart; chart
     */
    private JFreeChart createChart(final String xLabel)
    {
        NumberAxis xAxis = new NumberAxis(xLabel);
        xAxis.setFixedAutoRange(this.x[this.x.length - 1] - this.x[0]);
        NumberAxis yAxis = new NumberAxis("Count [-]");
        XYBarRenderer renderer = new XYBarRenderer();
        XYPlot plot = new XYPlot(this, xAxis, yAxis, renderer);
        return new JFreeChart(getCaption(), JFreeChart.DEFAULT_TITLE_FONT, plot, false);
    }

    @Override
    public int getSeriesCount()
    {
        return 1;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Comparable getSeriesKey(final int series)
    {
        return Integer.valueOf(1);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public int indexOf(final Comparable seriesKey)
    {
        return 0;
    }

    @Override
    public DomainOrder getDomainOrder()
    {
        return DomainOrder.ASCENDING;
    }

    @Override
    public int getItemCount(final int series)
    {
        return this.x.length - 1;
    }

    @Override
    public Number getX(final int series, final int item)
    {
        return this.x[item];
    }

    @Override
    public double getXValue(final int series, final int item)
    {
        return this.x[item];
    }

    @Override
    public Number getY(final int series, final int item)
    {
        return this.y[item];
    }

    @Override
    public double getYValue(final int series, final int item)
    {
        return this.y[item];
    }

    @Override
    public Number getStartX(final int series, final int item)
    {
        return this.x[item];
    }

    @Override
    public double getStartXValue(final int series, final int item)
    {
        return this.x[item];
    }

    @Override
    public Number getEndX(final int series, final int item)
    {
        return this.x[item + 1];
    }

    @Override
    public double getEndXValue(final int series, final int item)
    {
        return this.x[item + 1];
    }

    @Override
    public Number getStartY(final int series, final int item)
    {
        return getYValue(series, item);
    }

    @Override
    public double getStartYValue(final int series, final int item)
    {
        return getYValue(series, item);
    }

    @Override
    public Number getEndY(final int series, final int item)
    {
        return getYValue(series, item);
    }

    @Override
    public double getEndYValue(final int series, final int item)
    {
        return getYValue(series, item);
    }

    @Override
    public GraphType getGraphType()
    {
        return GraphType.OTHER;
    }

    @Override
    public String getStatusLabel(final double domainValue, final double rangeValue)
    {
        return " ";
    }

    @Override
    protected void increaseTime(final Duration time)
    {
        if (this.path == null)
        {
            return; // initializing
        }
        double dx = this.x[1] - this.x[0];
        for (Section<? extends LaneData<?>> section : this.path.getSections())
        {
            for (LaneData<?> lane : section.sections())
            {
                Optional<TrajectoryGroup<G>> group = this.samplerData.getTrajectoryGroup(lane);
                if (group.isPresent())
                {
                    for (Trajectory<G> trajectory : group.get())
                    {
                        int n = trajectory.size() - 1;
                        while (n >= 0 && trajectory.getT(n) > this.lastUpdateTime)
                        {
                            double value = this.function.apply(trajectory, n);
                            if (!Double.isNaN(value))
                            {
                                int index = (int) Math.floor((value - this.x[0]) / dx);
                                if (0 <= index && index < this.y.length)
                                {
                                    this.y[index]++;
                                }
                            }
                            n--;
                        }
                    }
                }
            }
        }
        this.lastUpdateTime = time.si;
    }

}
