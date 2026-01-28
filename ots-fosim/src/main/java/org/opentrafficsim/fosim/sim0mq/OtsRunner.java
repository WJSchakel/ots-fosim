package org.opentrafficsim.fosim.sim0mq;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JOptionPane;

import org.djunits.unit.FrequencyUnit;
import org.djunits.unit.SpeedUnit;
import org.djunits.value.vdouble.scalar.Duration;
import org.djunits.value.vdouble.scalar.Frequency;
import org.djunits.value.vdouble.scalar.Length;
import org.djunits.value.vdouble.scalar.Speed;
import org.djutils.data.Column;
import org.djutils.data.ListTable;
import org.djutils.data.csv.CsvData;
import org.djutils.data.serialization.TextSerializationException;
import org.djutils.event.Event;
import org.djutils.event.EventListener;
import org.djutils.io.ResourceResolver;
import org.opentrafficsim.animation.gtu.colorer.AttentionGtuColorer;
import org.opentrafficsim.animation.gtu.colorer.IncentiveGtuColorer;
import org.opentrafficsim.animation.gtu.colorer.SocialPressureGtuColorer;
import org.opentrafficsim.animation.gtu.colorer.TaskSaturationGtuColorer;
import org.opentrafficsim.base.logger.Logger;
import org.opentrafficsim.core.dsol.OtsSimulatorInterface;
import org.opentrafficsim.core.gtu.Gtu;
import org.opentrafficsim.core.network.Link;
import org.opentrafficsim.core.network.NetworkException;
import org.opentrafficsim.core.object.Detector;
import org.opentrafficsim.draw.colorer.Colorer;
import org.opentrafficsim.draw.graphs.ContourDataSource;
import org.opentrafficsim.draw.graphs.TrajectoryPlot;
import org.opentrafficsim.fosim.FosDetector;
import org.opentrafficsim.fosim.parser.FosParser;
import org.opentrafficsim.fosim.parser.FosSampler;
import org.opentrafficsim.fosim.parser.ParserSetting;
import org.opentrafficsim.fosim.plots.ContourPlotExtendedData;
import org.opentrafficsim.fosim.plots.DistributionPlotExtendedData;
import org.opentrafficsim.kpi.sampling.data.ExtendedDataNumber;
import org.opentrafficsim.kpi.sampling.data.ExtendedDataType;
import org.opentrafficsim.kpi.sampling.filter.FilterDataType;
import org.opentrafficsim.road.gtu.lane.perception.mental.Fuller;
import org.opentrafficsim.road.gtu.lane.tactical.lmrs.IncentiveSocioSpeed;
import org.opentrafficsim.road.network.RoadNetwork;
import org.opentrafficsim.road.network.lane.CrossSectionLink;
import org.opentrafficsim.road.network.lane.Lane;
import org.opentrafficsim.road.network.sampling.GtuDataRoad;
import org.opentrafficsim.swing.graphs.OtsPlotScheduler;
import org.opentrafficsim.swing.graphs.SwingContourPlot;
import org.opentrafficsim.swing.graphs.SwingPlot;
import org.opentrafficsim.swing.graphs.SwingTrajectoryPlot;
import org.opentrafficsim.swing.gui.Appearance;
import org.opentrafficsim.swing.gui.OtsSwingApplication;

import nl.tudelft.simulation.dsol.experiment.Replication;
import nl.tudelft.simulation.dsol.swing.gui.TablePanel;

/**
 * Run OTS simulation from FOSIM file.
 * @author wjschakel
 */
public class OtsRunner
{

    /** Milliseconds between time updates when running without GUI. */
    private static final long TIMER_DELTA_MS = 2000L;

    /**
     * Test main method.
     * @param args arguments
     * @throws NetworkException
     */
    public static void main(final String[] args) throws NetworkException
    {
        run(ResourceResolver.resolve("OtsRunnerTestFile.fos").asPath().toFile(), true, false, null);
    }

    /**
     * Run OTS from FOSIM file.
     * @param file FOSIM (.fos) file
     * @param showGui show the GUI
     * @param detectorOutput generate detector output
     * @param seed to override with
     * @throws NetworkException
     */
    public static final void run(final File file, final boolean showGui, final boolean detectorOutput, final Integer seed)
            throws NetworkException
    {
        // Read file contents as string
        String fosString;
        try
        {
            fosString = Files.readString(file.toPath());
            if (seed != null)
            {
                // Can't do it afterwards as parsing is 1-pass, so replace text as a hack
                fosString = fosString.replaceFirst("random seed: \\d+", "random seed: " + seed);
            }
        }
        catch (IOException ex)
        {
            JOptionPane.showMessageDialog(null, "Unable to load file.", "Unable to load file", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Parse the string from file
        Map<ParserSetting, Boolean> settings = new LinkedHashMap<>();
        settings.put(ParserSetting.GUI, showGui);
        settings.put(ParserSetting.FOS_DETECTORS, true);
        List<Colorer<? super Gtu>> colorers = new ArrayList<>(OtsSwingApplication.DEFAULT_GTU_COLORERS);
        colorers.add(new SocialPressureGtuColorer());
        colorers.add(new IncentiveGtuColorer(IncentiveSocioSpeed.class));
        colorers.add(new TaskSaturationGtuColorer());
        colorers.add(new AttentionGtuColorer());
        FosParser parser = new FosParser().setSettings(settings).setColorer(colorers);
        parser.parseFromString(fosString);
        RoadNetwork network = parser.getNetwork();

        // Sampler
        Set<ExtendedDataType<?, ?, ?, ? super GtuDataRoad>> extendedDataTypes = new LinkedHashSet<>();
        TimeToCollision ttc = new TimeToCollision();
        extendedDataTypes.add(ttc);
        ExtendedDataNumber<GtuDataRoad> taskSaturation = new ExtendedDataNumber<GtuDataRoad>("ts", "Task saturation")
        {
            @Override
            public Optional<Float> getValue(GtuDataRoad gtu)
            {
                Optional<Double> val = gtu.getGtu().getParameters().getOptionalParameter(Fuller.TS);
                return Optional.of(val.isPresent() ? (float) (double) val.get() : Float.NaN);
            }
        };
        extendedDataTypes.add(taskSaturation);
        Set<FilterDataType<?, ? super GtuDataRoad>> filterDataTypes = new LinkedHashSet<>();
        FosSampler fosSampler = new FosSampler(extendedDataTypes, filterDataTypes, parser, network, Frequency.ofSI(2.0));

        // Detector output
        if (detectorOutput)
        {
            prepareDetectorOutput(file, parser, network);
        }

        // User interface
        if (showGui)
        {
            // This does override the personal setting
            parser.getApplication().setAppearance(Appearance.FOSIM);

            // Data pool and plots
            ContourDataSource dataPool =
                    new ContourDataSource(fosSampler.getSampler().getSamplerData(), fosSampler.getAllGraphPath());
            TrajectoryPlot trajectoryPlot =
                    new TrajectoryPlot("Trajectories", Duration.ofSI(10.0), new OtsPlotScheduler(network.getSimulator()),
                            fosSampler.getSampler().getSamplerData(), fosSampler.getAllGraphPath());
            ContourPlotExtendedData contourTaskSaturation = new ContourPlotExtendedData("Task saturation",
                    network.getSimulator(), dataPool, taskSaturation, 0.0, 2.0, 0.2);
            DistributionPlotExtendedData<GtuDataRoad> accelerationDistribution = new DistributionPlotExtendedData<>(
                    fosSampler.getSampler().getSamplerData(), fosSampler.getAllGraphPath(), (t, n) -> (double) t.getA(n),
                    "Strong decelerations", "Acceleration [m/s\u00B2]", network.getSimulator(), -8.0, 0.2, -2.0);
            DistributionPlotExtendedData<GtuDataRoad> ttcDistribution =
                    new DistributionPlotExtendedData<>(fosSampler.getSampler().getSamplerData(), fosSampler.getAllGraphPath(),
                            (t, n) -> (double) t.getExtendedData(ttc, n).si, "Time-to-collision", "Time-to-collision [s]",
                            network.getSimulator(), 0.0, 0.2, 10.0);

            // Swing
            TablePanel charts = new TablePanel(2, 2);
            charts.setCell(new SwingTrajectoryPlot(trajectoryPlot).getContentPane(), 0, 0);
            charts.setCell(new SwingContourPlot(contourTaskSaturation).getContentPane(), 1, 0);
            charts.setCell(new SwingPlot(accelerationDistribution).getContentPane(), 0, 1);
            charts.setCell(new SwingPlot(ttcDistribution).getContentPane(), 1, 1);
            parser.getApplication().getAnimationPanel().getTabbedPane()
                    .addTab(parser.getApplication().getAnimationPanel().getTabbedPane().getTabCount(), "plots", charts);
        }
        else
        {
            // Timer update on console only
            OtsSimulatorInterface sim = network.getSimulator();
            Timer timer = new Timer();
            timer.scheduleAtFixedRate(new TimerTask()
            {
                @Override
                public void run()
                {
                    if (sim.isStopped())
                    {
                        Logger.ots().info("Done");
                        timer.cancel();
                    }
                    else
                    {
                        Logger.ots().info("Running in background");
                    }
                }
            }, TIMER_DELTA_MS, TIMER_DELTA_MS);
            sim.start();
        }
    }

    /**
     * Prepares detector output.
     * @param file fos file
     * @param parser
     * @param network
     */
    private static void prepareDetectorOutput(final File file, final FosParser parser, final RoadNetwork network)
    {
        network.getSimulator().addListener(new EventListener()
        {
            @Override
            public void notify(final Event event)
            {
                ListTable table = new ListTable("detectors", "Detector output",
                        List.of(new Column<>("t", "start time", Duration.class, "s"),
                                new Column<>("lane", "lane", Integer.class), new Column<>("x", "location", Length.class, "m"),
                                new Column<>("q", "flow", Frequency.class, "/h"),
                                new Column<>("v", "speed", Speed.class, "km/h")));

                for (Link link : network.getLinkMap().values())
                {
                    if (link instanceof CrossSectionLink cLink)
                    {
                        for (Lane lane : cLink.getLanes())
                        {
                            for (Detector detector : lane.getDetectors())
                            {
                                if (detector instanceof FosDetector fosDetector)
                                {
                                    Duration period = parser.getFirstPeriod();
                                    for (int i = 0; i < fosDetector.getCurrentPeriod(); i++)
                                    {
                                        Duration t = parser.getFirstPeriod().times(Math.min(i, 1))
                                                .plus(parser.getNextPeriods().times(Math.max(i - 1, 0)));
                                        String id = fosDetector.getLane().getId();
                                        int underscore = id.indexOf('_');
                                        int laneNum = Integer.valueOf(id.substring(underscore + 1));
                                        Length x = Length.ofSI(fosDetector.getLocation().x);
                                        int count = fosDetector.getCount(i);
                                        Frequency q = new Frequency(3600.0 * count / period.si, FrequencyUnit.PER_HOUR);
                                        Speed v = new Speed(3.6 * count / fosDetector.getSumReciprocalSpeed(i),
                                                SpeedUnit.KM_PER_HOUR);
                                        table.addRowByColumnIds(Map.of("t", t, "lane", laneNum, "x", x, "q", q, "v", v));
                                        period = parser.getNextPeriods();
                                    }
                                }
                            }
                        }
                    }
                }

                if (!table.isEmpty())
                {
                    int seed = parser.getSeed();
                    Path csv = Paths.get(file.getParent(), file.getName().toLowerCase().replace(".fos", "_" + seed + ".csv"));
                    Path meta =
                            Paths.get(file.getParent(), file.getName().toLowerCase().replace(".fos", "_" + seed + "_meta.csv"));
                    try
                    {
                        CsvData.writeData(csv.toString(), meta.toString(), table);
                    }
                    catch (IOException | TextSerializationException e)
                    {
                        Logger.ots().error("Unable to write output table.");
                    }
                }
            }
        }, Replication.END_REPLICATION_EVENT);
    }

}
