package janusgraph.util.batchimport.unsafe.helps;


import janusgraph.util.batchimport.unsafe.io.fs.FileSystem;
import janusgraph.util.batchimport.unsafe.io.fs.FileSystemImpl;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class Validators
{

    /*public static final IndexValueValidator INDEX_VALUE_VALIDATOR = IndexValueValidator.INSTANCE;

    public static final ExplicitIndexValueValidator EXPLICIT_INDEX_VALUE_VALIDATOR = ExplicitIndexValueValidator.INSTANCE;*/

    public static final Validator<File> REGEX_FILE_EXISTS = file ->
    {
        if ( matchingFiles( file ).isEmpty() )
        {
            throw new IllegalArgumentException( "File '" + file + "' doesn't exist" );
        }
    };

    private Validators()
    {
    }

    static List<File> matchingFiles( File fileWithRegexInName )
    {
        File parent = fileWithRegexInName.getAbsoluteFile().getParentFile();
        if ( parent == null || !parent.exists() )
        {
            throw new IllegalArgumentException( "Directory of " + fileWithRegexInName + " doesn't exist" );
        }
        final Pattern pattern = Pattern.compile( fileWithRegexInName.getName() );
        List<File> files = new ArrayList<>();
        for ( File file : parent.listFiles() )
        {
            if ( pattern.matcher( file.getName() ).matches() )
            {
                files.add( file );
            }
        }
        return files;
    }

    public static final Validator<File> DIRECTORY_IS_WRITABLE = value ->
    {
        if ( value.mkdirs() )
        {   // It's OK, we created the directory right now, which means we have write access to it
            return;
        }

        File test = new File( value, "_______test___" );
        try
        {
            test.createNewFile();
        }
        catch ( IOException e )
        {
            throw new IllegalArgumentException( "Directoy '" + value + "' not writable: " + e.getMessage() );
        }
        finally
        {
            test.delete();
        }
    };

    public static final Validator<File> CONTAINS_NO_EXISTING_DATABASE = value ->
    {
        try ( FileSystem fileSystem = new FileSystemImpl() )
        {
            if ( isExistingDatabase( fileSystem, value ) )
            {
                throw new IllegalArgumentException( "Directory '" + value + "' already contains a database" );
            }
        }
        catch ( IOException e )
        {
            throw new UncheckedIOException( e );
        }
    };

    public static final Validator<File> CONTAINS_EXISTING_DATABASE = value ->
    {
        try ( FileSystem fileSystem = new FileSystemImpl() )
        {
            if ( !isExistingDatabase( fileSystem, value ) )
            {
                throw new IllegalArgumentException( "Directory '" + value + "' does not contain a database" );
            }
        }
        catch ( IOException e )
        {
            throw new UncheckedIOException( e );
        }
    };

    private static boolean isExistingDatabase(FileSystem fileSystem, File value )
    {
        return fileSystem.fileExists( new File( value, StoreFileType.STORE.augment( MetaDataStore.DEFAULT_NAME ) ) );
    }

    public static Validator<String> inList( String[] validStrings )
    {
        return value ->
        {
            if ( Arrays.stream( validStrings ).noneMatch( s -> s.equals( value ) ) )
            {
                throw new IllegalArgumentException( "'" + value + "' found but must be one of: " +
                    Arrays.toString( validStrings ) + "." );
            }
        };
    }

    public static <T> Validator<T[]> atLeast( final String key, final int length )
    {
        return value ->
        {
            if ( value.length < length )
            {
                throw new IllegalArgumentException( "Expected '" + key + "' to have at least " +
                        length + " valid item" + (length == 1 ? "" : "s") + ", but had " + value.length +
                        " " + Arrays.toString( value ) );
            }
        };
    }

    public static <T> Validator<T> emptyValidator()
    {
        return value -> {};
    }
}
