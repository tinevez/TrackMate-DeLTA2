package fiji.plugin.trackmate.delta2;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.util.TMUtils;
import ij.IJ;
import ij.ImagePlus;
import net.imagej.ImgPlus;
import net.imglib2.Interval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Intervals;

public class DeLTA2PlaygroundEntry
{

	public static void main( final String[] args ) throws InterruptedException
	{
		run();
	}

	public static < T extends RealType< T > & NativeType< T > > void run()
	{
		final String path = "samples/20230331_washed_XY1.ome-1_stabilized_cropped.tif";
		final ImagePlus imp = IJ.openImage( path );
		@SuppressWarnings( "unchecked" )
		final ImgPlus< T > img = TMUtils.rawWraps( imp );

		final DeLTA2CLI cli = new DeLTA2CLI();
		cli.getCommandArg().set( "delta_env" );
		cli.minCellArea().set( 20 );

		System.out.println( cli );

		final Logger logger = Logger.DEFAULT_LOGGER;
		final Interval interval = Intervals.createMinSize( 1394, 800, 50, 262, 200, 5 );
		final DeLTA2Detector< T > detector = new DeLTA2Detector< T >( img, interval, cli, logger );
		
		final boolean ok = detector.checkInput() && detector.process();
		if ( !ok )
			System.err.println( detector.getErrorMessage() );
		else
			System.out.println( String.format( "Finished in %.1f s.", detector.getProcessingTime() / 1000. ) );
		System.out.println( detector.getResult() );
	}
}
