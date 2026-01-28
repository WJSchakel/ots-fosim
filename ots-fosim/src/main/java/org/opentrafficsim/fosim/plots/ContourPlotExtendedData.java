package org.opentrafficsim.fosim.plots;

import java.awt.Color;
import java.util.List;

import org.djunits.value.vdouble.scalar.Duration;
import org.djunits.value.vdouble.scalar.Length;
import org.djutils.math.means.ArithmeticMean;
import org.opentrafficsim.core.dsol.OtsSimulatorInterface;
import org.opentrafficsim.draw.BoundsPaintScale;
import org.opentrafficsim.draw.Colors;
import org.opentrafficsim.draw.egtf.Converter;
import org.opentrafficsim.draw.egtf.Quantity;
import org.opentrafficsim.draw.graphs.AbstractContourPlot;
import org.opentrafficsim.draw.graphs.ContourDataSource;
import org.opentrafficsim.draw.graphs.ContourDataSource.ContourDataType;
import org.opentrafficsim.draw.graphs.GraphType;
import org.opentrafficsim.draw.graphs.GraphUtil;
import org.opentrafficsim.kpi.sampling.SamplingException;
import org.opentrafficsim.kpi.sampling.Trajectory;
import org.opentrafficsim.kpi.sampling.TrajectoryGroup;
import org.opentrafficsim.kpi.sampling.data.ExtendedDataNumber;
import org.opentrafficsim.kpi.sampling.data.ExtendedDataType;
import org.opentrafficsim.swing.graphs.OtsPlotScheduler;

/**
 * Contour plot to plot the value of an extended data type.
 * @author wjschakel
 */
public class ContourPlotExtendedData extends AbstractContourPlot<Double>
{

    /**
     * Constructor.
     * @param caption caption
     * @param simulator simulator
     * @param dataPool data pool
     * @param extendedDataType extended data type
     * @param min minimum value
     * @param max maximum value
     * @param legendStep step between legend values
     */
    public ContourPlotExtendedData(final String caption, final OtsSimulatorInterface simulator,
            final ContourDataSource dataPool, final ExtendedDataNumber<?> extendedDataType, final double min, final double max,
            final double legendStep)
    {
        super(caption, new OtsPlotScheduler(simulator), dataPool, new ExtendedContourDataType(extendedDataType),
                createPaintScale(min, max), legendStep, "%.2f", "value %.2f");
    }

    /**
     * Creates a paint scale from red, via yellow to green.
     * @param min minimum value
     * @param max maximum value
     * @return paint scale
     */
    private static BoundsPaintScale createPaintScale(final double min, final double max)
    {
        Color[] colorValues = Colors.GREEN_RED;
        return new BoundsPaintScale(new double[] {min, 0.5 * (min + max), max}, colorValues);
    }

    @Override
    public GraphType getGraphType()
    {
        return GraphType.OTHER;
    }

    @Override
    protected double scale(final double si)
    {
        return si;
    }

    @Override
    protected double getValue(final int item, final double cellLength, final double cellSpan)
    {
        return getDataPool().get(item, getContourDataType());
    }

    /**
     * Attention contour data type.
     */
    public static class ExtendedContourDataType implements ContourDataType<Double, ArithmeticMean<Double, Double>>
    {

        /** Extended data type. */
        private final ExtendedDataType<?, ? extends float[], ?, ?> dataType;

        /** Quantity. */
        private final Quantity<Double, ?> quantity;

        /**
         * Constructor.
         * @param dataType extended data type
         */
        ExtendedContourDataType(final ExtendedDataType<?, ? extends float[], ?, ?> dataType)
        {
            this.dataType = dataType;
            this.quantity = new Quantity<>(dataType.getId(), Converter.SI);
        }

        @Override
        public ArithmeticMean<Double, Double> identity()
        {
            return new ArithmeticMean<>();
        }

        @Override
        public ArithmeticMean<Double, Double> processSeries(final ArithmeticMean<Double, Double> intermediate,
                final List<TrajectoryGroup<?>> trajectories, final List<Length> xFrom, final List<Length> xTo,
                final Duration tFrom, final Duration tTo)
        {
            for (int i = 0; i < trajectories.size(); i++)
            {
                TrajectoryGroup<?> trajectoryGroup = trajectories.get(i);
                for (Trajectory<?> trajectory : trajectoryGroup.getTrajectories())
                {
                    if (GraphUtil.considerTrajectory(trajectory, tFrom, tTo))
                    {
                        trajectory = trajectory.subSet(xFrom.get(i), xTo.get(i), tFrom, tTo);
                        try
                        {
                            float[] out = trajectory.getExtendedData(this.dataType);
                            for (float f : out)
                            {
                                if (!Float.isNaN(f))
                                {
                                    intermediate.add((double) f, 1.0);
                                }
                            }
                        }
                        catch (SamplingException ex)
                        {
                            throw new RuntimeException(ex);
                        }
                    }
                }
            }
            return intermediate;
        }

        /** {@inheritDoc} */
        @Override
        public Double finalize(final ArithmeticMean<Double, Double> intermediate)
        {
            return intermediate.getMean();
        }

        /** {@inheritDoc} */
        @Override
        public Quantity<Double, ?> getQuantity()
        {
            return this.quantity;
        }

    };
}
