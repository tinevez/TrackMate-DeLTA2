package fiji.plugin.trackmate.delta2;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.input.TailerListenerAdapter;

import fiji.plugin.trackmate.Logger;

public class DeLTA2Utils
{

	/**
	 * A tailer listener that parse YOLO log to fetch when an image has been
	 * processed, and increase the progress counter.
	 */
	public static class DeLTA2TailerListener extends TailerListenerAdapter
	{
		private final Logger logger;

		private final int nTodos;

		private int nDone;

		private final static Pattern IMAGE_NUMBER_PATTERN = Pattern.compile( "^image \\d+/\\d+.*" );

		public DeLTA2TailerListener( final Logger logger, final int nTodos )
		{
			this.logger = logger;
			this.nTodos = nTodos;
			this.nDone = 0;
		}

		@Override
		public void handle( final String line )
		{
			final Matcher matcher = IMAGE_NUMBER_PATTERN.matcher( line );

			if ( matcher.matches() )
			{
				// Simply increment the 'done' counter.
				nDone++;
				logger.setProgress( ( double ) nDone / nTodos );
			}
			else
			{
				if ( !line.trim().isEmpty() )
					logger.log( " - " + line + '\n' );
			}
		}
	}
}
