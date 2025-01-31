package fiji.plugin.trackmate.delta2;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import org.apache.commons.io.input.Tailer;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.delta2.DeLTA2Utils.DeLTA2TailerListener;
import fiji.plugin.trackmate.detection.DetectionUtils;
import fiji.plugin.trackmate.detection.SpotGlobalDetector;
import fiji.plugin.trackmate.util.cli.CommandBuilder;
import ij.IJ;
import ij.ImagePlus;
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imglib2.Interval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

public class DeLTA2Detector< T extends RealType< T > & NativeType< T > > implements SpotGlobalDetector< T >
{

	final static String BASE_ERROR_MESSAGE = "[DeLTA2] ";

	private static final String OUTPUT_FOLDER_NAME = "results";

	private static final String LOG_FILENAME = "delta2.log";

	private String errorMessage;

	private long processingTime;

	private SpotCollection output;

	private final ImgPlus< T > img;

	private final Interval interval;

	private final Logger logger;

	private final DeLTA2CLI cli;

	public DeLTA2Detector(
			final ImgPlus< T > img,
			final Interval interval,
			final DeLTA2CLI cli,
			final Logger logger )
	{
		this.img = img;
		this.interval = interval;
		this.cli = cli;
		this.logger = ( logger == null ) ? Logger.VOID_LOGGER : logger;
	}

	@Override
	public SpotCollection getResult()
	{
		return output;
	}

	@Override
	public boolean checkInput()
	{
		return true;
	}

	@Override
	public boolean process()
	{
		errorMessage = null;
		final long startTime = System.currentTimeMillis();

		/*
		 * Resave input image.
		 */

		final Path imgTmpFolder;
		Path outputTmpFolder;
		try
		{
			// Tmp image folder.
			imgTmpFolder = Files.createTempDirectory( "TrackMate-DeLTA2-imgs_" );
//			CLIUtils.recursiveDeleteOnShutdownHook( imgTmpFolder ); // DEBUG
			logger.setStatus( "Resaving source image" );
			logger.log( "Saving source image to " + imgTmpFolder + "\n" );

			final List< ImagePlus > timePoints = DetectionUtils.splitSingleTimePoints( img, interval, DetectionUtils.nameGen );
			for ( final ImagePlus tp : timePoints )
			{
				final String path = imgTmpFolder.resolve( tp.getTitle() ).toString();
				final boolean ok = IJ.saveAsTiff( tp, path );
				if ( !ok )
				{
					errorMessage = BASE_ERROR_MESSAGE + "Problem saving image frames to " + path + "\n";
					processingTime = System.currentTimeMillis() - startTime;
					return false;
				}
			}

			// Tmp output folder.
			outputTmpFolder = imgTmpFolder.resolve( OUTPUT_FOLDER_NAME );
		}
		catch ( final IOException e )
		{
			errorMessage = BASE_ERROR_MESSAGE + "Could not create temp folder to save input image:\n" + e.getMessage();
			processingTime = System.currentTimeMillis() - startTime;
			return false;
		}

		cli.input().set( imgTmpFolder.resolve( "img-t{t}.tif" ).toString() );
		cli.output().set( outputTmpFolder.toString() );

		// Check validity of the CLI.
		final String error = cli.check();
		final boolean ok = error == null;
		if ( !ok )
		{
			errorMessage = BASE_ERROR_MESSAGE + error;
			processingTime = System.currentTimeMillis() - startTime;
			return false;
		}

		final String executableName = cli.getCommand();

		// Redirect log to logger.
		final int nFrames = ( int ) img.dimension( img.dimensionIndex( Axes.TIME ) );
		final File logFile = imgTmpFolder.resolve( LOG_FILENAME ).toFile();
		final Tailer tailer = Tailer.builder()
				.setFile( logFile )
				.setTailerListener( new DeLTA2TailerListener( logger, nFrames ) )
				.setDelayDuration( Duration.ofMillis( 200 ) )
				.setTailFromEnd( true )
				.get();
		Process process;
		try
		{

			/*
			 * Run DeLTA2.
			 */

			final List< String > cmd = CommandBuilder.build( cli );
			logger.setStatus( "Running " + executableName );
			logger.log( "Running " + executableName + " with args:\n" );
			cmd.forEach( t -> {
				if ( t.contains( File.separator ) )
					logger.log( t + ' ' );
				else
					logger.log( t + ' ', Logger.GREEN_COLOR.darker() );
			} );
			logger.log( "\n" );

			final ProcessBuilder pb = new ProcessBuilder( cmd );
			pb.redirectOutput( ProcessBuilder.Redirect.appendTo( logFile ) );
			pb.redirectError( ProcessBuilder.Redirect.appendTo( logFile ) );

			// Go!
			process = pb.start();
			process.waitFor();

			/*
			 * Get results back and store them in the spot collection.
			 */

			// TODO

		}
		catch ( final IOException e )
		{
			final String msg = e.getMessage();
			if ( msg.matches( ".+error=13.+" ) )
			{
				errorMessage = BASE_ERROR_MESSAGE + "Problem running " + executableName + ":\n"
						+ "The executable does not have the file permission to run.\n";
			}
			else
			{
				errorMessage = BASE_ERROR_MESSAGE + "Problem running " + executableName + ":\n" + e.getMessage();
			}
			try
			{
				errorMessage = errorMessage + '\n' + new String( Files.readAllBytes( logFile.toPath() ) );
			}
			catch ( final IOException e1 )
			{}
			e.printStackTrace();
			processingTime = System.currentTimeMillis() - startTime;
			return false;
		}
		catch ( final Exception e )
		{
			errorMessage = BASE_ERROR_MESSAGE + "Problem running " + executableName + ":\n" + e.getMessage();
			try
			{
				errorMessage = errorMessage + '\n' + new String( Files.readAllBytes( logFile.toPath() ) );
			}
			catch ( final IOException e1 )
			{}
			e.printStackTrace();
			processingTime = System.currentTimeMillis() - startTime;
			return false;
		}
		finally
		{
			tailer.close();
			process = null;
		}

		processingTime = System.currentTimeMillis() - startTime;
		return true;
	}

	@Override
	public String getErrorMessage()
	{
		return errorMessage;
	}

	@Override
	public long getProcessingTime()
	{
		return processingTime;
	}
}
