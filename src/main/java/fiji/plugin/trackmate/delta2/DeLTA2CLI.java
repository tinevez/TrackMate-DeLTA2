package fiji.plugin.trackmate.delta2;

import java.util.Collections;

import fiji.plugin.trackmate.util.cli.CondaExecutableCLIConfigurator;

public class DeLTA2CLI extends CondaExecutableCLIConfigurator
{

	private static final int DEFAULT_MIN_CELL_AREA = 0;

	private static final String KEY_MIN_CELL_AREA = "MIN_CELL_AREA";

	private final IntArgument minCellArea;

	private final StringArgument input;

	private final StringArgument output;

	public DeLTA2CLI()
	{
		this.minCellArea = addIntArgument()
				.name( "Min cell area" )
				.argument( "-C" )
				.defaultValue( DEFAULT_MIN_CELL_AREA )
				.help( "Minimum area of detected cells in pixels." )
				.key( KEY_MIN_CELL_AREA )
				.get();

		// Hack to properly chain -C and min_cell_area in one token
		setTranslator( minCellArea, mca -> Collections.singletonList( "min_cell_area=" + mca ) );

		addStringArgument()
				.name( "Configuration file" )
				.help( "Configuration file." )
				.argument( "-c" )
				.defaultValue( "2D" )
				.required( false ) // will force using the default value
				.visible( false ) // not editable for now.
				.key( null )
				.get();

		addFlag()
				.name( "Display progress bars" )
				.help( "Display progress bars." )
				.argument( "--progress" )
				.required( true )
				.defaultValue( true )
				.visible( false )
				.key( null )
				.get();

		this.input = addStringArgument()
				.name( "Input file" )
				.help( "Input file." )
				.argument( "--input" )
				.required( true )
				.visible( false )
				.key( null )
				.get();

		this.output = addStringArgument()
				.name( "Output folder" )
				.help( "Output folder." )
				.argument( "--output" )
				.required( true )
				.visible( false )
				.key( null )
				.get();
	}

	public StringArgument input()
	{
		return input;
	}

	public StringArgument output()
	{
		return output;
	}

	public IntArgument minCellArea()
	{
		return minCellArea;
	}

	@Override
	protected String getCommand()
	{
		return "delta run";
	}

}
